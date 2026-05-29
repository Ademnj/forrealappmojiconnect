package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CurrentUser
import com.example.data.Flashcard
import com.example.data.MatchProfile
import com.example.data.AppRepository
import com.example.data.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Splash : Screen
    object Welcome : Screen
    object Login : Screen
    object SignUp : Screen
    object MainContainer : Screen
}

enum class MainTab {
    SWIPE, CHATS, FLASHCARDS, PROFILE
}

class AppViewModel(private val repository: AppRepository) : ViewModel() {

    // Screen navigation
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Sub tabs inside MainContainer
    private val _currentTab = MutableStateFlow(MainTab.SWIPE)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Active logged in user
    val currentUserState: StateFlow<CurrentUser?> = repository.currentUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Profiles from DB
    val allProfiles: StateFlow<List<MatchProfile>> = repository.allMatchProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Matched chats
    val matchedProfilesList: StateFlow<List<MatchProfile>> = repository.matchedProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved Mojis
    val flashcardsList: StateFlow<List<Flashcard>> = repository.allFlashcardsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently selected Chat room
    private val _activeChatProfile = MutableStateFlow<MatchProfile?>(null)
    val activeChatProfile: StateFlow<MatchProfile?> = _activeChatProfile.asStateFlow()

    // Active logs details
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // Successful Match Overlay Dialog
    private val _justMatchedProfile = MutableStateFlow<MatchProfile?>(null)
    val justMatchedProfile: StateFlow<MatchProfile?> = _justMatchedProfile.asStateFlow()

