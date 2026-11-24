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
    override var isPortraitLocked: Boolean = true
    override var isFastMode: Boolean = false
    private var habitWeights: List<Float> = List(16) { 0f }
    val unlocked = mutableSetOf(1, 2, 3)
    var savedGame: SavedGame? = null

    override fun getCellHabitWeights(): List<Float> = habitWeights

    override fun saveCellHabitWeights(weights: List<Float>) {
        habitWeights = List(16) { idx -> weights.getOrElse(idx) { 0f } }
    }

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
        // Before shuffle is used, the 1-time shuffle rescue prompt is offered first
        assertTrue(state.canShuffleRescue)
        assertEquals(1, sound.snapCount)
        assertEquals(0, sound.gameOverCount)
        assertNotNull(storage.savedGame)

        // Declining the shuffle transitions to the Game Over dialog and triggers game-over audio
        viewModel.onDeclineShuffle()
        assertFalse(viewModel.uiState.value.canShuffleRescue)
        assertEquals(1, sound.gameOverCount)

        // Starting a new game resets game over and saves a fresh board
        viewModel.onStartNewGame()
        assertFalse(viewModel.uiState.value.isGameOver)
        assertNotNull(storage.savedGame)
    }

    @Test
    fun testShuffleRescueConfirmContinuesGameAndOnlyAllowsOncePerGame() {
        storage.highScore = 5000
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
        assertTrue(viewModel.uiState.value.isGameOver)
        assertTrue(viewModel.uiState.value.canShuffleRescue)
        assertEquals(listOf(96, 48, 48), viewModel.uiState.value.lockedShuffleTileValues)
        assertTrue(viewModel.uiState.value.canUndo)

        // Confirming shuffle rescues the board, keeps top 3 tiles at (3,3), (2,3), (3,2), clears undo, and marks shuffle used
        viewModel.onConfirmShuffle()
        val afterShuffle = viewModel.uiState.value
        assertFalse(afterShuffle.isGameOver)
        assertFalse(afterShuffle.canShuffleRescue)
        assertFalse(afterShuffle.canUndo)
        assertEquals(96, afterShuffle.grid[3][3]?.value)
        assertEquals(48, afterShuffle.grid[2][3]?.value)
        assertEquals(48, afterShuffle.grid[3][2]?.value)
        assertTrue(storage.savedGame?.hasUsedShuffle == true)
    }

    @Test
    fun testPortraitLockAndFastModeHabitPlacementInViewModel() {
        storage.unlocked.addAll(listOf(6, 12, 24, 48, 96, 192, 384, 768))
        // Pre-populate habit preference favoring top-right corner (0, 3) with support at (0, 2) and (1, 3)
        val customWeights = MutableList(16) { 0f }
        customWeights[0 * 4 + 3] = 20f
        customWeights[0 * 4 + 2] = 12f
        customWeights[1 * 4 + 3] = 8f
        storage.saveCellHabitWeights(customWeights)

        val engine = GameEngine(Random(50))
        val viewModel = ThreesViewModel(storage, sound, engine, testScope)

        assertTrue(viewModel.uiState.value.isPortraitLocked)
        assertFalse(viewModel.uiState.value.isFastMode)

        viewModel.setPortraitLocked(false)
        assertFalse(storage.isPortraitLocked)
        assertFalse(viewModel.uiState.value.isPortraitLocked)

        // Enabling Fast Mode at moveCount == 0 immediately deals a Fast Mode board using habit corner (0, 3)
        viewModel.setFastMode(true)
        assertTrue(storage.isFastMode)
        assertTrue(viewModel.uiState.value.isFastMode)

        val grid = viewModel.uiState.value.grid
        // maxUnlocked = 768 => lead = 96 at (0, 3), 48 at (0, 2), 24 at (1, 3)
        assertEquals(96, grid[0][3]?.value)
        assertEquals(48, grid[0][2]?.value)
        assertEquals(24, grid[1][3]?.value)
    }
}
