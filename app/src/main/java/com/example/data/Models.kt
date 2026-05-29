package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "current_user")
data class CurrentUser(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val email: String,
    val nativeLanguage: String,
    val learningLanguage: String,
    val languageLevel: String,
    val age: Int,
    val location: String
)

@Entity(tableName = "match_profiles")
data class MatchProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val age: Int,
    val location: String,
    val nativeLanguage: String,
    val learningLanguage: String,
    val languageLevel: String,
    val bio: String,
    val iconName: String, // String identifier for fallback vector graphics/avatars
    val imageUrl: String? = null, // Profile image URL
    val isLiked: Boolean? = null, // null = neutral, true = liked, false = passed
    val isMatched: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchProfileId: Int,
    val senderId: String, // "current_user" or "match_user"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val term: String,
    val meaning: String,
    val language: String, // "Korece" or "Japonca"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
