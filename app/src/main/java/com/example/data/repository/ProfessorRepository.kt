package com.example.data.repository

import com.example.data.database.*
import com.example.data.api.GeminiApiClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ProfessorRepository(private val db: AppDatabase) {

    val userFlow: Flow<UserEntity?> = db.userDao().getUserFlow()
    val allProgressFlow: Flow<List<SubjectProgressEntity>> = db.subjectProgressDao().getAllProgressFlow()
    val allSessionsFlow: Flow<List<LessonSessionEntity>> = db.lessonSessionDao().getAllSessionsFlow()
    val allQuizResultsFlow: Flow<List<QuizResultEntity>> = db.quizResultDao().getAllQuizResultsFlow()
    val latestStudyPlanFlow: Flow<StudyPlanEntity?> = db.studyPlanDao().getLatestStudyPlanFlow()

    suspend fun getOrCreateUser(defaultName: String = "Student", defaultEmail: String = "student@example.com"): UserEntity = withContext(Dispatchers.IO) {
        val existing = db.userDao().getUserSync()
        if (existing != null) {
            existing
        } else {
            val newUser = UserEntity(
                name = defaultName,
                email = defaultEmail,
                level = "Beginner",
                streak = 1,
                lastLearnDate = System.currentTimeMillis()
            )
            db.userDao().insertUser(newUser)
            newUser
        }
    }

    suspend fun updateUserProfile(name: String, email: String, level: String) = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserSync() ?: UserEntity(name = name, email = email)
        db.userDao().insertUser(
            user.copy(name = name, email = email, level = level)
        )
    }

    suspend fun checkAndUpdateStreak() = withContext(Dispatchers.IO) {
        val user = db.userDao().getUserSync() ?: return@withContext
        val now = System.currentTimeMillis()
        val MILLIS_IN_DAY = 24 * 60 * 60 * 1000L
        
        val diff = now - user.lastLearnDate
        if (diff > MILLIS_IN_DAY && diff < 2 * MILLIS_IN_DAY) {
            // Consecutive day
            db.userDao().insertUser(
                user.copy(streak = user.streak + 1, lastLearnDate = now)
            )
        } else if (diff >= 2 * MILLIS_IN_DAY) {
            // Streak broken
            db.userDao().insertUser(
                user.copy(streak = 1, lastLearnDate = now)
            )
        } else if (user.lastLearnDate == 0L) {
            db.userDao().insertUser(
                user.copy(streak = 1, lastLearnDate = now)
            )
        }
    }

    suspend fun incrementProgressScore(subjectId: String, subjectName: String, extraScore: Int) = withContext(Dispatchers.IO) {
        val progress = db.subjectProgressDao().getProgressForSubject(subjectId)
        if (progress != null) {
            db.subjectProgressDao().insertProgress(
                progress.copy(totalScore = progress.totalScore + extraScore)
            )
        } else {
            db.subjectProgressDao().insertProgress(
                SubjectProgressEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    totalScore = extraScore,
                    lessonsCount = 1
                )
            )
        }
    }

    suspend fun markTopicCompleted(subjectId: String, subjectName: String, topic: String) = withContext(Dispatchers.IO) {
        val progress = db.subjectProgressDao().getProgressForSubject(subjectId) ?: SubjectProgressEntity(
            subjectId = subjectId,
            subjectName = subjectName
        )
        val completedList = JsonHelper.fromJsonStringList(progress.topicsCompleted).toMutableList()
        if (!completedList.contains(topic)) {
            completedList.add(topic)
            val updatedJson = JsonHelper.toJsonStringList(completedList)
            db.subjectProgressDao().insertProgress(
                progress.copy(
                    topicsCompleted = updatedJson,
                    lessonsCount = progress.lessonsCount + 1
                )
            )
        }
    }

    // --- Session / Lesson Chat Functions ---

    suspend fun createNewSession(subjectId: String, topicName: String, level: String): Int = withContext(Dispatchers.IO) {
        val newSession = LessonSessionEntity(
            subjectId = subjectId,
            topicName = topicName,
            chatHistoryJson = JsonHelper.toJsonMessageList(emptyList()),
            currentLevel = level,
            isCompleted = false
        )
        db.lessonSessionDao().insertSession(newSession).toInt()
    }

    suspend fun getSessionById(sessionId: Int): LessonSessionEntity? = withContext(Dispatchers.IO) {
        db.lessonSessionDao().getSessionById(sessionId)
    }

    suspend fun updateSessionChat(sessionId: Int, chatHistory: List<ChatMessage>, isCompleted: Boolean = false) = withContext(Dispatchers.IO) {
        val session = db.lessonSessionDao().getSessionById(sessionId) ?: return@withContext
        db.lessonSessionDao().insertSession(
            session.copy(
                chatHistoryJson = JsonHelper.toJsonMessageList(chatHistory),
                isCompleted = isCompleted,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    // --- Quiz Results ---

    suspend fun saveQuizResult(subjectId: String, topicName: String, score: Int, total: Int, quizData: QuizData) = withContext(Dispatchers.IO) {
        val quizJson = JsonHelper.toJson(quizData)
        val result = QuizResultEntity(
            subjectId = subjectId,
            topicName = topicName,
            score = score,
            totalQuestions = total,
            quizDataJson = quizJson
        )
        db.quizResultDao().insertQuizResult(result)
        incrementProgressScore(subjectId, getSubjectLabel(subjectId), score * 10)
    }

    // --- Study Plan ---

    suspend fun generateAndSaveStudyPlan(goal: String, days: Int): StudyPlanRoadmap? {
        val roadmap = GeminiApiClient.generateStudyPlan(goal, days)
        if (roadmap != null) {
            val json = JsonHelper.toJson(roadmap)
            db.studyPlanDao().insertStudyPlan(
                StudyPlanEntity(
                    goal = goal,
                    durationDays = days,
                    roadmapJson = json
                )
            )
        }
        return roadmap
    }

    suspend fun deleteStudyPlan(id: Int) = withContext(Dispatchers.IO) {
        db.studyPlanDao().deleteStudyPlanById(id)
    }

    // --- Study Group Collaborative Functions ---

    val allStudyGroupsFlow: Flow<List<StudyGroupEntity>> = db.studyGroupDao().getAllStudyGroupsFlow()

    suspend fun getStudyGroupById(id: Int): StudyGroupEntity? = withContext(Dispatchers.IO) {
        db.studyGroupDao().getStudyGroupById(id)
    }

    suspend fun createStudyGroup(name: String, description: String, subjectId: String, members: List<String>) = withContext(Dispatchers.IO) {
        val newGroup = StudyGroupEntity(
            name = name,
            description = description,
            subjectId = subjectId,
            membersJson = JsonHelper.toJsonStringList(members),
            messagesJson = "[]",
            sharedNotesJson = "[]"
        )
        db.studyGroupDao().insertStudyGroup(newGroup)
    }

    suspend fun updateStudyGroup(group: StudyGroupEntity) = withContext(Dispatchers.IO) {
        db.studyGroupDao().insertStudyGroup(group)
    }

    suspend fun joinStudyGroup(groupId: Int, memberName: String) = withContext(Dispatchers.IO) {
        val group = db.studyGroupDao().getStudyGroupById(groupId) ?: return@withContext
        val members = JsonHelper.fromJsonStringList(group.membersJson).toMutableList()
        if (!members.contains(memberName)) {
            members.add(memberName)
            db.studyGroupDao().insertStudyGroup(
                group.copy(membersJson = JsonHelper.toJsonStringList(members))
            )
        }
    }

    suspend fun addMessageToGroup(groupId: Int, senderName: String, role: String, text: String): GroupMessage = withContext(Dispatchers.IO) {
        val group = db.studyGroupDao().getStudyGroupById(groupId) ?: throw IllegalArgumentException("Group not found")
        val messages = JsonHelper.fromJsonGroupMessageList(group.messagesJson).toMutableList()
        val newMessage = GroupMessage(
            senderName = senderName,
            role = role,
            text = text
        )
        messages.add(newMessage)
        db.studyGroupDao().insertStudyGroup(
            group.copy(messagesJson = JsonHelper.toJsonGroupMessageList(messages))
        )
        newMessage
    }

    suspend fun shareNotesToGroup(groupId: Int, title: String, summary: String, senderName: String, flashcardsJson: String = "") = withContext(Dispatchers.IO) {
        val group = db.studyGroupDao().getStudyGroupById(groupId) ?: return@withContext
        val sharedNotes = JsonHelper.fromJsonSharedNotesList(group.sharedNotesJson).toMutableList()
        val entry = SharedNotes(
            title = title,
            summary = summary,
            senderName = senderName,
            flashcardsJson = flashcardsJson
        )
        sharedNotes.add(entry)
        db.studyGroupDao().insertStudyGroup(
            group.copy(sharedNotesJson = JsonHelper.toJsonSharedNotesList(sharedNotes))
        )
    }

    suspend fun prepopulateDefaultGroupsIfNeeded() = withContext(Dispatchers.IO) {
        if (db.studyGroupDao().getStudyGroupsCount() == 0) {
            val defaultGroups = listOf(
                StudyGroupEntity(
                    id = 1,
                    name = "Deep Learning & AI Systems",
                    description = "Collaborative MIT team designing the next generation of LLMs. Let's study multi-head attention!",
                    subjectId = "ai_ml",
                    membersJson = JsonHelper.toJsonStringList(listOf("You", "Alice (MIT)", "Bob (Stanford)", "Claire (Berkeley)")),
                    messagesJson = JsonHelper.toJsonGroupMessageList(listOf(
                        GroupMessage(senderName = "Bob (Stanford)", role = "student", text = "Does anyone understand why Transformers use multihead attention instead of one single massive attention layer?"),
                        GroupMessage(senderName = "Alice (MIT)", role = "student", text = "I think it allows the model to jointly attend to information from different representation subspaces at different positions!"),
                        GroupMessage(senderName = "Bob (Stanford)", role = "student", text = "That makes sense, but how does the linear projections help specifically? @professor can you provide an elite walkthrough?"),
                        GroupMessage(role = "professor", senderName = "HSB Professor AI", text = "To understand multihead attention intuitively, think of reading a research paper. Instead of analyzing the text using only one linguistic lens (e.g., just syntax), multihead attention projects the Query, Key, and Value vectors into different subspaces. Each 'head' acts as a specialized lens: Head 1 might focus on subject-verb dependencies, Head 2 on pronoun reference, and Head 3 on mathematical symbols. This parallelism allows the model to assemble a composite understanding of high-dimensional relations that a single attention block would average out!"),
                        GroupMessage(senderName = "Claire (Berkeley)", role = "student", text = "Wow! That explanation of linguistic lenses is so clear. Thank you Professor!")
                    )),
                    sharedNotesJson = JsonHelper.toJsonSharedNotesList(listOf(
                        SharedNotes(
                            title = "Backpropagation Mechanics Quick Sheet",
                            summary = "Notes detailing the partial derivatives chain rule through dense neural networks.",
                            senderName = "Alice (MIT)",
                            flashcardsJson = JsonHelper.toJson(listOf(
                                Flashcard("What is the primary role of the Jacobian matrix?", "It represents all first-order partial derivatives of a vector-valued function, mapping inputs to output gradients."),
                                Flashcard("Why does vanishing gradient happen?", "When using deep Sigmoid, compounding activations with derivative range [0, 0.25] shrinks gradients exponentially.")
                            ))
                        )
                    ))
                ),
                StudyGroupEntity(
                    id = 2,
                    name = "Red-Teaming & SQL injection Security",
                    description = "Stanford cyber defense room. Discussing modern SQL injection vectors and automated prevention tools.",
                    subjectId = "cybersecurity",
                    membersJson = JsonHelper.toJsonStringList(listOf("You", "Sanjay (Stanford)", "Linus (MIT)")),
                    messagesJson = JsonHelper.toJsonGroupMessageList(listOf(
                        GroupMessage(senderName = "Sanjay (Stanford)", role = "student", text = "Are there situations where prepared statements are NOT enough to prevent SQL injection?"),
                        GroupMessage(senderName = "Linus (MIT)", role = "student", text = "Yes, if the dynamic parameters are table names, column names, or raw ORDER BY directions. Prepared statements only bind values, not SQL structures!"),
                        GroupMessage(senderName = "Sanjay (Stanford)", role = "student", text = "Wow, is that true? @professor is there a safe way to handle column-sorting dynamically?")
                    )),
                    sharedNotesJson = JsonHelper.toJsonSharedNotesList(emptyList())
                ),
                StudyGroupEntity(
                    id = 3,
                    name = "Compiler Design & Code Optimization",
                    description = "UCLA syllabus study group for structural optimizations, SSA form, and instruction scheduling.",
                    subjectId = "programming",
                    membersJson = JsonHelper.toJsonStringList(listOf("You", "Sarah (UCLA)", "Yuki (Tokyo Tech)")),
                    messagesJson = JsonHelper.toJsonGroupMessageList(listOf(
                        GroupMessage(senderName = "Sarah (UCLA)", role = "student", text = "Does compiler SSA form simplify register allocation?"),
                        GroupMessage(senderName = "Yuki (Tokyo Tech)", role = "student", text = "Absolutely! SSA ensures Japanese standard of variable definitions and compiler allocations.")
                    )),
                    sharedNotesJson = JsonHelper.toJsonSharedNotesList(emptyList())
                )
            )
            for (dg in defaultGroups) {
                db.studyGroupDao().insertStudyGroup(dg)
            }
        }
    }

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
}
