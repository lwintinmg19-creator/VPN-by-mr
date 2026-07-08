package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.VpnConfig
import com.example.data.VpnLog
import com.example.service.MyVpnService
import com.example.service.VpnState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VpnScreen(viewModel: VpnViewModel) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val activeConfig by viewModel.activeConfig.collectAsStateWithLifecycle()
    val configs by viewModel.configs.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    val downloadSpeed by viewModel.downloadSpeed.collectAsStateWithLifecycle()
    val uploadSpeed by viewModel.uploadSpeed.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()

    val downloadHistory by viewModel.downloadSpeedHistory.collectAsStateWithLifecycle()
    val uploadHistory by viewModel.uploadSpeedHistory.collectAsStateWithLifecycle()
    val isPingTesting by viewModel.isPingTesting.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Servers, 2: Logs
    var showAddDialog by remember { mutableStateOf(false) }

    // Launcher for Android's system VPN permission dialog
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnAfterPermissionApproved(context)
        }
    }

    // Background Image Skin with Dark Overlay for maximum premium contrast
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B13))
    ) {
        // App Skin Background
        Image(
            painter = painterResource(id = R.drawable.img_app_skin_1783055341504),
            contentDescription = "App Skin",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center
        )
        // Dark premium translucent mask
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD070B13),
                            Color(0xF0070B13),
                            Color(0xFC070B13)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent, // Let the background skin show through!
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0x30000000),
                        titleContentColor = Color.White
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiTethering,
                                contentDescription = "Logo",
                                tint = Color(0xFF00D2FF),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "PyaePhyoHanVpn",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 1.2.sp,
                                color = Color.White,
                                modifier = Modifier.testTag("app_title")
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.pingAllServers() },
                            enabled = !isPingTesting,
                            modifier = Modifier.testTag("ping_refresh_button")
                        ) {
                            if (isPingTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF00FFCC)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.NetworkCheck,
                                    contentDescription = "Diagnostic",
                                    tint = Color(0xFF00FFCC)
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xF00A0F1D),
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("Dashboard", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = "Dashboard"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D2FF),
                            selectedTextColor = Color(0xFF00D2FF),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = Color(0x2000D2FF)
                        ),
                        modifier = Modifier.testTag("tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("Servers", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Dns,
                                contentDescription = "Servers"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D2FF),
                            selectedTextColor = Color(0xFF00D2FF),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = Color(0x2000D2FF)
                        ),
                        modifier = Modifier.testTag("tab_servers")
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        label = { Text("Logs", color = Color.White) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = "Logs"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00D2FF),
                            selectedTextColor = Color(0xFF00D2FF),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = Color(0x2000D2FF)
                        ),
                        modifier = Modifier.testTag("tab_logs")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        connectionState = connectionState,
                        activeConfig = activeConfig,
                        downloadSpeed = downloadSpeed,
                        uploadSpeed = uploadSpeed,
                        durationSeconds = durationSeconds,
                        downloadHistory = downloadHistory,
                        uploadHistory = uploadHistory,
                        onConnectClick = {
                            viewModel.toggleVpnConnection(context) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) {
                                    vpnPrepareLauncher.launch(prepareIntent)
                                }
                            }
                        }
                    )
                    1 -> ServersTab(
                        configs = configs,
                        activeConfig = activeConfig,
                        onSelectConfig = { config -> viewModel.selectActiveConfig(config.id, context) },
                        onDeleteConfig = { config -> viewModel.deleteConfig(config) },
                        onAddClick = { showAddDialog = true }
                    )
                    2 -> LogsTab(
                        logs = logs,
                        onClearClick = { viewModel.clearLogs() }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddServerDialog(
                onDismiss = { showAddDialog = false },
                onAddConfig = { name, server, port, type, uuid, pass ->
                    viewModel.addConfig(name, server, port, type, uuid, pass)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: VpnViewModel,
    connectionState: VpnState,
    activeConfig: VpnConfig?,
    downloadSpeed: Long,
    uploadSpeed: Long,
    durationSeconds: Int,
    downloadHistory: List<Long>,
    uploadHistory: List<Long>,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Active Server Configuration Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x750D1527))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0x2000D2FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dns,
                        contentDescription = "Server Node",
                        tint = Color(0xFF00D2FF),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeConfig?.name ?: "No Server Selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activeConfig != null) "${activeConfig.type} Protocol • ${activeConfig.server}:${activeConfig.port}" else "Select a proxy server from list",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }

                if (activeConfig != null && activeConfig.ping > 0) {
                    Text(
                        text = "${activeConfig.ping} ms",
                        color = getPingColor(activeConfig.ping),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // THE LARGE NEON VPN POWER TOGGLE BUTTON
        VpnConnectionButton(
            connectionState = connectionState,
            durationSeconds = durationSeconds,
            onClick = onConnectClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        // LIVE NETWORK SPEED GAUGES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SpeedCard(
                label = "DOWNLOAD SPEED",
                speedBytes = downloadSpeed,
                isDownload = true,
                modifier = Modifier.weight(1f)
            )
            SpeedCard(
                label = "UPLOAD SPEED",
                speedBytes = uploadSpeed,
                isDownload = false,
                modifier = Modifier.weight(1f)
            )
        }

        // REAL-TIME CANVAS SPEED GRAPH (HIGH FIDELITY)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x65080E1A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-time Traffic Graph (BPS)",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LegendItem(color = Color(0xFF00D2FF), text = "DL")
                        LegendItem(color = Color(0xFFFF007F), text = "UL")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                RealTimeSpeedGraph(
                    downloadHistory = downloadHistory,
                    uploadHistory = uploadHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(text = text, color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VpnConnectionButton(
    connectionState: VpnState,
    durationSeconds: Int,
    onClick: () -> Unit
) {
    // Elegant pulsing and glows
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (connectionState) {
            VpnState.CONNECTED -> Color(0xFF00FFCC)
            VpnState.CONNECTING, VpnState.DISCONNECTING -> Color(0xFF00D2FF)
            VpnState.DISCONNECTED -> Color(0xFFFF0055)
        },
        animationSpec = tween(600),
        label = "btn_color"
    )

    val statusText = when (connectionState) {
        VpnState.CONNECTED -> "CONNECTED"
        VpnState.CONNECTING -> "CONNECTING..."
        VpnState.DISCONNECTING -> "DISCONNECTING..."
        VpnState.DISCONNECTED -> "TAP TO CONNECT"
    }

    Box(
        modifier = Modifier
            .size(230.dp)
            .testTag("vpn_toggle_button"),
        contentAlignment = Alignment.Center
    ) {
        // Glowing Outer Ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        color = buttonColor.copy(alpha = if (connectionState == VpnState.CONNECTED) pulseAlpha * 0.15f else 0.05f),
                        radius = size.minDimension / 2f
                    )
                    drawCircle(
                        color = buttonColor,
                        radius = size.minDimension / 2f - 10.dp.toPx(),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                        )
                    )
                }
        )

        // Middle Inner Pulsing Circle
        Box(
            modifier = Modifier
                .size(175.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = buttonColor,
                    spotColor = buttonColor
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF09101F),
                            Color(0xFF050914)
                        )
                    )
                )
                .clickable { onClick() }
                .drawBehind {
                    drawCircle(
                        color = buttonColor.copy(alpha = 0.04f),
                        radius = size.minDimension / 2f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "Power State",
                    tint = buttonColor,
                    modifier = Modifier.size(54.dp)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = statusText,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )

                if (connectionState == VpnState.CONNECTED) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(durationSeconds),
                        color = Color(0xFF00D2FF),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedCard(
    label: String,
    speedBytes: Long,
    isDownload: Boolean,
    modifier: Modifier = Modifier
) {
    val speedText = formatSpeed(speedBytes)
    val color = if (isDownload) Color(0xFF00D2FF) else Color(0xFFFF007F)
    val icon = if (isDownload) Icons.Filled.CloudDownload else Icons.Filled.CloudUpload

    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x700C1324))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = speedText,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun RealTimeSpeedGraph(
    downloadHistory: List<Long>,
    uploadHistory: List<Long>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (downloadHistory.isEmpty() || uploadHistory.isEmpty()) return@Canvas

        val maxSpeed = (downloadHistory.maxOrNull() ?: 1L).coerceAtLeast(uploadHistory.maxOrNull() ?: 1L).coerceAtLeast(1000L)
        val dlPoints = downloadHistory.size
        val ulPoints = uploadHistory.size

        val dlPath = Path()
        val ulPath = Path()

        // 1. Draw Download Speed Line
        downloadHistory.forEachIndexed { index, value ->
            val x = (index.toFloat() / (dlPoints - 1)) * width
            // Scale and invert because Canvas coordinates start at top-left
            val y = height - ((value.toFloat() / maxSpeed) * (height - 10f)).coerceAtMost(height - 5f)
            
            if (index == 0) {
                dlPath.moveTo(x, y)
            } else {
                dlPath.lineTo(x, y)
            }
        }

        // Draw Download Line
        drawPath(
            path = dlPath,
            color = Color(0xFF00D2FF),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Download Area Gradient
        val dlAreaPath = Path().apply {
            addPath(dlPath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(
            path = dlAreaPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x3000D2FF), Color(0x0000D2FF)),
                startY = 0f,
                endY = height
            )
        )

        // 2. Draw Upload Speed Line
        uploadHistory.forEachIndexed { index, value ->
            val x = (index.toFloat() / (ulPoints - 1)) * width
            val y = height - ((value.toFloat() / maxSpeed) * (height - 10f)).coerceAtMost(height - 5f)
            
            if (index == 0) {
                ulPath.moveTo(x, y)
            } else {
                ulPath.lineTo(x, y)
            }
        }

        // Draw Upload Line
        drawPath(
            path = ulPath,
            color = Color(0xFFFF007F),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Grid reference horizontal line
        drawLine(
            color = Color(0x15FFFFFF),
            start = Offset(0f, height / 2f),
            end = Offset(width, height / 2f),
            strokeWidth = 1f
        )
    }
}

