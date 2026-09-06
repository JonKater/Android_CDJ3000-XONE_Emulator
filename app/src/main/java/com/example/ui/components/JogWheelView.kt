package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Pioneer CDJ-3000 Full-Size Mechanical Platter
 * with On-Jog Color LCD Display, real-time rotating needle,
 * textured outer dimple ring, and tactile pitch nudge rim.
 */
@Composable
fun JogWheelView(
    deckId: String,
    rotationAngleDeg: Float,
    isPlaying: Boolean,
    isScratching: Boolean,
    accentColor: Color,
    onJogTouch: (deltaAngleDeg: Float, deltaTimeSec: Float) -> Unit,
    onJogRelease: () -> Unit,
    onJogNudge: (deltaAngleDeg: Float) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 165.dp,
    trackBpm: Double = 126.0,
    isSync: Boolean = false,
    isMaster: Boolean = false
) {
    var lastTouchAngle by remember { mutableFloatStateOf(0f) }
    var lastTouchTime by remember { mutableLongStateOf(0L) }
    var isOuterRim by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(wheelSize)
                .testTag("jog_wheel_$deckId")
                .pointerInput(deckId) {
                    val componentWidth = this.size.width.toFloat()
                    val componentHeight = this.size.height.toFloat()
                    val radius = minOf(componentWidth, componentHeight) / 2f
                    val center = Offset(componentWidth / 2f, componentHeight / 2f)

                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)

                            // Outer 22% rim is for pitch bend nudge; inner 78% is vinyl scratch
                            isOuterRim = dist > (radius * 0.78f)

                            lastTouchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            lastTouchTime = System.currentTimeMillis()
                        },
                        onDragEnd = {
                            onJogRelease()
                        },
                        onDragCancel = {
                            onJogRelease()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val currentPos = change.position
                            val dx = currentPos.x - center.x
                            val dy = currentPos.y - center.y

                            val currentAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            var delta = currentAngle - lastTouchAngle

                            // Discontinuity wrap-around (-180 to 180)
                            if (delta > 180f) delta -= 360f
                            if (delta < -180f) delta += 360f

                            val now = System.currentTimeMillis()
                            val dtSec = max(0.001f, (now - lastTouchTime) / 1000.0f)

                            lastTouchAngle = currentAngle
                            lastTouchTime = now

                            if (isOuterRim) {
                                onJogNudge(delta)
                            } else {
                                onJogTouch(delta, dtSec)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. Pioneer CDJ-3000 Platter Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                val center = Offset(this.size.width / 2f, this.size.height / 2f)

                // Outer CDJ Bezel: Deep charcoal metallic with outer chamfer
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF262930), Color(0xFF14161A), Color(0xFF0D0E12)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // CDJ-3000 Outer Edge Dimple Texture (Finger Grip Notches around perimeter)
                val dimpleCount = 48
                for (i in 0 until dimpleCount) {
                    val deg = (i * 360.0 / dimpleCount)
                    val rad = Math.toRadians(deg)
                    val p1 = Offset(
                        center.x + ((radius - 1.5.dp.toPx()) * cos(rad)).toFloat(),
                        center.y + ((radius - 1.5.dp.toPx()) * sin(rad)).toFloat()
                    )
                    val p2 = Offset(
                        center.x + ((radius - 6.dp.toPx()) * cos(rad)).toFloat(),
                        center.y + ((radius - 6.dp.toPx()) * sin(rad)).toFloat()
                    )
                    drawLine(
                        color = Color(0xFF3E4452),
                        start = p1,
                        end = p2,
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Tactile Nudge Rim separator ring
                val rimInnerRadius = radius * 0.78f
                drawCircle(
                    color = Color(0xFF1B1E24),
                    radius = rimInnerRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Mechanical Vinyl Platter Base (brushed concentric grooves)
                val vinylRadius = rimInnerRadius - 1.5.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1F2228), Color(0xFF121418), Color(0xFF0B0C0E)),
                        center = center,
                        radius = vinylRadius
                    ),
                    radius = vinylRadius,
                    center = center
                )

                // Concentric Vinyl Micro-Grooves
                val grooveSteps = 8
                for (g in 1..grooveSteps) {
                    val gr = vinylRadius * (0.46f + 0.50f * (g.toFloat() / grooveSteps))
                    drawCircle(
                        color = Color(0xFF2E333D).copy(alpha = 0.45f),
                        radius = gr,
                        center = center,
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                // 2. CDJ-3000 On-Jog LCD Bezel Ring
                val lcdBezelRadius = vinylRadius * 0.46f
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3A3F4B), Color(0xFF1E2128)),
                        start = Offset(center.x - lcdBezelRadius, center.y - lcdBezelRadius),
                        end = Offset(center.x + lcdBezelRadius, center.y + lcdBezelRadius)
                    ),
                    radius = lcdBezelRadius,
                    center = center
                )

                drawCircle(
                    color = Color(0xFF121418),
                    radius = lcdBezelRadius - 1.5.dp.toPx(),
                    center = center
                )

                // 3. CDJ-3000 Rotating Outer Needle Indicator Ring
                val needleTrackRadius = lcdBezelRadius - 3.5.dp.toPx()
                drawCircle(
                    color = Color(0xFF181B22),
                    radius = needleTrackRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Rotating Needle Marker (shows precise rotational position of the deck)
                val needleRad = Math.toRadians((rotationAngleDeg - 90.0))
                val needleLength = 7.dp.toPx()
                val needleOuter = Offset(
                    center.x + (needleTrackRadius * cos(needleRad)).toFloat(),
                    center.y + (needleTrackRadius * sin(needleRad)).toFloat()
                )
                val needleInner = Offset(
                    center.x + ((needleTrackRadius - needleLength) * cos(needleRad)).toFloat(),
                    center.y + ((needleTrackRadius - needleLength) * sin(needleRad)).toFloat()
                )

                // Needle Trail Glow Arc
                drawArc(
                    color = if (isScratching) CdjKeyLockRed else CdjWaveformHighBlue.copy(alpha = 0.85f),
                    startAngle = rotationAngleDeg - 105f,
                    sweepAngle = 30f,
                    useCenter = false,
                    topLeft = Offset(center.x - needleTrackRadius, center.y - needleTrackRadius),
                    size = Size(needleTrackRadius * 2, needleTrackRadius * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Sharp White / Red Needle Marker
                drawLine(
                    color = if (isScratching) Color(0xFFFF5252) else Color.White,
                    start = needleInner,
                    end = needleOuter,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Square
                )

                // 4. Center High-Resolution LCD Surface
                val lcdScreenRadius = needleTrackRadius - needleLength - 1.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF12151C), Color(0xFF090A0D)),
                        center = center,
                        radius = lcdScreenRadius
                    ),
                    radius = lcdScreenRadius,
                    center = center
                )

                // Inner LCD Album Art / Track Ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f),
                    radius = lcdScreenRadius * 0.88f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. Center On-Jog LCD Content Overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .size(wheelSize * 0.35f)
                    .clip(CircleShape)
            ) {
                // VINYL badge
                Text(
                    text = if (isScratching) "TOUCH" else "VINYL",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isScratching) CdjKeyLockRed else if (isPlaying) CdjPlayGreen else Color(0xFF8E95A5),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                // Deck Player Badge (DECK 1 / DECK 2)
                Box(
                    modifier = Modifier
                        .padding(vertical = 1.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "DECK $deckId",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // BPM / SYNC readout in On-Jog LCD
                Text(
                    text = String.format("%.1f", trackBpm),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Pioneer CDJ-3000 Jog Wheel Feel & Mode Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isScratching) CdjKeyLockRed else if (isPlaying) CdjPlayGreen else Color(0xFF5A6273))
            )
            Text(
                text = if (isScratching) "SCRATCH ACTIVE" else if (isPlaying) "CDJ-3000 MOTOR ON" else "CDJ-3000 PAUSED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isScratching) CdjKeyLockRed else if (isPlaying) CdjPlayGreen else Color(0xFF7A8394),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.4.sp
            )
        }
    }
}

