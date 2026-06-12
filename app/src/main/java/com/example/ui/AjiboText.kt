package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.NeonRed
import com.example.ui.theme.EerieWhite
import kotlin.random.Random

object AjiboTextEngine {
    private val ZALGO_UP = arrayOf(
        "\u030d", "\u030e", "\u0304", "\u0305", "\u033f", "\u0311", "\u0306", "\u0310",
        "\u0352", "\u0357", "\u030a", "\u0317", "\u0300", "\u0301", "\u0303", "\u0309"
    )
    private val ZALGO_DOWN = arrayOf(
        "\u0316", "\u0317", "\u0318", "\u0319", "\u031c", "\u031d", "\u031e", "\u031f",
        "\u0320", "\u0324", "\u0325", "\u0326", "\u0329", "\u032a", "\u032b", "\u032c"
    )
    private val ZALGO_MID = arrayOf(
        "\u0315", "\u031b", "\u0321", "\u0322", "\u0327", "\u0328", "\u032d", "\u032e",
        "\u0332", "\u0333", "\u0334", "\u0335", "\u0336", "\u0337", "\u0338", "\u032f"
    )

    private val OCCULT_GLYPH_MAP = mapOf(
        'A' to "⛧", 'B' to "☣", 'C' to "☽", 'D' to "𐏋", 'E' to "𐕏",
        'F' to "☥", 'G' to "𐌲", 'H' to "☠", 'I' to "†", 'J' to "𐌾",
        'K' to "𐌺", 'L' to "♆", 'M' to "♏", 'N' to "и", 'O' to "ø",
        'P' to "𐍀", 'Q' to "𐍁", 'R' to "ℜ", 'S' to "🜏", 'T' to "⸸",
        'U' to "μ", 'V' to "✓", 'W' to "𐍈", 'X' to "⛥", 'Y' to "Ψ", 'Z' to "ℤ"
    )

    fun toZalgo(text: String, intensity: Int = 2): String {
        val result = StringBuilder()
        for (char in text) {
            result.append(char)
            if (char != ' ' && char != '\n') {
                for (i in 0 until intensity) {
                    result.append(ZALGO_UP[Random.nextInt(ZALGO_UP.size)])
                    result.append(ZALGO_DOWN[Random.nextInt(ZALGO_DOWN.size)])
                    result.append(ZALGO_MID[Random.nextInt(ZALGO_MID.size)])
                }
            }
        }
        return result.toString()
    }

    fun toOccultGlow(text: String): String {
        return text.uppercase().map { char ->
            OCCULT_GLYPH_MAP[char] ?: char.toString()
        }.joinToString("")
    }

    fun generateCorruption(length: Int): String {
        val base = "Ø𐕏⛧☠☣🜏⸸♆𐏋𐌾☽⛥☥†"
        return (1..length).map { base[Random.nextInt(base.length)] }.joinToString("")
    }
}

@Composable
fun GlowText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonRed,
    glowColor: Color = BloodCrimson,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    textAlign: TextAlign = TextAlign.Start,
    isZalgo: Boolean = false,
    isOccult: Boolean = false,
    glitchIntensity: Int = 2,
) {
    val processedText = remember(text, isZalgo, isOccult, glitchIntensity) {
        var t = text
        if (isOccult) {
            t = AjiboTextEngine.toOccultGlow(t)
        }
        if (isZalgo) {
            t = AjiboTextEngine.toZalgo(t, glitchIntensity)
        }
        t
    }

    Text(
        text = processedText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        style = TextStyle(
            shadow = Shadow(
                color = glowColor,
                offset = Offset(0f, 0f),
                blurRadius = 12f
            )
        )
    )
}

@Composable
fun GlitchText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonRed,
    glowColor: Color = Color.Red.copy(alpha = 0.8f),
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = FontFamily.Monospace,
    textAlign: TextAlign = TextAlign.Start,
    speedMs: Int = 300,
) {
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(Random.nextLong(200, 800))
            tick++
        }
    }

    val glitchText = remember(text, tick) {
        if (Random.nextFloat() < 0.25f) {
            var splitted = text.toCharArray()
            val glitchCount = (text.length * 0.15).toInt().coerceAtLeast(1)
            for (i in 0 until glitchCount) {
                val idx = Random.nextInt(text.length)
                splitted[idx] = AjiboTextEngine.generateCorruption(1)[0]
            }
            String(splitted)
        } else {
            text
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "GlitchOffset")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = speedMs
                -2f at 0
                2f at 50
                -1f at 100
                3f at 150
                -3f at 200
                0f at 250
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitchX"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = speedMs
                -1f at 0
                1f at 70
                2f at 140
                -2f at 210
                0f at 280
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitchY"
    )

    val actualOffset = if (Random.nextFloat() < 0.15f) Offset(offsetX, offsetY) else Offset.Zero

    Text(
        text = glitchText,
        modifier = modifier.offset(actualOffset.x.dp, actualOffset.y.dp),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        style = TextStyle(
            shadow = Shadow(
                color = glowColor,
                offset = Offset(-offsetX, offsetY),
                blurRadius = 10f
            )
        )
    )
}

@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val lineSpacing = 8.dp.toPx()
                val lineThickness = 1.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawRect(
                        color = Color.Red.copy(alpha = 0.05f),
                        topLeft = Offset(0f, y),
                        size = androidx.compose.ui.geometry.Size(size.width, lineThickness)
                    )
                    y += lineSpacing
                }

                // Add horizontal flickering scanning bars
                val scanPercentage = (System.currentTimeMillis() % 4000) / 4000f
                val barY = scanPercentage * size.height
                drawRect(
                    color = Color.Red.copy(alpha = 0.1f),
                    topLeft = Offset(0f, barY - 10.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width, 20.dp.toPx())
                )
            }
    )
}
