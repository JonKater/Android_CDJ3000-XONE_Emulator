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
import com.example.ui.FxUnitUiState
import com.example.ui.theme.*

@Composable
fun FxPanel(
    fxState: FxUnitUiState,
    onDryWet: (Float) -> Unit,
    onParam1: (Float) -> Unit,
    onParam2: (Float) -> Unit,
    onParam3: (Float) -> Unit,
    onToggleAssignA: () -> Unit,
    onToggleAssignB: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DjPanelDark)
            .border(1.dp, DjPanelBorder, RoundedCornerShape(4.dp))
            .padding(4.dp)
            .testTag("fx_unit_${fxState.unitNumber}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unit Badge & Routing
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "FX ${fxState.unitNumber}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = DjSyncAmber
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (fxState.assignDeckA) DjDeckACyan else DjPanelElevated)
                        .clickable { onToggleAssignA() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "A", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (fxState.assignDeckA) Color.Black else DjTextMuted)
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (fxState.assignDeckB) DjDeckBOrange else DjPanelElevated)
                        .clickable { onToggleAssignB() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "B", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (fxState.assignDeckB) Color.Black else DjTextMuted)
                }
            }
        }

        // Dry/Wet & Params
        RotaryKnob(
            value = fxState.dryWet,
            onValueChange = onDryWet,
            label = "D/W",
            size = 28.dp,
            accentColor = DjSyncAmber,
            centerDetent = false
        )

        RotaryKnob(
            value = fxState.param1,
            onValueChange = onParam1,
            label = "TIME",
            size = 28.dp,
            accentColor = DjDeckACyan,
            centerDetent = false
        )

        RotaryKnob(
            value = fxState.param2,
            onValueChange = onParam2,
            label = "FEEDBK",
            size = 28.dp,
            accentColor = DjDeckACyan,
            centerDetent = false
        )

        RotaryKnob(
            value = fxState.param3,
            onValueChange = onParam3,
            label = "COLOR",
            size = 28.dp,
            accentColor = DjDeckACyan,
            centerDetent = false
        )
    }
}
