package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HotCueUiState
import com.example.ui.theme.*

/**
 * Pioneer CDJ-3000 8 Backlit Hot Cue Buttons (A through H)
 * Positioned in a sleek linear bank with illuminated color frames and time positions.
 */
@Composable
fun HotCuePads(
    deckId: String,
    hotCues: List<HotCueUiState>,
    onCueTrigger: (Int) -> Unit,
    onCueClear: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hot_cues_$deckId")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HOT CUE (A - H)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9AA0B0),
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "LONG PRESS TO CLEAR",
                fontSize = 7.sp,
                color = Color(0xFF6B7280),
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 8 Hot Cues: A through H in 2 rows of 4 (or 1 row of 8 for wide screens)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // First 4 cues (A, B, C, D)
            val firstBatch = hotCues.take(4)
            val secondBatch = hotCues.drop(4).take(4)

            // Render all 8 in a responsive grid or 8-wide row
            hotCues.take(8).forEach { cue ->
                val isSet = cue.isSet
                val cueColor = try {
                    if (isSet) Color(android.graphics.Color.parseColor(cue.colorHex)) else Color(0xFF1C2028)
                } catch (e: Exception) {
                    CdjWaveformHighBlue
                }

                val borderGlow = if (isSet) cueColor else Color(0xFF2C3240)
                val bgGlow = if (isSet) cueColor.copy(alpha = 0.35f) else Color(0xFF14171E)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(bgGlow)
                        .border(
                            width = if (isSet) 1.5.dp else 0.8.dp,
                            color = borderGlow,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .testTag("hot_cue_${deckId}_${cue.index}")
                        .pointerInput(deckId, cue.index) {
                            detectTapGestures(
                                onTap = { onCueTrigger(cue.index) },
                                onLongPress = { onCueClear(cue.index) }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = cue.label.ifEmpty { ('A' + cue.index - 1).toString() },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSet) Color.White else Color(0xFF6B7280)
                        )
                        if (isSet) {
                            val sec = (cue.positionMs / 1000).toInt()
                            Text(
                                text = String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60),
                                fontSize = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
