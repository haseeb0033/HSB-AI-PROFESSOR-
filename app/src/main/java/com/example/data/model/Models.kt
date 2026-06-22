package com.example.data.model

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@JsonClass(generateAdapter = true)
data class QuizData(
    val questions: List<QuizQuestion>
)

@JsonClass(generateAdapter = true)
data class StudyPlanPart(
    val title: String, // e.g., "Day 1: Introduction"
    val description: String,
    val practiceTask: String
)

@JsonClass(generateAdapter = true)
data class StudyPlanRoadmap(
    val goal: String,
    val duration: String,
    val roadmap: List<StudyPlanPart>
)

@JsonClass(generateAdapter = true)
data class Flashcard(
    val question: String,
    val answer: String
)

@JsonClass(generateAdapter = true)
data class NotesAnalysis(
    val summary: String,
    val keyPoints: List<String>,
    val flashcards: List<Flashcard>,
    val quiz: List<QuizQuestion>
)

@JsonClass(generateAdapter = true)
data class SharedNotes(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val summary: String,
    val senderName: String,
    val flashcardsJson: String = "", // JSON string list of Flashcard items
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class GroupMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val role: String, // "student" or "professor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class StudyGroup(
    val id: Int,
    val name: String,
    val description: String,
    val subjectId: String,
    val members: List<String>,
    val messages: List<GroupMessage> = emptyList(),
    val sharedNotes: List<SharedNotes> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

object JsonHelper {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    inline fun <reified T> toJson(value: T): String {
        val adapter = moshi.adapter(T::class.java)
        return adapter.toJson(value)
    }

    inline fun <reified T> fromJson(json: String): T? {
        return try {
            val adapter = moshi.adapter(T::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toJsonMessageList(list: List<ChatMessage>): String {
        val type = Types.newParameterizedType(List::class.java, ChatMessage::class.java)
        val adapter = moshi.adapter<List<ChatMessage>>(type)
        return adapter.toJson(list)
    }

    fun fromJsonMessageList(json: String): List<ChatMessage> {
        return try {
            val type = Types.newParameterizedType(List::class.java, ChatMessage::class.java)
            val adapter = moshi.adapter<List<ChatMessage>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun toJsonStringList(list: List<String>): String {
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(list)
    }

    fun fromJsonStringList(json: String): List<String> {
        return try {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun toJsonGroupMessageList(list: List<GroupMessage>): String {
        val type = Types.newParameterizedType(List::class.java, GroupMessage::class.java)
        val adapter = moshi.adapter<List<GroupMessage>>(type)
        return adapter.toJson(list)
    }

    fun fromJsonGroupMessageList(json: String): List<GroupMessage> {
        return try {
            val type = Types.newParameterizedType(List::class.java, GroupMessage::class.java)
            val adapter = moshi.adapter<List<GroupMessage>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun toJsonSharedNotesList(list: List<SharedNotes>): String {
        val type = Types.newParameterizedType(List::class.java, SharedNotes::class.java)
        val adapter = moshi.adapter<List<SharedNotes>>(type)
        return adapter.toJson(list)
    }

    fun fromJsonSharedNotesList(json: String): List<SharedNotes> {
        return try {
            val type = Types.newParameterizedType(List::class.java, SharedNotes::class.java)
            val adapter = moshi.adapter<List<SharedNotes>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
