package com.example.data.api

import com.example.BuildConfig
import com.example.data.model.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response structures (using Moshi) ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null, // "user" or "model"
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

// --- Retrofit API Service ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val DEFAULT_MODEL = "gemini-3.5-flash"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key == "MY_GEMINI_API_KEY" || key.isEmpty()) {
            // Log warning or fallback
            ""
        } else {
            key
        }
    }

    // --- High-level functions ---

    suspend fun generateLessonResponse(
        subject: String,
        topic: String,
        userLevel: String,
        history: List<ChatMessage>,
        latestUserMessage: String,
        systemPrompt: String
    ): String {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return "Error: Gemini API key is missing. Please add it to the Secrets panel in AI Studio."

        val apiContents = mutableListOf<Content>()
        
        // Add chat history
        for (msg in history) {
            apiContents.add(
                Content(
                    role = if (msg.role == "user") "user" else "model",
                    parts = listOf(Part(text = msg.text))
                )
            )
        }

        // Add latest user query
        apiContents.add(
            Content(
                role = "user",
                parts = listOf(Part(text = latestUserMessage))
            )
        )

        val request = GeminiRequest(
            contents = apiContents,
            generationConfig = GenerationConfig(temperature = 0.7f),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(DEFAULT_MODEL, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response received from HSB Professor AI."
        } catch (e: Exception) {
            "An error occurred: ${e.message ?: "Unknown API Error"}"
        }
    }

    suspend fun generateQuiz(
        subject: String,
        topic: String,
        userLevel: String
    ): QuizData? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val prompt = """
            Generate an interactive quiz on the topic '$topic' in the subject '$subject'.
            The targeted user difficulty level is $userLevel.
            
            Return ONLY a JSON object that adheres exactly to this structure. Do not wrap it in ```json or markdown tags.
            
            {
              "questions": [
                {
                  "question": "What is ...?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 0,
                  "explanation": "Detailed explanation of why this option is correct."
                }
              ]
            }
            
            Please generate exactly 5 multiple choice questions.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert academic professor exam generator. Always export valid, parseable JSON only.")))
        )

        return try {
            val response = service.generateContent(DEFAULT_MODEL, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                JsonHelper.fromJson<QuizData>(jsonText)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateStudyPlan(
        goal: String,
        durationDays: Int
    ): StudyPlanRoadmap? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val prompt = """
            Create a detailed academic study plan to: "$goal".
            This is a $durationDays-day learning plan.
            
            Return ONLY a JSON object with this exact structure. Do not wrap it in ```json or markdown tags.
            
            {
              "goal": "$goal",
              "duration": "$durationDays days",
              "roadmap": [
                {
                  "title": "Day 1: Topic Title",
                  "description": "Short explanation of what to learn and review today.",
                  "practiceTask": "Specific practice exercise to solidify today's learning."
                }
              ]
            }
            
            Provide a daily plan of about 5 key checkpoints/milestones spanning this timeframe.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert curriculum builder and study planner AI.")))
        )

        return try {
            val response = service.generateContent(DEFAULT_MODEL, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                JsonHelper.fromJson<StudyPlanRoadmap>(jsonText)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun analyzeNotes(notesText: String): NotesAnalysis? {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return null

        val prompt = """
            Analyze the following study notes and convert them into structured materials (Summary, Key Points, Flashcards, and a Quiz).
            
            Notes:
            ${notesText.take(4000)}
            
            Return ONLY a JSON object that adheres exactly to this structure. Do not wrap it in markdown block.
            
            {
              "summary": "Deep summary of the core concepts in the notes.",
              "keyPoints": [
                "Key Point 1",
                "Key Point 2"
              ],
              "flashcards": [
                {
                  "question": "Question/Term",
                  "answer": "Answer/Definition"
                }
              ],
              "quiz": [
                {
                  "question": "Question based on notes",
                  "options": ["A", "B", "C", "D"],
                  "correctIndex": 0,
                  "explanation": "Why this is correct based on notes."
                }
              ]
            }
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.5f
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are an advanced note summarizer and lecture converter.")))
        )

        return try {
            val response = service.generateContent(DEFAULT_MODEL, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                JsonHelper.fromJson<NotesAnalysis>(jsonText)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
