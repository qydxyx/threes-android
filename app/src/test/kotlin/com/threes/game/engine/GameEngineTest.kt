package com.threes.game.engine

import com.threes.game.model.Direction
import com.threes.game.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun testTileScoreCalculation() {
        assertEquals(0, Tile.create(1).score)
        assertEquals(0, Tile.create(2).score)
        assertEquals(3, Tile.create(3).score)
        assertEquals(9, Tile.create(6).score)
        assertEquals(27, Tile.create(12).score)
        assertEquals(81, Tile.create(24).score)
        assertEquals(243, Tile.create(48).score)
        assertEquals(729, Tile.create(96).score)
        assertEquals(2187, Tile.create(192).score)
        assertEquals(6561, Tile.create(384).score)
        assertEquals(19683, Tile.create(768).score)
        assertEquals(59049, Tile.create(1536).score)
        assertEquals(177147, Tile.create(3072).score)
        assertEquals(531441, Tile.create(6144).score)
    }

    @Test
    fun testMergeRules() {
        val t1 = Tile.create(1)
        val t2 = Tile.create(2)
        val t3 = Tile.create(3)
        val t6 = Tile.create(6)

        // 1 and 2 merge to 3
        assertTrue(Tile.canMerge(t1, t2))
        assertTrue(Tile.canMerge(t2, t1))
        assertEquals(3, Tile.merge(t1, t2).value)
        assertEquals(3, Tile.merge(t2, t1).value)

        // 1 and 1 cannot merge
        assertFalse(Tile.canMerge(t1, t1))
        // 2 and 2 cannot merge
        assertFalse(Tile.canMerge(t2, t2))

        // Same numbers >= 3 merge to double
        assertTrue(Tile.canMerge(t3, t3))
        assertEquals(6, Tile.merge(t3, t3).value)
        assertTrue(Tile.canMerge(t6, t6))
        assertEquals(12, Tile.merge(t6, t6).value)

        // Different numbers >= 3 cannot merge
        assertFalse(Tile.canMerge(t3, t6))
        assertFalse(Tile.canMerge(t1, t3))
        assertFalse(Tile.canMerge(t2, t6))
    }

    @Test
    fun testShiftLineCases() {
        val engine = GameEngine(Random(42))

        val t1 = Tile.create(1)
        val t2 = Tile.create(2)
        val t3a = Tile.create(3)
        val t3b = Tile.create(3)
        val t6 = Tile.create(6)

        // Case 1: [null, 1, 2, 3] -> [1, 2, 3, null]
        val res1 = engine.shiftLine(listOf(null, t1, t2, t3a))
        assertTrue(res1.moved)
        assertEquals(1, res1.firstMovedIndex)
        assertEquals(1, res1.newTiles[0]?.value)
        assertEquals(2, res1.newTiles[1]?.value)
        assertEquals(3, res1.newTiles[2]?.value)
        assertNull(res1.newTiles[3])

        // Case 2: [1, 2, 3, 3] -> [3(merged), 3, 3, null]
        val res2 = engine.shiftLine(listOf(t1, t2, t3a, t3b))
        assertTrue(res2.moved)
        assertEquals(1, res2.firstMovedIndex)
        assertEquals(3, res2.newTiles[0]?.value)
        assertEquals(3, res2.newTiles[1]?.value)
        assertEquals(3, res2.newTiles[2]?.value)
        assertNull(res2.newTiles[3])

        // Case 3: [1, null, 2, 3] -> [1, 2, 3, null]
        val res3 = engine.shiftLine(listOf(t1, null, t2, t3a))
        assertTrue(res3.moved)
        assertEquals(2, res3.firstMovedIndex)
        assertEquals(1, res3.newTiles[0]?.value)
        assertEquals(2, res3.newTiles[1]?.value)
        assertEquals(3, res3.newTiles[2]?.value)
        assertNull(res3.newTiles[3])

        // Case 4: [1, 1, 2, 3] -> 1+1 no, 1+2 yes -> [1, 3(merged), 3, null]
        val res4 = engine.shiftLine(listOf(t1, t1, t2, t3a))
        assertTrue(res4.moved)
        assertEquals(2, res4.firstMovedIndex)
        assertEquals(1, res4.newTiles[0]?.value)
        assertEquals(3, res4.newTiles[1]?.value)
        assertEquals(3, res4.newTiles[2]?.value)
        assertNull(res4.newTiles[3])

        // Case 6: [1, 1, 3, 3] -> 1+1 no, 1+3 no, 3+3 yes -> [1, 1, 6(merged), null]
        val res5 = engine.shiftLine(listOf(t1, t1, t3a, t3b))
        assertTrue(res5.moved)
        assertEquals(3, res5.firstMovedIndex)
        assertEquals(1, res5.newTiles[0]?.value)
        assertEquals(1, res5.newTiles[1]?.value)
        assertEquals(6, res5.newTiles[2]?.value)
        assertNull(res5.newTiles[3])

        // Case 7: [1, 1, 1, 1] cannot move left!
        val resBlocked = engine.shiftLine(listOf(t1, t1, t1, t1))
        assertFalse(resBlocked.moved)
        assertNull(resBlocked.firstMovedIndex)
        assertEquals(1, resBlocked.newTiles[0]?.value)
        assertEquals(1, resBlocked.newTiles[1]?.value)
        assertEquals(1, resBlocked.newTiles[2]?.value)
        assertEquals(1, resBlocked.newTiles[3]?.value)

        // Case 7: [1, 3, 6, 12] cannot move left!
        val t12 = Tile.create(12)
        val resBlocked2 = engine.shiftLine(listOf(t1, t3a, t6, t12))
        assertFalse(resBlocked2.moved)
        assertNull(resBlocked2.firstMovedIndex)
    }

    @Test
    fun testGetMovableCellsPerCellGranularity() {
        val engine = GameEngine(Random(99))
        // Row 0: [3, 2, null, 1] -> moving LEFT: (0,0)=3 and (0,1)=2 are blocked; only (0,3)=1 moves
        // Row 1: [3, 1, 2, 6]    -> moving LEFT: (1,0)=3 is blocked, (1,1)=1 is merge target; (1,2)=2 and (1,3)=6 move
        // Row 2: [1, 1, 1, 1]    -> moving LEFT: completely blocked
        // Row 3: [null, 3, null, 6] -> moving LEFT: (3,1)=3 and (3,3)=6 both move
        val customGrid = listOf(
            listOf(Tile.create(3), Tile.create(2), null, Tile.create(1)),
            listOf(Tile.create(3), Tile.create(1), Tile.create(2), Tile.create(6)),
            listOf(Tile.create(1), Tile.create(1), Tile.create(1), Tile.create(1)),
            listOf(null, Tile.create(3), null, Tile.create(6))
        )
        engine.restoreFromState(
            loadedGrid = customGrid,
            loadedNextTile = Tile.create(2),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 24,
            loadedMaxTile = 6,
            loadedMoveCount = 5
        )

        val movableLeft = engine.getMovableCells(Direction.LEFT)
        // Row 0: only (0, 3)
        assertFalse(movableLeft.contains(0 to 0))
        assertFalse(movableLeft.contains(0 to 1))
        assertTrue(movableLeft.contains(0 to 3))

        // Row 1: (1, 2) and (1, 3) move; (1, 0) and (1, 1) stay
        assertFalse(movableLeft.contains(1 to 0))
        assertFalse(movableLeft.contains(1 to 1))
        assertTrue(movableLeft.contains(1 to 2))
        assertTrue(movableLeft.contains(1 to 3))

        // Row 2: none move
        assertFalse(movableLeft.contains(2 to 0))
        assertFalse(movableLeft.contains(2 to 1))
        assertFalse(movableLeft.contains(2 to 2))
        assertFalse(movableLeft.contains(2 to 3))

        // Row 3: (3, 1) and (3, 3) move
        assertTrue(movableLeft.contains(3 to 1))
        assertTrue(movableLeft.contains(3 to 3))
    }

    @Test
    fun testInitialBoardDealAndDeckBalance() {
        val engine = GameEngine(Random(2026))
        var tileCount = 0
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val tile = engine.grid[r][c]
                if (tile != null) {
                    tileCount++
                    assertTrue(tile.value in listOf(1, 2, 3))
                    assertTrue(tile.isNew)
                }
            }
        }
        assertEquals(9, tileCount)
        assertFalse(engine.isGameOver)
        assertNull(engine.lastMoveDirection)
        // 12-card deck: 9 on board + 1 nextTile => 2 left in deck
        assertEquals(2, engine.deck.size)
    }

    @Test
    fun testFourDirectionMovesAndSpawnEdge() {
        // Test LEFT, RIGHT, UP, DOWN spawn edges deterministically
        val directions = listOf(Direction.LEFT, Direction.RIGHT, Direction.UP, Direction.DOWN)
        for (dir in directions) {
            val engine = GameEngine(Random(11))
            val customGrid = listOf(
                listOf(null, null, null, null),
                listOf(null, Tile.create(1), Tile.create(2), null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            )
            engine.restoreFromState(
                loadedGrid = customGrid,
                loadedNextTile = Tile.create(3),
                loadedDeck = listOf(1, 2, 3),
                loadedScore = 0,
                loadedMaxTile = 3,
                loadedMoveCount = 0
            )

            val result = engine.move(dir)
            assertTrue("Expected move $dir to succeed", result.moved)
            assertEquals(dir, engine.lastMoveDirection)
            assertEquals(1, engine.moveCount)
            val spawnPos = result.spawnedPosition!!
            when (dir) {
                Direction.LEFT -> assertEquals(3, spawnPos.second)
                Direction.RIGHT -> assertEquals(0, spawnPos.second)
                Direction.UP -> assertEquals(3, spawnPos.first)
                Direction.DOWN -> assertEquals(0, spawnPos.first)
            }
        }
    }

    @Test
    fun testRestoreFromStateSyncsTileMaxIdAndUndoSnapshot() {
        val engine = GameEngine(Random(7))
        val highIdTile = Tile(id = 5000L, value = 3)
        val snapTile = Tile(id = 6000L, value = 2)
        val grid = List(4) { r ->
            List(4) { c -> if (r == 0 && c == 0) highIdTile else null }
        }
        val prevGrid = List(4) { r ->
            List(4) { c -> if (r == 0 && c == 1) snapTile else null }
        }
        val snapshot = GameSnapshot(
            grid = prevGrid,
            score = 0,
            nextTile = Tile(id = 6005L, value = 1),
            deck = listOf(1, 2, 3),
            maxTileValue = 2,
            moveCount = 0
        )
        engine.restoreFromState(
            loadedGrid = grid,
            loadedNextTile = Tile(id = 5005L, value = 1),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 3,
            loadedMaxTile = 3,
            loadedMoveCount = 1,
            loadedPreviousSnapshot = snapshot
        )
        val newlyCreated = Tile.create(2)
        assertTrue(newlyCreated.id > 6005L)
        assertTrue(engine.canUndo())
        assertTrue(engine.undo())
        assertEquals(0, engine.score)
        assertEquals(snapTile.id, engine.grid[0][1]?.id)
    }

    @Test
    fun testUndoDeterministicAndClearsAnimationFlags() {
        val engine = GameEngine(Random(123))
        val customGrid = listOf(
            listOf(Tile.create(1), Tile.create(2), null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null)
        )
        engine.restoreFromState(
            loadedGrid = customGrid,
            loadedNextTile = Tile.create(3),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 0,
            loadedMaxTile = 3,
            loadedMoveCount = 0
        )

        val moveRes = engine.move(Direction.LEFT)
        assertTrue(moveRes.moved)
        assertEquals(1, moveRes.mergedTiles.size)
        assertTrue(engine.grid[0][0]?.isMerged == true)
        assertEquals(Direction.LEFT, engine.lastMoveDirection)
        assertTrue(engine.canUndo())

        assertTrue(engine.undo())
        assertEquals(0, engine.score)
        assertEquals(0, engine.moveCount)
        assertNull(engine.lastMoveDirection)
        assertFalse(engine.canUndo())
        assertEquals(1, engine.grid[0][0]?.value)
        assertEquals(2, engine.grid[0][1]?.value)
        assertFalse(engine.grid[0][0]?.isMerged == true)
        assertFalse(engine.grid[0][0]?.isNew == true)
    }

    @Test
    fun testGameOverDetectionAndMoveRejection() {
        val engine = GameEngine(Random(55))
        // Checkerboard of 1s and 3/6/12 so no empty cells and no mergeable neighbors exist
        val deadGrid = listOf(
            listOf(Tile.create(1), Tile.create(3), Tile.create(6), Tile.create(12)),
            listOf(Tile.create(3), Tile.create(6), Tile.create(12), Tile.create(24)),
            listOf(Tile.create(6), Tile.create(12), Tile.create(24), Tile.create(48)),
            listOf(Tile.create(12), Tile.create(24), Tile.create(48), Tile.create(96))
        )
        engine.restoreFromState(
            loadedGrid = deadGrid,
            loadedNextTile = Tile.create(1),
            loadedDeck = listOf(2, 3),
            loadedScore = 500,
            loadedMaxTile = 96,
            loadedMoveCount = 40
        )

        assertTrue(engine.isGameOver)
        for (dir in Direction.values()) {
            assertFalse(engine.canMove(dir))
            assertFalse(engine.move(dir).moved)
        }
    }

    @Test
    fun testBonusCardGenerationAndHintWindow() {
        val engine = GameEngine(Random(777))
        var observedBonus = false
        for (trial in 0 until 300) {
            val board = listOf(
                listOf(Tile.create(384), null, null, null),
                listOf(null, Tile.create(6), null, null),
                listOf(null, null, null, null),
                listOf(null, null, null, null)
            )
            engine.restoreFromState(
                loadedGrid = board,
                loadedNextTile = Tile.create(1),
                loadedDeck = listOf(1, 2, 3),
                loadedScore = 0,
                loadedMaxTile = 384,
                loadedMoveCount = 10
            )
            val res = engine.move(Direction.RIGHT)
            assertTrue(res.moved)
            val next = engine.nextTile
            if (next.isBonus) {
                observedBonus = true
                // Max tile = 384 => limit = 384 / 8 = 48 => candidates: [6, 12, 24, 48]
                assertTrue(next.value in listOf(6, 12, 24, 48))
                assertTrue(next.bonusCandidates.size in 1..3)
                assertTrue(next.bonusCandidates.contains(next.value))
                assertTrue(next.bonusCandidates.all { it in listOf(6, 12, 24, 48) })
            }
        }
        assertTrue("Expected at least one bonus card across 300 moves with maxTile=384", observedBonus)
    }

    @Test
    fun testGetMergingCellsReturnsBothTargetAndMoverAcrossDirections() {
        val engine = GameEngine(Random(99))
        val board = listOf(
            listOf(Tile.create(1), Tile.create(2), Tile.create(6), Tile.create(6)),
            listOf(Tile.create(3), Tile.create(6), Tile.create(3), Tile.create(3)),
            listOf(null, Tile.create(3), null, null),
            listOf(null, Tile.create(3), null, null)
        )
        engine.restoreFromState(
            loadedGrid = board,
            loadedNextTile = Tile.create(1),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 0,
            loadedMaxTile = 6,
            loadedMoveCount = 1
        )

        // LEFT: row 0 merges 1+2 at (0,0)-(0,1); row 1 has 3,6,3,3 -> slot2&3 merge at (1,2)-(1,3)
        assertEquals(
            setOf(0 to 0, 0 to 1, 1 to 2, 1 to 3),
            engine.getMergingCells(Direction.LEFT)
        )

        // RIGHT: row 0 front pair 6+6 merges at (0,2)-(0,3); row 1 front pair 3+3 merges at (1,2)-(1,3)
        assertEquals(
            setOf(0 to 2, 0 to 3, 1 to 2, 1 to 3),
            engine.getMergingCells(Direction.RIGHT)
        )

        // UP: col 0 has 1,3,null,null (no merge); col 1 has 2,6,3,3 -> 3+3 merges at (2,1)-(3,1)
        assertEquals(
            setOf(2 to 1, 3 to 1),
            engine.getMergingCells(Direction.UP)
        )

        // DOWN: col 1 has from bottom 3,3,6,2 -> 3+3 merges at (2,1)-(3,1)
        assertEquals(
            setOf(2 to 1, 3 to 1),
            engine.getMergingCells(Direction.DOWN)
        )
    }

    @Test
    fun testFastModeOpeningBoardScalesWithUnlockedTileAndPreservesDeck() {
        val engine = GameEngine(Random(2026))
        engine.startNewGame(fastMode = true, maxUnlockedTile = 768)

        var totalTiles = 0
        val largeTileValues = mutableListOf<Int>()
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val tile = engine.grid[r][c]
                if (tile != null) {
                    totalTiles++
                    if (tile.value >= 6) {
                        largeTileValues.add(tile.value)
                    }
                }
            }
        }
        assertEquals(9, totalTiles)
        // For maxUnlockedTile = 768, lead limit = 768 / 8 = 96 -> large cluster is [96, 48, 24]
        assertEquals(listOf(96, 48, 24), largeTileValues.sortedDescending())
        assertEquals(96, engine.maxTileValue)
    }

    @Test
    fun testFastModePlacesLargeTilesInPlayerPreferredStationaryCorner() {
        val engine = GameEngine(Random(42))
        // Simulate a player who keeps their largest tiles parked in the bottom-left corner (3, 0),
        // with supporting large tiles at (3, 1) and (2, 0), while moving other tiles on the board.
        val habitBoard = listOf(
            listOf(null, Tile.create(1), Tile.create(2), null),
            listOf(null, null, null, null),
            listOf(Tile.create(24), null, null, null),
            listOf(Tile.create(96), Tile.create(48), Tile.create(3), Tile.create(3))
        )
        engine.restoreFromState(
            loadedGrid = habitBoard,
            loadedNextTile = Tile.create(1),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 1000,
            loadedMaxTile = 96,
            loadedMoveCount = 25
        )

        // Swiping LEFT: (3, 0)=96, (3, 1)=48, and (2, 0)=24 stay stationary while row 0 and (3,2)+(3,3) merge!
        val moveRes = engine.move(Direction.LEFT)
        assertTrue(moveRes.moved)

        val weights = engine.getCellHabitWeights()
        // Verify bottom-left (3, 0) recorded the highest weight, followed by (3, 1) and (2, 0)
        val w30 = weights[3 * 4 + 0]
        val w31 = weights[3 * 4 + 1]
        val w20 = weights[2 * 4 + 0]
        assertTrue("Expected bottom-left anchor (3,0) to have highest habit weight", w30 > w31)
        assertTrue("Expected support cell (3,1) to have higher weight than (2,0)", w31 > w20)
        assertTrue("Expected support cell (2,0) to have positive habit weight", w20 > 0f)

        // Now start a new Fast Mode game (with maxUnlockedTile = 384 -> large tiles [48, 24, 12])
        engine.startNewGame(fastMode = true, maxUnlockedTile = 384)

        // Verify the largest starting tile (48) is placed in the user's preferred corner (3, 0),
        // second largest (24) at (3, 1), and third largest (12) at (2, 0)!
        assertEquals(48, engine.grid[3][0]?.value)
        assertEquals(24, engine.grid[3][1]?.value)
        assertEquals(12, engine.grid[2][0]?.value)

        // Now suppose in the next game the user stacked their large tiles in the bottom-right corner (3, 3),
        // with supporting tiles at (3, 2) and (2, 3):
        val bottomRightBoard = listOf(
            listOf(null, Tile.create(1), Tile.create(2), null),
            listOf(null, null, null, null),
            listOf(null, null, null, Tile.create(48)),
            listOf(Tile.create(1), Tile.create(2), Tile.create(96), Tile.create(192))
        )
        engine.restoreFromState(
            loadedGrid = bottomRightBoard,
            loadedNextTile = Tile.create(3),
            loadedDeck = listOf(1, 2, 3),
            loadedScore = 3000,
            loadedMaxTile = 192,
            loadedMoveCount = 40
        )

        // Start a new Fast Mode game -> should immediately place large tiles in the bottom-right corner (3, 3)
        // instead of sticking to the old bottom-left corner!
        engine.startNewGame(fastMode = true, maxUnlockedTile = 384)
        assertEquals(48, engine.grid[3][3]?.value)
        assertEquals(24, engine.grid[3][2]?.value)
        assertEquals(12, engine.grid[2][3]?.value)
    }

    @Test
    fun testOneTimeShuffleKeepsMaxTileAnchoredClearsUndoAndGuaranteesPlayableBoard() {
        val engine = GameEngine(Random(1234))
        // Deadlocked 4x4 board where max tile 96 is at (3, 3)
        val deadGrid = listOf(
            listOf(Tile.create(1), Tile.create(3), Tile.create(6), Tile.create(12)),
            listOf(Tile.create(3), Tile.create(6), Tile.create(12), Tile.create(24)),
            listOf(Tile.create(6), Tile.create(12), Tile.create(24), Tile.create(48)),
            listOf(Tile.create(12), Tile.create(24), Tile.create(48), Tile.create(96))
        )
        val dummySnapshot = GameSnapshot(
            grid = deadGrid,
            score = 1000,
            nextTile = Tile.create(1),
            deck = listOf(2, 3),
            maxTileValue = 96,
            moveCount = 39
        )
        engine.restoreFromState(
            loadedGrid = deadGrid,
            loadedNextTile = Tile.create(1),
            loadedDeck = listOf(2, 3),
            loadedScore = 1599,
            loadedMaxTile = 96,
            loadedMoveCount = 40,
            loadedPreviousSnapshot = dummySnapshot
        )

        assertTrue(engine.isGameOver)
        assertTrue(engine.canUndo())
        assertTrue(engine.canShuffle())
        assertFalse(engine.hasUsedShuffle)

        val shuffled = engine.shuffleBoard()
        assertTrue(shuffled)
        assertTrue(engine.hasUsedShuffle)
        assertFalse(engine.canShuffle())
        assertFalse("Expected board to have valid moves after shuffle", engine.isGameOver)
        assertFalse("Expected undo snapshot to be cleared after shuffle", engine.canUndo())
        // Max tile 96 must remain anchored at (3, 3)
        assertEquals(96, engine.grid[3][3]?.value)
        // Total tile values on board must remain identical (same score)
        assertEquals(1599, engine.score)

        // Second shuffle attempt in the same game must be rejected
        assertFalse(engine.shuffleBoard())
    }
}
