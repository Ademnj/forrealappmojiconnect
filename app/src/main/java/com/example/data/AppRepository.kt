package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val dao: AppDao) {

    val currentUserFlow: Flow<CurrentUser?> = dao.getCurrentUserFlow()
    val allMatchProfilesFlow: Flow<List<MatchProfile>> = dao.getAllMatchProfilesFlow()
    val matchedProfilesFlow: Flow<List<MatchProfile>> = dao.getMatchedProfilesFlow()
    val allFlashcardsFlow: Flow<List<Flashcard>> = dao.getAllFlashcardsFlow()

    suspend fun getCurrentUser(): CurrentUser? = dao.getCurrentUser()

    suspend fun registerUser(user: CurrentUser) {
        dao.insertCurrentUser(user)
        // Auto seed the database whenever a user signs up to make the flow immediately functional
        seedProfilesIfEmpty()
    }

    suspend fun logout() {
        dao.clearCurrentUser()
    }

    suspend fun getMessagesForProfile(profileId: Int): Flow<List<ChatMessage>> {
        return dao.getMessagesForProfileFlow(profileId)
    }

    suspend fun sendMessage(profileId: Int, senderId: String, text: String) {
        val chatMessage = ChatMessage(
            matchProfileId = profileId,
            senderId = senderId,
            text = text
        )
        dao.insertChatMessage(chatMessage)
    }

    suspend fun updateMatchProfile(profile: MatchProfile) {
        dao.updateMatchProfile(profile)
    }

    suspend fun saveFlashcard(flashcard: Flashcard) {
        dao.insertFlashcard(flashcard)
    }

    suspend fun deleteFlashcard(flashcard: Flashcard) {
        dao.deleteFlashcard(flashcard)
    }

    suspend fun seedProfilesIfEmpty() {
        // Only seed if empty
        val currentProfiles = dao.getAllMatchProfilesFlow().firstOrNull() ?: emptyList()
        if (currentProfiles.isEmpty()) {
            val list = listOf(
                MatchProfile(
                    name = "Ji-min Park (박지민)",
                    age = 21,
                    location = "Seul",
                    nativeLanguage = "Korece",
                    learningLanguage = "Türkçe",
                    languageLevel = "B1 - Orta Seviye",
                    bio = "Merhaba! Korece ve Türkçe çalışıyorum. Seul'de yaşıyorum. K-Pop ve Türk dizilerini çok severim. Birlikte pratik yapalım! 🇰🇷🇹🇷",
                    iconName = "avatar_korean_female_1",
                    imageUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=300&h=300&q=80"
                ),
                MatchProfile(
                    name = "Haruto Sato (佐藤ハルト)",
                    age = 23,
                    location = "Tokyo",
                    nativeLanguage = "Japonca",
                    learningLanguage = "Türkçe",
                    languageLevel = "A2 - Temel Seviye",
                    bio = "Tokyo'dan selamlar! Kültür alışverişi yapmak ve Türkçe becerilerimi geliştirmek istiyorum. Sana Japonca seve seve öğretebilirim! 🇯🇵🇹🇷",
                    iconName = "avatar_japanese_male_1",
                    imageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&h=300&q=80"
                ),
                MatchProfile(
                    name = "Min-ji Kim (김민지)",
                    age = 22,
                    location = "Busan",
                    nativeLanguage = "Korece",
                    learningLanguage = "Türkçe",
                    languageLevel = "A1 - Başlangıç Seviye",
                    bio = "Yeni kelimeler öğrenmeyi seviyorum. Busanlıyım. Karşılıklı dil değişimi yapacak samimi arkadaşlar arıyorum. Hadi hemen sohbet edelim! 🌸",
                    iconName = "avatar_korean_female_2",
                    imageUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=300&h=300&q=80"
                ),
                MatchProfile(
                    name = "Yuki Tanaka (田中ユキ)",
                    age = 24,
                    location = "Kyoto",
                    nativeLanguage = "Japonca",
                    learningLanguage = "Türkçe",
                    languageLevel = "B2 - İyi Seviye",
                    bio = "Kyoto'da yaşıyorum. Tarih ve gastronomiye aşırı meraklıyım. Türkçe pratik yapmak istiyorum. Saygılı ve eğlenceli sohbetler! 🍣🍵",
                    iconName = "avatar_japanese_female_1",
                    imageUrl = "https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=300&h=300&q=80"
                ),
                MatchProfile(
                    name = "Sora Takahashi (高橋ソラ)",
                    age = 25,
                    location = "Osaka",
                    nativeLanguage = "Japonca",
                    learningLanguage = "Türkçe",
                    languageLevel = "C1 - İleri Seviye",
                    bio = "Merhaba, Türkçe edebiyatına ilgi duyuyorum. Osaka'da yazılımcıyım. Türkiye'ye gitmek en büyük hayalim. Japonca pratik için yazabilirsiniz! 🗼",
                    iconName = "avatar_japanese_male_2",
                    imageUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=300&h=300&q=80"
                ),
                MatchProfile(
                    name = "Haneul Lee (이하늘)",
                    age = 20,
                    location = "Incheon",
                    nativeLanguage = "Korece",
                    learningLanguage = "Türkçe",
                    languageLevel = "A2 - Temel Seviye",
                    bio = "Sıcak kanlı biriyim! Türk arkadaşlar edinmek ve dilimi geliştirmek istiyorum. Birlikte Korece şarkı sözlerini kelime kelime çevirelim mi? 🎵🎙️",
                    iconName = "avatar_korean_male_1",
                    imageUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&h=300&q=80"
                )
            )
            dao.insertMatchProfiles(list)
        }
    }
}
