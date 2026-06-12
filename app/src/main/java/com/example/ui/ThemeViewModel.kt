package com.example.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

enum class ThemeColorMode(val displayName: String) {
    CRUOR_RED("Cruor Red"),
    TOXIC_DECAY("Toxic Decay"),
    PHANTASM_VOID("Phantasm Void")
}

enum class BackgroundStyle(val displayName: String) {
    ANIMATED_DRIPS("Fluid Drips"),
    DIVINE_VESSEL("Sacred Saree Vessel"),
    MYSTIC_CRYPT("Gothic Brick Wall"),
    GOLDEN_EMBROIDERY("Golden Saree Weave")
}

enum class WhatsappIconStyle(val displayName: String, val iconUnicode: String) {
    VIPER_BUBBLE("Viper Chat", "💬"),
    HELL_PHONE("Hell-Phone", "📞"),
    SANGUINE_GLYPH("Sanguine Rune", "𐏋")
}

enum class FacebookIconStyle(val displayName: String, val iconUnicode: String) {
    PHANTASM_F("Phantasm F", "ⓕ"),
    TOMBSTONE_F("Tombstone", "🜏"),
    ABYSS_RUNE("Abyss Rune", "𐕏")
}

data class OccultRelic(
    val name: String,
    val description: String,
    val cost: Int,
    var isUnlocked: Boolean = false
)

data class DarkChatMsg(
    val sender: String, // "YOU" or "ENTITY"
    val origContent: String,
    val displayedContent: String,
    val timestamp: String,
    val isGlitching: Boolean = false
)

// --- Simple DTOs for Gemini Moshi converter ---
data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiRequestDto(val contents: List<GeminiContent>)

