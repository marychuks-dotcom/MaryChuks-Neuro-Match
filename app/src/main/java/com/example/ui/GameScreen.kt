package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CognitiveSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern luxury theme styling palette
private val DeepSpaceBackground = Color(0xFF0F131E)
private val CardNavy = Color(0xFF192033)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonPink = Color(0xFFFF2A85)
private val GoldAccent = Color(0xFFFFAA00)
private val TextWhite = Color(0xFFF0F4FF)
private val TextMuted = Color(0xFF8C9AB8)

// Color map for "Colour" twist mode (index 0..5)
private val TileColors = listOf(
    Color(0xFFFF3366), // Glowing Red
    Color(0xFF3399FF), // Vibrant Blue
    Color(0xFF33CC66), // Emerald Green
    Color(0xFFFFCC00), // Gold Yellow
    Color(0xFFFF8833), // Sunset Orange
    Color(0xFFCC33FF)  // Cosmic Purple
)

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessionsList.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Play, 1 = Stats / Cognitive Profile

    LaunchedEffect(state.showGameSavedToast) {
        if (state.showGameSavedToast) {
            snackbarHostState.showSnackbar("Game Over! Cognitive performance scores analyzed and saved.")
            viewModel.dismissToast()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBackground)
                .padding(innerPadding)
        ) {
            // Header Bar
            AppHeader()

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DeepSpaceBackground,
                contentColor = TextWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                },
                divider = { Spacer(modifier = Modifier.height(1.dp).background(CardNavy)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Play Puzzle",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    modifier = Modifier.testTag("tab_play")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Cognitive Stats",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.SansSerif
                        )
                    },
                    modifier = Modifier.testTag("tab_stats")
                )
            }

            if (selectedTab == 0) {
                // PLAY PUZZLE TAB
                PlayTabContent(state = state, viewModel = viewModel)
            } else {
                // STATS TAB
                StatsTabContent(sessions = sessions, onClearHistory = { viewModel.clearAllSessions() })
            }
        }
    }

    // Game Over Alert Dialog
    if (state.gameOver) {
        GameOverDialog(state = state, onPlayAgain = { viewModel.resetGame() })
    }
}

@Composable
fun AppHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardNavy)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.radialGradient(listOf(NeonCyan, Color.Transparent)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "App Icon",
                    tint = GoldAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Match-3 Puzzle",
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Cognitive Fitness Engine",
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                .background(DeepSpaceBackground)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "ACTIVE",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PlayTabContent(
    state: GameState,
    viewModel: GameViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row of Stats: Score, Combo, Moves
        item {
            ScoreBoard(state = state, onReset = { viewModel.resetGame() })
        }

        // Twist Selectors: Color, Emotion, Numbers, Symbols, Letters
        item {
            TwistModeSelector(
                selectedMode = state.twistMode,
                onModeSelected = { viewModel.setTwistMode(it) }
            )
        }

        // Extra Mechanics Control: Working Memory Mode Toggle
        item {
            MemoryModeControl(
                memoryMode = state.memoryMode,
                onToggle = { viewModel.setMemoryMode(it) }
            )
        }

        // The Match-3 Grid Board
        item {
            GameBoardSection(state = state, viewModel = viewModel)
        }

        // Live Performance Feedback Dashboard
        item {
            LiveCognitiveGauges(state = state)
        }
    }
}

