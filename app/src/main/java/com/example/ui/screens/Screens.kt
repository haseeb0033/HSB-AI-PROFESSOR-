package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.LessonSessionEntity
import com.example.data.database.QuizResultEntity
import com.example.data.database.StudyPlanEntity
import com.example.data.database.UserEntity
import com.example.data.database.StudyGroupEntity
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProfessorViewModel
import kotlinx.coroutines.launch

// --- REUSABLE GLASS COMPONENTS & STYLES ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderWidth: Float = 1.5f,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Column(
        modifier = cardModifier
            .background(
                color = SlateCard.copy(alpha = 0.85f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = borderWidth.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SlateBorder.copy(alpha = 0.5f),
                        SlateDark.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun MainScreenContainer(
    viewModel: ProfessorViewModel,
    onNavigateToQuiz: (String, String) -> Unit
) {
    var activeTab by remember { mutableStateOf("dashboard") }
    val user by viewModel.userState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SlateCard,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = SlateBorder.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                listOf(
                    Triple("dashboard", "Dashboard", Icons.Default.Home),
                    Triple("chat", "Lectures", Icons.Default.PlayArrow),
                    Triple("notes", "Notes Tutor", Icons.Default.Add),
                    Triple("groups", "Study Hub", Icons.Default.Share),
                    Triple("study", "Study Plan", Icons.Default.Star),
                    Triple("profile", "Profile", Icons.Default.Person)
                ).forEach { (tabId, label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == tabId,
                        onClick = { activeTab = tabId },
                        label = { Text(label, fontSize = 9.sp, color = Color.White, maxLines = 1) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (activeTab == tabId) IndigoAccent else Color.White.copy(alpha = 0.6f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = SlateDark
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateDark)
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    onStartLesson = { activeTab = "chat" },
                    onNavigateToGroups = { activeTab = "groups" }
                )
                "chat" -> ChatProfessorScreen(
                    viewModel = viewModel,
                    onNavigateToQuiz = onNavigateToQuiz
                )
                "notes" -> ExplainNotesScreen(
                    viewModel = viewModel,
                    onShareToGroupClicked = { activeTab = "groups" }
                )
                "groups" -> StudyGroupsScreen(viewModel = viewModel)
                "study" -> StudyPlanScreen(viewModel = viewModel)
                "profile" -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}

// --- SCREEN 1: DASHBOARD ---

@Composable
fun DashboardScreen(
    viewModel: ProfessorViewModel,
    onStartLesson: () -> Unit,
    onNavigateToGroups: () -> Unit
) {
    val user by viewModel.userState.collectAsState()
    val progressList by viewModel.allProgressState.collectAsState()
    val quizResults by viewModel.allQuizResultsState.collectAsState()
    val pastSessions by viewModel.allSessionsState.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()

    val totalScore = progressList.sumOf { it.totalScore }
    val completedCount = progressList.sumOf { JsonHelper.fromJsonStringList(it.topicsCompleted).size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Streak
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Circle Mascot
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(IndigoAccent, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name?.take(1) ?: "H").uppercase(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkPurple
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Hello, ${user?.name ?: "Student"}!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                        Text(
                            text = "Academic level: ${user?.level ?: "Beginner"}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Streak / Rank Badge
                Box(
                    modifier = Modifier
                        .background(DarkPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, IndigoAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RANK: SCHOLAR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                }
            }
        }

        // BENTO CARD 1: Progress & Hero Streak Card (Full Width)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = SlateBorder, shape = RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LEARNING STREAK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${user?.streak ?: 12} Days",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .background(DarkPurple, RoundedCornerShape(20.dp))
                            .border(1.dp, IndigoAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "MIT SYLLABUS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column {
                    val activeTopicName = activeSession?.topicName ?: "Neural Networks Module"
                    val progressValue = if (totalScore > 0) ((totalScore % 100).toFloat() / 100f).coerceIn(0.2f, 0.95f) else 0.84f
                    val progressPercent = (progressValue * 100).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = activeTopicName,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$progressPercent%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Custom Linear Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(SlateDark, RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressValue)
                                .height(8.dp)
                                .background(IndigoAccent, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // BENTO CARDS Row 2 (Resume Lesson & Daily Quiz)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Bento: Resume Lesson
                Column(
                    modifier = Modifier
                        .weight(1.6f)
                        .height(140.dp)
                        .background(color = IndigoAccent, shape = RoundedCornerShape(28.dp))
                        .clickable {
                            if (activeSession != null) {
                                onStartLesson()
                            } else {
                                val selectedSubId = viewModel.selectedSubjectId.value
                                val defaultTopic = when (selectedSubId) {
                                    "ai_ml" -> "Neural Networks"
                                    "programming" -> "Recursion & Call Stacking"
                                    "cybersecurity" -> "SQL Injection Vectors"
                                    "math" -> "Matrix Multiplication"
                                    "physics" -> "Newtonian Mechanics"
                                    else -> "Scientific Method"
                                }
                                viewModel.startNewLesson(selectedSubId, defaultTopic)
                                onStartLesson()
                            }
                        }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(DarkPurple, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("▶", color = Color.White, fontSize = 10.sp)
                    }

                    Column {
                        Text(
                            text = "Resume Lesson",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkPurple
                        )
                        Text(
                            text = activeSession?.topicName ?: "Backpropagation II",
                            color = DarkPurple.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Bento: Daily Quiz
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp)
                        .background(color = TealAccent, shape = RoundedCornerShape(28.dp))
                        .clickable {
                            val subId = viewModel.selectedSubjectId.value
                            val topic = activeSession?.topicName ?: "Theoretical Foundations"
                            viewModel.startNewLesson(subId, topic)
                            onStartLesson()
                        }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📝",
                        fontSize = 28.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Daily\nQuiz",
                        color = Color(0xFF21005D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // BENTO SECTION: Department Selectors Grid
        item {
            Column {
                Text(
                    text = "Select Academic Department",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                val departments = listOf(
                    Triple("ai_ml", "CS / AI Mechanics", "💻"),
                    Triple("cybersecurity", "Cyber Security", "🛡️"),
                    Triple("programming", "Systems Developer", "⚙️"),
                    Triple("math", "Advanced Math", "📐"),
                    Triple("physics", "Physics Space", "⚛️"),
                    Triple("general", "Creative Logic", "🔮")
                )

                // Render as a clean list of grid blocks matching the Bento format
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in departments.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (j in i..i + 1) {
                                if (j < departments.size) {
                                    val (subId, label, emoji) = departments[j]
                                    val isSelected = viewModel.selectedSubjectId.collectAsState().value == subId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) IndigoAccent else SlateCard,
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color.White else SlateBorder.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(24.dp)
                                            )
                                            .clickable { viewModel.selectSubject(subId) }
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(
                                                        if (isSelected) DarkPurple else SlateBorder,
                                                        RoundedCornerShape(8.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 16.sp)
                                            }
                                            Text(
                                                text = label,
                                                color = if (isSelected) DarkPurple else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // BENTO CARD: Explain PDF / Notes wide banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = DarkPurple, shape = RoundedCornerShape(28.dp))
                    .border(1.dp, IndigoAccent.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                    .clickable {
                        // Will trigger or lead the user to change tabs or explainers
                    }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Analyze PDF & Notes",
                        color = TealAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Convert research to flashcards & summary notes instantly.",
                        color = IndigoAccent.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
                
                // Floating Action Trigger circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(IndigoAccent, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = DarkPurple,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // BENTO CARD: Collaborative Study Hub wide banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = SlateCard, shape = RoundedCornerShape(28.dp))
                    .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                    .clickable {
                        onNavigateToGroups()
                    }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "👥 Research & Study Hub",
                        color = TealAccent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Join study squads and participate in group discussions moderated by HSB Professor AI.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                // Floating Action Trigger circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(TealAccent, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Go to groups",
                        tint = SlateDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Syllabus Quick Lesson Topics list
        item {
            Column {
                Text(
                    text = "Syllabus Micro-Lectures",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                val selectedSubId = viewModel.selectedSubjectId.collectAsState().value
                val sampleTopics = when (selectedSubId) {
                    "ai_ml" -> listOf("Neural Networks", "Deep Convolutional Nets", "Reinforcement Learning")
                    "programming" -> listOf("Recursion & Call Stacking", "Binary Trees & Sorting", "REST API Systems")
                    "cybersecurity" -> listOf("SQL Injection Vectors", "Asymmetric Cryptography", "Buffer Overflow Protection")
                    "math" -> listOf("Matrix Multiplication", "Taylor Series Expansion", "Calculus Integrals")
                    "physics" -> listOf("Newtonian Mechanics", "Maxwell's Equations", "Special Relativity")
                    else -> listOf("Scientific Method", "Socratic Querying", "Critical Logic Theory")
                }

                sampleTopics.forEach { topic ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(SlateCard, RoundedCornerShape(16.dp))
                            .border(1.dp, SlateBorder.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.startNewLesson(selectedSubId, topic)
                                onStartLesson()
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                topic,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Start",
                                tint = IndigoAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 2: PROFESSOR CHAT ---

@Composable
fun ChatProfessorScreen(
    viewModel: ProfessorViewModel,
    onNavigateToQuiz: (String, String) -> Unit
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val chatHistory by viewModel.activeChatHistory.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val listState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }

    if (activeSession == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Academic Office",
                tint = IndigoAccent,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Academic Office Terminal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Choose any subject mode or lesson syllabus block from the Dashboard to summon Professor AI.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Professor Active Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .border(width = 1.dp, color = SlateBorder.copy(alpha = 0.3f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_profile_avatar),
                    contentDescription = "Professor Icon",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Prof. AI (${activeSession?.currentLevel ?: "Beginner"})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = activeSession?.topicName ?: "Syllabus Lecture",
                        fontSize = 11.sp,
                        color = TealAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Exam Action Launcher
            Button(
                onClick = {
                    onNavigateToQuiz(
                        activeSession?.subjectId ?: "general",
                        activeSession?.topicName ?: "Concepts"
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Exam",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Start Quiz", fontSize = 12.sp, color = Color.White)
            }
        }

        // Chat Conversation Frame
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(listState)
                .padding(16.dp)
        ) {
            // Lecture introduction
            Text(
                text = "HSB CLASSROOM DISCUSSION FOR TOPIC: ${activeSession?.topicName?.uppercase()}",
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            chatHistory.forEach { chat ->
                val isProfessor = chat.role == "model"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = if (isProfessor) Arrangement.Start else Arrangement.End
                ) {
                    if (isProfessor) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(SlateCard, RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                                .border(1.dp, SlateBorder.copy(alpha = 0.4f), RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                "HSB PROFESSOR AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoAccent,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            AcademicFormattedText(text = chat.text)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(IndigoAccent.copy(alpha = 0.25f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp))
                                .border(1.dp, IndigoAccent, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp))
                                .padding(12.dp)
                        ) {
                            Text(text = chat.text, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (isGenerating) {
                // Professor Typing/Generating Animation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(SlateCard, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Professor AI is typing notes...",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scroll helper anchor
            LaunchedEffect(chatHistory.size, isGenerating) {
                listState.scrollTo(listState.maxValue)
            }
        }

        // Input Console Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateCard)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask the Professor to expand, give examples, or verify...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SlateBorder
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputQuery.isNotBlank() && !isGenerating) {
                        val query = inputQuery
                        inputQuery = ""
                        viewModel.sendMessageToProfessor(query)
                    }
                },
                modifier = Modifier
                    .background(IndigoAccent, RoundedCornerShape(12.dp))
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

// --- SCREEN 3: INTERACTIVE QUIZ INTERFACE ---

@Composable
fun QuizScreen(
    viewModel: ProfessorViewModel,
    subjectId: String,
    topicName: String,
    onBackToLesson: () -> Unit
) {
    val quizQuestions by viewModel.quizQuestions.collectAsState()
    val currentIndex by viewModel.currentQuizIndex.collectAsState()
    val selection by viewModel.selectedOptions.collectAsState()
    val isSubmitted by viewModel.quizSubmitted.collectAsState()
    val calculatedScore by viewModel.quizScore.collectAsState()
    val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsState()

    // Trigger quiz creation if empty or mismatching content
    LaunchedEffect(subjectId, topicName) {
        viewModel.generateQuizForTopic(subjectId, topicName)
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard)
                    .border(width = 1.dp, color = SlateBorder.copy(alpha = 0.3f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLesson) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Topic Assessment: $topicName",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Complete the MCQs in this assessment grid.",
                        fontSize = 11.sp,
                        color = TealAccent
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateDark)
                .padding(padding)
        ) {
            if (isGeneratingQuiz) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = IndigoAccent)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Compiling custom assessment test...", color = Color.White, fontSize = 15.sp)
                }
                return@Scaffold
            }

            if (quizQuestions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Failed to compile quiz.", color = Color.White)
                }
                return@Scaffold
            }

            val total = quizQuestions.size

            if (isSubmitted) {
                // Score card summary
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (calculatedScore >= 3) Icons.Default.Check else Icons.Default.Star,
                        contentDescription = "Test Complete",
                        tint = if (calculatedScore >= 3) TealAccent else AmberStreak,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Assessment Compiled",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Score earned: $calculatedScore / $total correct",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealAccent
                    )
                    Text(
                        text = "Professor feedback: ${if (calculatedScore >= 4) "Excellent insight!" else if (calculatedScore >= 3) "Satisfactory understanding!" else "Please review research highlights."}",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onBackToLesson,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                    ) {
                        Text("Finish and Return to Classroom", color = Color.White)
                    }
                }
                return@Scaffold
            }

            val question = quizQuestions[currentIndex]
            val selectedOption = selection[currentIndex]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Assessment progress indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of $total",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    LinearProgressIndicator(
                        progress = (currentIndex + 1).toFloat() / total.toFloat(),
                        color = IndigoAccent,
                        trackColor = SlateBorder,
                        modifier = Modifier
                            .width(120.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Question card
                GlassCard {
                    Text(
                        text = question.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Options cards
                question.options.forEachIndexed { optIndex, optionText ->
                    val isCurrentSelection = selectedOption == optIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                color = if (isCurrentSelection) IndigoAccent.copy(alpha = 0.2f) else SlateCard,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isCurrentSelection) 2.dp else 1.dp,
                                color = if (isCurrentSelection) IndigoAccent else SlateBorder.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectQuizOption(currentIndex, optIndex) }
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (isCurrentSelection) IndigoAccent else SlateDark,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrentSelection) {
                                    Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = optionText, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Controls footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.previousQuizQuestion() },
                        enabled = currentIndex > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard)
                    ) {
                        Text("Prev", color = Color.White)
                    }

                    if (currentIndex == total - 1) {
                        Button(
                            onClick = { viewModel.submitQuiz() },
                            enabled = selection.size == total,
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                        ) {
                            Text("Submit Exam", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.nextQuizQuestion() },
                            enabled = selectedOption != null,
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                        ) {
                            Text("Next", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 4: NOTES EXPLAINER / SYLLABUS MAKER ---

@Composable
fun ExplainNotesScreen(
    viewModel: ProfessorViewModel,
    onShareToGroupClicked: () -> Unit = {}
) {
    var rawNotes by remember { mutableStateOf("") }
    val isAnalyzing by viewModel.isAnalyzingNotes.collectAsState()
    val analysis by viewModel.notesAnalysisState.collectAsState()

    // Lecture pre-load curated samples
    val sampleCurriculums = listOf(
        Pair("Linear Regression", "Linear Regression fits a straight line represented as Y = WX + b. We minimize the Mean Squared Error (MSE) loss via Gradient Descent optimizations. Key assumptions: Linearity, Independence, Homoscedasticity, Normality of residuals."),
        Pair("SQL Injections", "SQL Injection allows attackers to manipulate backend database SQL commands. By entering inputs like ' OR '1'='1, malicious nodes bypass login verification blocks. Prevention strategies: parameterized queries, raw inputs sanitization."),
        Pair("Special Relativity", "Special Relativity is Einstein's formulation of spacetime. Postulate 1: Physics laws are invariant in all inertial systems. Postulate 2: Speed of light c is constant. Results: Time dilation (moving clocks tick slower), length contraction.")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explainer banner
        Text(
            text = "📄 Lecture Summarizer & Explainer Engine",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Convert any raw notes, textbook chapters, or concepts into study summary summaries, digital key points flashcards, and assessment quizzes instantly.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        if (analysis == null) {
            // Lecture paste inputs
            Text(
                text = "Paste notes or textbook syllabus block:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            OutlinedTextField(
                value = rawNotes,
                onValueChange = { rawNotes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("Paste academic notes directly here (e.g. key slides formulas, bullet lists, threat maps)...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SlateBorder
                )
            )

            // Dynamic Sample Chips
            Column {
                Text(
                    text = "Try curated university samples:",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sampleCurriculums.forEach { (label, content) ->
                        Box(
                            modifier = Modifier
                                .background(SlateCard, RoundedCornerShape(8.dp))
                                .border(1.dp, SlateBorder.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { rawNotes = content }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Button(
                onClick = { if (rawNotes.isNotBlank()) viewModel.analyzeStudentNotes(rawNotes) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                enabled = !isAnalyzing && rawNotes.isNotBlank()
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Transform Notes with Professor AI", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Display Notes Analysis Artifacts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Study Artifacts Generated", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 16.sp)
                Button(
                    onClick = { viewModel.clearNotesAnalysis() },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Notes", fontSize = 12.sp, color = Color.White)
                }
            }

            // Summary Section
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, "S", tint = IndigoAccent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Syllabus Summary", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(analysis!!.summary, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
            }

            // Key Highlights / Flashcards Section
            Column {
                Text("Key Concepts Highlights", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                analysis!!.keyPoints.forEach { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(SlateCard.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text("•", color = IndigoAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(point, color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            // Interactive Flashcards (Flippable Card)
            if (analysis!!.flashcards.isNotEmpty()) {
                Text("Digital Revision Flashcards", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Text("Tap on card below to flip definition details:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                
                var showAnswerIndex by remember { mutableStateOf<Int?>(null) }
                analysis!!.flashcards.take(3).forEachIndexed { index, card ->
                    val flipped = showAnswerIndex == index
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(if (flipped) TealAccent.copy(alpha = 0.15f) else SlateCard, RoundedCornerShape(12.dp))
                            .border(1.dp, if (flipped) TealAccent else SlateBorder, RoundedCornerShape(12.dp))
                            .clickable { showAnswerIndex = if (flipped) null else index }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (flipped) "DEFINITION ANSWER" else "CONCEPT / TERM QUESTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (flipped) TealAccent else IndigoAccent
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (flipped) card.answer else card.question,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Share section
            var showShareDialog by remember { mutableStateOf(false) }
            val studyGroups by viewModel.allStudyGroupsState.collectAsState()

            if (showShareDialog) {
                AlertDialog(
                    onDismissRequest = { showShareDialog = false },
                    title = { Text("Share to Study Group", color = Color.White) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose a study room to post your notes summary and flashcards instantly:", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                            if (studyGroups.isEmpty()) {
                                Text("No study groups found. Go to Study Hub to create one first!", color = IndigoAccent, fontSize = 12.sp)
                            } else {
                                studyGroups.forEach { group ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SlateCard, RoundedCornerShape(12.dp))
                                            .border(1.dp, SlateBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.postSharedNoteToStudyGroup(
                                                    groupId = group.id,
                                                    title = "Syllabus Notes: " + if (rawNotes.length > 30) (rawNotes.take(30) + "...") else rawNotes,
                                                    summary = analysis!!.summary,
                                                    flashcardsJson = JsonHelper.toJson(analysis!!.flashcards)
                                                )
                                                showShareDialog = false
                                                onShareToGroupClicked() // redirects/navigates them to groups tab!
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(group.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Text(group.description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showShareDialog = false }) {
                            Text("Cancel", color = IndigoAccent)
                        }
                    },
                    containerColor = SlateDark,
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showShareDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = DarkPurple, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share with Study Group Hub", color = DarkPurple, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- SCREEN 5: STUDY ROADMAP PLANNER ---

@Composable
fun StudyPlanScreen(viewModel: ProfessorViewModel) {
    var userGoal by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("14") }
    val isGenerating by viewModel.isGeneratingStudyPlan.collectAsState()
    val activePlanEntity by viewModel.latestStudyPlanState.collectAsState()

    val planRoadmap = activePlanEntity?.let {
        JsonHelper.fromJson<StudyPlanRoadmap>(it.roadmapJson)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Planner banner
        Text(
            text = "📅 Smart Study Roadmap Planner",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Enter your learning goal (e.g. 'Build a React dashboard', 'Master calculus bounds') and Professor AI constructs a structured daily research syllabus checklist.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        if (planRoadmap == null) {
            Text("Create Professional Roadmap Blueprint:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            OutlinedTextField(
                value = userGoal,
                onValueChange = { userGoal = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("E.g., Learn Convolutional Neural Networks in 30 days...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.5f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SlateBorder
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("7", "14", "30").forEach { dayOption ->
                    val isDaySelected = targetDays == dayOption
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isDaySelected) IndigoAccent else SlateCard, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isDaySelected) Color.White else SlateBorder, RoundedCornerShape(10.dp))
                            .clickable { targetDays = dayOption }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$dayOption Days Plan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    if (userGoal.isNotBlank()) {
                        viewModel.generateStudyPlan(userGoal, targetDays.toIntOrNull() ?: 14)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                enabled = !isGenerating && userGoal.isNotBlank()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Generate Custom Academic Roadmap", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Display generated Study Roadmap items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Roadmap Schedule",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )
                    Text(
                        text = "Goal: ${planRoadmap.goal}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Button(
                    onClick = { activePlanEntity?.let { viewModel.deleteStudyPlan(it.id) } },
                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontSize = 12.sp, color = Color.White)
                }
            }

            // Checklist of daily tasks
            planRoadmap.roadmap.forEachIndexed { idx, stage ->
                var isDone by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDone) TealAccent.copy(alpha = 0.08f) else SlateCard, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (isDone) TealAccent else SlateBorder.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { isDone = !isDone }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isDone) TealAccent else SlateDark,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(1.dp, SlateBorder, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stage.title,
                                color = if (isDone) Color.White.copy(alpha = 0.6f) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stage.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateDark.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, "Exercise", tint = IndigoAccent, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Practice task: ${stage.practiceTask}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 6: USER PROFILE OFFICE ---

@Composable
fun ProfileScreen(viewModel: ProfessorViewModel) {
    val user by viewModel.userState.collectAsState()
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("Beginner") }

    LaunchedEffect(user) {
        user?.let {
            editName = it.name
            editEmail = it.email
            selectedLevel = it.level
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("💾 User Academic Record Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_profile_avatar),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5f.dp, IndigoAccent, RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user?.name ?: "Academic Student", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Role: Active Student Scholar", fontSize = 12.sp, color = TealAccent)
                Text("Member Since: June 2026", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            }
        }

        GlassCard {
            Text("Update Student Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Scholar Name:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SlateBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Verified Email Account:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            OutlinedTextField(
                value = editEmail,
                onValueChange = { editEmail = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = IndigoAccent,
                    unfocusedBorderColor = SlateBorder
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Selected Skill Difficulty level:", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
                    val isLvlSel = selectedLevel == lvl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isLvlSel) IndigoAccent else SlateDark, RoundedCornerShape(8.dp))
                            .border(1.dp, if (isLvlSel) Color.White else SlateBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedLevel = lvl }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lvl, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.updateUserProfile(editName, editEmail, selectedLevel) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
            ) {
                Text("Save Profile Records", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- ACADEMIC MARKDOWN CONTEXT PARSER HEADER RENDERING ---

@Composable
fun AcademicFormattedText(text: String) {
    val lines = text.split("\n")
    Column {
        lines.forEach { line ->
            when {
                line.startsWith("### ") -> {
                    val contentText = line.removePrefix("### ").trim()
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = contentText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val contentText = line.removePrefix("- ").removePrefix("* ").trim()
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp)) {
                        Text("•", color = IndigoAccent, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = contentText, color = Color.White, fontSize = 13.sp)
                    }
                }
                line.startsWith("```") && line.endsWith("```") && line.length > 5 -> {
                    val codeContent = line.removePrefix("```").removeSuffix("```").trim()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateDark, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(text = codeContent, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TealAccent)
                    }
                }
                line.contains("`") -> {
                    // Quick monospace highlight rendering inline fallbacks
                    Text(text = line, color = Color.White, fontSize = 13.sp)
                }
                line.isNotBlank() -> {
                    Text(
                        text = line,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.5.dp)
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun StudyGroupsScreen(viewModel: ProfessorViewModel) {
    val groups by viewModel.allStudyGroupsState.collectAsState()
    val selectedGroup by viewModel.selectedStudyGroup.collectAsState()
    val userState by viewModel.userState.collectAsState()
    val isModerating by viewModel.isModerating.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }
    var newGroupSubject by remember { mutableStateOf("ai_ml") }

    val myName = userState?.name ?: "Student"

    if (selectedGroup == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "👥 Research & Study Hub",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Join study squads moderated by Professor AI.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("New Group", color = SlateDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Group List Block
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No study cohorts available yet.", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                Text(
                    text = "Active Cohorts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                groups.forEach { group ->
                    val members = JsonHelper.fromJsonStringList(group.membersJson)
                    val isMember = members.contains(myName)

                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (group.subjectId) {
                                                    "ai_ml" -> IndigoAccent.copy(alpha = 0.2f)
                                                    "cybersecurity" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> TealAccent.copy(alpha = 0.2f)
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (group.subjectId) {
                                                "ai_ml" -> "AI / ML"
                                                "programming" -> "Code"
                                                "cybersecurity" -> "Cyber"
                                                "math" -> "Math"
                                                "physics" -> "Physics"
                                                else -> "General"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (group.subjectId) {
                                                "ai_ml" -> IndigoAccent
                                                "cybersecurity" -> Color.Red
                                                else -> TealAccent
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = group.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = group.description,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = "Members", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${members.size} members", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Shared resources", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val notes = JsonHelper.fromJsonSharedNotesList(group.sharedNotesJson)
                                        Text("${notes.size} shared documents", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            if (isMember) {
                                Button(
                                    onClick = { viewModel.selectStudyGroup(group.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Enter Room", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.joinStudyGroup(group.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SlateBorder.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Join Group", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Create Research Group", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                label = { Text("Group Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = IndigoAccent,
                                    unfocusedBorderColor = SlateBorder
                                )
                            )
                            OutlinedTextField(
                                value = newGroupDesc,
                                onValueChange = { newGroupDesc = it },
                                label = { Text("Goal / Syllabus Area") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = IndigoAccent,
                                    unfocusedBorderColor = SlateBorder
                                )
                            )
                            Text("Academic Subject:", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Pair("ai_ml", "AI / ML"),
                                    Pair("programming", "Programming"),
                                    Pair("cybersecurity", "Cybersecurity"),
                                    Pair("math", "Mathematics"),
                                    Pair("physics", "Physics")
                                ).forEach { (id, label) ->
                                    val isSel = newGroupSubject == id
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSel) IndigoAccent else SlateCard, RoundedCornerShape(8.dp))
                                            .clickable { newGroupSubject = id }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(label, color = if (isSel) DarkPurple else Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    viewModel.createStudyGroup(newGroupName, newGroupDesc, newGroupSubject)
                                    newGroupName = ""
                                    newGroupDesc = ""
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                        ) {
                            Text("Launch Cohort", color = DarkPurple, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    },
                    containerColor = SlateDark,
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
    } else {
        // OPENED STUDY ROOM SCREEN
        val group = selectedGroup!!
        val members = JsonHelper.fromJsonStringList(group.membersJson)
        val messages = JsonHelper.fromJsonGroupMessageList(group.messagesJson)
        val sharedNotes = JsonHelper.fromJsonSharedNotesList(group.sharedNotesJson)

        var chatTxt by remember { mutableStateOf("") }
        var activeRoomTab by remember { mutableStateOf("discussion") } // discussion or materials
        var selectedSharedNote by remember { mutableStateOf<SharedNotes?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.leaveStudyGroupSelection() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${members.size} connected: " + members.joinToString(", "),
                        fontSize = 11.sp,
                        color = TealAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .background(IndigoAccent.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (group.subjectId) {
                            "ai_ml" -> "AI / ML"
                            "programming" -> "Code"
                            "cybersecurity" -> "Cyber"
                            "math" -> "Math"
                            "physics" -> "Physics"
                            else -> "General"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Tab selection (Discussion / Shared Materials)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateCard, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Pair("discussion", "💬 Cohort Board"),
                    Pair("materials", "📄 Shared Sheets")
                ).forEach { (tabId, label) ->
                    val isSel = activeRoomTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isSel) IndigoAccent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                activeRoomTab = tabId
                                selectedSharedNote = null // reset document viewer
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) DarkPurple else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeRoomTab == "discussion") {
                // CHAT LAYOUT
                Column(modifier = Modifier.weight(1f)) {
                    // Call Debate or Moderator Prompt Indicator at Top
                    if (group.isDebateModeActive) {
                        // --- DEBATE MODE ACTIVE BANNER ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(DarkPurple, SlateCard)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(1.5.dp, TealAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "LIVE DEBATE",
                                            color = Color.Red,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚔️ Academic Debate Arena",
                                        color = TealAccent,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Text(
                                    text = "Moderated by HSB Professor AI",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = group.debateTopic,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rules: " + group.debateTaskDescription,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.requestProfessorDebateModeration(group.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    enabled = !isModerating
                                ) {
                                    if (isModerating) {
                                        CircularProgressIndicator(color = SlateDark, modifier = Modifier.size(16.dp))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Assess",
                                                tint = SlateDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Assess Arguments",
                                                color = SlateDark,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = { viewModel.concludeDebateMode(group.id) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    enabled = !isModerating
                                ) {
                                    if (isModerating) {
                                        CircularProgressIndicator(color = DarkPurple, modifier = Modifier.size(16.dp))
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Conclude",
                                                tint = DarkPurple,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Deliver Verdict",
                                                color = DarkPurple,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- STANDARD CHAT MODULE + START DEBATE BUTTON ---
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkPurple.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .border(1.dp, IndigoAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🎓 Want AI Professor feedback?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Invite dynamic moderation summaries or ask direct Q&A.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                                Button(
                                    onClick = { viewModel.requestProfessorModeration(group.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isModerating
                                ) {
                                    if (isModerating) {
                                        CircularProgressIndicator(color = DarkPurple, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text("Call Professor", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkPurple)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SlateCard, RoundedCornerShape(12.dp))
                                    .border(1.dp, TealAccent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!isModerating) {
                                            viewModel.startDebateMode(group.id)
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("⚔️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Launch Academic Debate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealAccent)
                                        Text("Host rigorous, scholastic debate moderated by Professor.", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(TealAccent.copy(alpha = 0.15f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isModerating) {
                                        CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(14.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Start Debate",
                                            tint = TealAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chat messages list
                    Box(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberLazyListState()
                        // Scroll to bottom whenever messages list changes size
                        LaunchedEffect(messages.size) {
                            if (messages.isNotEmpty()) {
                                scrollState.animateScrollToItem(messages.size - 1)
                            }
                        }

                        LazyColumn(
                            state = scrollState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isProf = msg.role == "professor"
                                val isMe = msg.senderName == myName
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isProf) Arrangement.Start else if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth(if (isProf) 0.9f else 0.85f)
                                            .background(
                                                color = if (isProf) DarkPurple else if (isMe) IndigoAccent.copy(alpha = 0.15f) else SlateCard,
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isProf) 0.dp else 16.dp,
                                                    bottomEnd = if (isMe) 0.dp else 16.dp
                                                )
                                            )
                                            .border(
                                                0.5.dp,
                                                if (isProf) IndigoAccent else if (isMe) IndigoAccent.copy(alpha = 0.5f) else SlateBorder.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isProf) 0.dp else 16.dp,
                                                    bottomEnd = if (isMe) 0.dp else 16.dp
                                                )
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isProf) "🏆 AI PROFESSOR MODERATOR" else msg.senderName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isProf) TealAccent else if (isMe) IndigoAccent else Color.White.copy(alpha = 0.8f)
                                            )
                                            Text(
                                               text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(msg.timestamp)),
                                               fontSize = 9.sp,
                                               color = Color.White.copy(alpha = 0.4f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (isProf) {
                                            AcademicFormattedText(msg.text)
                                        } else {
                                            Text(msg.text, fontSize = 13.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text input editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatTxt,
                            onValueChange = { chatTxt = it },
                            placeholder = { Text("Ask study buddies or tag @professor...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.weight(1f),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IndigoAccent,
                                unfocusedBorderColor = SlateBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        IconButton(
                            onClick = {
                                if (chatTxt.isNotBlank()) {
                                    viewModel.sendMessageToStudyGroup(group.id, chatTxt)
                                    chatTxt = ""
                                }
                            },
                            modifier = Modifier
                                .background(IndigoAccent, RoundedCornerShape(12.dp))
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send Message", tint = DarkPurple)
                        }
                    }
                }
            } else {
                // SHARED STUDY SHEETS PORTION
                if (selectedSharedNote == null) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Syllabus Notes Shared by Buddies", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("Study real summaries & revision cards compiled dynamically during other individual revision cycles.", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))

                        if (sharedNotes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No shared sheets found in this cohort yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        } else {
                            sharedNotes.forEach { sheet ->
                                GlassCard(
                                    onClick = { selectedSharedNote = sheet }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sheet.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Shared by ${sheet.senderName}", fontSize = 11.sp, color = TealAccent)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(sheet.summary, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Read Model", tint = IndigoAccent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // DISPLAY SPECIFIC DOCUMENT PREVIEWER
                    val sheet = selectedSharedNote!!
                    val flashcards: List<Flashcard> = try {
                        if (sheet.flashcardsJson.isNotBlank()) {
                            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Flashcard::class.java)
                            JsonHelper.moshi.adapter<List<Flashcard>>(type).fromJson(sheet.flashcardsJson) ?: emptyList()
                        } else emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reading Shared Notes", fontWeight = FontWeight.Bold, color = TealAccent, fontSize = 14.sp)
                            Button(
                                onClick = { selectedSharedNote = null },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Close Document", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        GlassCard {
                            Text(sheet.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Posted by: ${sheet.senderName}", fontSize = 11.sp, color = TealAccent)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(sheet.summary, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
                        }

                        if (flashcards.isNotEmpty()) {
                            Text("Flippable Revision Cards (${flashcards.size}):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            var flippedIndex by remember { mutableStateOf<Int?>(null) }
                            flashcards.forEachIndexed { i, card ->
                                val flip = flippedIndex == i
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(if (flip) TealAccent.copy(alpha = 0.15f) else SlateCard, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (flip) TealAccent else SlateBorder, RoundedCornerShape(12.dp))
                                        .clickable { flippedIndex = if (flip) null else i }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (flip) "REVISION ANSWER" else "STUDY POINT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (flip) TealAccent else IndigoAccent)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(if (flip) card.answer else card.question, fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
