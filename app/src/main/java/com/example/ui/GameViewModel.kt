package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CognitiveSession
import com.example.data.SessionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

data class GridItem(
    val id: String = UUID.randomUUID().toString(),
    val type: Int, // 0 to 5
    val isMatched: Boolean = false,
    val isTempRevealed: Boolean = false
)

enum class TwistMode(val displayName: String) {
    COLOUR("Colour"),
    EMOTION("Emotion"),
    NUMBERS("Numbers"),
    SYMBOLS("Symbols"),
    LETTERS("Letters")
}

enum class GameType {
    CLASSIC_20, // Classic 20 Moves
    ZEN         // Unlimited moves
}

data class GameState(
    val board: List<List<GridItem>> = emptyList(),
    val selectedPosition: Pair<Int, Int>? = null,
    val score: Int = 0,
    val movesRemaining: Int = 20,
    val totalMovesMade: Int = 0,
    val twistMode: TwistMode = TwistMode.COLOUR,
    val gameType: GameType = GameType.CLASSIC_20,
    val memoryMode: Boolean = false,
    val memoryModeActiveRevealed: Boolean = false, // True when tiles are shown at settle
    val isAnimating: Boolean = false,
    val currentCombo: Int = 0,
    val gameOver: Boolean = false,
    // Live rolling psychology metrics for current game
    val patternRecognition: Float = 50f,
    val planningAhead: Float = 50f,
    val workingMemory: Float = 100f,
    val decisionMaking: Float = 50f,
    val processingSpeed: Float = 50f,
    val showGameSavedToast: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SessionRepository
    val sessionsList: StateFlow<List<CognitiveSession>>

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Internal board dimensions
    private val rows = 6
    private val cols = 6

    // Timing metrics
    private var lastBoardSettleTime: Long = System.currentTimeMillis()
    private val moveTimes = mutableListOf<Long>()
    private var correctMemorySwaps = 0
    private var totalMemorySwaps = 0
    private var comboCounts = mutableListOf<Int>()
    private var decisionOptimalMoves = 0
    private var matchesOf4 = 0
    private var matchesOf5 = 0

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SessionRepository(database.sessionDao())
        sessionsList = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        resetGame()
    }

    fun resetGame() {
        val cleanBoard = generateValidStartingBoard()
        moveTimes.clear()
        correctMemorySwaps = 0
        totalMemorySwaps = 0
        comboCounts.clear()
        decisionOptimalMoves = 0
        matchesOf4 = 0
        matchesOf5 = 0

        _gameState.update {
            it.copy(
                board = cleanBoard,
                selectedPosition = null,
                score = 0,
                movesRemaining = if (it.gameType == GameType.CLASSIC_20) 20 else 999,
                totalMovesMade = 0,
                gameOver = false,
                currentCombo = 0,
                isAnimating = false,
                patternRecognition = 50f,
                planningAhead = 50f,
                workingMemory = 100f,
                decisionMaking = 50f,
                processingSpeed = 50f,
                showGameSavedToast = false
            )
        }
        lastBoardSettleTime = System.currentTimeMillis()
        triggerMemoryPreview()
    }

    private fun triggerMemoryPreview() {
        if (_gameState.value.memoryMode) {
            viewModelScope.launch {
                _gameState.update { it.copy(memoryModeActiveRevealed = true, isAnimating = true) }
                delay(2500) // Show tiles for 2.5 seconds initially
                _gameState.update { it.copy(memoryModeActiveRevealed = false, isAnimating = false) }
                lastBoardSettleTime = System.currentTimeMillis()
            }
        }
    }

    fun setTwistMode(mode: TwistMode) {
        if (_gameState.value.isAnimating) return
        _gameState.update { it.copy(twistMode = mode) }
        resetGame()
    }

    fun setGameType(type: GameType) {
        if (_gameState.value.isAnimating) return
        _gameState.update { it.copy(gameType = type) }
        resetGame()
    }

    fun setMemoryMode(enabled: Boolean) {
        if (_gameState.value.isAnimating) return
        _gameState.update { it.copy(memoryMode = enabled) }
        resetGame()
    }

    fun dismissToast() {
        _gameState.update { it.copy(showGameSavedToast = false) }
    }

    fun selectCell(row: Int, col: Int) {
        val currentState = _gameState.value
        if (currentState.gameOver || currentState.isAnimating) return

        val selected = currentState.selectedPosition
        if (selected == null) {
            // First tap: Select cell
            _gameState.update { it.copy(selectedPosition = Pair(row, col)) }
            // In Memory Mode, we can also temporarily reveal the tapped item for visual feedback
            if (currentState.memoryMode) {
                tempRevealCell(row, col)
            }
        } else {
            // Second tap: Check adjacency
            val (selRow, selCol) = selected
            val isAdjacent = (abs(selRow - row) == 1 && selCol == col) || (abs(selCol - col) == 1 && selRow == row)

            if (isAdjacent) {
                // Swap cells
                performMove(selRow, selCol, row, col)
            } else {
                // If not adjacent, make this cell the new selection (or clear if tapped same)
                if (selRow == row && selCol == col) {
                    _gameState.update { it.copy(selectedPosition = null) }
                } else {
                    _gameState.update {
                        it.copy(selectedPosition = Pair(row, col))
                    }
                    if (currentState.memoryMode) {
                        tempRevealCell(row, col)
                    }
                }
            }
        }
    }

    private fun tempRevealCell(row: Int, col: Int) {
        val currentBoard = _gameState.value.board.map { r -> r.toMutableList() }
        currentBoard[row][col] = currentBoard[row][col].copy(isTempRevealed = true)
        _gameState.update { it.copy(board = currentBoard) }
        viewModelScope.launch {
            delay(1000)
            val resetBoard = _gameState.value.board.map { r -> r.toMutableList() }
            if (row < resetBoard.size && col < resetBoard[row].size) {
                resetBoard[row][col] = resetBoard[row][col].copy(isTempRevealed = false)
                _gameState.update { it.copy(board = resetBoard) }
            }
        }
    }

    private fun performMove(row1: Int, col1: Int, row2: Int, col2: Int) {
        viewModelScope.launch {
            _gameState.update { it.copy(isAnimating = true, selectedPosition = null) }

            // Log time taken for this decision
            val timeTaken = System.currentTimeMillis() - lastBoardSettleTime
            moveTimes.add(timeTaken)

            // Swap board elements
            val boardCopy = _gameState.value.board.map { r -> r.toMutableList() }
            val temp = boardCopy[row1][col1]
            boardCopy[row1][col1] = boardCopy[row2][col2]
            boardCopy[row2][col2] = temp

            _gameState.update { it.copy(board = boardCopy) }
            delay(250) // Wait for swap animation visual

            // Check matches
            val (matchesFound, matchMap) = detectMatches(boardCopy)

            if (matchesFound) {
                // Successful move
                if (_gameState.value.memoryMode) {
                    correctMemorySwaps++
                    totalMemorySwaps++
                }

                // Execute cascade process
                executeCascades(boardCopy, matchMap)
            } else {
                // Unsuccessful move: Swap back
                if (_gameState.value.memoryMode) {
                    totalMemorySwaps++
                }

                val boardRevert = _gameState.value.board.map { r -> r.toMutableList() }
                val tempRevert = boardRevert[row1][col1]
                boardRevert[row1][col1] = boardRevert[row2][col2]
                boardRevert[row2][col2] = tempRevert

                _gameState.update { it.copy(board = boardRevert, isAnimating = false) }
                lastBoardSettleTime = System.currentTimeMillis()
            }
        }
    }

    private suspend fun executeCascades(initialBoard: List<List<GridItem>>, initialMatchMap: List<List<Boolean>>) {
        var currentBoard = initialBoard
        var currentMatchMap = initialMatchMap
        var combo = 0

        while (true) {
            combo++
            _gameState.update { it.copy(currentCombo = combo) }

            // Clear matched items & calculate local statistics
            val clearedBoard = currentBoard.mapIndexed { rIndex, row ->
                row.mapIndexed { cIndex, item ->
                    if (currentMatchMap[rIndex][cIndex]) {
                        item.copy(isMatched = true)
                    } else {
                        item
                    }
                }
            }

            // Update board state to show match particles or transparent items
            _gameState.update { it.copy(board = clearedBoard) }
            delay(350) // Match flash delay

            // Score and evaluate complexity
            val clearedCount = currentMatchMap.flatten().count { it }
            val roundScore = clearedCount * 10 * combo
            updateScoreAndStats(roundScore, clearedCount, combo)

            // Cascade down (items fall down)
            val fellBoard = applyGravity(clearedBoard)
            _gameState.update { it.copy(board = fellBoard) }
            delay(300) // Fall delay

            // Fill empty cells from top
            val refilledBoard = refillGrid(fellBoard)
            _gameState.update { it.copy(board = refilledBoard) }
            delay(250) // Refill delay

            // If Memory Mode, we show newly refilled tiles briefly
            if (_gameState.value.memoryMode) {
                _gameState.update { it.copy(memoryModeActiveRevealed = true) }
                delay(1200)
                _gameState.update { it.copy(memoryModeActiveRevealed = false) }
            }

            // Check for new chain-reaction matches
            val (nextMatchesFound, nextMatchMap) = detectMatches(refilledBoard)
            if (nextMatchesFound) {
                currentBoard = refilledBoard
                currentMatchMap = nextMatchMap
            } else {
                // Board has settled!
                comboCounts.add(combo)
                break
            }
        }

        // Complete the move
        val finalMovesMade = _gameState.value.totalMovesMade + 1
        val finalMovesRemaining = if (_gameState.value.gameType == GameType.CLASSIC_20) {
            max(0, _gameState.value.movesRemaining - 1)
        } else {
            999
        }

        _gameState.update {
            it.copy(
                totalMovesMade = finalMovesMade,
                movesRemaining = finalMovesRemaining,
                currentCombo = 0,
                isAnimating = false
            )
        }

        // Recalculate rolling psychology variables
        recomputeRollingMetrics()

        lastBoardSettleTime = System.currentTimeMillis()

        // Check for Game Over condition
        if (_gameState.value.gameType == GameType.CLASSIC_20 && finalMovesRemaining <= 0) {
            endGameAndSave()
        }
    }

    private fun updateScoreAndStats(points: Int, clearedCount: Int, combo: Int) {
        _gameState.update { it.copy(score = it.score + points) }

        // Track match sizes for Pattern Recognition
        if (clearedCount >= 5) {
            matchesOf5++
        } else if (clearedCount == 4) {
            matchesOf4++
        }

        if (combo >= 2 || clearedCount >= 4) {
            decisionOptimalMoves++
        }
    }

    private fun applyGravity(board: List<List<GridItem>>): List<List<GridItem>> {
        val newBoard = MutableList(rows) { MutableList(cols) { GridItem(type = 0) } }

        for (c in 0 until cols) {
            val columnItems = mutableListOf<GridItem>()
            // Collect all non-matched items from bottom to top
            for (r in rows - 1 downTo 0) {
                if (!board[r][c].isMatched) {
                    columnItems.add(board[r][c])
                }
            }

            // Place remaining items from bottom up
            for (r in rows - 1 downTo 0) {
                val index = rows - 1 - r
                if (index < columnItems.size) {
                    newBoard[r][c] = columnItems[index]
                } else {
                    // Mark as empty (type = -1)
                    newBoard[r][c] = GridItem(type = -1)
                }
            }
        }
        return newBoard
    }

    private fun refillGrid(board: List<List<GridItem>>): List<List<GridItem>> {
        return board.map { row ->
            row.map { item ->
                if (item.type == -1) {
                    GridItem(type = (0..5).random())
                } else {
                    item
                }
            }
        }
    }

    private fun detectMatches(board: List<List<GridItem>>): Pair<Boolean, List<List<Boolean>>> {
        val matchMap = MutableList(rows) { MutableList(cols) { false } }
        var matchesFound = false

        // Horizontal matches
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val type = board[r][c].type
                if (type == -1) {
                    c++
                    continue
                }
                var matchLength = 1
                while (c + matchLength < cols && board[r][c + matchLength].type == type) {
                    matchLength++
                }
                if (matchLength >= 3) {
                    matchesFound = true
                    for (i in 0 until matchLength) {
                        matchMap[r][c + i] = true
                    }
                }
                c += matchLength
            }
        }

        // Vertical matches
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val type = board[r][c].type
                if (type == -1) {
                    r++
                    continue
                }
                var matchLength = 1
                while (r + matchLength < rows && board[r + matchLength][c].type == type) {
                    matchLength++
                }
                if (matchLength >= 3) {
                    matchesFound = true
                    for (i in 0 until matchLength) {
                        matchMap[r + i][c] = true
                    }
                }
                r += matchLength
            }
        }

        return Pair(matchesFound, matchMap)
    }

    private fun recomputeRollingMetrics() {
        val moves = max(1, _gameState.value.totalMovesMade)

        // 1. Processing Speed
        val avgTimeMs = (if (moveTimes.isNotEmpty()) moveTimes.average() else 3000.0).toFloat()
        val procSpeed = when {
            avgTimeMs <= 1500f -> 100f
            avgTimeMs <= 3000f -> 85f - ((avgTimeMs - 1500f) / 1500f * 15f)
            avgTimeMs <= 5000f -> 70f - ((avgTimeMs - 3000f) / 2000f * 15f)
            avgTimeMs <= 10000f -> 50f - ((avgTimeMs - 5000f) / 5000f * 20f)
            else -> 30f
        }.coerceIn(10f, 100f)

        // 2. Pattern Recognition
        val patternPoints = (matchesOf4 * 25f) + (matchesOf5 * 50f) + (comboCounts.size * 10f)
        val patternRec = (50f + patternPoints).coerceIn(40f, 100f)

        // 3. Planning Ahead
        val maxCombo = if (comboCounts.isNotEmpty()) comboCounts.maxOrNull() ?: 1 else 1
        val planAhead = (45f + (maxCombo * 15f) + (comboCounts.count { it >= 2 } * 10f)).coerceIn(40f, 100f)

        // 4. Decision Making
        val decisionScore = (decisionOptimalMoves.toFloat() / moves * 100f + 40f).coerceIn(30f, 100f)

        // 5. Working Memory
        val workMem = if (_gameState.value.memoryMode) {
            if (totalMemorySwaps > 0) {
                (correctMemorySwaps.toFloat() / totalMemorySwaps * 100f).coerceIn(20f, 100f)
            } else {
                100f
            }
        } else {
            // Default or helper score based on standard score progress
            val memoryPenalty = max(0, moves - (decisionOptimalMoves + 2))
            (90f - (memoryPenalty * 2f)).coerceIn(50f, 95f)
        }

        _gameState.update {
            it.copy(
                processingSpeed = procSpeed,
                patternRecognition = patternRec,
                planningAhead = planAhead,
                decisionMaking = decisionScore,
                workingMemory = workMem
            )
        }
    }

    private fun endGameAndSave() {
        val finalState = _gameState.value
        _gameState.update { it.copy(gameOver = true) }

        viewModelScope.launch {
            val session = CognitiveSession(
                score = finalState.score,
                movesCount = finalState.totalMovesMade,
                twistMode = finalState.twistMode.displayName,
                memoryMode = finalState.memoryMode,
                patternRecognition = finalState.patternRecognition,
                planningAhead = finalState.planningAhead,
                workingMemory = finalState.workingMemory,
                decisionMaking = finalState.decisionMaking,
                processingSpeed = finalState.processingSpeed
            )
            repository.insertSession(session)
            _gameState.update { it.copy(showGameSavedToast = true) }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            repository.clearAllSessions()
        }
    }

    private fun generateValidStartingBoard(): List<List<GridItem>> {
        var board = List(rows) { List(cols) { GridItem(type = (0..5).random()) } }
        var hasMatches = true

        while (hasMatches) {
            val (found, matchMap) = detectMatches(board)
            if (found) {
                board = board.mapIndexed { r, row ->
                    row.mapIndexed { c, item ->
                        if (matchMap[r][c]) {
                            GridItem(type = (0..5).random())
                        } else {
                            item
                        }
                    }
                }
            } else {
                hasMatches = false
            }
        }
        return board
    }
}
