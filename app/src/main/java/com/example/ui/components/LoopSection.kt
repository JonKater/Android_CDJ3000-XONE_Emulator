package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Pioneer CDJ-3000 Loop & Beat Jump Section
 * Features 4/8-Beat Loop, Halve/Double (/2, x2), and Pioneer Beat Jump buttons (< 1, 1 >, < 4, 4 >).
 */
@Composable
fun LoopSection(
    deckId: String,
    isLooping: Boolean,
    loopBeats: Double,
    onToggleLoop: () -> Unit,
    onSetLoopSize: (Double) -> Unit,
    onBeatJump: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("loop_section_$deckId"),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Top Row: Loop controls (IN, 4-BEAT LOOP, OUT, /2, x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Halve Loop (/2)
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1B1E26))
                    .border(0.8.dp, Color(0xFF2E3544), RoundedCornerShape(3.dp))
                    .clickable {
                        val next = (loopBeats / 2.0).coerceAtLeast(0.125)
                        onSetLoopSize(next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "÷2",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            // CDJ-3000 4-BEAT / AUTO LOOP Button
            val loopLabel = when (loopBeats) {
                0.125 -> "1/8"
                0.25 -> "1/4"
                0.5 -> "1/2"
                else -> "${loopBeats.toInt()}"
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isLooping) CdjCueAmber else Color(0xFF1E222C))
                    .border(
                        1.dp,
                        if (isLooping) CdjCueAmber else Color(0xFF384052),
                        RoundedCornerShape(3.dp)
                    )
                    .clickable { onToggleLoop() }
                    .testTag("loop_toggle_$deckId"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "4-BEAT",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLooping) Color.Black else Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "[$loopLabel]",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isLooping) Color.Black else CdjWaveformHighBlue,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Double Loop (x2)
            Box(
                modifier = Modifier
                    .weight(0.7f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF1B1E26))
                    .border(0.8.dp, Color(0xFF2E3544), RoundedCornerShape(3.dp))
                    .clickable {
                        val next = (loopBeats * 2.0).coerceAtMost(64.0)
                        onSetLoopSize(next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×2",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }

            // CDJ-3000 Beat Jump Bar (< 1, 1 >, < 4, 4 >)
            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF151820))
                    .border(0.8.dp, Color(0xFF2C3240), RoundedCornerShape(3.dp))
                    .clickable { onBeatJump(-1.0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "< 1",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjWaveformHighBlue,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF151820))
                    .border(0.8.dp, Color(0xFF2C3240), RoundedCornerShape(3.dp))
                    .clickable { onBeatJump(1.0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1 >",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjWaveformHighBlue,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(0.8f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF151820))
                    .border(0.8.dp, Color(0xFF2C3240), RoundedCornerShape(3.dp))
                    .clickable { onBeatJump(4.0) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "4 >",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CdjWaveformHighBlue,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
