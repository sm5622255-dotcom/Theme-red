package com.example.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.BloodCrimson
import com.example.ui.theme.NeonRed
import kotlin.math.sin
import kotlin.random.Random

data class BloodDrop(
    val id: Long = Random.nextLong(),
    var x: Float,
    var y: Float,
    var speed: Float,
    var width: Float,
    var trail: MutableList<Offset> = mutableStateListOf(),
    var lengthLimit: Int = Random.nextInt(20, 60),
    var swayAmplitude: Float = Random.nextFloat() * 1.5f,
    var swayFrequency: Float = Random.nextFloat() * 0.05f + 0.02f,
    var phase: Float = Random.nextFloat() * 10f,
    var speedMultiplier: Float = 1.0f
) {
    fun reset(screenWidth: Float) {
        x = Random.nextFloat() * screenWidth
        y = -Random.nextFloat() * 50f
        speed = Random.nextFloat() * 1.5f + 0.8f
        this.width = Random.nextFloat() * 4f + 2f
        trail.clear()
        lengthLimit = Random.nextInt(25, 75)
        phase = Random.nextFloat() * 10f
    }
}

data class SplashParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var life: Float, // 1f down to 0f
    var color: Color
)

data class PaintStroke(
    val id: Long = Random.nextLong(),
    val points: MutableList<Offset> = mutableStateListOf(),
    val color: Color = BloodCrimson,
    val width: Float = 8f,
    var isDripping: Boolean = true
)

class BloodDripSimulation {
    val drops = mutableStateListOf<BloodDrop>()
    val particles = mutableStateListOf<SplashParticle>()
    val paintStrokes = mutableStateListOf<PaintStroke>()

    // Pool accumulation heights (discrete columns across screen)
    val poolResolution = 40
    val poolHeights = FloatArray(poolResolution) { 0f }

    private var frameCount = 0L

    fun setup(width: Float, initialCount: Int = 12) {
        if (drops.isNotEmpty() || width <= 0f) return
        for (i in 0 until initialCount) {
            drops.add(
                BloodDrop(
                    x = Random.nextFloat() * width,
                    y = -Random.nextFloat() * 200f,
                    speed = Random.nextFloat() * 2f + 0.5f,
                    width = Random.nextFloat() * 5f + 2f
                )
            )
        }
    }

