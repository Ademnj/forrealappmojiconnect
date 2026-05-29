package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Current User Queries
    @Query("SELECT * FROM current_user WHERE id = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<CurrentUser?>

    @Query("SELECT * FROM current_user WHERE id = 1 LIMIT 1")
    suspend fun getCurrentUser(): CurrentUser?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrentUser(user: CurrentUser)

    @Query("DELETE FROM current_user")
    suspend fun clearCurrentUser()

    // Match Discovery Profiles
    @Query("SELECT * FROM match_profiles")
    fun getAllMatchProfilesFlow(): Flow<List<MatchProfile>>

    @Query("SELECT * FROM match_profiles WHERE isMatched = 1")
    fun getMatchedProfilesFlow(): Flow<List<MatchProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchProfiles(profiles: List<MatchProfile>)

    @Update
    suspend fun updateMatchProfile(profile: MatchProfile)

    @Query("SELECT * FROM match_profiles WHERE id = :id LIMIT 1")
    suspend fun getMatchProfileById(id: Int): MatchProfile?

    // Real-time local chat logs
    @Query("SELECT * FROM chat_messages WHERE matchProfileId = :profileId ORDER BY timestamp ASC")
    fun getMessagesForProfileFlow(profileId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    // Saved Flashcards (Mojis)
    @Query("SELECT * FROM flashcards ORDER BY timestamp DESC")
    fun getAllFlashcardsFlow(): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)
}