@Composable
fun ServersTab(
    configs: List<VpnConfig>,
    activeConfig: VpnConfig?,
    onSelectConfig: (VpnConfig) -> Unit,
    onDeleteConfig: (VpnConfig) -> Unit,
    onAddClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Server Nodes (${configs.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_config_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Node", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Dns,
                            contentDescription = "No configs",
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No proxy server profiles found", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        val isSelected = activeConfig?.id == config.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .clickable { onSelectConfig(config) }
                                .testTag("server_item_${config.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0x950D2447) else Color(0x650D1527)
                            ),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF00D2FF)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectConfig(config) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF00D2FF),
                                        unselectedColor = Color.LightGray
                                    )
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = config.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${config.type} • ${config.server}:${config.port}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                if (config.ping > 0) {
                                    Text(
                                        text = "${config.ping} ms",
                                        color = getPingColor(config.ping),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteConfig(config) },
                                    modifier = Modifier.testTag("delete_config_${config.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete Server",
                                        tint = Color(0xFFCC445F),
                                        modifier = Modifier.size(20.dp)
                                    )
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
fun LogsTab(
    logs: List<VpnLog>,
    onClearClick: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Automatically scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            coroutineScope.launch {
                scrollState.animateScrollToItem(0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Service Terminal Logs",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            TextButton(
                onClick = onClearClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF0055))
            ) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Clear")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Logs", fontWeight = FontWeight.Bold)
            }
        }

        // Terminal Console Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(4.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xED050811))
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No log data available.", color = Color.DarkGray, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true, // Shows newest logs at the bottom
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { log ->
                        val timeString = remember(log.timestamp) {
                            SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
                        }
                        val color = when (log.level) {
                            "ERROR" -> Color(0xFFFB7185)
                            "WARN" -> Color(0xFFFBBF24)
                            "DEBUG" -> Color(0xFF38BDF8)
                            else -> Color(0xFF34D399) // INFO: Terminal green
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = timeString,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "[${log.level}]",
                                color = color,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.message,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onAddConfig: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var portString by remember { mutableStateOf("443") }
    var type by remember { mutableStateOf("VLESS") } // VLESS, VMess, Shadowsocks, Trojan
    var uuid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val protocols = listOf("VLESS", "VMess", "Trojan", "Shadowsocks")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2F))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Proxy Configuration",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )

                // Server Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Configuration Name", color = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00D2FF),
                        unfocusedBorderColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_config_name_input")
                )

                // Server IP & Port Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it },
                        label = { Text("Server Host / IP", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00D2FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("add_config_host_input")
                    )

                    OutlinedTextField(
                        value = portString,
                        onValueChange = { portString = it },
                        label = { Text("Port", color = Color.LightGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00D2FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_config_port_input")
                    )
                }

                // Type Row
                Column {
                    Text("Protocol Type:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        protocols.forEach { proto ->
                            val isSelected = proto == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF00D2FF) else Color(0x30FFFFFF))
                                    .clickable { type = proto }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = proto,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Secret Credential based on Protocol Selection
                if (type == "VLESS" || type == "VMess") {
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text("UUID", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00D2FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_config_uuid_input")
                    )
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = Color.LightGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00D2FF),
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_config_pass_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color.LightGray, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val finalPort = portString.toIntOrNull() ?: 443
                            if (name.isNotBlank() && server.isNotBlank()) {
                                onAddConfig(name, server, finalPort, type, uuid, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialog_submit_button")
                    ) {
                        Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun getPingColor(ping: Int): Color {
    return when {
        ping <= 0 -> Color.LightGray
        ping < 60 -> Color(0xFF00FFCC) // Fast: neon teal/green
        ping < 140 -> Color(0xFFFBBF24) // Normal: gold
        else -> Color(0xFFFB7185) // Slow: red
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    val kb = bytesPerSec / 1024.0
    return if (kb > 1024) {
        String.format(Locale.getDefault(), "%.1f MB/s", kb / 1024)
    } else {
        String.format(Locale.getDefault(), "%.1f KB/s", kb)
    }
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
