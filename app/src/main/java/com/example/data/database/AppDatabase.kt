package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        SubjectProgressEntity::class,
        LessonSessionEntity::class,
        QuizResultEntity::class,
        StudyPlanEntity::class,
        StudyGroupEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectProgressDao(): SubjectProgressDao
    abstract fun lessonSessionDao(): LessonSessionDao
    abstract fun quizResultDao(): QuizResultDao
    abstract fun studyPlanDao(): StudyPlanDao
    abstract fun studyGroupDao(): StudyGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hsb_professor_ai_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
