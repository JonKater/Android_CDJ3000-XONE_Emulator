package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.TrackEntity
import com.example.ui.theme.*

@Composable
fun LibraryBrowserDialog(
    tracks: List<TrackEntity>,
    onLoadToDeckA: (TrackEntity) -> Unit,
    onLoadToDeckB: (TrackEntity) -> Unit,
    onAddTrack: (TrackEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTracks = remember(tracks, searchQuery) {
        if (searchQuery.isBlank()) tracks
        else tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true) ||
            it.genre.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 380.dp, max = 560.dp)
                .testTag("library_browser_dialog"),
            colors = CardDefaults.cardColors(containerColor = DjPanelDark),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DjPanelBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryMusic,
                            contentDescription = "Library",
                            tint = DjDeckACyan
                        )
                        Text(
                            text = "TRACK COLLECTION",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = DjTextPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_browser_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = DjTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_track_input"),
                    placeholder = { Text("Search by title, artist, genre...", fontSize = 12.sp, color = DjTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DjTextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DjDeckACyan,
                        unfocusedBorderColor = DjPanelBorder,
                        focusedTextColor = DjTextPrimary,
                        unfocusedTextColor = DjTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Track List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackRowItem(
                            track = track,
                            onLoadA = {
                                onLoadToDeckA(track)
                                onDismiss()
                            },
                            onLoadB = {
                                onLoadToDeckB(track)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Add Demo Groove Button
                Button(
                    onClick = {
                        val newTrack = TrackEntity(
                            title = "Club Groove #${tracks.size + 1}",
                            artist = "Native Synth",
                            bpm = 125.0 + (tracks.size * 2 % 10),
                            initialKey = if (tracks.size % 2 == 0) "8A (Am)" else "5A (Cm)",
                            durationMs = 210_000L,
                            genre = "Tech House"
                        )
                        onAddTrack(newTrack)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_track_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DjPanelElevated),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = DjPlayGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "ADD NEW STEM / TRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DjTextPrimary)
                }
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: TrackEntity,
    onLoadA: () -> Unit,
    onLoadB: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(DjPanelElevated)
            .border(1.dp, DjPanelBorder, RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DjTextPrimary,
                maxLines = 1
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = track.artist, fontSize = 11.sp, color = DjTextSecondary, maxLines = 1)
                Text(text = "• ${track.genre}", fontSize = 10.sp, color = DjTextMuted)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f BPM", track.bpm),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DjLedYellow
                )
                Text(
                    text = track.initialKey,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DjDeckACyan
                )
                val sec = (track.durationMs / 1000).toInt()
                Text(
                    text = String.format(java.util.Locale.US, "%02d:%02d", sec / 60, sec % 60),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = DjTextSecondary
                )
            }
        }

        // Load Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(DjDeckACyan)
                    .clickable { onLoadA() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("load_deck_a_${track.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "LOAD A", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(DjDeckBOrange)
                    .clickable { onLoadB() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("load_deck_b_${track.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "LOAD B", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }
    }
}
