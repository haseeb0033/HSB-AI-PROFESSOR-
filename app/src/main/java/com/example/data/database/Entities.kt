package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "current_user",
    val name: String,
    val email: String,
    val level: String = "Beginner", // Beginner, Intermediate, Advanced
    val streak: Int = 0,
    val lastLearnDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subject_progress")
data class SubjectProgressEntity(
    @PrimaryKey val subjectId: String, // ai_ml, programming, cybersecurity, math, physics, general
    val subjectName: String,
    val topicsCompleted: String = "", // Comma-separated or JSON list of completed topic names
    val totalScore: Int = 0,
    val lessonsCount: Int = 0
)

@Entity(tableName = "lesson_sessions")
data class LessonSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val topicName: String,
    val chatHistoryJson: String, // JSON array of Message model
    val currentLevel: String, // Beginner, Intermediate, Advanced
    val isCompleted: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: String,
    val topicName: String,
    val score: Int,
    val totalQuestions: Int,
    val quizDataJson: String, // JSON representing the quiz questions, options, explain
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_plans")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goal: String,
    val durationDays: Int,
    val roadmapJson: String, // JSON of structure plan
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_groups")
data class StudyGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val subjectId: String,
    val membersJson: String, // JSON Array of Strings
    val messagesJson: String = "[]", // JSON Array of GroupMessage
    val sharedNotesJson: String = "[]", // JSON Array of SharedNotes
    val createdAt: Long = System.currentTimeMillis(),
    val isDebateModeActive: Boolean = false,
    val debateTopic: String = "",
    val debateTaskDescription: String = "",
    val debateAcademicPrinciples: String = ""
)
