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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioSampler
import com.example.ui.theme.*

@Composable
fun SamplerPanel(
    sampler: AudioSampler,
    onTriggerSample: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DjPanelDark)
            .border(1.dp, DjPanelBorder, RoundedCornerShape(4.dp))
            .padding(6.dp)
            .testTag("sampler_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REMIX SAMPLES",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = DjTextSecondary,
                letterSpacing = 1.sp
            )
            Text(
                text = "INSTANT TRIGGER",
                fontSize = 8.sp,
                color = DjSyncAmber
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sampler.slots.forEachIndexed { index, slot ->
                val slotColor = try {
                    Color(android.graphics.Color.parseColor(slot.colorHex))
                } catch (e: Exception) {
                    DjDeckACyan
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (slot.isPlaying) slotColor else DjPanelElevated)
                        .border(
                            1.dp,
                            if (slot.isPlaying) slotColor else DjPanelBorder,
                            RoundedCornerShape(3.dp)
                        )
                        .clickable { onTriggerSample(index) }
                        .testTag("sample_pad_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = slot.name,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = if (slot.isPlaying) Color.Black else DjTextPrimary
                        )
                        Text(
                            text = if (slot.isPlaying) "HIT" else "READY",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (slot.isPlaying) Color.Black.copy(alpha = 0.8f) else DjTextMuted
                        )
                    }
                }
            }
        }
    }
}
