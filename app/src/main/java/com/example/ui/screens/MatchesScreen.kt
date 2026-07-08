package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.QuinielaViewModel

@Composable
fun MatchesScreen(
    viewModel: QuinielaViewModel,
    onNavigateToMatchDetails: (Int) -> Unit
) {
    val matches by viewModel.matches.collectAsState()
    val predictions by viewModel.predictions.collectAsState()

    var selectedFilter by remember { mutableStateOf("TODOS") } // "TODOS", "PREDICHOS", "SIN_PREDECIR"

    val homeDrafts = remember { mutableStateMapOf<Int, String>() }
    val awayDrafts = remember { mutableStateMapOf<Int, String>() }

    val filteredList = remember(matches, predictions, selectedFilter) {
        when (selectedFilter) {
            "PREDICHOS" -> matches.filter { m -> predictions.any { p -> p.matchId == m.id } }
            "SIN_PREDECIR" -> matches.filter { m -> m.status == "SCHEDULED" && !predictions.any { p -> p.matchId == m.id } }
            else -> matches
        }
    }

    LaunchedEffect(Unit) {
        viewModel.triggerManualSync()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Partidos Copa 2026",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                IconButton(onClick = { viewModel.triggerManualSync() }) {
                    Icon(Icons.Default.Sync, contentDescription = "Manual sync", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Horizontal Segment filter buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TODOS", "PREDICHOS", "SIN_PREDECIR").forEach { filter ->
                val isSel = selectedFilter == filter
                Button(
                    onClick = { selectedFilter = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (filter == "SIN_PREDECIR") "Por Jugar" else if (filter == "PREDICHOS") "Pronósticos" else "Todos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No hay partidos programados en esta sección.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList) { match ->
                    val prediction = predictions.find { it.matchId == match.id }
                    val homePredStr = homeDrafts[match.id] ?: prediction?.predictedHomeScore?.toString() ?: ""
                    val awayPredStr = awayDrafts[match.id] ?: prediction?.predictedAwayScore?.toString() ?: ""

                    val isPast = System.currentTimeMillis() >= match.dateTime

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMatchDetails(match.id) }
                            .testTag("match_item_${match.id}"),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header: Status, Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (match.status == "LIVE") Color(0xFFFFEBEE) else MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = match.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (match.status == "LIVE") Color.Red else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Text(
                                    text = formatTimestamp(match.dateTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Team and Scores layout matches
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Home
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text(getTeamFlag(match.homeTeam), fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(match.homeTeam, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                // Match score
                                Box(
                                    modifier = Modifier.weight(0.6f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (match.status == "SCHEDULED") {
                                        Text("vs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        Text(
                                            text = "${match.homeScore ?: 0} - ${match.awayScore ?: 0}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (match.status == "LIVE") Color.Red else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 16.sp
                                        )
                                    }
                                }

                                // Away
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text(match.awayTeam, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(getTeamFlag(match.awayTeam), fontSize = 24.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom prediction inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (prediction != null) "Mi Pronóstico" else "Sin Pronóstico",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prediction != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                    if (prediction != null && match.status == "FINISHED") {
                                        Text("Ganado: +${prediction.pointsEarned} pts", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isPast) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (prediction != null) {
                                            Text("${prediction.predictedHomeScore} - ${prediction.predictedAwayScore}", fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Cerrado", color = MaterialTheme.colorScheme.outlineVariant, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ScoreInputField(
                                            value = homePredStr,
                                            onValueChange = { homeDrafts[match.id] = it },
                                            testTag = "match_${match.id}_home_score"
                                        )

                                        Text(":", modifier = Modifier.padding(horizontal = 4.dp), fontWeight = FontWeight.Bold)

                                        ScoreInputField(
                                            value = awayPredStr,
                                            onValueChange = { awayDrafts[match.id] = it },
                                            testTag = "match_${match.id}_away_score"
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                val h = homePredStr.toIntOrNull()
                                                val a = awayPredStr.toIntOrNull()
                                                if (h != null && a != null) {
                                                    viewModel.submitUserPrediction(match.id, h, a)
                                                    homeDrafts.remove(match.id)
                                                    awayDrafts.remove(match.id)
                                                }
                                            },
                                            enabled = homePredStr.isNotEmpty() && awayPredStr.isNotEmpty(),
                                            modifier = Modifier.size(34.dp).testTag("save_prediction_btn_${match.id}")
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Salvar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreInputField(
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(54.dp)
            .height(42.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 2 && newValue.all { it.isDigit() }) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag(testTag),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = "-",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
