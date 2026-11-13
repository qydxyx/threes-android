package com.threes.game.ui.viewmodel

import com.threes.game.audio.SoundController
import com.threes.game.data.GameStorage
import com.threes.game.data.SavedGame
import com.threes.game.engine.GameEngine
import com.threes.game.model.Direction
import com.threes.game.model.Tile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private class FakeGameStorage : GameStorage {
    override var highScore: Int = 0
    override var isNightMode: Boolean = false
    override var isSoundEnabled: Boolean = true
    override var isHapticsEnabled: Boolean = true
    override var isUndoEnabled: Boolean = true
    val unlocked = mutableSetOf(1, 2, 3)
    var savedGame: SavedGame? = null

    override fun getUnlockedCharacters(): Set<Int> = unlocked.toSet()

    override fun unlockCharacter(value: Int) {
        unlocked.add(value)
    }

    override fun saveGame(savedGame: SavedGame) {
        this.savedGame = savedGame
    }

    override fun loadGame(): SavedGame? = savedGame

    override fun clearSavedGame() {
        savedGame = null
    }
}

private class FakeSoundController : SoundController {
    override var soundEnabled: Boolean = true
    override var hapticsEnabled: Boolean = true
    val playedMerges = mutableListOf<Int>()
    var snapCount = 0
    var slideCount = 0
    var gameOverCount = 0
    var newRecordCount = 0

    override fun playMerge(tileValue: Int) {
        playedMerges.add(tileValue)
    }

    override fun playSlide() {
        slideCount++
    }

    override fun playSnap() {
        snapCount++
    }

    override fun playGameOver() {
        gameOverCount++
    }

    override fun playNewRecord() {
        newRecordCount++
    }

    override fun playCharacterVoice(value: Int) {}
    override fun vibrateTick() {}
    override fun vibrateSlide() {}
    override fun vibrateDragThreshold() {}
    override fun vibrateBlocked() {}
    override fun vibrateMerge() {}
    override fun vibrateNewRecord() {}
    override fun vibrateGameOver() {}
    override fun release() {}
}

class ThreesViewModelTest {

    private lateinit var storage: FakeGameStorage
    private lateinit var sound: FakeSoundController
    private lateinit var testScope: CoroutineScope

    @Before
    fun setup() {
        storage = FakeGameStorage()
        sound = FakeSoundController()
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun testResetHighScoreReallyClearsBestScore() {
        storage.highScore = 9
        storage.savedGame = SavedGame(
            grid = listOf(
                listOf(Tile.create(1), Tile.create(2), null, null),
                listOf(Tile.create(6), null, null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            ),
            nextTile = Tile.create(3),
            deck = listOf(1, 2, 3),
            score = 9,
            maxTile = 6,
            moveCount = 4,
            isNewRecord = true,
            baselineHighScore = 5
        )
        val engine = GameEngine(Random(10))
        val viewModel = ThreesViewModel(storage, sound, engine, testScope)

        assertEquals(9, viewModel.uiState.value.highScore)
        assertTrue(viewModel.uiState.value.isNewRecord)
        viewModel.resetHighScore()

        assertEquals(0, storage.highScore)
        assertEquals(0, viewModel.uiState.value.highScore)
        assertFalse(viewModel.uiState.value.isNewRecord)
    }

    @Test
    fun testMoveUnlocksCharacterUpdatesHighScoreAndPersistsUndoSnapshot() {
        storage.highScore = 6
        storage.savedGame = SavedGame(
            grid = listOf(
                listOf(Tile.create(3), Tile.create(3), null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            ),
            nextTile = Tile.create(1),
            deck = listOf(1, 2, 3),
            score = 6,
            maxTile = 3,
            moveCount = 1,
            isNewRecord = false,
            baselineHighScore = 6
        )
        val engine = GameEngine(Random(20))
        val viewModel = ThreesViewModel(storage, sound, engine, testScope)

        viewModel.onMove(Direction.LEFT)

        val state = viewModel.uiState.value
        assertEquals(9, state.score)
        assertEquals(9, state.highScore)
        assertTrue(state.isNewRecord)
        assertTrue(state.unlockedCharacters.contains(6))
        assertEquals(listOf(6), sound.playedMerges)
        assertEquals(Direction.LEFT, state.lastMoveDirection)
        assertTrue(state.canUndo)
        assertNotNull(storage.savedGame?.previousSnapshot)

        // Undo reverts score and new-record high score back to baseline 6
        viewModel.onUndo()
        val afterUndo = viewModel.uiState.value
        assertEquals(6, afterUndo.score)
        assertEquals(6, afterUndo.highScore)
        assertFalse(afterUndo.isNewRecord)
        assertFalse(afterUndo.canUndo)
    }

    @Test
    fun testGameOverPersistsBoardAndTriggersNonMergeGameOverAudio() {
        storage.highScore = 5000
        // Move LEFT slides (0,3)=24 into (0,2)=null, and spawns nextTile=1 at (0,3) -> deadlocked!
        storage.savedGame = SavedGame(
            grid = listOf(
                listOf(Tile.create(1), Tile.create(3), null, Tile.create(24)),
                listOf(Tile.create(3), Tile.create(6), Tile.create(12), Tile.create(24)),
                listOf(Tile.create(6), Tile.create(12), Tile.create(24), Tile.create(48)),
                listOf(Tile.create(12), Tile.create(24), Tile.create(48), Tile.create(96))
            ),
            nextTile = Tile.create(1),
            deck = listOf(2, 3),
            score = 1644,
            maxTile = 96,
            moveCount = 30,
            isNewRecord = false,
            baselineHighScore = 5000
        )
        val engine = GameEngine(Random(30))
        val viewModel = ThreesViewModel(storage, sound, engine, testScope)

        viewModel.onMove(Direction.LEFT)

        val state = viewModel.uiState.value
        assertTrue(state.isGameOver)
        assertEquals(1, sound.snapCount)
        assertEquals(1, sound.gameOverCount)
        assertNotNull(storage.savedGame)

        // Starting a new game resets game over and saves a fresh board
        viewModel.onStartNewGame()
        assertFalse(viewModel.uiState.value.isGameOver)
        assertNotNull(storage.savedGame)
    }
}
