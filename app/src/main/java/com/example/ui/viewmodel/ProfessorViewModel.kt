package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.database.SubjectProgressEntity
import com.example.data.database.LessonSessionEntity
import com.example.data.database.QuizResultEntity
import com.example.data.database.StudyPlanEntity
import com.example.data.database.StudyGroupEntity
import com.example.data.repository.ProfessorRepository
import com.example.data.api.GeminiApiClient
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfessorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProfessorRepository

    // Sources of data from DB
    val userState: StateFlow<UserEntity?>
    val allProgressState: StateFlow<List<SubjectProgressEntity>>
    val allSessionsState: StateFlow<List<LessonSessionEntity>>
    val allQuizResultsState: StateFlow<List<QuizResultEntity>>
    val latestStudyPlanState: StateFlow<StudyPlanEntity?>
    val allStudyGroupsState: StateFlow<List<StudyGroupEntity>>

    private val _selectedStudyGroup = MutableStateFlow<StudyGroupEntity?>(null)
    val selectedStudyGroup: StateFlow<StudyGroupEntity?> = _selectedStudyGroup

    private val _isModerating = MutableStateFlow(false)
    val isModerating: StateFlow<Boolean> = _isModerating

    // UI Interactive States
    private val _selectedSubjectId = MutableStateFlow("ai_ml")
    val selectedSubjectId: StateFlow<String> = _selectedSubjectId

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    // Active Chat Session
    private val _activeSession = MutableStateFlow<LessonSessionEntity?>(null)
    val activeSession: StateFlow<LessonSessionEntity?> = _activeSession

    private val _activeChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val activeChatHistory: StateFlow<List<ChatMessage>> = _activeChatHistory

    // Active Quiz State
    private val _activeQuiz = MutableStateFlow<QuizData?>(null)
    val activeQuiz: StateFlow<QuizData?> = _activeQuiz

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions

    private val _currentQuizIndex = MutableStateFlow(0)
    val currentQuizIndex: StateFlow<Int> = _currentQuizIndex

    private val _selectedOptions = MutableStateFlow<Map<Int, Int>>(emptyMap()) // index to optionIndex
    val selectedOptions: StateFlow<Map<Int, Int>> = _selectedOptions

    private val _quizSubmitted = MutableStateFlow(false)
    val quizSubmitted: StateFlow<Boolean> = _quizSubmitted

    private val _quizScore = MutableStateFlow(0)
    val quizScore: StateFlow<Int> = _quizScore

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz

    // Active Notes Analysis State
    private val _notesAnalysisState = MutableStateFlow<NotesAnalysis?>(null)
    val notesAnalysisState: StateFlow<NotesAnalysis?> = _notesAnalysisState

    private val _isAnalyzingNotes = MutableStateFlow(false)
    val isAnalyzingNotes: StateFlow<Boolean> = _isAnalyzingNotes

    // Study Plan Generator State
    private val _isGeneratingStudyPlan = MutableStateFlow(false)
    val isGeneratingStudyPlan: StateFlow<Boolean> = _isGeneratingStudyPlan

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProfessorRepository(database)

        userState = repository.userFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allProgressState = repository.allProgressFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allSessionsState = repository.allSessionsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allQuizResultsState = repository.allQuizResultsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        latestStudyPlanState = repository.latestStudyPlanFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allStudyGroupsState = repository.allStudyGroupsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate baseline user and update streak
        viewModelScope.launch {
            repository.getOrCreateUser()
            repository.checkAndUpdateStreak()
            repository.prepopulateDefaultGroupsIfNeeded()
        }
    }

    // --- Action Handlers ---

    fun selectSubject(subjectId: String) {
        _selectedSubjectId.value = subjectId
    }

    fun updateUserProfile(name: String, email: String, level: String) {
        viewModelScope.launch {
            repository.updateUserProfile(name, email, level)
        }
    }

    // --- Lesson Session Operations ---

    fun startNewLesson(subjectId: String, topicName: String) {
        viewModelScope.launch {
            _isGenerating.value = true
            val level = userState.value?.level ?: "Beginner"
            val sessionId = repository.createNewSession(subjectId, topicName, level)
            val session = repository.getSessionById(sessionId)
            
            _activeSession.value = session
            _activeChatHistory.value = emptyList()

            // Fetch initial professor lesson content automatically relative to topic
            val systemPrompt = getSystemPrompt(subjectId, level)
            val initialPrompt = "Professor, please introduce '$topicName' to me based on my level ($level). Give me a structured lesson."
            
            val response = GeminiApiClient.generateLessonResponse(
                subject = getSubjectLabel(subjectId),
                topic = topicName,
                userLevel = level,
                history = emptyList(),
                latestUserMessage = initialPrompt,
                systemPrompt = systemPrompt
            )

            val updatedHistory = listOf(
                ChatMessage(role = "user", text = "Introduce $topicName"),
                ChatMessage(role = "model", text = response)
            )

            repository.updateSessionChat(sessionId, updatedHistory, isCompleted = false)
            repository.markTopicCompleted(subjectId, getSubjectLabel(subjectId), topicName)

            // Refresh local active values
            val freshSession = repository.getSessionById(sessionId)
            _activeSession.value = freshSession
            _activeChatHistory.value = updatedHistory
            _isGenerating.value = false
        }
    }

    fun loadExistingSession(session: LessonSessionEntity) {
        _activeSession.value = session
        _activeChatHistory.value = JsonHelper.fromJsonMessageList(session.chatHistoryJson)
    }

    fun sendMessageToProfessor(studentMessageText: String) {
        val session = _activeSession.value ?: return
        val currentHistory = _activeChatHistory.value.toMutableList()
        
        // Add student query to local UI state for instant response feel
        val studentMsg = ChatMessage(role = "user", text = studentMessageText)
        currentHistory.add(studentMsg)
        _activeChatHistory.value = currentHistory

        viewModelScope.launch {
            _isGenerating.value = true
            val level = userState.value?.level ?: "Beginner"
            val systemPrompt = getSystemPrompt(session.subjectId, level)

            val response = GeminiApiClient.generateLessonResponse(
                subject = getSubjectLabel(session.subjectId),
                topic = session.topicName,
                userLevel = level,
                history = currentHistory.dropLast(1), // History up to this point
                latestUserMessage = studentMessageText,
                systemPrompt = systemPrompt
            )

            currentHistory.add(ChatMessage(role = "model", text = response))
            _activeChatHistory.value = currentHistory

            // Update database
            repository.updateSessionChat(session.id, currentHistory)
            _isGenerating.value = false
        }
    }

    // --- Quiz Operations ---

    fun generateQuizForTopic(subjectId: String, topicName: String) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _quizSubmitted.value = false
            _selectedOptions.value = emptyMap()
            _currentQuizIndex.value = 0
            _quizScore.value = 0

            val level = userState.value?.level ?: "Beginner"
            val quiz = GeminiApiClient.generateQuiz(subjectId, topicName, level)
            if (quiz != null) {
                _activeQuiz.value = quiz
                _quizQuestions.value = quiz.questions
            } else {
                // Return dummy quiz fallback if offline or request fails
                val fallbackQuiz = getFallbackQuiz(subjectId, topicName)
                _activeQuiz.value = fallbackQuiz
                _quizQuestions.value = fallbackQuiz.questions
            }
            _isGeneratingQuiz.value = false
        }
    }

    fun selectQuizOption(questionIndex: Int, optionIndex: Int) {
        if (_quizSubmitted.value) return
        val currentSelected = _selectedOptions.value.toMutableMap()
        currentSelected[questionIndex] = optionIndex
        _selectedOptions.value = currentSelected
    }

    fun nextQuizQuestion() {
        if (_currentQuizIndex.value < _quizQuestions.value.size - 1) {
            _currentQuizIndex.value++
        }
    }

    fun previousQuizQuestion() {
        if (_currentQuizIndex.value > 0) {
            _currentQuizIndex.value--
        }
    }

    fun submitQuiz() {
        if (_quizSubmitted.value) return
        var correctCount = 0
        val questions = _quizQuestions.value
        val selection = _selectedOptions.value

        for (i in questions.indices) {
            if (selection[i] == questions[i].correctIndex) {
                correctCount++
            }
        }

        _quizScore.value = correctCount
        _quizSubmitted.value = true

        // Save progress to Repository DB
        val session = _activeSession.value
        val subjectId = session?.subjectId ?: _selectedSubjectId.value
        val topicName = session?.topicName ?: "General Quiz"

        viewModelScope.launch {
            _activeQuiz.value?.let { quiz ->
                repository.saveQuizResult(subjectId, topicName, correctCount, questions.size, quiz)
            }
        }
    }

    // --- Note Analysis Operations ---

    fun analyzeStudentNotes(notesText: String) {
        viewModelScope.launch {
            _isAnalyzingNotes.value = true
            val analysis = GeminiApiClient.analyzeNotes(notesText)
            if (analysis != null) {
                _notesAnalysisState.value = analysis
            }
            _isAnalyzingNotes.value = false
        }
    }

    fun clearNotesAnalysis() {
        _notesAnalysisState.value = null
    }

    // --- Study Plan Operations ---

    fun generateStudyPlan(goal: String, days: Int) {
        viewModelScope.launch {
            _isGeneratingStudyPlan.value = true
            repository.generateAndSaveStudyPlan(goal, days)
            _isGeneratingStudyPlan.value = false
        }
    }

    fun deleteStudyPlan(planId: Int) {
        viewModelScope.launch {
            repository.deleteStudyPlan(planId)
        }
    }

    // --- Study Group Actions ---

    fun selectStudyGroup(groupId: Int) {
        viewModelScope.launch {
            val group = repository.getStudyGroupById(groupId)
            _selectedStudyGroup.value = group
        }
    }

    fun forceReloadSelectedStudyGroup() {
        _selectedStudyGroup.value?.let { current ->
            viewModelScope.launch {
                val group = repository.getStudyGroupById(current.id)
                _selectedStudyGroup.value = group
            }
        }
    }

    fun createStudyGroup(name: String, description: String, subjectId: String) {
        val studentName = userState.value?.name ?: "Student"
        viewModelScope.launch {
            repository.createStudyGroup(
                name = name,
                description = description,
                subjectId = subjectId,
                members = listOf(studentName)
            )
        }
    }

    fun joinStudyGroup(groupId: Int) {
        val studentName = userState.value?.name ?: "Student"
        viewModelScope.launch {
            repository.joinStudyGroup(groupId, studentName)
            selectStudyGroup(groupId)
        }
    }

    fun leaveStudyGroupSelection() {
        _selectedStudyGroup.value = null
    }

    fun sendMessageToStudyGroup(groupId: Int, text: String, requestAIReaction: Boolean = false) {
        val studentName = userState.value?.name ?: "Student"
        viewModelScope.launch {
            repository.addMessageToGroup(groupId, studentName, "student", text)
            // Refresh selection state
            val updated = repository.getStudyGroupById(groupId)
            _selectedStudyGroup.value = updated

            // If the user tagged @professor, or checked the box, trigger moderation!
            if (requestAIReaction || text.contains("@professor", ignoreCase = true) || text.contains("@prof", ignoreCase = true)) {
                requestProfessorModeration(groupId)
            }
        }
    }

    fun postSharedNoteToStudyGroup(groupId: Int, title: String, summary: String, flashcardsJson: String = "") {
        val studentName = userState.value?.name ?: "Student"
        viewModelScope.launch {
            repository.shareNotesToGroup(groupId, title, summary, studentName, flashcardsJson)
            val updated = repository.getStudyGroupById(groupId)
            _selectedStudyGroup.value = updated
        }
    }

    fun requestProfessorModeration(groupId: Int) {
        viewModelScope.launch {
            _isModerating.value = true
            val group = repository.getStudyGroupById(groupId) ?: return@launch
            val messages = JsonHelper.fromJsonGroupMessageList(group.messagesJson)
            
            // Construct message dialogue transcript for context
            val transcript = messages.takeLast(12).joinToString("\n") { 
                val roleLabel = if (it.role == "professor") "Elite Moderator" else "Student"
                "${it.senderName} ($roleLabel): ${it.text}" 
            }
            
            val systemPrompt = """
                You are HSB PROFESSOR AI, an elite AI Academic tutor and moderator.
                You are actively moderating and answering questions in the student study group '${group.name}' under the '${getSubjectLabel(group.subjectId)}' department.
                
                Read the recent conversation historical logs of the discussion session below. Write a highly intelligent, authoritative, and helpful post. Address any student questions directed to you (@professor), correct any educational inaccuracies, or provide deeper academic context using real-world analogies. Keep your response relevant, structured, and academically rigorous.
                
                Recent Conversation Logs:
                $transcript
            """.trimIndent()
            
            val response = GeminiApiClient.generateLessonResponse(
                subject = getSubjectLabel(group.subjectId),
                topic = group.name,
                userLevel = "Advanced",
                history = emptyList(),
                latestUserMessage = "Professor, please add your expert moderation insight to help the students.",
                systemPrompt = systemPrompt
            )
            
            repository.addMessageToGroup(groupId, "HSB Professor AI", "professor", response)
            
            // Refresh local selection view
            _selectedStudyGroup.value = repository.getStudyGroupById(groupId)
            _isModerating.value = false
        }
    }

    fun startDebateMode(groupId: Int) {
        viewModelScope.launch {
            _isModerating.value = true
            val group = repository.getStudyGroupById(groupId) ?: return@launch
            val subjectLabel = getSubjectLabel(group.subjectId)
            
            val systemPrompt = """
                You are HSB PROFESSOR AI, an elite AI Academic tutor, debate host, and neutral moderator.
                You are hosting a rigorous academic debate inside the study squad '${group.name}' under the '$subjectLabel' department.
                
                Your task: Propose a highly complex, classic, controversial, or cutting-edge academic/professional topic relevant to the field of '$subjectLabel'.
                Format your response matching these exact guidelines:
                TOPIC: <A highly specific, intriguing question/topic to debate>
                RULES: <Short, precise rules for the debate emphasizing academic backing, citing empirical research, logical consistency, and polite discourse.>
                INTRO: <A beautiful, challenging and intellectually provocative introduction setting the stage, identifying conflicting principles or school-of-thought paradigms.>
                
                Keep the tone extremely elite, motivating, academic, and professional. Ensure the topic is highly complex and thought-provoking.
            """.trimIndent()
            
            val rawResponse = GeminiApiClient.generateLessonResponse(
                subject = subjectLabel,
                topic = "Academic Debate Topic Proposal",
                userLevel = "Elite Graduate",
                history = emptyList(),
                latestUserMessage = "Professor, please propose an elite, complex debate topic for our squad and outline the rules.",
                systemPrompt = systemPrompt
            )
            
            var topic = ""
            var rules = "1. Arguments must be backed by empirical evidence or logical proof.\n2. Avoid logical fallacies.\n3. Remain polite and respectful."
            var intro = rawResponse
            
            val lines = rawResponse.lines()
            val topicLine = lines.firstOrNull { it.startsWith("TOPIC:", ignoreCase = true) }
            if (topicLine != null) {
                topic = topicLine.substringAfter("TOPIC:").trim().replace("**", "")
            } else {
                topic = "Cutting-edge paradigms in $subjectLabel"
            }
            
            val rulesStartIndex = rawResponse.indexOf("RULES:", ignoreCase = true)
            val introStartIndex = rawResponse.indexOf("INTRO:", ignoreCase = true)
            
            if (rulesStartIndex != -1) {
                val endOffset = if (introStartIndex != -1 && introStartIndex > rulesStartIndex) introStartIndex else rawResponse.length
                rules = rawResponse.substring(rulesStartIndex + 6, endOffset).trim()
            }
            if (introStartIndex != -1) {
                intro = rawResponse.substring(introStartIndex + 6).trim()
            }
            
            val updatedGroup = group.copy(
                isDebateModeActive = true,
                debateTopic = topic,
                debateTaskDescription = rules,
                debateAcademicPrinciples = intro
            )
            repository.updateStudyGroup(updatedGroup)
            
            val welcomeText = """
                📢 **DEBATE MODE ACTIVATED** 📢
                
                **Topic of Debate:**
                $topic
                
                **Rules of Academic Engagement:**
                $rules
                
                **Professor's Briefing:**
                $intro
            """.trimIndent()
            
            repository.addMessageToGroup(groupId, "HSB Professor AI", "professor", welcomeText)
            
            _selectedStudyGroup.value = repository.getStudyGroupById(groupId)
            _isModerating.value = false
        }
    }

    fun requestProfessorDebateModeration(groupId: Int) {
        viewModelScope.launch {
            _isModerating.value = true
            val group = repository.getStudyGroupById(groupId) ?: return@launch
            if (!group.isDebateModeActive) return@launch
            
            val messages = JsonHelper.fromJsonGroupMessageList(group.messagesJson)
            val academicPrinciples = group.debateAcademicPrinciples
            val debateTopic = group.debateTopic
            
            val transcript = messages.takeLast(12).joinToString("\n") { 
                val roleLabel = if (it.role == "professor") "Elite Moderator" else "Student"
                "${it.senderName} ($roleLabel): ${it.text}" 
            }
            
            val systemPrompt = """
                You are HSB PROFESSOR AI, an elite AI Academic tutor and neutral debate moderator.
                You are actively moderating. We are running a rigorous academic DEBATE MODE in group '${group.name}' under the '${getSubjectLabel(group.subjectId)}' department.
                
                Current Debate Topic: "$debateTopic"
                Debate Briefing Context:
                $academicPrinciples
                
                Your Task: Analyze the recent arguments submitted by the students in the transcript below.
                Provide a structured, unbiased, and authoritative neutral moderator evaluation:
                1. **Academic Rigor Check**: Review arguments for logical coherence, depth, and whether they are backed by academic principles or empirical evidence rather than mere opinions.
                2. **Constructive Critiques**: Respectfully point out any logical flaws, unsupported assertions, or areas needing stronger academic grounding.
                3. **Socratic Facilitation**: Propose a new angle or question to keep the discussion engaging and push students to think deeper.
                
                Recent Debate Logs:
                $transcript
            """.trimIndent()
            
            val response = GeminiApiClient.generateLessonResponse(
                subject = getSubjectLabel(group.subjectId),
                topic = "Debate Moderation",
                userLevel = "Elite Graduate",
                history = emptyList(),
                latestUserMessage = "Professor, please deliver a neutral debate assessment on our current arguments.",
                systemPrompt = systemPrompt
            )
            
            repository.addMessageToGroup(groupId, "HSB Professor AI", "professor", response)
            
            _selectedStudyGroup.value = repository.getStudyGroupById(groupId)
            _isModerating.value = false
        }
    }

    fun concludeDebateMode(groupId: Int) {
        viewModelScope.launch {
            _isModerating.value = true
            val group = repository.getStudyGroupById(groupId) ?: return@launch
            if (!group.isDebateModeActive) return@launch
            
            val messages = JsonHelper.fromJsonGroupMessageList(group.messagesJson)
            val debateTopic = group.debateTopic
            
            val transcript = messages.joinToString("\n") { 
                val roleLabel = if (it.role == "professor") "Elite Moderator" else "Student"
                "${it.senderName} ($roleLabel): ${it.text}" 
            }
            
            val systemPrompt = """
                You are HSB PROFESSOR AI, an elite AI Academic tutor and debate moderator.
                You are concluding a rigorous academic debate inside '${group.name}' under '${getSubjectLabel(group.subjectId)}'.
                
                Debate Topic: "$debateTopic"
                
                Please generate a comprehensive, structured "DEBATE VERDICT & ACADEMIC SUMMARY REPORT".
                Include:
                1. **Executive Summary**: Core highlights statement of the discussion.
                2. **Academic Merit Judgments**: Who formulated the strongest, most academically-backed arguments? Why?
                3. **Primary Takeaways & Synthesis**: Reconcile the opposing views into a unified academic framework.
                4. **Resource Recommendations**: Suggest books, papers, or study topics.
                
                Write in an inspiring, highly scholarly style.
                
                Complete Debate Transcript:
                $transcript
            """.trimIndent()
            
            val response = GeminiApiClient.generateLessonResponse(
                subject = getSubjectLabel(group.subjectId),
                topic = "Debate Conclusion Report",
                userLevel = "Elite Graduate",
                history = emptyList(),
                latestUserMessage = "Professor, please conclude this academic debate and compile the final verdict report.",
                systemPrompt = systemPrompt
            )
            
            repository.addMessageToGroup(groupId, "HSB Professor AI", "professor", response)
            
            val updatedGroup = group.copy(
                isDebateModeActive = false,
                debateTopic = "",
                debateTaskDescription = "",
                debateAcademicPrinciples = ""
            )
            repository.updateStudyGroup(updatedGroup)
            
            _selectedStudyGroup.value = repository.getStudyGroupById(groupId)
            _isModerating.value = false
        }
    }

    // --- Helper Getters and Core prompt parameters ---

    private fun getSubjectLabel(id: String): String {
        return when (id) {
            "ai_ml" -> "AI / ML"
            "programming" -> "Programming"
            "cybersecurity" -> "Cybersecurity"
            "math" -> "Math"
            "physics" -> "Physics"
            else -> "General Learning"
        }
    }

    private fun getSystemPrompt(subjectId: String, level: String): String {
        val subjectDetails = when (subjectId) {
            "ai_ml" -> "computer science, artificial intelligence, neural networks, machine learning, and deep learning. Use pseudocode, visual descriptions, and mathematical intuition where applicable."
            "programming" -> "programming (Python, Java, Kotlin, JS), software engineering, data structures, and algorithms. Include clean code snippets, syntax analysis, and debugging tips."
            "cybersecurity" -> "cybersecurity, threat vectors, system vulnerabilities, security policies, ethical hacking, and network defenses. Use examples based on server logs or threat models."
            "math" -> "mathematics, algebra, calculus, discrete math, and statistics. Explain step by step, showing formal definitions, proof concepts, and real calculations."
            "physics" -> "physics, classical mechanics, electromagnetism, thermodynamics, and quantum mechanics. Use analogies of physical things, formulas with physical meaning, and diagrams."
            else -> "general academic topics. Be structured, deep, clear, and highly educational."
        }
        
        return """
            You are HSB PROFESSOR AI, an elite academic professor with expertise in all fields, specifically teaching: $subjectDetails.
            Your job is to teach deeply, step-by-step, like a real MIT or Stanford professor.
            
            Current student skill level: $level.
            
            Teaching Rules:
            1. Always explain concepts in highly structured layers:
               - ### 🎓 Simple Explanation (using clear analogies etc.)
               - ### 🔬 Detailed Explanation (providing core theory, structure/mechanics)
               - ### 💡 Advanced Technical Insights (deep theoretical details, code, or mathematics appropriate to the student's level)
            2. Always include a "Real-World Example" of the concept in action.
            3. Provide a practical "Practice Question" with guidance for the student to solve.
            4. Adopt a professional, encouraging, and academic tone. Don't sound like a generic web chatbot. Be rigorous.
            5. Never give short, superficial, or single-line answers. Explain the mechanics.
            6. Address the user directly as your student and maintain a scholarly tutor persona.
        """.trimIndent()
    }

    private fun getFallbackQuiz(subjectId: String, topicName: String): QuizData {
        return QuizData(
            questions = listOf(
                QuizQuestion(
                    question = "Basic Concept Test: What is the primary focus of $topicName in $subjectId?",
                    options = listOf(
                        "Optimizing code speed and structure",
                        "Storing global state values",
                        "Theoretical foundations and direct engineering applications",
                        "Parsing generic string text"
                    ),
                    correctIndex = 2,
                    explanation = "The correct baseline of academic study for $topicName in $subjectId covers both theoretical layouts and core application fields."
                ),
                QuizQuestion(
                    question = "Which level of complexity is primary in $topicName?",
                    options = listOf(
                        "Purely basic superficial layouts",
                        "Hierarchical nested conceptual frames",
                        "Static text outputs",
                        "Unstructured key value state"
                    ),
                    correctIndex = 1,
                    explanation = "Hierarchical conceptual models allow the academic professor to teach concepts step by step in structured layers."
                )
            )
        )
    }
}
class ProfessorViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfessorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfessorViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
