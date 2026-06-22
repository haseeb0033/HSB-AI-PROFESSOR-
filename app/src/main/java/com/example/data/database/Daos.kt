package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = 'current_user' LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = 'current_user' LIMIT 1")
    suspend fun getUserSync(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface SubjectProgressDao {
    @Query("SELECT * FROM subject_progress")
    fun getAllProgressFlow(): Flow<List<SubjectProgressEntity>>

    @Query("SELECT * FROM subject_progress WHERE subjectId = :subjectId LIMIT 1")
    suspend fun getProgressForSubject(subjectId: String): SubjectProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: SubjectProgressEntity)
}

@Dao
interface LessonSessionDao {
    @Query("SELECT * FROM lesson_sessions ORDER BY lastUpdated DESC")
    fun getAllSessionsFlow(): Flow<List<LessonSessionEntity>>

    @Query("SELECT * FROM lesson_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Int): LessonSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: LessonSessionEntity): Long

    @Query("DELETE FROM lesson_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)
}

@Dao
interface QuizResultDao {
    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllQuizResultsFlow(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity)
}

@Dao
interface StudyPlanDao {
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllStudyPlansFlow(): Flow<List<StudyPlanEntity>>

    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC LIMIT 1")
    fun getLatestStudyPlanFlow(): Flow<StudyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(plan: StudyPlanEntity)

    @Query("DELETE FROM study_plans WHERE id = :id")
    suspend fun deleteStudyPlanById(id: Int)
}

@Dao
interface StudyGroupDao {
    @Query("SELECT COUNT(*) FROM study_groups")
    suspend fun getStudyGroupsCount(): Int

    @Query("SELECT * FROM study_groups ORDER BY createdAt DESC")
    fun getAllStudyGroupsFlow(): Flow<List<StudyGroupEntity>>

    @Query("SELECT * FROM study_groups WHERE id = :id LIMIT 1")
    suspend fun getStudyGroupById(id: Int): StudyGroupEntity?

    @Query("SELECT * FROM study_groups WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getStudyGroupsForSubjectFlow(subjectId: String): Flow<List<StudyGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroup(group: StudyGroupEntity): Long

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteStudyGroupById(id: Int)
}
