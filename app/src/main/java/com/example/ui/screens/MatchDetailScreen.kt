package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.QuinielaViewModel

@Composable
fun MatchDetailScreen(
    viewModel: QuinielaViewModel,
    matchId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToStadium: (Int) -> Unit
) {
    val matches by viewModel.matches.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    val match = matches.find { it.id == matchId }
    val userPred = predictions.find { it.matchId == matchId }

    var homeStr by remember(userPred) { mutableStateOf(userPred?.predictedHomeScore?.toString() ?: "") }
    var awayStr by remember(userPred) { mutableStateOf(userPred?.predictedAwayScore?.toString() ?: "") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ficha del Partido",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    ) { innerPadding ->
        if (match == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val isPast = System.currentTimeMillis() >= match.dateTime

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(formatTimestamp(match.dateTime), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(getTeamFlag(match.homeTeam), fontSize = 44.sp)
                                Text(match.homeTeam, fontWeight = FontWeight.Bold)
                            }

                            if (match.status == "SCHEDULED") {
                                Text("vs", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.outline)
                            } else {
                                Text("${match.homeScore ?: 0} - ${match.awayScore ?: 0}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(getTeamFlag(match.awayTeam), fontSize = 44.sp)
                                Text(match.awayTeam, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Estado: ${match.status}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Interactive prediction card form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Mi Apuesta / Pronóstico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isPast) {
                            if (userPred != null) {
                                Text("Tu pronóstico fue: ${userPred.predictedHomeScore} - ${userPred.predictedAwayScore}", fontWeight = FontWeight.Bold)
                                if (match.status == "FINISHED") {
                                    Text("Puntos ganados: ${userPred.pointsEarned}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text("No realizaste ningún pronóstico. El partido ya ha comenzado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = homeStr,
                                    onValueChange = { if (it.length <= 2) homeStr = it },
                                    label = { Text(match.homeTeam) },
                                    modifier = Modifier.width(110.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                Text(" - ", fontSize = 24.sp, modifier = Modifier.padding(horizontal = 12.dp))

                                OutlinedTextField(
                                    value = awayStr,
                                    onValueChange = { if (it.length <= 2) awayStr = it },
                                    label = { Text(match.awayTeam) },
                                    modifier = Modifier.width(110.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    val h = homeStr.toIntOrNull()
                                    val a = awayStr.toIntOrNull()
                                    if (h != null && a != null) {
                                        viewModel.submitUserPrediction(match.id, h, a)
                                    }
                                },
                                enabled = homeStr.isNotEmpty() && awayStr.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Salvar Pronóstico")
                            }
                        }
                    }
                }

                // Associated Stadium click helper
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToStadium(match.stadiumId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ver Información del Estadio", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
