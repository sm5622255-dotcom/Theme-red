package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Ambient Blood background Canvas ---
@Composable
fun BloodDripBg(
    viewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val simulation = viewModel.activeSimulation
    val currentTheme = viewModel.themeColorMode

    val primaryColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14) // Acid Green
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    val secondaryColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> BloodCrimson
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF1B6E09) // Clotted Green
            ThemeColorMode.PHANTASM_VOID -> Color(0xFF4B4B50) // Ash Grey
        }
    }

    val backgroundStyle = viewModel.backgroundStyle

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    simulation.spawnUserDrop(offset.x, offset.y)
                    viewModel.earnPoints(1)
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // 1. Draw custom background image underlay
        val currentDimness = viewModel.backgroundDimness
        when (backgroundStyle) {
            BackgroundStyle.DIVINE_VESSEL -> {
                Image(
                    painter = painterResource(id = R.drawable.img_divine_vessel),
                    contentDescription = "Divine Saree Vessel Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentDimness))
                )
            }
            BackgroundStyle.MYSTIC_CRYPT -> {
                Image(
                    painter = painterResource(id = R.drawable.img_mystic_crypt),
                    contentDescription = "Mystic Stone Crypt Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentDimness))
                )
            }
            BackgroundStyle.GOLDEN_EMBROIDERY -> {
                Image(
                    painter = painterResource(id = R.drawable.img_golden_embroidery),
                    contentDescription = "Golden Ancestral Saree Weave Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = currentDimness))
                )
            }
            BackgroundStyle.ANIMATED_DRIPS -> {
                // Drawn inside the Canvas
            }
        }

        // Init drops on first layout
        LaunchedEffect(width) {
            if (width > 0f) {
                simulation.setup(width, initialCount = 10)
            }
        }

        // Ticker loop for particles, drops, pools
        LaunchedEffect(width, height, viewModel.dripSpeedMultiplier) {
            while (true) {
                simulation.tick(width, height, viewModel.dripSpeedMultiplier)
                delay(16) // ~60fps
            }
        }

        // Main Drawing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (backgroundStyle == BackgroundStyle.ANIMATED_DRIPS) {
                // Background color base
                drawRect(color = DarkVoid)

                // Draw static background atmospheric gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            DarkVoid,
                            CharcoalGrey.copy(alpha = 0.5f),
                            DarkVoid
                        )
                    )
                )
            } else {
                // Subtle dark vignette to make content pop over photos
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        center = center,
                        radius = size.maxDimension / 1.3f
                    )
                )
            }

            // Draw drawn paint strokes from Ritual Canvas
            simulation.paintStrokes.forEach { stroke ->
                val path = Path()
                if (stroke.points.isNotEmpty()) {
                    path.moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        path.lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                    val strokeColor = when (viewModel.themeColorMode) {
                        ThemeColorMode.CRUOR_RED -> stroke.color
                        ThemeColorMode.TOXIC_DECAY -> Color(0xFF2E8A15)
                        ThemeColorMode.PHANTASM_VOID -> Color(0xFF55555C)
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = stroke.width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
            }

            // Draw blood dripping drops and trails
            simulation.drops.forEach { drop ->
                // Draw trailing line
                if (drop.trail.size > 1) {
                    for (i in 0 until drop.trail.size - 1) {
                        val thickness = drop.width * (i.toFloat() / drop.trail.size)
                        drawLine(
                            color = secondaryColor.copy(alpha = 0.5f),
                            start = drop.trail[i],
                            end = drop.trail[i + 1],
                            strokeWidth = thickness
                        )
                    }
                }

                // Draw moving head droplet
                drawCircle(
                    color = primaryColor,
                    radius = drop.width,
                    center = Offset(drop.x, drop.y)
                )

                // Add liquid specular highlight inside the head
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = drop.width * 0.4f,
                    center = Offset(drop.x - drop.width * 0.3f, drop.y - drop.width * 0.3f)
                )
            }

            // Draw bottom pool accumulation
            val poolPath = simulation.makePoolPath(width, height)
            drawPath(
                path = poolPath,
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor.copy(alpha = 0.85f), DarkVoid),
                    startY = height - 120f,
                    endY = height
                )
            )

            // Draw top bleeding edge (where blood origin is pooling down)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, Color.Transparent),
                    startY = 0f,
                    endY = 32f
                ),
                size = Size(width, 32f)
            )

            // Draw splash particles
            simulation.particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.life),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

