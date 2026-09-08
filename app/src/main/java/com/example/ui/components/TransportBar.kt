package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Pioneer CDJ-3000 Transport Section
 * Features the iconic large circular illuminated CUE and PLAY/PAUSE buttons,
 * along with SYNC / MASTER and BEAT JUMP controls.
 */
@Composable
fun TransportBar(
    deckId: String,
    isPlaying: Boolean,
    isCueActive: Boolean,
    isSyncActive: Boolean,
    phraseSyncActive: Boolean,
    quantizeActive: Boolean,
    snapActive: Boolean,
    onPlayPause: () -> Unit,
    onCuePress: () -> Unit,
    onCueRelease: () -> Unit,
    onSync: () -> Unit,
    onPhraseSyncToggle: () -> Unit,
    onQuantizeToggle: () -> Unit,
    onSnapToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transport_$deckId"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pioneer CDJ Large Circular PLAY / PAUSE Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF282D36), Color(0xFF14161B))
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (isPlaying) CdjPlayGreen else Color(0xFF383F4E),
                    shape = CircleShape
                )
                .clickable { onPlayPause() }
                .testTag("play_button_$deckId"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                // Inner illuminated ring
                drawCircle(
                    color = if (isPlaying) CdjPlayGreen.copy(alpha = 0.35f) else Color.Transparent,
                    radius = r - 4.dp.toPx()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) CdjPlayGreen else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isPlaying) "PAUSE" else "PLAY",
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isPlaying) CdjPlayGreen else Color.White
                )
            }
        }

        // 2. Pioneer CDJ Large Circular CUE Button (supports Press & Hold)
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF282D36), Color(0xFF14161B))
                    )
                )
                .border(
                    width = 2.dp,
                    color = if (isCueActive) CdjCueAmber else Color(0xFF383F4E),
                    shape = CircleShape
                )
                .testTag("cue_button_$deckId")
                .pointerInput(deckId) {
                    detectTapGestures(
                        onPress = {
                            onCuePress()
                            tryAwaitRelease()
                            onCueRelease()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                // Inner illuminated ring
                drawCircle(
                    color = if (isCueActive) CdjCueAmber.copy(alpha = 0.35f) else Color.Transparent,
                    radius = r - 4.dp.toPx()
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CUE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isCueActive) CdjCueAmber else Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        // 3. Sync & Quantize/Snap Controls
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // BEAT SYNC Button
                TransportButton(
                    title = "BEAT SYNC",
                    subtitle = if (isSyncActive) "ON" else "OFF",
                    isActive = isSyncActive,
                    activeColor = CdjCueAmber,
                    onClick = onSync,
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )
                // PHRASE SYNC Button
                TransportButton(
                    title = "PHRASE SYNC",
                    subtitle = if (phraseSyncActive) "ON" else "OFF",
                    isActive = phraseSyncActive,
                    activeColor = CdjWaveformHighBlue,
                    onClick = onPhraseSyncToggle,
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )
            }
            
            Column(
                modifier = Modifier.width(44.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // QUANTIZE Button
                TransportButton(
                    title = "Q",
                    isActive = quantizeActive,
                    activeColor = CdjPlayGreen,
                    onClick = onQuantizeToggle,
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )
                // SNAP Button
                TransportButton(
                    title = "S",
                    isActive = snapActive,
                    activeColor = CdjPlayGreen,
                    onClick = onSnapToggle,
                    modifier = Modifier.fillMaxWidth().height(26.dp)
                )
            }
        }
    }
}

@Composable
fun TransportButton(
    title: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) activeColor else Color(0xFF1B1E26))
            .border(
                width = 1.dp,
                color = if (isActive) activeColor else Color(0xFF2E3544),
                shape = RoundedCornerShape(3.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) Color.Black else Color.White
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color.Black.copy(alpha = 0.7f) else Color(0xFF8890A0)
                    )
                }
            }
        }
    }
}