    fun triggerSplat(x: Float, y: Float, count: Int = 8, scale: Float = 1.0f) {
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (Random.nextFloat() * 4f + 1f) * scale
            particles.add(
                SplashParticle(
                    x = x,
                    y = y,
                    vx = Math.cos(angle.toDouble()).toFloat() * speed,
                    vy = Math.sin(angle.toDouble()).toFloat() * speed - (Random.nextFloat() * 1.5f), // drift upward path
                    radius = Random.nextFloat() * 5f + 2f,
                    life = 1.0f,
                    color = if (Random.nextBoolean()) NeonRed else BloodCrimson
                )
            )
        }
    }

    fun spawnUserDrop(x: Float, y: Float) {
        drops.add(
            BloodDrop(
                x = x,
                y = y,
                speed = Random.nextFloat() * 3.5f + 1.5f,
                width = Random.nextFloat() * 6f + 3f
            )
        )
        triggerSplat(x, y, count = 4, scale = 0.6f)
    }

    fun tick(screenWidth: Float, screenHeight: Float, speedMultiplier: Float = 1.0f) {
        frameCount++
        if (screenWidth <= 0f || screenHeight <= 0f) return

        // 1. Spawning natural head drips
        if (drops.size < 20 && Random.nextFloat() < 0.05f) {
            drops.add(
                BloodDrop(
                    x = Random.nextFloat() * screenWidth,
                    y = -Random.nextFloat() * 50f,
                    speed = Random.nextFloat() * 1.8f + 0.4f,
                    width = Random.nextFloat() * 4.5f + 2f
                )
            )
        }

        // 2. Physics: Trickle blood drops
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val drop = iterator.next()
            drop.phase += drop.swayFrequency
            val horizontalSway = sin(drop.phase.toDouble()).toFloat() * drop.swayAmplitude

            drop.x = (drop.x + horizontalSway).coerceIn(0f, screenWidth)
            drop.y += drop.speed * speedMultiplier

            // Add point to trail
            drop.trail.add(Offset(drop.x, drop.y))
            if (drop.trail.size > drop.lengthLimit) {
                drop.trail.removeAt(0)
            }

            // Check pool/ground collision
            val colIndex = ((drop.x / screenWidth) * poolResolution).toInt().coerceIn(0, poolResolution - 1)
            val poolTop = screenHeight - poolHeights[colIndex] - 5f

            if (drop.y >= poolTop) {
                // Splash on hit
                triggerSplat(drop.x, poolTop, count = 5, scale = 0.8f)
                // Accumulate volume at bottom pool (increase height of nearest slices)
                val spreadRadius = 2
                for (i in -spreadRadius..spreadRadius) {
                    val idx = colIndex + i
                    if (idx in 0 until poolResolution) {
                        val falloff = 1.0f - (Math.abs(i).toFloat() / (spreadRadius + 1))
                        poolHeights[idx] = (poolHeights[idx] + (drop.width * 0.7f * falloff)).coerceAtMost(110f)
                    }
                }
                drop.reset(screenWidth)
            }
        }

        // 3. Ambient pooling growth (slow bleeding from above pooling onto edges)
        for (i in 0 until poolResolution) {
            if (Random.nextFloat() < 0.03f) {
                poolHeights[i] = (poolHeights[i] + 0.15f * speedMultiplier).coerceAtMost(110f)
            }
            // Smooth pool heights (liquid leveling effect)
            if (i > 0 && i < poolResolution - 1) {
                val average = (poolHeights[i - 1] + poolHeights[i] + poolHeights[i + 1]) / 3f
                poolHeights[i] = poolHeights[i] * 0.95f + average * 0.05f
            }
        }

        // 4. Update splash particles
        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.15f // gravity acceleration
            p.life -= 0.015f
        }
        particles.removeAll { it.life <= 0f }

        // 5. Update custom paint strokes (slow drip from stroke points)
        paintStrokes.forEach { stroke ->
            if (stroke.isDripping && frameCount % 6 == 0L && stroke.points.isNotEmpty()) {
                if (Random.nextFloat() < 0.35f) {
                    val randomPoint = stroke.points[Random.nextInt(stroke.points.size)]
                    // Spawn a droplet trickling from this drawn seal point!
                    drops.add(
                        BloodDrop(
                            x = randomPoint.x,
                            y = randomPoint.y,
                            speed = Random.nextFloat() * 1.6f + 1.0f,
                            width = Random.nextFloat() * 4f + 2f,
                            lengthLimit = Random.nextInt(15, 45)
                        )
                    )
                }
            }
        }
    }

    fun makePoolPath(width: Float, height: Float): Path {
        val path = Path()
        if (width <= 0f) return path
        val sliceWidth = width / (poolResolution - 1)

        path.moveTo(0f, height)
        path.lineTo(0f, height - poolHeights[0])

        for (i in 1 until poolResolution) {
            val cx1 = (i - 0.5f) * sliceWidth
            val cy1 = height - poolHeights[i - 1]
            val cx2 = cx1
            val cy2 = height - poolHeights[i]
            val px = i * sliceWidth
            val py = height - poolHeights[i]
            path.cubicTo(cx1, cy1, cx2, cy2, px, py)
        }

        path.lineTo(width, height)
        path.close()
        return path
    }

    fun clear() {
        drops.clear()
        particles.clear()
        paintStrokes.clear()
        for (i in 0 until poolResolution) {
            poolHeights[i] = 0f
        }
    }
}