    // Smart prioritizing ranking algorithm
    val discoveryQueue: StateFlow<List<MatchProfile>> = combine(
        currentUserState,
        allProfiles
    ) { user, profiles ->
        if (user == null || profiles.isEmpty()) {
            profiles.filter { it.isLiked == null }
        } else {
            profiles
                .filter { it.isLiked == null }
                .map { profile ->
                    // Priority Match Scoring Matrix
                    var score = 0
                    
                    // Case 1: Reciprocal dilution (Direct translation match)
                    // (Turkish native learning Korean matches Korean native learning Turkish)
                    val isReciprocalNativeToLearning = profile.nativeLanguage == user.learningLanguage &&
                            profile.learningLanguage == user.nativeLanguage
                    
                    if (isReciprocalNativeToLearning) {
                        score += 100
                    } else {
                        // Case 2: Standard match
                        if (profile.nativeLanguage == user.learningLanguage) score += 40
                        if (profile.learningLanguage == user.nativeLanguage) score += 20
                    }
                    
                    // Case 3: Same Learning Target (potential study buddy)
                    if (profile.learningLanguage == user.learningLanguage) {
                        score += 5
                    }
                    
                    Pair(profile, score)
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            delay(1800) // Beautiful splash timing
            val user = repository.getCurrentUser()
            if (user != null) {
                repository.seedProfilesIfEmpty()
                _currentScreen.value = Screen.MainContainer
            } else {
                _currentScreen.value = Screen.Welcome
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun switchTab(tab: MainTab) {
        _currentTab.value = tab
        if (tab != MainTab.CHATS) {
            _activeChatProfile.value = null
        }
    }

    fun performLogin(email: String) {
        viewModelScope.launch {
            // Simulated secure authentication; saves profile or defaults
            val user = CurrentUser(
                name = if (email.startsWith("adem")) "Adem" else "Kullanıcı",
                email = email,
                nativeLanguage = "Türkçe",
                learningLanguage = "Korece",
                languageLevel = "A2 - Başlangıç Seviye",
                age = 23,
                location = "İstanbul"
            )
            repository.registerUser(user)
            _currentScreen.value = Screen.MainContainer
        }
    }

    fun performSignUp(
        name: String,
        email: String,
        nativeLang: String,
        learningLang: String,
        level: String,
        age: Int,
        location: String
    ) {
        viewModelScope.launch {
            val user = CurrentUser(
                name = name,
                email = email,
                nativeLanguage = nativeLang,
                learningLanguage = learningLang,
                languageLevel = level,
                age = age,
                location = location
            )
            repository.registerUser(user)
            _currentScreen.value = Screen.MainContainer
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _currentScreen.value = Screen.Welcome
        }
    }

    // Swipe card actions
    fun handleSwipeLeft(profile: MatchProfile) {
        viewModelScope.launch {
            val updated = profile.copy(isLiked = false)
            repository.updateMatchProfile(updated)
        }
    }

    fun handleSwipeRight(profile: MatchProfile) {
        viewModelScope.launch {
            // Since user learning language fits their native, we establish a Reciprocal match outcome!
            val updated = profile.copy(isLiked = true, isMatched = true)
            repository.updateMatchProfile(updated)
            
            // Trigger overlay
            _justMatchedProfile.value = updated
            
            // Seed welcome chat instructions
            val firstMsgText = if (updated.nativeLanguage == "Korece") {
                "안녕하세요! 🌟 Türkçe öğreniyorum. Senin da Korece öğrendiğini gördüm. Tanıştığımıza memnun oldum! Mesajlardaki kelimelerin üzerine dokunarak anında 'Kelime Kartı' oluşturabilirsin!"
            } else {
                "こんにちは! 🌸 Türkçe pratik yapmak istiyorum. Seninle Japonca çalışabiliriz! Konuşmamızdaki kelimelere tıklayıp kartlarına kaydedebilirsin."
            }
            repository.sendMessage(
                profileId = updated.id,
                senderId = "match_user",
                text = firstMsgText
            )
        }
    }

    fun clearJustMatchedPopup() {
        _justMatchedProfile.value = null
    }

    // Open active chat dialog
    fun openChat(profile: MatchProfile) {
        _activeChatProfile.value = profile
        _currentTab.value = MainTab.CHATS
        
        // Listen to messages
        viewModelScope.launch {
            repository.getMessagesForProfile(profile.id).collect { list ->
                _chatMessages.value = list
            }
        }
    }

    // User sends chat text
    fun userSendTextMessage(text: String) {
        val currentProfile = _activeChatProfile.value ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            // 1. Save user text
            repository.sendMessage(
                profileId = currentProfile.id,
                senderId = "current_user",
                text = text
            )

            // 2. Schedule smart, responsive learning replies
            delay(1200)
            val replyText = generateBotReply(currentProfile, text)
            repository.sendMessage(
                profileId = currentProfile.id,
                senderId = "match_user",
                text = replyText
            )
        }
    }

    private fun generateBotReply(profile: MatchProfile, userText: String): String {
        val normalizedText = userText.lowercase()
        return if (profile.nativeLanguage == "Korece") {
            when {
                normalizedText.contains("selam") || normalizedText.contains("merhaba") -> {
                    "Merhaba! Korece'de selamlaşmak için '안녕하세요' (An-nyeong-ha-se-yo) deriz. Bu kelimeye tıklayıp öğrenebilirsin!"
                }
                normalizedText.contains("teşekkür") || normalizedText.contains("sagol") -> {
                    "Rica ederim! Korece '감사합니다' (Gam-sa-ham-ni-da) teşekkürler demektir. Üzerine dokunup hemen Moji kartına kaydet!"
                }
                normalizedText.contains("nasılsın") -> {
                    "İyiyim, sen nasılsın? Korece '잘 지냈어요?' (Jal ji-nae-sseo-yo?) nasılsın anlamına gelir. Bu faydalı cümleyi de kartlarına mutlaka al."
                }
                else -> {
                    "Harika gidiyoruz! Şu kelimeyi bilmek çok işine yarayacak: '친구' (Chin-gu) arkadaş demektir. Hemen kartlarına ekle! 😊"
                }
            }
        } else { // Japonca
            when {
                normalizedText.contains("selam") || normalizedText.contains("merhaba") -> {
                    "Selam! Japonca'da merhaba demek için 'こんにちは' (Konnichiwa) ifadesini kullanırız. Üzerine tıklayıp Moji oluştur!"
                }
                normalizedText.contains("teşekkür") || normalizedText.contains("sagol") -> {
                    "Harika! Japonca teşekkür etmek 'ありがとう' (Arigatou) demektir. Tek tıkla kelime kartı yap!"
                }
                normalizedText.contains("nasılsın") -> {
                    "Çok iyiyim! Japonca nasılsın demek için 'お元気ですか' (O-genki desu ka) deriz. Bu cümleyi de kartı yapmayı unutma."
                }
                else -> {
                    "Birlikte harika büyüyoruz! Japon kültüründe 'ともだち' (Tomodachi) arkadaş demektir. Üzerine tıklayıp hemen sesli kart yap!"
                }
            }
        }
    }

    // Convert chat message to vocabulary Moji card
    fun createMojiCard(term: String, meaning: String, language: String) {
        viewModelScope.launch {
            val card = Flashcard(
                term = term.trim(),
                meaning = meaning.trim(),
                language = language
            )
            repository.saveFlashcard(card)
        }
    }

    fun deleteMojiCard(card: Flashcard) {
        viewModelScope.launch {
            repository.deleteFlashcard(card)
        }
    }
}