// --- Dynamic Glowing Cryptic Icons ---
@Composable
fun CrypticAppIcon(
    glyph: String,
    label: String,
    viewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val isSigilUnlocked = viewModel.unlockedRelics.find { it.name == "Occult Sigil" }?.isUnlocked == true

    // Periodic sharp spasmic glitch simulation inside the App Icon (CSS-like glitch effect)
    var glitchActive by remember { mutableStateOf(false) }
    var glitchOffsetX by remember { mutableStateOf(0f) }
    var glitchOffsetY by remember { mutableStateOf(0f) }
    var glitchColor by remember { mutableStateOf(Color.White) }
    var glitchScaleX by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        val random = kotlin.random.Random
        while (true) {
            // Keep icon stable for 3.0 to 5.5 seconds
            delay(random.nextLong(3000, 5500))
            
            // Trigger 4 to 8 very rapid sharp spasm frames (glitch bursts)
            val framesCount = random.nextInt(4, 9)
            for (f in 0 until framesCount) {
                glitchActive = true
                glitchOffsetX = random.nextInt(-7, 8).toFloat() // random shift in dp
                glitchOffsetY = random.nextInt(-4, 5).toFloat() // random shift in dp
                
                // Random scale factor matches keyframe distortion
                glitchScaleX = if (random.nextFloat() < 0.25f) 1.35f else if (random.nextFloat() < 0.5f) 0.75f else 1.05f
                
                // Color choices: shifting red and white hues
                glitchColor = if (random.nextBoolean()) Color.White else NeonRed
                
                delay(random.nextLong(35, 75)) // fast rapid CSS animation step frame delay
            }
            
            // Return to absolute perfect stability
            glitchActive = false
            glitchOffsetX = 0f
            glitchOffsetY = 0f
            glitchScaleX = 1f
        }
    }

    // Pulse Glow effect animation
    val infiniteTransition = rememberInfiniteTransition(label = "IconGlowPulse")
    val pulseGlowBlur by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blur"
    )

    // Breathing scale factors
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else (if (glitchActive) glitchScaleX else 1.0f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }
    val shadowColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> BloodCrimson
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF0F5A04)
            ThemeColorMode.PHANTASM_VOID -> Color(0xFF63636B)
        }
    }

    val displayLabel = remember(label, isSigilUnlocked) {
        if (isSigilUnlocked) {
            AjiboTextEngine.toOccultGlow(label)
        } else {
            label
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(82.dp)
            .scale(scaleFactor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        viewModel.makeVibration(15, 100)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = {
                        viewModel.makeVibration(45, 180)
                        onClick()
                    }
                )
            }
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .rotate(if (isPressed) -10f else (if (glitchActive) glitchOffsetX * 1.5f else 0f))
                .drawBehind {
                    // Draw outer pulsing glow circle
                    drawCircle(
                        color = shadowColor.copy(alpha = 0.45f),
                        radius = (size.width / 2f) + pulseGlowBlur.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                .border(1.5.dp, if (glitchActive) glitchColor.copy(alpha = 0.9f) else neonColor.copy(alpha = 0.8f), CircleShape)
                .background(DarkVoid, CircleShape)
                .clip(CircleShape)
        ) {
            if (glitchActive) {
                // Background Layer 1: Red silhouette copy shifted in one direction
                Text(
                    text = glyph,
                    color = NeonRed.copy(alpha = 0.85f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(x = (-glitchOffsetX).dp, y = (-glitchOffsetY).dp)
                )

                // Background Layer 2: White/hue copy shifted opposite direction
                Text(
                    text = glyph,
                    color = glitchColor.copy(alpha = 0.8f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(x = (glitchOffsetX * 0.6f).dp, y = (glitchOffsetY * 0.6f).dp)
                )
            }

            // Main central glyph text in center
            Text(
                text = glyph,
                color = if (glitchActive) (if (glitchColor == Color.White) NeonRed else Color.White) else neonColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = if (glitchActive) Modifier.offset(x = (glitchOffsetX * 0.2f).dp, y = (glitchOffsetY * 0.2f).dp) else Modifier,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = shadowColor,
                        blurRadius = pulseGlowBlur
                    )
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label styled dynamically
        Text(
            text = displayLabel,
            color = neonColor.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// --- Main Launchpad Desk ---
@Composable
fun HomeScreenLayout(
    viewModel: ThemeViewModel,
    onNavigate: (String) -> Unit
) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Eerie system tagline
            GlitchText(
                text = "CRUOR SYSTEM OS v5.0",
                fontSize = 18.sp,
                color = neonColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            GlowText(
                text = viewModel.getBatterySoul(),
                fontSize = 12.sp,
                color = neonColor.copy(alpha = 0.7f),
                isZalgo = false
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Witching progress Countdown widget
            Text(
                text = viewModel.getWitchingHourProgress(),
                color = neonColor.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            )
        }

        // Mid App Launcher Icons grid
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CrypticAppIcon("𐕏", "CRUOR CORE", viewModel) { onNavigate("CRUOR") }
                CrypticAppIcon("☠", "NECRO DIAL", viewModel) { onNavigate("NECRO") }
                CrypticAppIcon("☣", "KILN EYE", viewModel) { onNavigate("KILN") }
                CrypticAppIcon("𐏋", "MESG VOID", viewModel) { onNavigate("MESG") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CrypticAppIcon("⛥", "TEMP CLOCK", viewModel) { onNavigate("TEMP") }
                CrypticAppIcon("🜏", "VOIDEX MALL", viewModel) { onNavigate("VOIDEX") }
                CrypticAppIcon("♆", "SENS BOARD", viewModel) { onNavigate("SENS") }
                CrypticAppIcon("🩸", "PAINT SEAL", viewModel) { onNavigate("RITUAL") }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CrypticAppIcon(viewModel.whatsappStyle.iconUnicode, "WHATSAPP", viewModel) { onNavigate("WHATSAPP") }
                Spacer(modifier = Modifier.width(36.dp))
                CrypticAppIcon(viewModel.facebookStyle.iconUnicode, "FACEBOOK", viewModel) { onNavigate("FACEBOOK") }
            }
        }

        // Bottom Dashboard Points Widget
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkVoid.copy(alpha = 0.7f)),
            border = BorderStroke(1.dp, neonColor.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "HARVEST POOLS",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${viewModel.cruorPoints} CRUOR-POINTS",
                        color = neonColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "TAP SCREEN TO SPAWN BLOOD DROPLETS",
                    color = neonColor.copy(alpha = 0.45f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

// ------ APPLICATION 1: CRUOR CORE (Settings Settings Panel) ------
@Composable
fun CruorCoreApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    CoreWindowShell(title = "CRUOR CORE SYSTEM", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Customize gravity/viscosity drips
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DROP CLOTTING VELOCITY", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("${(viewModel.dripSpeedMultiplier * 100).toInt()}%", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = viewModel.dripSpeedMultiplier,
                    onValueChange = { viewModel.dripSpeedMultiplier = it },
                    valueRange = 0.3f..4.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = neonColor,
                        inactiveTrackColor = neonColor.copy(alpha = 0.2f),
                        thumbColor = neonColor
                    )
                )
            }

            // Theme selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("VIBRATORY PRESET SIGNAL", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeColorMode.values().forEach { mode ->
                        val isSelected = viewModel.themeColorMode == mode
                        Button(
                            onClick = {
                                viewModel.themeColorMode = mode
                                viewModel.makeVibration(60, 220)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) neonColor else Color.Gray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Audio & vibration toggle cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("AMBIENT HUM", color = neonColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.audioHumEnabled = !viewModel.audioHumEnabled },
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (viewModel.audioHumEnabled) "ACTIVE_STATIC" else "MUTED",
                                color = if (viewModel.audioHumEnabled) neonColor else Color.DarkGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (viewModel.audioHumEnabled) "🔊" else "🔇",
                                color = if (viewModel.audioHumEnabled) neonColor else Color.DarkGray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("HAPTIC SHOCK", color = neonColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.vibrationEnabled = !viewModel.vibrationEnabled },
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (viewModel.vibrationEnabled) "ENGAGED" else "VOID",
                                color = if (viewModel.vibrationEnabled) neonColor else Color.DarkGray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (viewModel.vibrationEnabled) "📳" else "❌",
                                color = if (viewModel.vibrationEnabled) neonColor else Color.DarkGray,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }
            }

            // Custom Backgrounds selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("INTEGRATED BACKGROUND CODES", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(BackgroundStyle.ANIMATED_DRIPS, BackgroundStyle.DIVINE_VESSEL).forEach { bg ->
                        val isSelected = viewModel.backgroundStyle == bg
                        Button(
                            onClick = {
                                viewModel.backgroundStyle = bg
                                viewModel.makeVibration(50, 180)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bg.displayName,
                                color = if (isSelected) neonColor else Color.Gray,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(BackgroundStyle.MYSTIC_CRYPT, BackgroundStyle.GOLDEN_EMBROIDERY).forEach { bg ->
                        val isSelected = viewModel.backgroundStyle == bg
                        Button(
                            onClick = {
                                viewModel.backgroundStyle = bg
                                viewModel.makeVibration(50, 180)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bg.displayName,
                                color = if (isSelected) neonColor else Color.Gray,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Custom Background Opacity / Clarity Selector
            if (viewModel.backgroundStyle != BackgroundStyle.ANIMATED_DRIPS) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "BACKGROUND IMAGE CLARITY",
                            color = neonColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${((1f - viewModel.backgroundDimness) * 100f).toInt()}% VISIBLE",
                            color = neonColor,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.85f to "DIM VOID", 0.6f to "ATMOSPHERIC", 0.35f to "RADIANT", 0.1f to "GLARING").forEach { (dimValue, text) ->
                            val isSelected = viewModel.backgroundDimness == dimValue
                            Button(
                                onClick = {
                                    viewModel.backgroundDimness = dimValue
                                    viewModel.makeVibration(40, 150)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                                ),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = text,
                                    color = if (isSelected) neonColor else Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp Icon selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("WHATSAPP (VIPER) ICON GLYPHS", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WhatsappIconStyle.values().forEach { item ->
                        val isSelected = viewModel.whatsappStyle == item
                        Button(
                            onClick = {
                                viewModel.whatsappStyle = item
                                viewModel.makeVibration(40, 160)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${item.iconUnicode} ${item.displayName}",
                                color = if (isSelected) neonColor else Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Facebook Icon selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("FACEBOOK (SOUL) ICON GLYPHS", color = neonColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FacebookIconStyle.values().forEach { item ->
                        val isSelected = viewModel.facebookStyle == item
                        Button(
                            onClick = {
                                viewModel.facebookStyle = item
                                viewModel.makeVibration(40, 160)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) neonColor else neonColor.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) neonColor.copy(alpha = 0.2f) else DarkVoid
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${item.iconUnicode} ${item.displayName}",
                                color = if (isSelected) neonColor else Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Drain blood pools
            Button(
                onClick = {
                    viewModel.activeSimulation.clear()
                    viewModel.makeVibration(200, 255)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonColor)
            ) {
                Text(
                    text = "PURGE GLASS SCREEN (WIPE DRIPS)",
                    color = DarkVoid,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ------ APPLICATION 2: NECRO DIALER (Spooky dialer experience) ------
@Composable
fun NecroDialerApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    CoreWindowShell(title = "NECRO SPEAKER TERMINAL", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dialer readout screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f),
                colors = CardDefaults.cardColors(containerColor = DarkVoid),
                border = BorderStroke(1.dp, neonColor.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.dialedNumber.ifEmpty { "--- --- ---" },
                        color = neonColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = viewModel.dialerWhisper,
                        color = neonColor.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dialer Buttons Grid (12-pad)
            Column(
                modifier = Modifier.weight(0.65f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rowConfigs = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('*', '0', '#')
                )

                rowConfigs.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { digit ->
                            Button(
                                onClick = { viewModel.pressDialerKey(digit) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.8f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, neonColor.copy(alpha = 0.2f)),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkVoid)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = digit.toString(),
                                        color = neonColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    // Spooky mini symbols under digits
                                    Text(
                                        text = "⚡✝☣♆"[digit.code % 4].toString(),
                                        color = neonColor.copy(alpha = 0.4f),
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Call/Clear actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.clearDialer() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("PURGE", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.performDialCall() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = neonColor)
                    ) {
                        Text("ENGAGE VOID", color = DarkVoid, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ------ APPLICATION 3: KILN EYE (Simulated Spectral camera) ------
@Composable
fun KilnEyeApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    val isLensUnlocked = viewModel.unlockedRelics.find { it.name == "Corrupt Lens" }?.isUnlocked == true
    var targetNoiseFreq by remember { mutableStateOf(0.45f) }

    // Simulating moving noise radar scans
    var scanY by remember { mutableStateOf(0f) }
    var frameTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            scanY = (scanY + 0.05f) % 1.0f
            frameTick++
        }
    }

    CoreWindowShell(title = "KILN EYE SPECTROGRAPH", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Viewfinder Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.5.dp, neonColor, RoundedCornerShape(12.dp))
            ) {
                // Interactive Simulated Cam view background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Spectral grid drawing
                    val cols = 8
                    val rows = 12
                    for (x in 0..cols) {
                        val px = (x.toFloat() / cols) * size.width
                        drawLine(
                            color = neonColor.copy(alpha = 0.08f),
                            start = Offset(px, 0f),
                            end = Offset(px, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..rows) {
                        val py = (y.toFloat() / rows) * size.height
                        drawLine(
                            color = neonColor.copy(alpha = 0.08f),
                            start = Offset(0f, py),
                            end = Offset(size.width, py),
                            strokeWidth = 1f
                        )
                    }

                    // Rotating crosshair ring
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width * 0.25f
                    val phaseRotation = frameTick * 2f

                    drawCircle(
                        color = neonColor.copy(alpha = 0.25f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phaseRotation))
                    )

                    // Draw focus lines in corners
                    val bracketLen = 30f
                    // Top-Left
                    drawLine(neonColor, Offset(15f, 15f), Offset(15f + bracketLen, 15f), strokeWidth = 3f)
                    drawLine(neonColor, Offset(15f, 15f), Offset(15f, 15f + bracketLen), strokeWidth = 3f)
                    // Top-Right
                    drawLine(neonColor, Offset(size.width - 15f, 15f), Offset(size.width - 15f - bracketLen, 15f), strokeWidth = 3f)
                    drawLine(neonColor, Offset(size.width - 15f, 15f), Offset(size.width - 15f, 15f + bracketLen), strokeWidth = 3f)
                    // Bottom-Left
                    drawLine(neonColor, Offset(15f, size.height - 15f), Offset(15f + bracketLen, size.height - 15f), strokeWidth = 3f)
                    drawLine(neonColor, Offset(15f, size.height - 15f), Offset(15f, size.height - 15f - bracketLen), strokeWidth = 3f)
                    // Bottom-Right
                    drawLine(neonColor, Offset(size.width - 15f, size.height - 15f), Offset(size.width - 15f - bracketLen, size.height - 15f), strokeWidth = 3f)
                    drawLine(neonColor, Offset(size.width - 15f, size.height - 15f), Offset(size.width - 15f, size.height - 15f - bracketLen), strokeWidth = 3f)

                    // Draw moving frequency sweep scanbar
                    val barY = scanY * size.height
                    drawLine(
                        color = neonColor.copy(alpha = 0.45f),
                        start = Offset(0f, barY),
                        end = Offset(size.width, barY),
                        strokeWidth = 4f
                    )

                    // Render ghosts / souls if Corrupt Lens has been unlocked!
                    if (isLensUnlocked) {
                        for (i in 0..2) {
                            val seedX = (frameTick * 3 + i * 400) % size.width.toInt()
                            val seedY = (sin((frameTick.toFloat() / 20f) + i.toFloat()) * 60f + (size.height / 3f) + (i * 120).toFloat())
                            // Drawing creepy ectoplasmic shapes
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = 25f + (sin(frameTick.toFloat() / 10f) * 5f),
                                center = Offset(seedX.toFloat(), seedY)
                            )
                            // Draw eyes on ghost
                            drawCircle(
                                color = neonColor.copy(alpha = 0.5f),
                                radius = 4f,
                                center = Offset(seedX.toFloat() - 7f, seedY - 3f)
                            )
                            drawCircle(
                                color = neonColor.copy(alpha = 0.5f),
                                radius = 4f,
                                center = Offset(seedX.toFloat() + 7f, seedY - 3f)
                            )
                        }
                    }
                }

                // Foreground live counters
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "REC [•] SIGNAL",
                            color = neonColor,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isLensUnlocked) "LENS: CORRUPT_INFUSED" else "LENS: MATTE_VOID",
                            color = neonColor.copy(alpha = 0.6f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Bottom info readings
                    Column {
                        Text(
                            "RESONANCE DETECTED: " + (30 + (System.currentTimeMillis() % 40)).toString() + " Hz",
                            color = neonColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "SPECTRAL DENSITY: " + if (isLensUnlocked) "HIGH (SHADOWS SPOTTED)" else "DORMANT",
                            color = neonColor.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calibration slider controls
            Column(
                modifier = Modifier.weight(0.3f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "CALIBRATE WAVE SCAN FREQUENCY",
                    color = neonColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = targetNoiseFreq,
                    onValueChange = { targetNoiseFreq = it },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        activeTrackColor = neonColor,
                        thumbColor = neonColor,
                        inactiveTrackColor = neonColor.copy(alpha = 0.15f)
                    )
                )

                Text(
                    text = if (isLensUnlocked)
                        "THE CORRUPTED SPECTRAL CORE REVEALS THE UNSEEN."
                    else
                        "THE LENS IS SEALED. HARVEST 120 CRUOR-POINTS IN MALL TO UNLOCK PHANTASMS.",
                    color = neonColor.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ------ APPLICATION 4: MESG VOID (Occult terminal chat bot) ------
@Composable
fun MesgVoidApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom of chat
    LaunchedEffect(viewModel.chatMessages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    CoreWindowShell(title = "MESG OCCULT TERMINAL", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Messages Scroll Area
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .verticalScroll(scrollState)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                viewModel.chatMessages.forEach { msg ->
                    val isYou = msg.sender == "YOU"
                    val itemBg = if (isYou) DarkVoid else CharcoalGrey

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isYou) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isYou) "ψ YOUR VESSEL" else "𐕏 NAMELESS ENTITY",
                            color = neonColor.copy(alpha = 0.5f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, neonColor.copy(alpha = if (isYou) 0.35f else 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = itemBg),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                if (msg.isGlitching) {
                                    GlitchText(
                                        text = msg.displayedContent,
                                        fontSize = 11.sp,
                                        color = neonColor
                                    )
                                } else {
                                    Text(
                                        text = msg.displayedContent,
                                        color = if (isYou) EerieWhite else neonColor,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Text(
                            text = msg.timestamp,
                            color = Color.DarkGray,
                            fontSize = 7.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                if (viewModel.isGeneratingReply) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = neonColor,
                            strokeWidth = 1.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ENTITY TRANSMITTING SECRETS...",
                            color = neonColor.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Input prompt strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.promptInput,
                    onValueChange = { viewModel.promptInput = it },
                    placeholder = {
                        Text(
                            "TRANSMIT RAW FREQUENCIES...",
                            color = Color.DarkGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.weight(0.8f),
                    textStyle = TextStyle(color = EerieWhite, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonColor,
                        unfocusedBorderColor = neonColor.copy(alpha = 0.25f),
                        unfocusedContainerColor = DarkVoid,
                        focusedContainerColor = DarkVoid
                    ),
                    maxLines = 2,
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = { viewModel.sendChatMessage() },
                    enabled = viewModel.promptInput.isNotBlank() && !viewModel.isGeneratingReply,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (viewModel.promptInput.isNotBlank() && !viewModel.isGeneratingReply) neonColor else Color.DarkGray,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = DarkVoid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ------ APPLICATION 5: TEMP CLOCK (Countdown countdown clocks) ------
@Composable
fun TempClockApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    var countdownSecs by remember { mutableStateOf(300) } // 5-min timer
    var isTimerActive by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerActive, countdownSecs) {
        if (isTimerActive && countdownSecs > 0) {
            delay(1000)
            countdownSecs--
            if (countdownSecs == 0) {
                isTimerActive = false
                viewModel.makeVibration(2000, 255) // Cursed buzzer!
            }
        }
    }

    val angleDegree = remember(countdownSecs) {
        (countdownSecs.toFloat() / 300f) * 360f
    }

    CoreWindowShell(title = "RITUAL TEMP COUNTDOWN", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual clock core
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .drawBehind {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width / 2f

                        // Outer rim
                        drawCircle(
                            color = neonColor,
                            radius = radius,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Glowing countdown track arc
                        drawArc(
                            color = neonColor.copy(alpha = 0.2f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )

                        drawArc(
                            color = neonColor,
                            startAngle = -90f,
                            sweepAngle = angleDegree,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )

                        // Dagger Needle clock hands
                        val needleLen = radius * 0.75f
                        val needleAngleRad = Math.toRadians((angleDegree - 90f).toDouble())
                        val endX = center.x + Math.cos(needleAngleRad).toFloat() * needleLen
                        val endY = center.y + Math.sin(needleAngleRad).toFloat() * needleLen

                        drawLine(
                            color = neonColor,
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 4.dp.toPx()
                        )

                        // Small second sweep needle
                        val secAngle = (System.currentTimeMillis() % 60000) / 60000f * 360f - 90f
                        val secRad = Math.toRadians(secAngle.toDouble())
                        val endSecX = center.x + Math.cos(secRad).toFloat() * (radius * 0.85f)
                        val endSecY = center.y + Math.sin(secRad).toFloat() * (radius * 0.85f)
                        drawLine(
                            color = neonColor.copy(alpha = 0.5f),
                            start = center,
                            end = Offset(endSecX, endSecY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Center eye cap
                        drawCircle(color = neonColor, radius = 6.dp.toPx())
                        drawCircle(color = DarkVoid, radius = 2.dp.toPx())
                    }
            ) {
                // Formatting time inside
                val mins = countdownSecs / 60
                val secs = countdownSecs % 60
                val timeStr = String.format("%02d:%02d", mins, secs)

                Text(
                    text = timeStr,
                    color = neonColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(color = Color.Red, blurRadius = 8f)
                    )
                )
            }

            // Quick Countdown config modifiers
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "THE WITCHING ALIGNMENT TIMER",
                    color = neonColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            countdownSecs = (countdownSecs + 60).coerceAtMost(600)
                            viewModel.makeVibration(40, 150)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.3f))
                    ) {
                        Text("+1 MIN", color = neonColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            isTimerActive = !isTimerActive
                            viewModel.makeVibration(80, 200)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = neonColor)
                    ) {
                        Text(
                            text = if (isTimerActive) "HALT RITUAL" else "INITIATE TRIGGER",
                            color = DarkVoid,
                            maxLines = 1,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }

                    Button(
                        onClick = {
                            countdownSecs = 300
                            isTimerActive = false
                            viewModel.makeVibration(40, 150)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.3f))
                    ) {
                        Text("RESET", color = neonColor, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Text(
                    text = "WARNING: ENABLING TIMER CAUSES EXTREME RESONANCE UPON ZERO VALUE.",
                    color = neonColor.copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

// ------ APPLICATION 6: VOIDEX MALL (Marketplace shop) ------
@Composable
fun VoidexMallApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    CoreWindowShell(title = "VOIDEX CORRUPTED MARKET", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // User current Balance Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CURRENCY: VOID INVENTORY",
                    color = neonColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    "${viewModel.cruorPoints} CRUOR-POINTS",
                    color = neonColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Products Grid Array scrollable list
            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                viewModel.unlockedRelics.forEach { relic ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = if (relic.isUnlocked) 0.8f else 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.65f)) {
                                Text(
                                    text = relic.name,
                                    color = if (relic.isUnlocked) neonColor else EerieWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = relic.description,
                                    color = Color.Gray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 12.sp
                                )
                            }

                            Column(
                                modifier = Modifier.weight(0.35f),
                                horizontalAlignment = Alignment.End
                            ) {
                                if (relic.isUnlocked) {
                                    Text(
                                        "INFUSED✓",
                                        color = neonColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.purchaseRelic(relic) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (viewModel.cruorPoints >= relic.cost) neonColor else Color.DarkGray
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "${relic.cost} CP",
                                            color = DarkVoid,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                "TAP EXTREMELY HIGH STACKS OF BLOOD ON SCREEN TO MINT CRUOR-POINTS INSTANTLY.",
                color = neonColor.copy(alpha = 0.35f),
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

// ------ APPLICATION 7: SENS BOARD (Occult ghost-hunting reader) ------
@Composable
fun SensBoardApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    var ghostSignalIntensity by remember { mutableStateOf(12f) }
    var waveTick by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            waveTick = (waveTick + 0.1f) % 100f
            ghostSignalIntensity = (20f + Math.sin(waveTick.toDouble()).toFloat() * 10f + Random.nextFloat() * 15f)
        }
    }

    CoreWindowShell(title = "SENS PORTAL RECEIVER", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Analog static spectrum graphs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "OCCULT ECTOMAGNETIC FIELDS (EMF)",
                    color = neonColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Simulated radar graph layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.5.dp, neonColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerY = size.height / 2f
                        val path = Path()
                        path.moveTo(0f, centerY)

                        val step = size.width / 40f
                        for (i in 0..40) {
                            val px = i * step
                            // Sinusoidal noise curve
                            val waveVal = sin((i * 0.4f) + waveTick) * ghostSignalIntensity
                            val py = centerY + waveVal + (Random.nextFloat() * 6f)
                            path.lineTo(px, py)
                        }

                        drawPath(
                            path = path,
                            color = neonColor,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Center scanline grid mark
                        drawLine(
                            color = neonColor.copy(alpha = 0.15f),
                            start = Offset(0f, centerY),
                            end = Offset(size.width, centerY)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bio-Indicators indicators
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "BIOMETRI_S TRANSMISSIONS",
                    color = neonColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Custom Indicators List
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Soul charge item
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("SOUL CARGO", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text(viewModel.getBatterySoul(), color = neonColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Ghost charge item
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("SPIRIT DEPTH", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            val ghostStr = remember(ghostSignalIntensity) { String.format("%.1f mHz", ghostSignalIntensity) }
                            Text(ghostStr, color = neonColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Tremors frequency warning card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkVoid),
                    border = BorderStroke(1.dp, neonColor.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "VESSEL ACCELEROMETER CODES (TREMORS)",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "X: -0.1989 | Y: 9.8065 (STATIC STasis) | Z: 1.1345",
                            color = neonColor.copy(alpha = 0.85f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ------ APPLICATION 8: RITUAL CANVAS (Blood Paint seal generator) ------
@Composable
fun RitualPaintApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    val drawingPoints = remember { mutableStateListOf<Offset>() }
    val simulation = viewModel.activeSimulation

    CoreWindowShell(title = "PAINT BLOOD RITUAL SEAL", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DRAFT RITUAL RUNES WITH FINGER",
                    color = neonColor.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "LINES BLEED IN REALTIME",
                    color = neonColor,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Paint canvas screen pad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.5.dp, neonColor, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                drawingPoints.clear()
                                drawingPoints.add(offset)
                                viewModel.makeVibration(25, 140)
                            },
                            onDrag = { change, dragAmount ->
                                drawingPoints.add(change.position)
                                if (drawingPoints.size % 4 == 0) {
                                    viewModel.makeVibration(10, 100)
                                }
                            },
                            onDragEnd = {
                                if (drawingPoints.isNotEmpty()) {
                                    // Submit drag strokes into our unified simulation drawing buffer!
                                    val newStrokePoints = drawingPoints.toList()
                                    simulation.paintStrokes.add(
                                        PaintStroke(points = newStrokePoints.toMutableStateList())
                                    )
                                    drawingPoints.clear()
                                    viewModel.makeVibration(80, 180)
                                }
                            }
                        )
                    }
            ) {
                // Background visual scan grid lines inside drafting board
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Sweep grids
                    val rows = 10
                    for (i in 1 until rows) {
                        val py = (i.toFloat() / rows) * size.height
                        drawLine(
                            color = neonColor.copy(alpha = 0.05f),
                            start = Offset(0f, py),
                            end = Offset(size.width, py)
                        )
                    }

                    // Render active drawing stroke under finger
                    if (drawingPoints.size > 1) {
                        val path = Path()
                        path.moveTo(drawingPoints[0].x, drawingPoints[0].y)
                        for (i in 1 until drawingPoints.size) {
                            path.lineTo(drawingPoints[i].x, drawingPoints[i].y)
                        }
                        drawPath(
                            path = path,
                            color = neonColor,
                            style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )
                    }
                }

                // If Board is untouched empty
                if (simulation.paintStrokes.isEmpty() && drawingPoints.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[DRAW SACRED SIGILS]",
                            color = neonColor.copy(alpha = 0.3f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action triggers bottom deck
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        simulation.paintStrokes.clear()
                        drawingPoints.clear()
                        viewModel.makeVibration(50, 150)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("ERASE SEAL", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        // Secure seal points payout
                        if (simulation.paintStrokes.isNotEmpty()) {
                            viewModel.earnPoints(25)
                            simulation.paintStrokes.clear()
                            drawingPoints.clear()
                            viewModel.makeVibration(400, 250) // Blinding seal haptic
                        }
                    },
                    enabled = simulation.paintStrokes.isNotEmpty(),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = neonColor)
                ) {
                    Text(
                        "SUBMIT RITUAL PACT",
                        color = DarkVoid,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ------ SYSTEM SHELL WINDOW WRAPPER LAYER ------
@Composable
fun CoreWindowShell(
    title: String,
    themeColor: Color,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkVoid),
        border = BorderStroke(1.5.dp, themeColor),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header frame panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColor.copy(alpha = 0.12f))
                    .border(
                        border = BorderStroke(0.dp, Color.Transparent),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "• $title",
                    color = themeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                // Close Button glyph
                Text(
                    text = "[X]",
                    color = themeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(horizontal = 6.dp)
                )
            }

            // App Content slot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkVoid)
            ) {
                content()
            }
        }
    }
}

// ------ APPLICATION 9: WHATSAPP SIMULATION: SANGUINE CHAT ------
@Composable
fun SanguineChatApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    var selectedContact by remember { mutableStateOf<String?>(null) }
    val chatsHistory = remember {
        mutableStateMapOf<String, List<Pair<String, String>>>().apply {
            put("VESSEL PORTRAIT", listOf("VESSEL PORTRAIT" to "I am woven into the terminal... do you see the golden embroidery of my saree?"))
            put("LURKER IN THE WALL", listOf("LURKER IN THE WALL" to "The white brick wall behind her holds secrets. We've been trapped here forever..."))
            put("VIPER CHANNEL", listOf("VIPER CHANNEL" to "[COMMUNICATIONS INTERCEPTED] Viper decrypted protocols active. Ask away, vessel owner."))
        }
    }

    CoreWindowShell(title = "VIPER SECURE WHATSAPP VOID", onClose = onClose, themeColor = neonColor) {
        if (selectedContact == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = "ACTIVE CHANNELS (GREEN ENCRYPTED)",
                    color = neonColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val contacts = listOf(
                        Triple("VESSEL PORTRAIT", "👩 Vessel alpha is smiling pleasantly", "Sacred Host"),
                        Triple("LURKER IN THE WALL", "🧱 Cold masonry cracks are widening...", "Wall dweller"),
                        Triple("VIPER CHANNEL", "🐍 Corrupted WhatsApp secure portal", "System Daemon")
                    )

                    contacts.forEach { (name, tag, role) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedContact = name
                                    viewModel.makeVibration(25, 120)
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkVoid),
                            border = BorderStroke(1.2.dp, neonColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(neonColor.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, neonColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (name == "VESSEL PORTRAIT") "👩" else if (name == "LURKER IN THE WALL") "🧱" else "🐍",
                                            fontSize = 18.sp
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = name,
                                            color = neonColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = tag,
                                            color = Color.LightGray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = neonColor.copy(alpha = 0.12f)),
                                    border = BorderStroke(1.dp, neonColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = role,
                                        color = neonColor,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "SYSTEM: Tap a portal to request decryption. All actions cost 0 cruor but emit high haptic feedback.",
                    color = Color.DarkGray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val contact = selectedContact!!
            val history = chatsHistory[contact] ?: emptyList()
            val listState = rememberLazyListState()

            LaunchedEffect(history.size) {
                if (history.isNotEmpty()) {
                    listState.animateScrollToItem(history.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "←",
                            color = neonColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    selectedContact = null
                                    viewModel.makeVibration(15, 100)
                                }
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = contact,
                            color = neonColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "SECURED [AES-VOID]",
                        color = Color.DarkGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history) { (sender, msg) ->
                        val isSelf = sender == "YOU"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isSelf) 12.dp else 2.dp,
                                            bottomEnd = if (isSelf) 2.dp else 12.dp
                                        )
                                    )
                                    .background(if (isSelf) neonColor.copy(alpha = 0.15f) else Color(0xFF15151A))
                                    .border(
                                        1.dp,
                                        if (isSelf) neonColor else neonColor.copy(alpha = 0.15f),
                                        RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isSelf) 12.dp else 2.dp,
                                            bottomEnd = if (isSelf) 2.dp else 12.dp
                                        )
                                    )
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isSelf) "YOU" else sender,
                                        color = if (isSelf) neonColor else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Text(
                                        text = msg,
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "[TAP COGNITIVE INITIATOR RUNES]",
                        color = Color.DarkGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    val triggers = when (contact) {
                        "VESSEL PORTRAIT" -> listOf(
                            "Are you trapped in this portrait?" to "Not trapped... manifested. The gold thread on my saree is of ancient occult weave.",
                            "What is behind the brick wall?" to "Only the system's void, but the brick masonry remains solid and protective.",
                            "Sacrifice 5 soul points for advice" to "Drip blood on the glass screen. Each drop feeds the entity container."
                        )
                        "LURKER IN THE WALL" -> listOf(
                            "Who built this masonry wall?" to "An architect who wanted to seal the sacred vessel from outside glare.",
                            "Is there an escape route?" to "No. The system loops back onto itself forever when you press close.",
                            "Can I touch the stone cracks?" to "Yes. Drag your finger across the PAINT SEAL. Real clotted red paint will seep out."
                        )
                        else -> listOf(
                            "Request decrypt of status" to "All indicators are healthy. Main loop running at optimal clotted speed of 60Hz.",
                            "Simulate general crash" to "ALERT: Cryptic Viber system warning emitted. No overflow detected.",
                            "Sing a cyber-gothic lullaby" to "Sleep deep, user... the scanlines are glowing, the vessel is smiling, the soul is flowing."
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        triggers.forEach { (trigger, reply) ->
                            Button(
                                onClick = {
                                    val currentHistory = chatsHistory[contact] ?: emptyList()
                                    chatsHistory[contact] = currentHistory + Pair("YOU", trigger) + Pair(contact, reply)
                                    viewModel.earnPoints(2)
                                    viewModel.makeVibration(80, 200)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                border = BorderStroke(1.dp, neonColor.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = trigger.substringBefore(" for"),
                                    color = neonColor,
                                    fontSize = 7.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 2,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------ APPLICATION 10: FACEBOOK SIMULATION: SOULBOOK ------
@Composable
fun SoulBookApp(viewModel: ThemeViewModel, onClose: () -> Unit) {
    val currentTheme = viewModel.themeColorMode
    val neonColor = remember(currentTheme) {
        when (currentTheme) {
            ThemeColorMode.CRUOR_RED -> NeonRed
            ThemeColorMode.TOXIC_DECAY -> Color(0xFF39FF14)
            ThemeColorMode.PHANTASM_VOID -> EerieWhite
        }
    }

    var userPostText by remember { mutableStateOf("") }
    val mockPosts = remember {
        mutableStateListOf(
            Triple(
                "VESSEL PORTRAIT",
                "Woven into the canvas thread today... the crimson ritual powder on my forehead is perfectly dry. How do you like the golden saree embroidery? 🩸✨ #SareeAesthetic #SacredVessel",
                Random.nextInt(100, 500)
            ),
            Triple(
                "LURKER",
                 "The white brick mortar is chillingly cold are we locked in? Tap my masonry to hear the hollow response. #BricksAndBones",
                 Random.nextInt(10, 80)
            ),
            Triple(
                "SYSTEM ANNOUNCER",
                "Cyber-occult Terminal V5 is running smoothly. Remember to visit the VOIDEX MALL to purchase the Corrupt Lens! #MallVoid #CruorCore",
                Random.nextInt(500, 2000)
            )
        )
    }

    CoreWindowShell(title = "SOULBOOK NEURAL SHADOW GRID", onClose = onClose, themeColor = neonColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STATUS FEED (UNMASKED SOULS)",
                    color = neonColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${mockPosts.size} COGNITIONS ACTIVE",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(mockPosts) { index, (author, body, likes) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkVoid),
                        border = BorderStroke(1.dp, neonColor.copy(alpha = 0.25f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(neonColor.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, neonColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (author == "VESSEL PORTRAIT") "👩" else if (author == "LURKER") "🧱" else "👿",
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = author,
                                        color = neonColor,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "3m ago",
                                    color = Color.DarkGray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = body,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "❤️",
                                        fontSize = 10.sp,
                                        modifier = Modifier.clickable {
                                            mockPosts[index] = Triple(author, body, likes + 1)
                                            viewModel.earnPoints(1)
                                            viewModel.makeVibration(20, 110)
                                        }
                                    )
                                    Text(
                                        text = "$likes souls",
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "⚡ RIPPLE ENERGY",
                                    color = neonColor.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable {
                                        viewModel.earnPoints(5)
                                        viewModel.makeVibration(100, 220)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = userPostText,
                    onValueChange = { userPostText = it },
                    placeholder = {
                        Text(
                            "Share your soul statement with the void...",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = neonColor,
                        unfocusedBorderColor = neonColor.copy(alpha = 0.3f),
                        cursorColor = neonColor
                    ),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (userPostText.isNotBlank()) {
                                mockPosts.add(0, Triple("YOU (VESSEL OWNER)", userPostText, 1))
                                userPostText = ""
                                viewModel.earnPoints(10)
                                viewModel.makeVibration(180, 240)
                                
                                val responseOptions = listOf(
                                    Triple("VESSEL PORTRAIT", "I felt that vibration inside the weave! Deep ritual. 🪐", 15),
                                    Triple("LURKER", "The brick mortar shuddered at your post... keep typing.", 3),
                                    Triple("SYSTEM ANNOUNCER", "Warning: High thought-wave density registered in sector 7.", 33)
                                )
                                mockPosts.add(1, responseOptions[Random.nextInt(responseOptions.size)])
                            }
                        },
                        enabled = userPostText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = neonColor),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "TRANSMIT TO ALL FEED (10pts payout)",
                            color = DarkVoid,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Helper expansion list builder
fun <T> List<T>.toMutableStateList() = mutableStateListOf<T>().apply { addAll(this@toMutableStateList) }
