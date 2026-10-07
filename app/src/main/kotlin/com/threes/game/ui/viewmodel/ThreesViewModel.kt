package com.threes.game.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.threes.game.audio.SoundEngine
import com.threes.game.data.PreferencesRepository
import com.threes.game.data.SavedGame
import com.threes.game.engine.GameEngine
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val grid: Array<Array<Tile?>> = Array(4) { Array(4) { null } },
    val score: Int = 0,
    val highScore: Int = 0,
    val nextTile: Tile = Tile.create(1),
    val isGameOver: Boolean = false,
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

class ThreesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesRepository(application)
    private val sound = SoundEngine(application)
    private val engine = GameEngine()

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
            engine.restoreFromState(
                loadedGrid = saved.grid,
                loadedNextTile = saved.nextTile,
                loadedDeck = saved.deck,
                loadedScore = saved.score,
                loadedMaxTile = saved.maxTile,
                loadedMoveCount = saved.moveCount
            )
        } else {
            engine.startNewGame()
        }

        updateUiState()
    }

    fun getMovableLines(direction: Direction): List<Int> {
        return engine.getMovableLines(direction)
    }

    fun onMove(direction: Direction) {
        val result = engine.move(direction)
        if (!result.moved) return

        sound.playSlide()

        // Handle merged tiles: play chimes and unlock characters
        if (result.mergedTiles.isNotEmpty()) {
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

        // Check high score
        var newRecord = false
        if (engine.score > prefs.highScore) {
            prefs.highScore = engine.score
            newRecord = true
        }

        if (engine.isGameOver) {
            if (newRecord) {
                sound.playNewRecord()
            } else {
                sound.playGameOver()
            }
            prefs.clearSavedGame()
        } else {
            saveCurrentGame()
        }

        updateUiState(isNewRecord = newRecord)
    }

    fun onUndo() {
        if (!prefs.isUndoEnabled || !engine.canUndo()) return
        if (engine.undo()) {
            sound.playSnap()
            saveCurrentGame()
            updateUiState()
        }
    }

    fun onStartNewGame() {
        engine.startNewGame()
        sound.playSnap()
        saveCurrentGame()
        updateUiState(isNewRecord = false)
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
        _uiState.value = _uiState.value.copy(isUndoEnabled = enabled)
    }

    fun resetHighScore() {
        prefs.highScore = 0
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
        if (engine.isGameOver) return
        prefs.saveGame(
            SavedGame(
                grid = engine.grid,
                nextTile = engine.nextTile,
                deck = engine.deck.toList(),
                score = engine.score,
                maxTile = engine.maxTileValue,
                moveCount = engine.moveCount
            )
        )
    }

    private fun updateUiState(isNewRecord: Boolean = false) {
        // Create fresh deep copy of grid for Compose observation
        val gridCopy = Array(4) { r ->
            Array(4) { c -> engine.grid[r][c]?.copy() }
        }

        _uiState.value = _uiState.value.copy(
            grid = gridCopy,
            score = engine.score,
            highScore = prefs.highScore,
            nextTile = engine.nextTile,
            isGameOver = engine.isGameOver,
            isNewRecord = isNewRecord,
            canUndo = prefs.isUndoEnabled && engine.canUndo(),
            unlockedCharacters = prefs.getUnlockedCharacters(),
            isNightMode = prefs.isNightMode,
            isSoundEnabled = prefs.isSoundEnabled,
            isHapticsEnabled = prefs.isHapticsEnabled,
            isUndoEnabled = prefs.isUndoEnabled
        )
    }
}