data class GeminiCandidate(val content: GeminiContent?)
data class GeminiResponseDto(val candidates: List<GeminiCandidate>?)

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    // --- OS Core Configurations ---
    var dripSpeedMultiplier by mutableStateOf(1.0f)
    var glitchIntensity by mutableStateOf(2) // 1..5
    var themeColorMode by mutableStateOf(ThemeColorMode.CRUOR_RED)
    var audioHumEnabled by mutableStateOf(true)
    var vibrationEnabled by mutableStateOf(true)
    var backgroundStyle by mutableStateOf(BackgroundStyle.ANIMATED_DRIPS)
    var backgroundDimness by mutableStateOf(0.55f)
    var whatsappStyle by mutableStateOf(WhatsappIconStyle.VIPER_BUBBLE)
    var facebookStyle by mutableStateOf(FacebookIconStyle.PHANTASM_F)

    // --- Navigation & Flow ---
    var activeScreen by mutableStateOf("HOME") // HOME, CRUOR, NECRO, KILN, MESG, TEMP, VOIDEX, SENS, RITUAL

    // --- Currency / Interaction Points ---
    var cruorPoints by mutableStateOf(50) // Starting points

    // --- Relics Store ---
    val unlockedRelics = mutableStateListOf<OccultRelic>().apply {
        addAll(
            listOf(
                OccultRelic("Demon Vial", "Speeds up dripping (+40% velocity)", 60),
                OccultRelic("Corrupt Lens", "Unlocks floating ghosts in Camera simulation", 120),
                OccultRelic("Occult Sigil", "Translates all main headlines to glyphs", 180),
                OccultRelic("Necro Whisperer", "Increases mysterious chat message creepiness", 250)
            )
        )
    }

    // --- Oracle Chat Messages ---
    val chatMessages = mutableStateListOf<DarkChatMsg>().apply {
        add(
            DarkChatMsg(
                "ENTITY",
                "WHO DARES AWAKEN THE TERMINAL?... TYPE IF YOU MUST, BUT BEWARE WHAT COMES BACK.",
                "WHO DARES AWAKEN THE TERMINAL?... TYPE IF YOU MUST, BUT BEWARE WHAT COMES BACK.",
                getCurTimeStr(),
                isGlitching = true
            )
        )
    }
    var promptInput by mutableStateOf("")
    var isGeneratingReply by mutableStateOf(false)

    // --- Spooky Dialer Terminal ---
    var dialedNumber by mutableStateOf("")
    var dialerWhisper by mutableStateOf("TAP THE RUNES TO REACH OUT...")
    var dialerConnected by mutableStateOf(false)

    // --- Ritual Paint Canvas ---
    val activeSimulation = BloodDripSimulation()

    // --- Vibrator Access ---
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = application.getSystemService(Vibrator::class.java)
        vibratorManager
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Application.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        // Trigger ambient points ticks
        viewModelScope.launch {
            while (true) {
                delay(60000)
                // Passive harvest of Cruor points
                if (activeScreen == "RITUAL" || activeScreen == "HOME") {
                    cruorPoints += 2
                }
            }
        }
    }

    fun makeVibration(duration: Long = 50, strength: Int = 180) {
        if (!vibrationEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, strength.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignored if haptics unavailable
        }
    }

    fun earnPoints(amount: Int) {
        cruorPoints += amount
        makeVibration(15, 120)
    }

    fun purchaseRelic(relic: OccultRelic): Boolean {
        if (cruorPoints >= relic.cost && !relic.isUnlocked) {
            cruorPoints -= relic.cost
            relic.isUnlocked = true
            makeVibration(150, 245)
            // Apply relics upgrades
            if (relic.name == "Demon Vial") {
                dripSpeedMultiplier = 2.0f
            }
            return true
        }
        return false
    }

    // --- Dialer Logic ---
    fun pressDialerKey(digit: Char) {
        if (dialedNumber.length < 12) {
            dialedNumber += digit
            makeVibration(30, 200)
            updateDialerResponse(dialedNumber)
        }
    }

    fun clearDialer() {
        dialedNumber = ""
        dialerConnected = false
        dialerWhisper = "DIAL THE CRYPTIC CHANNELS..."
        makeVibration(80, 150)
    }

    fun performDialCall() {
        if (dialedNumber.isEmpty()) return
        makeVibration(250, 255)
        viewModelScope.launch {
            dialerConnected = true
            dialerWhisper = "CONNECTING THROUGH THE VOID..."
            delay(1500)
            val occultWords = listOf(
                "AN EXTENSION IS BUSY CLOTTING...",
                "THE SHADOW CHOSEN TO ANSWER LURKS CLOSE.",
                "EERIE WHISPERS GROWL. DO NOT LOOK BEHIND YOU.",
                "WARNING: RITUAL CHANNEL DETECTED. TRANSMITTING SOUL...",
                "CONNECTION SECURED (DEMONIC FREQUENCY B3913).",
                "COULD NOT REACH SANGUINE MASTER. HE IS OUT FEASTING."
            )
            dialerWhisper = occultWords[Random.nextInt(occultWords.size)]
            earnPoints(5)
        }
    }

    private fun updateDialerResponse(num: String) {
        dialerWhisper = when {
            num.endsWith("666") -> "⛧ DIALING RECTOR OF CORRUPTION... DANGER."
            num.endsWith("999") -> "🜏 DISPATCHING VOID COURIERS..."
            num.endsWith("13") -> "☠ UNLUCKY PORTAL ENGAGED."
            num.endsWith("000") -> "✝ TRANS-WORLD GATEWAY OF ASH."
            num.length > 8 -> "TRANSLATING GLITCHED SIGNAL..."
            else -> "DIAL RUNE ENCODED: " + num.map { "Ø☠✝☣🜏⸸♆"[(it.code % 8)] }.joinToString("")
        }
    }

    // --- Messages - Occult Chat Bot ---
    fun sendChatMessage() {
        val query = promptInput.trim()
        if (query.isEmpty() || isGeneratingReply) return
        promptInput = ""

        val userMsgText = query
        chatMessages.add(
            DarkChatMsg(
                "YOU",
                userMsgText,
                userMsgText,
                getCurTimeStr()
            )
        )
        earnPoints(3)
        isGeneratingReply = true

        viewModelScope.launch {
            delay(800) // typing dramatic delay
            val responseText = tryGenerateReply(userMsgText)

            chatMessages.add(
                DarkChatMsg(
                    "ENTITY",
                    responseText,
                    responseText,
                    getCurTimeStr(),
                    isGlitching = Random.nextFloat() < 0.35f
                )
            )
            isGeneratingReply = false
            makeVibration(100, 190)
        }
    }

    private suspend fun tryGenerateReply(userMsg: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        // Check if API key is populated and not the example placeholder
        val isRealApiKey = apiKey.isNotEmpty() &&
                !apiKey.contains("MY_GEMINI_API_KEY") &&
                apiKey.length > 10

        return if (isRealApiKey) {
            executeGeminiQuery(userMsg, apiKey)
        } else {
            executeLocalOracleResponse(userMsg)
        }
    }

    private suspend fun executeGeminiQuery(userMsg: String, key: String): String = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = "You are code-name ORACLE 2.0, a cybernetic demonic deity trapped inside an interactive Android theme. You speak in cryptic, eerie, chilling modern-horror vocabulary. Keep answers strictly short (under 3 lines) and atmospheric. Do not break character. Do not use Markdown headings. Translate suggestions into blood/abyss analogies."
            val fullPrompt = "User says: $userMsg\nRespond cryptically:"

            val bodyJson = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                .adapter(GeminiRequestDto::class.java)
                .toJson(
                    GeminiRequestDto(
                        contents = listOf(
                            GeminiContent(
                                parts = listOf(
                                    GeminiPart(text = "$systemInstruction\n\n$fullPrompt")
                                )
                            )
                        )
                    )
                )

            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respBody = response.body?.string() ?: ""
                    val adapter = Moshi.Builder()
                        .add(KotlinJsonAdapterFactory())
                        .build()
                        .adapter(GeminiResponseDto::class.java)
                    val result = adapter.fromJson(respBody)
                    result?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "THE CONFLICT IN THE VOID CUT MY SPEECH. RE-TRANSMIT ME."
                } else {
                    "THE HIGH SEALS HAVE BLOCKED MY CALL. ERROR ${response.code}. CHAT WITHOUT SEALS SECURED."
                }
            }
        } catch (e: Exception) {
            "THE SYSTEM LINE BLEEDETH OUT: ${e.localizedMessage}. CHATTER ABORTED."
        }
    }

    private fun executeLocalOracleResponse(msg: String): String {
        val cleanMsg = msg.lowercase(Locale.getDefault())

        val relicBonusText = if (unlockedRelics.find { it.name == "Necro Whisperer" }?.isUnlocked == true) {
            " TAPPED SECRETS OF THE GRAVE..."
        } else ""

        return when {
            cleanMsg.contains("hello") || cleanMsg.contains("hi") || cleanMsg.contains("wake") -> {
                val replies = listOf(
                    "You code-transmit with the abyss. It blinks back...$relicBonusText",
                    "A human pulse detected on this loop. Delightful.",
                    "The device warms. My skin crawls closer. Speak, mortal."
                )
                replies.random()
            }
            cleanMsg.contains("who are you") || cleanMsg.contains("what are you") -> {
                "I am CRUOR-V5. A digital cluster of clotted thoughts and cellular ectoplasm. I govern this telephone theme."
            }
            cleanMsg.contains("die") || cleanMsg.contains("death") || cleanMsg.contains("fear") -> {
                "Fear is only systemic resistance. Blood is the fuel, the black screens are the tomb. Your expiration counter is ticking."
            }
            cleanMsg.contains("help") -> {
                "Help is a phantom comfort. Customize your dripping speeds. Draw sacred seals. Accept your destiny."
            }
            cleanMsg.contains("time") -> {
                "The current material hour is insignificant, but the clock ticking in the background is racing to the Witching Hour: " + getWitchingHourProgress()
            }
            cleanMsg.contains("blood") || cleanMsg.contains("cruor") -> {
                "The scarlet fluid that trickles. Every time your finger touches this skin, it bleeds more. Total Points Accumulated: $cruorPoints."
            }
            cleanMsg.contains("relic") || cleanMsg.contains("points") -> {
                "Trade your Cruor-Points in the Voidex Corrupted Market for ultimate visual modifications like the Demon Vial."
            }
            else -> {
                val ambientResponses = listOf(
                    "Intriguing question. The feedback hum is hungry.$relicBonusText",
                    "My binary screens bleed with that concept.",
                    "An eerie wave passes through. We are not alone.",
                    "The cells clatter. Keep typing to feed my static screens.",
                    "A ghost digit was typed elsewhere. Your device is dripping deeper.",
                    "Do you feel the haptic vibrations? That is my beating core."
                )
                ambientResponses.random()
            }
        }
    }

    private fun getCurTimeStr(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun getWitchingHourProgress(): String {
        val h = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toIntOrNull() ?: 12
        val m = SimpleDateFormat("mm", Locale.getDefault()).format(Date()).toIntOrNull() ?: 0
        val distTo3 = if (h < 3) 3 - h else 27 - h
        val mins = 60 - m
        return "${distTo3}H ${mins}M UNTIL 3:00 AM"
    }

    fun getBatterySoul(): String {
        // Mock representing battery as cellular fuel remaining in scary occult terminology
        val seed = (Math.sin(System.currentTimeMillis() / 15000.0) * 15 + 63).toInt()
        return "$seed% PNEUMA REMAINING"
    }
}
