package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.local.entity.StadiumEntity
import com.example.ui.viewmodel.QuinielaViewModel

@Composable
fun StadiumsMapScreen(
    viewModel: QuinielaViewModel,
    onNavigateToStadiumDetails: (Int) -> Unit
) {
    val stadiums by viewModel.stadiums.collectAsState()
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    // 0 = Ilustrado (Custom Canvas), 1 = Lista (Card view)
    var selectedViewMode by remember { mutableStateOf(0) }
    var selectedStadium by remember { mutableStateOf<StadiumEntity?>(null) }

    // Location Permission Flow
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fineGranted || coarseGranted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Styled Header with Mode Switches
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Canchas & Sedes Oficiales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modes = listOf("Ilustrado", "Lista")
                    modes.forEachIndexed { index, modeTitle ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedViewMode == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    selectedViewMode = index
                                    if (index != 0) {
                                        selectedStadium = null
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = modeTitle,
                                color = if (selectedViewMode == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedViewMode) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F172A))
                            .pointerInput(stadiums) {
                                detectTapGestures { tapOffset ->
                                    val canvasW = size.width
                                    val canvasH = size.height
                                    val xMin = -130.0
                                    val xMax = -65.0
                                    val yMin = 15.0
                                    val yMax = 55.0

                                    var bestStadium: StadiumEntity? = null
                                    var bestDistance = Float.MAX_VALUE

                                    stadiums.forEach { stadium ->
                                        val pctX = (stadium.longitude - xMin) / (xMax - xMin)
                                        val pctY = (stadium.latitude - yMin) / (yMax - yMin)

                                        val projX = (pctX * canvasW).toFloat()
                                        val projY = ((1.0 - pctY) * canvasH).toFloat()

                                        val dx = tapOffset.x - projX
                                        val dy = tapOffset.y - projY
                                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                                        if (dist < 50f && dist < bestDistance) {
                                            bestStadium = stadium
                                            bestDistance = dist
                                        }
                                    }

                                    if (bestStadium != null) {
                                        selectedStadium = bestStadium
                                    }
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasW = size.width
                            val canvasH = size.height

                            val xMin = -130.0
                            val xMax = -65.0
                            val yMin = 15.0
                            val yMax = 55.0

                            val gridColor = Color(0x1A94A3B8)
                            for (lat in 20..50 step 10) {
                                val pctY = (lat - yMin) / (yMax - yMin)
                                val y = ((1.0 - pctY) * canvasH).toFloat()
                                drawLine(gridColor, Offset(0f, y), Offset(canvasW.toFloat(), y), strokeWidth = 1f)
                            }
                            for (lon in -120..-70 step 10) {
                                val pctX = (lon - xMin) / (xMax - xMin)
                                val x = (pctX * canvasW).toFloat()
                                drawLine(gridColor, Offset(x, 0f), Offset(x, canvasH.toFloat()), strokeWidth = 1f)
                            }

                            stadiums.forEach { s1 ->
                                val s2 = stadiums.filter { it.id != s1.id }
                                    .minByOrNull { s2 ->
                                        val dx = s1.longitude - s2.longitude
                                        val dy = s1.latitude - s2.latitude
                                        dx * dx + dy * dy
                                    }

                                if (s2 != null) {
                                    val pctX1 = (s1.longitude - xMin) / (xMax - xMin)
                                    val pctY1 = (s1.latitude - yMin) / (yMax - yMin)
                                    val projX1 = (pctX1 * canvasW).toFloat()
                                    val projY1 = ((1.0 - pctY1) * canvasH).toFloat()

                                    val pctX2 = (s2.longitude - xMin) / (xMax - xMin)
                                    val pctY2 = (s2.latitude - yMin) / (yMax - yMin)
                                    val projX2 = (pctX2 * canvasW).toFloat()
                                    val projY2 = ((1.0 - pctY2) * canvasH).toFloat()

                                    drawLine(
                                        color = Color(0x2238BDF8),
                                        start = Offset(projX1, projY1),
                                        end = Offset(projX2, projY2),
                                        strokeWidth = 2f
                                    )
                                }
                            }

                            stadiums.forEach { stadium ->
                                val pctX = (stadium.longitude - xMin) / (xMax - xMin)
                                val pctY = (stadium.latitude - yMin) / (yMax - yMin)

                                val projX = (pctX * canvasW).toFloat()
                                val projY = ((1.0 - pctY) * canvasH).toFloat()

                                val isSelected = stadium.id == selectedStadium?.id

                                if (isSelected) {
                                    drawCircle(color = Color(0x44F43F5E), radius = 24f, center = Offset(projX, projY))
                                    drawCircle(color = Color(0xFFF43F5E), radius = 8f, center = Offset(projX, projY))
                                } else {
                                    drawCircle(color = Color(0x3338BDF8), radius = 16f, center = Offset(projX, projY))
                                    drawCircle(color = Color(0xFF38BDF8), radius = 6f, center = Offset(projX, projY))
                                }

                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = stadium.city,
                                    style = TextStyle(
                                        color = if (isSelected) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.8f),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    topLeft = Offset(projX + 16f, projY - 12f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Toca un nodo de la cancha para ver detalles de la sede",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(stadiums) { stadium ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToStadiumDetails(stadium.id) }
                                    .testTag("stadium_card_${stadium.id}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column {
                                    AsyncImage(
                                        model = stadium.imageUrl,
                                        contentDescription = stadium.name,
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stadium.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stadium.city,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Capacidad: ${stadium.capacity} personas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stadium.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Map,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Lat: ${stadium.latitude}° N | Lon: ${stadium.longitude}° O",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedViewMode == 0 && selectedStadium != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                selectedStadium?.let { stadium ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToStadiumDetails(stadium.id) }
                            .testTag("stadium_map_popup_${stadium.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = stadium.imageUrl,
                                contentDescription = stadium.name,
                                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stadium.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stadium.city,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Capacidad: ${"%,d".format(stadium.capacity)} personas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Toca para ver partidos ➔",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { selectedStadium = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}