package com.example

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var ttsEngine: TextToSpeech
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room local database securely with migrations fallback
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mojiconnect_local_db"
        ).fallbackToDestructiveMigration().build()

        val repository = AppRepository(database.dao())
        val viewModel = AppViewModel(repository)

        // 2. Initialize Text-to-Speech
        ttsEngine = TextToSpeech(this, this)

        setContent {
            MyApplicationTheme {
                MojiConnectAppEntry(
                    viewModel = viewModel,
                    onSpeak = { term, language -> speakPronunciation(term, language) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
        } else {
            runOnUiThread {
                Toast.makeText(this, "Sesli Telaffuz Altyapısı yüklenemedi.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun speakPronunciation(term: String, language: String) {
        if (!isTtsInitialized) {
            Toast.makeText(this, "Ses motoru hazır değil.", Toast.LENGTH_SHORT).show()
            return
        }

        val speechLocale = if (language.lowercase().contains("kor") || language == "ko") {
            Locale.KOREAN
        } else {
            Locale.JAPANESE
        }

        val result = ttsEngine.setLanguage(speechLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Bu dil telaffuz paketi yüklü değil.", Toast.LENGTH_SHORT).show()
        }

        ttsEngine.speak(term, TextToSpeech.QUEUE_FLUSH, null, "moji_speech_id")
    }

    override fun onDestroy() {
        if (::ttsEngine.isInitialized) {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
        super.onDestroy()
    }
}

@Composable
fun MojiConnectAppEntry(
    viewModel: AppViewModel,
    onSpeak: (String, String) -> Unit
) {
    val screenState by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUserState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                is Screen.Splash -> SplashScreen()
                is Screen.Welcome -> WelcomeScreen(viewModel)
                is Screen.Login -> LoginScreen(viewModel)
                is Screen.SignUp -> SignUpScreen(viewModel)
                is Screen.MainContainer -> {
                    if (currentUser != null) {
                        MainContainerScreen(
                            viewModel = viewModel,
                            currentUser = currentUser!!,
                            onSpeak = onSpeak
                        )
                    } else {
                        WelcomeScreen(viewModel)
                    }
                }
            }
        }
    }
}

// =================== SCREENS ===================

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        CosmicDark,
                        MetallicSurface
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberViolet, CyberPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Logo Star",
                    tint = GoldMoji,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MojiConnect",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Koreliler ve Japonlarla Dil Alışverişi",
                fontSize = 14.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = Color(0xFFFF2E93),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun WelcomeScreen(viewModel: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MojiConnect",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberViolet,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = "Öğren & Sosyalleş",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MetallicSurface.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Globe Icon",
                        tint = CyberPink,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Birbirinden Uzak Kültürleri Yakınlaştırın",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "InterPals sosyal gücünü, akıllı kelime kartları ile birleştirdik. Korece ve Japonca öğrenenlerin ideal buluşma noktası.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(Screen.Login) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("welcome_login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.navigateTo(Screen.SignUp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("welcome_signup_button"),
                    border = BorderStroke(1.5.dp, CyberPink),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberPink),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Yeni Hesap Oluştur", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp)
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.navigateTo(Screen.Welcome) }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Geri git", color = Color.White, fontSize = 16.sp)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Güvenli Giriş",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "MojiConnect hesabınızla giriş yaparak dil ortaklarınızla konuşmaya devam edin.",
                    fontSize = 14.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-posta Adresi") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberViolet,
                        unfocusedBorderColor = TextMuted
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parola") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberViolet,
                        unfocusedBorderColor = TextMuted
                    ),
                    singleLine = true
                )

                Text(
                    text = "İpucu: Test etmek için 'adem@gmail.com' yazıp direkt giriş yapabilirsiniz.",
                    fontSize = 12.sp,
                    color = CyberPink,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        email = "adem@gmail.com"
                        password = "password123"
                    }
                    viewModel.performLogin(email)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Giriş Yap", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SignUpScreen(viewModel: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("İstanbul") }
    
    var nativeLanguage by remember { mutableStateOf("Türkçe") }
    val nativeList = listOf("Türkçe", "Korece", "Japonca")
    var nativeExpanded by remember { mutableStateOf(false) }

    var learningLanguage by remember { mutableStateOf("Korece") }
    val learningList = listOf("Korece", "Japonca", "Türkçe")
    var learningExpanded by remember { mutableStateOf(false) }

    var languageLevel by remember { mutableStateOf("A2 - Temel Seviye") }
    val levelList = listOf(
        "A1 - Başlangıç Seviye",
        "A2 - Temel Seviye",
        "B1 - Orta Seviye",
        "B2 - İyi Seviye",
        "C1 - İleri Seviye"
    )
    var levelExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(Screen.Welcome) }
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "Geri git", color = Color.White, fontSize = 16.sp)
            }

            Text(
                text = "Kaydol ve Başla 🚀",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ad Soyad") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberViolet
                        )
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-posta") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberViolet
                        )
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = age,
                            onValueChange = { age = it },
                            label = { Text("Yaş") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("signup_age_input"),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberViolet
                            )
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Konum (Şehir)") },
                            modifier = Modifier
                                .weight(2.5f)
                                .testTag("signup_location_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyberViolet
                            )
                        )
                    }
                }

                item {
                    Column {
                        Text(text = "Ana Diliniz", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box {
                            Button(
                                onClick = { nativeExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicSurface),
                                border = BorderStroke(1.dp, CyberViolet.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = nativeLanguage, color = Color.White)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = nativeExpanded,
                                onDismissRequest = { nativeExpanded = false },
                                modifier = Modifier.background(MetallicSurface)
                            ) {
                                nativeList.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(text = language, color = Color.White) },
                                        onClick = {
                                            nativeLanguage = language
                                            nativeExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text(text = "Öğrenilen Dil", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box {
                            Button(
                                onClick = { learningExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicSurface),
                                border = BorderStroke(1.dp, CyberPink.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = learningLanguage, color = Color.White)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = learningExpanded,
                                onDismissRequest = { learningExpanded = false },
                                modifier = Modifier.background(MetallicSurface)
                            ) {
                                learningList.forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(text = language, color = Color.White) },
                                        onClick = {
                                            learningLanguage = language
                                            learningExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text(text = "Dil Seviyeniz", color = TextMuted, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                        Box {
                            Button(
                                onClick = { levelExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MetallicSurface),
                                border = BorderStroke(1.dp, GoldMoji.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = languageLevel, color = Color.White)
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            }
                            DropdownMenu(
                                expanded = levelExpanded,
                                onDismissRequest = { levelExpanded = false },
                                modifier = Modifier.background(MetallicSurface)
                            ) {
                                levelList.forEach { lvl ->
                                    DropdownMenuItem(
                                        text = { Text(text = lvl, color = Color.White) },
                                        onClick = {
                                            languageLevel = lvl
                                            levelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val finalName = if (name.isBlank()) "Gezgin Öğrenici" else name
                    val finalAge = age.toIntOrNull() ?: 22
                    val finalEmail = if (email.isBlank()) "new_user@mojiconnect.app" else email

                    viewModel.performSignUp(
                        name = finalName,
                        email = finalEmail,
                        nativeLang = nativeLanguage,
                        learningLang = learningLanguage,
                        level = languageLevel,
                        age = finalAge,
                        location = location
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("signup_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Hesabı Aç ve Keşfet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================= CONTAINER FRAME =================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainContainerScreen(
    viewModel: AppViewModel,
    currentUser: CurrentUser,
    onSpeak: (String, String) -> Unit
) {
    val activeTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val justMatchedProfile by viewModel.justMatchedProfile.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CosmicDark,
        bottomBar = {
            MojiNavigationBar(
                currentTab = activeTab,
                onTabSelected = { viewModel.switchTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                MainTab.SWIPE -> SwipeDiscoverScreen(viewModel, currentUser)
                MainTab.CHATS -> ChatListScreen(viewModel, onSpeak)
                MainTab.FLASHCARDS -> FlashcardDeckScreen(viewModel, onSpeak)
                MainTab.PROFILE -> ProfileSummaryScreen(viewModel, currentUser)
            }

            if (justMatchedProfile != null) {
                MojiMatchDialog(
                    profile = justMatchedProfile!!,
                    onStartChat = {
                        viewModel.clearJustMatchedPopup()
                        viewModel.openChat(justMatchedProfile!!)
                    },
                    onDismiss = { viewModel.clearJustMatchedPopup() }
                )
            }
        }
    }
}

@Composable
fun MojiNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar(
        containerColor = MetallicSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.SWIPE,
            onClick = { onTabSelected(MainTab.SWIPE) },
            icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = "Eşleşme") },
            label = { Text("Keşfet", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberPink,
                selectedTextColor = CyberPink,
                indicatorColor = CyberViolet.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
 
        NavigationBarItem(
            selected = currentTab == MainTab.CHATS,
            onClick = { onTabSelected(MainTab.CHATS) },
            icon = { Icon(imageVector = Icons.Default.MailOutline, contentDescription = "Sohbet") },
            label = { Text("Sohbetler", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberViolet,
                selectedTextColor = CyberViolet,
                indicatorColor = CyberViolet.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
 
        NavigationBarItem(
            selected = currentTab == MainTab.FLASHCARDS,
            onClick = { onTabSelected(MainTab.FLASHCARDS) },
            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = "Kelimeler") },
            label = { Text("Moji Kartı", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GoldMoji,
                selectedTextColor = GoldMoji,
                indicatorColor = CyberViolet.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
 
        NavigationBarItem(
            selected = currentTab == MainTab.PROFILE,
            onClick = { onTabSelected(MainTab.PROFILE) },
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profilin", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TextLight,
                selectedTextColor = TextLight,
                indicatorColor = CyberViolet.copy(alpha = 0.15f),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}

// ================= TAB - SWIPE (DISCOVER) =================

@Composable
fun SwipeDiscoverScreen(
    viewModel: AppViewModel,
    currentUser: CurrentUser
) {
    val discoveryQueue by viewModel.discoveryQueue.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MojiConnect",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Eşleşme Kartları Akışı",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberPink.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Öğreniyor: ${currentUser.learningLanguage}",
                    color = CyberPink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (discoveryQueue.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MetallicSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Empty Deck",
                            tint = CyberViolet,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tüm canlar eşleşti! 🎉",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bölgenizdeki veya seçtiğiniz hedef dildeki tüm adayları incelediniz. En son eşleştiğiniz kişilerle konuşmaya gidin ya da profil ayarlarından öğrenme dilini güncelleyin.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val topCandidate = discoveryQueue.first()

            val isDirectReciprocal = topCandidate.nativeLanguage == currentUser.learningLanguage &&
                    topCandidate.learningLanguage == currentUser.nativeLanguage

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MetallicSurface),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(
                        width = if (isDirectReciprocal) 2.dp else 1.dp,
                        color = if (isDirectReciprocal) GoldMoji else CyberViolet.copy(alpha = 0.3f)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        if (isDirectReciprocal) {
                                            listOf(
                                                Color(0xFF26123D),
                                                Color(0xFF130920),
                                                CosmicDark
                                            )
                                        } else {
                                            listOf(
                                                MetallicSurface,
                                                MetallicSurface.copy(alpha = 0.8f),
                                                CosmicDark
                                            )
                                        }
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isDirectReciprocal) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(GoldMoji)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Reciprocal",
                                                tint = MetallicSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tam Dil Eşleşmesi Önceliği",
                                                color = MetallicSurface,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(32.dp))
                                            .background(CyberViolet.copy(alpha = 0.2f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Aday Partner",
                                            color = TextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Location",
                                        tint = CyberPink,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = topCandidate.location, color = Color.White, fontSize = 11.sp)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .align(Alignment.CenterHorizontally)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CyberViolet, CyberPink)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!topCandidate.imageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = topCandidate.imageUrl,
                                        contentDescription = "${topCandidate.name} profile",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    val initials = topCandidate.name.take(2).uppercase()
                                    Text(
                                        text = initials,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(24.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = topCandidate.name,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "${topCandidate.age} yaşında",
                                        fontSize = 14.sp,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MojiChip(
                                        label = "Ana Dili: ${topCandidate.nativeLanguage}",
                                        color = CyberViolet
                                    )
                                    MojiChip(
                                        label = "Öğreniyor: ${topCandidate.learningLanguage}",
                                        color = CyberPink
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Seviye: ${topCandidate.languageLevel}",
                                    color = GoldMoji,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color.White.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = topCandidate.bio,
                                    fontSize = 13.sp,
                                    color = Color(0xFFE2E2E2),
                                    lineHeight = 18.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.handleSwipeLeft(topCandidate) },
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
                            .testTag("action_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Swipe Left Pas",
                            tint = CyberPink,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.handleSwipeRight(topCandidate) },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(CyberViolet)
                            .testTag("action_like_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Swipe Right Like",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MojiChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MojiMatchDialog(
    profile: MatchProfile,
    onStartChat: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MetallicSurface),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, GoldMoji)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MojiMatch! 🎉",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = GoldMoji,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Karşılıklı dil alışverişi ortağınızı buldunuz!",
                    fontSize = 13.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-25).dp)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CyberViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Siz", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = 25.dp)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CyberPink),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile.imageUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = profile.imageUrl,
                                contentDescription = "${profile.name} match",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initials = profile.name.take(2).uppercase()
                            Text(text = initials, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "${profile.name} size Korece/Japonca pratik yaptırmak için bekliyor!",
                    fontSize = 14.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onStartChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dialog_chat_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Hemen Mesaj Yaz", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Sonra Sohbet Et", color = TextMuted, fontSize = 13.sp)
                }
            }
        }
    }
}

// ================= TAB - CHATS (MESSAGES) =================

@Composable
fun ChatListScreen(
    viewModel: AppViewModel,
    onSpeak: (String, String) -> Unit
) {
    val matchedProfiles by viewModel.matchedProfilesList.collectAsStateWithLifecycle()
    val activeChatProfile by viewModel.activeChatProfile.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = activeChatProfile != null,
        label = "ChatListToggle"
    ) { hasActiveChat ->
        if (hasActiveChat) {
            ChatArenaView(
                viewModel = viewModel,
                profile = activeChatProfile!!,
                onSpeak = onSpeak
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Mesajkutusu",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Konuşurken kelimelere tıkla ve anında Moji yap!",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (matchedProfiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MailOutline,
                                contentDescription = "No Chats",
                                tint = CyberPink.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Henüz eşleşme yok",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keşfet sayfasına dönüp adayları sağa kaydırın.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(matchedProfiles) { match ->
                            Card(
                                onClick = { viewModel.openChat(match) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_item_${match.id}"),
                                colors = CardDefaults.cardColors(containerColor = MetallicSurface.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(CyberViolet),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!match.imageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = match.imageUrl,
                                                contentDescription = "${match.name} thumbnail",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = match.name.take(2).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = match.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Seviye: ${match.languageLevel} • ${match.location}",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Open Chat",
                                        tint = CyberPink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatArenaView(
    viewModel: AppViewModel,
    profile: MatchProfile,
    onSpeak: (String, String) -> Unit
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    
    var selectedWordForExtraction by remember { mutableStateOf<String?>(null) }
    var translationInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MetallicSurface)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.switchTab(MainTab.CHATS) }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CyberPink),
                contentAlignment = Alignment.Center
            ) {
                if (!profile.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profile.imageUrl,
                        contentDescription = "${profile.name} header thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${profile.nativeLanguage} (Ana Dil) • ${profile.languageLevel}",
                    color = GoldMoji,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val isMe = message.senderId == "current_user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                )
                            )
                            .background(if (isMe) CyberViolet else MetallicSurface.copy(alpha = 0.6f))
                            .border(
                                1.dp,
                                if (isMe) Color.Transparent else CyberViolet.copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .clickable(!isMe) {
                                val cleanedText = message.text
                                    .replace("안녕하세요", "안녕하세요 (Merhaba)")
                                    .replace("こんにちは", "こんにちは (Merhaba)")
                                    .replace("감사합니다", "감사합니다 (Teşekkürler)")
                                    .replace("ありがとう", "ありがとう (Teşekkürler)")
                                    .replace("잘 지냈어요?", "잘 지냈어요? (Nasılsın?)")
                                    .replace("お元気ですか", "お元気ですか (Nasılsın?)")
                                    .replace("친구", "친구 (Arkadaş)")
                                    .replace("ともだち", "ともだち (Arkadaş)")
                                    
                                val regex = "[\\uac00-\\ud7a3\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]+".toRegex()
                                val foreignWords = regex.findAll(cleanedText).map { it.value }.toList()
                                if (foreignWords.isNotEmpty()) {
                                    val topWord = foreignWords.first()
                                    selectedWordForExtraction = topWord
                                    translationInput = getPresetTranslation(profile.nativeLanguage, topWord)
                                } else {
                                    selectedWordForExtraction = message.text
                                    translationInput = ""
                                }
                            }
                    ) {
                        Column {
                            Text(
                                text = message.text,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            
                            if (!isMe) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Moji extractor icon",
                                        tint = GoldMoji,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Moji yapmak için dokun",
                                        fontSize = 10.sp,
                                        color = GoldMoji,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MetallicSurface)
                .padding(8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Mesaj yazın...", fontSize = 14.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyberViolet,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (messageText.trim().isNotEmpty()) {
                        viewModel.userSendTextMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CyberViolet)
                    .size(48.dp)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Gönder",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (selectedWordForExtraction != null) {
        Dialog(onDismissRequest = { selectedWordForExtraction = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MetallicSurface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, GoldMoji),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Kelime Kartına Dönüştür 📝",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Konuşmadan çıkarılan bu kelimeyi Moji kartına ekleyip dilediğiniz an telaffuz edebilirsiniz.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    Button(
                        onClick = { onSpeak(selectedWordForExtraction!!, profile.nativeLanguage) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldMoji),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Pronunciation", tint = MetallicSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Telaffuzu Dinle (TTS)", color = MetallicSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = selectedWordForExtraction!!,
                        onValueChange = { selectedWordForExtraction = it },
                        label = { Text("Kelime / İfade (Korece/Japonca)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberViolet
                        )
                    )

                    OutlinedTextField(
                        value = translationInput,
                        onValueChange = { translationInput = it },
                        label = { Text("Türkçe Anlamı / Çevirisi") },
                        modifier = Modifier.fillMaxWidth().testTag("extra_translation_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyberPink
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedWordForExtraction = null },
                            modifier = Modifier.weight(1.0f),
                            border = BorderStroke(1.dp, TextMuted)
                        ) {
                            Text(text = "Vazgeç", color = TextMuted)
                        }

                        Button(
                            onClick = {
                                if (selectedWordForExtraction!!.trim().isNotEmpty() && translationInput.trim().isNotEmpty()) {
                                    viewModel.createMojiCard(
                                        term = selectedWordForExtraction!!,
                                        meaning = translationInput,
                                        language = profile.nativeLanguage
                                    )
                                    selectedWordForExtraction = null
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("extra_save_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberViolet)
                        ) {
                            Text(text = "Kelimeyi Kaydet", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Preset dictionary helper logic for quick extraction helper matching Turkish
fun getPresetTranslation(language: String, foreignTerm: String): String {
    val term = foreignTerm.trim()
    return if (language == "Korece") {
        when {
            term.contains("안녕하세요") -> "Merhaba"
            term.contains("감사합니다") -> "Teşekkürler"
            term.contains("잘 지냈어요") -> "Nasılsın? / İyi misin?"
            term.contains("친구") -> "Arkadaş"
            else -> ""
        }
    } else {
        when {
            term.contains("こんにちは") -> "Merhaba"
            term.contains("ありがとう") -> "Teşekkür ederim"
            term.contains("元気ですか") -> "Nasılsın? / İyi misin?"
            term.contains("ともだち") -> "Arkadaş"
            else -> ""
        }
    }
}

// ================= TAB - FLASHCARDS (DEK) =================

@Composable
fun FlashcardDeckScreen(
    viewModel: AppViewModel,
    onSpeak: (String, String) -> Unit
) {
    val cards by viewModel.flashcardsList.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Moji Kelime Kartlarımdan 📚",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Yabancı arkadaşlarınızla konuşurken çıkardığınız sesli kelime kartları.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Empty Deck",
                        tint = GoldMoji.copy(alpha = 0.3f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Kelime kutunuz boş",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sohbet ekranında yabancı partnerlerin gönderdiği kelimelere dokunarak ilk Moji kartınızı çıkarın!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cards) { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("flashcard_item_${card.id}"),
                        colors = CardDefaults.cardColors(containerColor = MetallicSurface.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, GoldMoji.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberViolet.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = card.language,
                                        color = CyberViolet,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { onSpeak(card.term, card.language) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldMoji.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PlayArrow,
                                            contentDescription = "Sesli Telaffuz",
                                            tint = GoldMoji,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteMojiCard(card) },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(CyberPink.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Kartı Sil",
                                            tint = CyberPink,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = card.term,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "= ${card.meaning}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyberPink
                            )
                        }
                    }
                }
            }
        }
    }
}

// ================= TAB - PROFILE (SUMMARY) =================

@Composable
fun ProfileSummaryScreen(
    viewModel: AppViewModel,
    currentUser: CurrentUser
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Profil Bilgilerim 🧑‍💻",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyberViolet, CyberPink)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentUser.name.take(2).uppercase(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${currentUser.name}, ${currentUser.age}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = CyberPink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = currentUser.location, color = TextMuted, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MetallicSurface.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileRow(label = "E-posta Adresi", value = currentUser.email, icon = Icons.Outlined.Email)
                    ProfileRow(label = "Ana Diliniz", value = currentUser.nativeLanguage, icon = Icons.Outlined.Info)
                    ProfileRow(label = "Öğrendiğiniz Dil", value = currentUser.learningLanguage, icon = Icons.Outlined.Star)
                    ProfileRow(label = "Mevcut Seviye", value = currentUser.languageLevel, icon = Icons.Outlined.CheckCircle)
                }
            }
        }

        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = CyberPink),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("profile_logout_button")
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Log out")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Oturumu Kapat", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = CyberViolet, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, color = TextMuted, fontSize = 11.sp)
            Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
