package com.threes.game.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.threes.game.audio.SoundController
import com.threes.game.audio.SoundEngine
import com.threes.game.data.GameStorage
import com.threes.game.data.PreferencesRepository
import com.threes.game.data.SavedGame
import com.threes.game.engine.GameEngine
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class GameUiState(
    val grid: List<List<Tile?>> = List(4) { List(4) { null } },
    val score: Int = 0,
    val highScore: Int = 0,
    val nextTile: Tile = Tile.preview(1),
    val lastMoveDirection: Direction? = null,
    val isGameOver: Boolean = false,
    val isGameOverDialogDismissed: Boolean = false,
    val isNewRecord: Boolean = false,
    val canUndo: Boolean = false,
    val unlockedCharacters: Set<Int> = setOf(1, 2, 3),
    val isNightMode: Boolean = false,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isUndoEnabled: Boolean = true,
    val isCastBookOpen: Boolean = false,
    val isSettingsOpen: Boolean = false
)

class ThreesViewModel internal constructor(
    private val prefs: GameStorage,
    private val sound: SoundController,
    private val engine: GameEngine = GameEngine(),
    private val injectedScope: CoroutineScope? = null
) : ViewModel() {

    private val activeScope: CoroutineScope
        get() = injectedScope ?: viewModelScope

    private var baselineHighScore: Int = 0
    private var hasBrokenRecordInSession: Boolean = false
    private var gameOverAudioJob: Job? = null

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        loadSettingsAndState()
    }

    private fun loadSettingsAndState() {
        sound.soundEnabled = prefs.isSoundEnabled
        sound.hapticsEnabled = prefs.isHapticsEnabled

        val saved = prefs.loadGame()
        if (saved != null) {
            baselineHighScore = saved.baselineHighScore
            hasBrokenRecordInSession = saved.isNewRecord
            engine.restoreFromState(
                loadedGrid = saved.grid,
                loadedNextTile = saved.nextTile,
                loadedDeck = saved.deck,
                loadedScore = saved.score,
                loadedMaxTile = saved.maxTile,
                loadedMoveCount = saved.moveCount,
                loadedPreviousSnapshot = saved.previousSnapshot
            )
            if (engine.score > baselineHighScore) {
                hasBrokenRecordInSession = true
            }
        } else {
            baselineHighScore = prefs.highScore
            hasBrokenRecordInSession = false
            engine.startNewGame()
        }

        updateUiState()
    }

    fun getMovableLines(direction: Direction): List<Int> {
        return engine.getMovableLines(direction)
    }

    fun getMovableCells(direction: Direction): Set<Pair<Int, Int>> {
        return engine.getMovableCells(direction)
    }

    fun onSlideCommitStarted() {
        sound.playSlide()
    }

    fun onMove(direction: Direction) {
        val result = engine.move(direction)
        if (!result.moved) return

        gameOverAudioJob?.cancel()

        // Handle merged tiles: play chimes and unlock characters
        val hadMerge = result.mergedTiles.isNotEmpty()
        if (hadMerge) {
            val maxMerged = result.mergedTiles.maxByOrNull { it.value }
            if (maxMerged != null) {
                sound.playMerge(maxMerged.value)
            }
            result.mergedTiles.forEach { tile ->
                prefs.unlockCharacter(tile.value)
            }
        } else {
            sound.playSnap()
        }

        // Also unlock bonus spawned tile if applicable
        result.spawnedTile?.let { spawned ->
            if (spawned.value >= 6) {
                prefs.unlockCharacter(spawned.value)
            }
        }

        // Track high score across the entire session
        if (engine.score > baselineHighScore) {
            hasBrokenRecordInSession = true
        }
        if (engine.score > prefs.highScore) {
            prefs.highScore = engine.score
        }

        if (engine.isGameOver) {
            val isNewRec = hasBrokenRecordInSession
            if (hadMerge) {
                gameOverAudioJob = activeScope.launch {
                    delay(280L)
                    if (isNewRec) sound.playNewRecord() else sound.playGameOver()
                }
            } else {
                if (isNewRec) sound.playNewRecord() else sound.playGameOver()
            }
        }

        saveCurrentGame()
        updateUiState(isGameOverDialogDismissed = false)
    }

    fun onUndo() {
        if (!prefs.isUndoEnabled || !engine.canUndo()) return
        gameOverAudioJob?.cancel()
        if (engine.undo()) {
            sound.playSnap()
            if (hasBrokenRecordInSession) {
                if (engine.score <= baselineHighScore) {
                    hasBrokenRecordInSession = false
                    prefs.highScore = baselineHighScore
                } else if (engine.score < prefs.highScore) {
                    prefs.highScore = engine.score
                }
            }
            saveCurrentGame()
            updateUiState(isGameOverDialogDismissed = false)
        }
    }

    fun onStartNewGame() {
        gameOverAudioJob?.cancel()
        baselineHighScore = prefs.highScore
        hasBrokenRecordInSession = false
        engine.startNewGame()
        sound.playSnap()
        saveCurrentGame()
        updateUiState(isGameOverDialogDismissed = false)
    }

    fun dismissGameOverDialog() {
        _uiState.value = _uiState.value.copy(isGameOverDialogDismissed = true)
    }

    fun showGameOverDialog() {
        _uiState.value = _uiState.value.copy(isGameOverDialogDismissed = false)
    }

    fun playCharacterVoice(value: Int) {
        sound.playCharacterVoice(value)
    }

    fun onDragThresholdReached() {
        sound.vibrateDragThreshold()
    }

    fun onDragBlocked() {
        sound.vibrateBlocked()
    }

    fun onSnapBack() {
        sound.vibrateTick()
    }

    // Settings actions
    fun setNightMode(enabled: Boolean) {
        prefs.isNightMode = enabled
        _uiState.value = _uiState.value.copy(isNightMode = enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.isSoundEnabled = enabled
        sound.soundEnabled = enabled
        _uiState.value = _uiState.value.copy(isSoundEnabled = enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.isHapticsEnabled = enabled
        sound.hapticsEnabled = enabled
        _uiState.value = _uiState.value.copy(isHapticsEnabled = enabled)
    }

    fun setUndoEnabled(enabled: Boolean) {
        prefs.isUndoEnabled = enabled
        _uiState.value = _uiState.value.copy(
            isUndoEnabled = enabled,
            canUndo = enabled && engine.canUndo()
        )
    }

    fun resetHighScore() {
        prefs.highScore = 0
        baselineHighScore = 0
        hasBrokenRecordInSession = false
        saveCurrentGame()
        updateUiState()
    }

    fun openCastBook() {
        _uiState.value = _uiState.value.copy(isCastBookOpen = true)
    }

    fun closeCastBook() {
        _uiState.value = _uiState.value.copy(isCastBookOpen = false)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    private fun saveCurrentGame() {
        prefs.saveGame(
            SavedGame(
                grid = engine.getGridSnapshot(),
                nextTile = engine.nextTile,
                deck = engine.deck.toList(),
                score = engine.score,
                maxTile = engine.maxTileValue,
                moveCount = engine.moveCount,
                isNewRecord = hasBrokenRecordInSession,
                baselineHighScore = baselineHighScore,
                previousSnapshot = engine.getUndoSnapshot()
            )
        )
    }

    private fun updateUiState(
        isGameOverDialogDismissed: Boolean = _uiState.value.isGameOverDialogDismissed
    ) {
        _uiState.value = _uiState.value.copy(
            grid = engine.getGridSnapshot(),
            score = engine.score,
            highScore = prefs.highScore,
            nextTile = engine.nextTile,
            lastMoveDirection = engine.lastMoveDirection,
            isGameOver = engine.isGameOver,
            isGameOverDialogDismissed = if (engine.isGameOver) isGameOverDialogDismissed else false,
            isNewRecord = hasBrokenRecordInSession,
            canUndo = prefs.isUndoEnabled && engine.canUndo(),
            unlockedCharacters = prefs.getUnlockedCharacters(),
            isNightMode = prefs.isNightMode,
            isSoundEnabled = prefs.isSoundEnabled,
            isHapticsEnabled = prefs.isHapticsEnabled,
            isUndoEnabled = prefs.isUndoEnabled
        )
    }

    override fun onCleared() {
        super.onCleared()
        gameOverAudioJob?.cancel()
        sound.release()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ThreesViewModel(
                        prefs = PreferencesRepository(appContext),
                        sound = SoundEngine(appContext)
                    ) as T
                }
            }
        }
    }
}