@Composable
fun ScoreBoard(
    state: GameState,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("SCORE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    "${state.score}",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES LEFT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    if (state.gameType == GameType.CLASSIC_20) "${state.movesRemaining}" else "∞",
                    color = if (state.movesRemaining <= 5 && state.gameType == GameType.CLASSIC_20) NeonPink else NeonCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Combo Banner or Reset Button
            Box(contentAlignment = Alignment.Center) {
                if (state.currentCombo > 1) {
                    Box(
                        modifier = Modifier
                            .background(NeonPink, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "COMBO x${state.currentCombo}",
                            color = TextWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .background(DeepSpaceBackground, CircleShape)
                            .testTag("reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Board", tint = TextWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun TwistModeSelector(
    selectedMode: TwistMode,
    onModeSelected: (TwistMode) -> Unit
) {
    Column {
        Text(
            "CHOOSE GAME TWIST (PATTERN TYPE)",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardNavy, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TwistMode.values().forEach { mode ->
                val isSelected = mode == selectedMode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) DeepSpaceBackground else Color.Transparent)
                        .clickable { onModeSelected(mode) }
                        .padding(vertical = 10.dp)
                        .testTag("twist_tab_${mode.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryModeControl(
    memoryMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(DeepSpaceBackground, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Memory Mode Icon",
                        tint = if (memoryMode) NeonPink else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Working Memory Mode",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tiles hide automatically. Test memory matches!",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = memoryMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonPink,
                    checkedTrackColor = NeonPink.copy(alpha = 0.4f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DeepSpaceBackground
                ),
                modifier = Modifier.testTag("memory_mode_switch")
            )
        }
    }
}

@Composable
fun GameBoardSection(
    state: GameState,
    viewModel: GameViewModel
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(CardNavy, RoundedCornerShape(16.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val board = state.board
        val selected = state.selectedPosition

        val cellWidth = maxWidth / 6
        val cellHeight = maxHeight / 6

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
            for (r in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (c in 0 until 6) {
                        if (r < board.size && c < board[r].size) {
                            val item = board[r][c]
                            val isSelected = selected != null && selected.first == r && selected.second == c

                            Box(
                                modifier = Modifier
                                    .size(cellWidth - 4.dp)
                                    .testTag("cell_${r}_${c}")
                            ) {
                                GridItemView(
                                    item = item,
                                    twistMode = state.twistMode,
                                    memoryMode = state.memoryMode,
                                    memoryActiveRevealed = state.memoryModeActiveRevealed,
                                    isSelected = isSelected,
                                    onTap = { viewModel.selectCell(r, c) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridItemView(
    item: GridItem,
    twistMode: TwistMode,
    memoryMode: Boolean,
    memoryActiveRevealed: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit
) {
    // Determine whether to reveal the item
    // In Memory Mode, reveal if matched, if memoryActiveRevealed is active, if selected, or temporarily revealed
    val shouldReveal = !memoryMode || item.isMatched || memoryActiveRevealed || isSelected || item.isTempRevealed

    val infiniteTransition = rememberInfiniteTransition()
    val isSelectedPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        )
    )

    val scaleValue = if (isSelected) isSelectedPulse else 1.0f
    val selectionBorder = if (isSelected) BorderStroke(2.dp, NeonCyan) else null

    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scaleValue, scaleY = scaleValue)
            .clickable { onTap() },
        shape = RoundedCornerShape(8.dp),
        border = selectionBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (item.isMatched) Color.Transparent else DeepSpaceBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (item.isMatched) {
                // Empty matched cell (will fade out / pop)
            } else if (!shouldReveal) {
                // Hidden item in Memory Mode
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Hidden item",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                // Revealed content depending on twist mode
                when (twistMode) {
                    TwistMode.COLOUR -> {
                        val colorIndex = item.type.coerceIn(0..5)
                        val colorValue = TileColors[colorIndex]
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .shadow(4.dp, CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(colorValue, colorValue.copy(alpha = 0.3f))
                                    ),
                                    shape = CircleShape
                                )
                                .border(1.dp, colorValue.copy(alpha = 0.8f), CircleShape)
                        )
                    }
                    TwistMode.EMOTION -> {
                        val emoji = when (item.type) {
                            0 -> "😄"
                            1 -> "😢"
                            2 -> "😡"
                            3 -> "😱"
                            4 -> "😎"
                            else -> "😴"
                        }
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    TwistMode.NUMBERS -> {
                        val numberText = when (item.type) {
                            0 -> "❶"
                            1 -> "❷"
                            2 -> "❸"
                            3 -> "❹"
                            4 -> "❺"
                            else -> "❻"
                        }
                        Text(
                            text = numberText,
                            color = NeonCyan,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                    TwistMode.SYMBOLS -> {
                        val symbolText = when (item.type) {
                            0 -> "★"
                            1 -> "✦"
                            2 -> "⬥"
                            3 -> "✿"
                            4 -> "☯"
                            else -> "▲"
                        }
                        Text(
                            text = symbolText,
                            color = GoldAccent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    TwistMode.LETTERS -> {
                        val letterText = when (item.type) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            4 -> "E"
                            else -> "F"
                        }
                        Text(
                            text = letterText,
                            color = NeonPink,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCognitiveGauges(state: GameState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "COGNITIVE PERFORMANCE FEEDBACK",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Render 5 psychology variables progress indicators
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CognitiveGaugeItem(
                    label = "Pattern Recognition",
                    score = state.patternRecognition,
                    color = NeonCyan,
                    desc = "Matching complexity & length"
                )
                CognitiveGaugeItem(
                    label = "Planning Ahead",
                    score = state.planningAhead,
                    color = NeonPink,
                    desc = "Chain reaction cascading matches"
                )
                CognitiveGaugeItem(
                    label = "Working Memory",
                    score = state.workingMemory,
                    color = GoldAccent,
                    desc = "Performance under tile concealment"
                )
                CognitiveGaugeItem(
                    label = "Decision Making",
                    score = state.decisionMaking,
                    color = Color(0xFFCC33FF),
                    desc = "Combo optimization & score output"
                )
                CognitiveGaugeItem(
                    label = "Processing Speed",
                    score = state.processingSpeed,
                    color = Color(0xFF33CC66),
                    desc = "Match reaction and search velocity"
                )
            }
        }
    }
}

@Composable
fun CognitiveGaugeItem(
    label: String,
    score: Float,
    color: Color,
    desc: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = TextMuted, fontSize = 10.sp)
            }
            Text(
                "${score.toInt()}/100",
                color = color,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DeepSpaceBackground
        )
    }
}

@Composable
fun StatsTabContent(
    sessions: List<CognitiveSession>,
    onClearHistory: () -> Unit
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "No data yet",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Cognitive Sessions Logged",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Play a complete game in Classic mode (20 moves) to calculate and log your cognitive scores!",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )
            }
        }
    } else {
        // Calculate historical averages
        val avgPattern = sessions.map { it.patternRecognition }.average().toFloat()
        val avgPlanning = sessions.map { it.planningAhead }.average().toFloat()
        val avgMemory = sessions.map { it.workingMemory }.average().toFloat()
        val avgDecision = sessions.map { it.decisionMaking }.average().toFloat()
        val avgSpeed = sessions.map { it.processingSpeed }.average().toFloat()
        val highScore = sessions.maxOf { it.score }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High level Averages Profile Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardNavy),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "COGNITIVE FITNESS PROFILE",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = onClearHistory,
                                modifier = Modifier.size(24.dp).testTag("clear_history_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear History",
                                    tint = NeonPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Historical Performance Average",
                            color = TextWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Based on your last ${sessions.size} sessions",
                            color = TextMuted,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CognitiveGaugeItem(
                                label = "Pattern Recognition",
                                score = avgPattern,
                                color = NeonCyan,
                                desc = "Visual processing & group detection"
                            )
                            CognitiveGaugeItem(
                                label = "Planning Ahead",
                                score = avgPlanning,
                                color = NeonPink,
                                desc = "Cascade set-up and chain anticipation"
                            )
                            CognitiveGaugeItem(
                                label = "Working Memory",
                                score = avgMemory,
                                color = GoldAccent,
                                desc = "Retention & recall with concealed boards"
                            )
                            CognitiveGaugeItem(
                                label = "Decision Making",
                                score = avgDecision,
                                color = Color(0xFFCC33FF),
                                desc = "Yield efficiency per swap"
                            )
                            CognitiveGaugeItem(
                                label = "Processing Speed",
                                score = avgSpeed,
                                color = Color(0xFF33CC66),
                                desc = "Motor response and match velocity"
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DeepSpaceBackground, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🏆 LIFETIME HIGH SCORE: $highScore PTS",
                                color = GoldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Recent Sessions Header
            item {
                Text(
                    "RECENT SESSIONS HISTORY",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Historical List of games
            items(sessions) { session ->
                SessionHistoryItem(session = session)
            }
        }
    }
}

@Composable
fun SessionHistoryItem(session: CognitiveSession) {
    val dateText = remember(session.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        sdf.format(Date(session.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardNavy),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Score: ${session.score} pts",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$dateText • ${session.twistMode}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                // Memory Mode Tag
                if (session.memoryMode) {
                    Box(
                        modifier = Modifier
                            .background(NeonPink.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "MEMORY MODE",
                            color = NeonPink,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini stats indicators in row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MiniStatsIndicator(label = "Pattern", valStr = "${session.patternRecognition.toInt()}", color = NeonCyan)
                MiniStatsIndicator(label = "Planning", valStr = "${session.planningAhead.toInt()}", color = NeonPink)
                MiniStatsIndicator(label = "Memory", valStr = "${session.workingMemory.toInt()}", color = GoldAccent)
                MiniStatsIndicator(label = "Decision", valStr = "${session.decisionMaking.toInt()}", color = Color(0xFFCC33FF))
                MiniStatsIndicator(label = "Speed", valStr = "${session.processingSpeed.toInt()}", color = Color(0xFF33CC66))
            }
        }
    }
}

@Composable
fun MiniStatsIndicator(
    label: String,
    valStr: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(valStr, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun GameOverDialog(
    state: GameState,
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = CardNavy,
        title = {
            Text(
                "Game Session Completed!",
                color = TextWhite,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "FINAL SCORE",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "${state.score}",
                    color = GoldAccent,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "COGNITIVE PERFORMANCE SUMMARY",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Summary progress lists
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    MiniGaugeSummary(label = "Pattern Recognition", score = state.patternRecognition, color = NeonCyan)
                    MiniGaugeSummary(label = "Planning Ahead", score = state.planningAhead, color = NeonPink)
                    MiniGaugeSummary(label = "Working Memory", score = state.workingMemory, color = GoldAccent)
                    MiniGaugeSummary(label = "Decision Making", score = state.decisionMaking, color = Color(0xFFCC33FF))
                    MiniGaugeSummary(label = "Processing Speed", score = state.processingSpeed, color = Color(0xFF33CC66))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_play_again_btn")
            ) {
                Text("Play Again", color = DeepSpaceBackground, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun MiniGaugeSummary(
    label: String,
    score: Float,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(
            "${score.toInt()}/100",
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
