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
        assertEquals(1, res1.newTiles[0]?.value)
        assertEquals(2, res1.newTiles[1]?.value)
        assertEquals(3, res1.newTiles[2]?.value)
        assertNull(res1.newTiles[3])

        // Case 2: [1, 2, 3, 3] -> [3(merged), 3, 3, null]
        val res2 = engine.shiftLine(listOf(t1, t2, t3a, t3b))
        assertTrue(res2.moved)
        assertEquals(3, res2.newTiles[0]?.value)
        assertEquals(3, res2.newTiles[1]?.value)
        assertEquals(3, res2.newTiles[2]?.value)
        assertNull(res2.newTiles[3])

        // Case 3: [1, null, 2, 3] -> [1, 2, 3, null]
        val res3 = engine.shiftLine(listOf(t1, null, t2, t3a))
        assertTrue(res3.moved)
        assertEquals(1, res3.newTiles[0]?.value)
        assertEquals(2, res3.newTiles[1]?.value)
        assertEquals(3, res3.newTiles[2]?.value)
        assertNull(res3.newTiles[3])

        // Case 4: [1, 1, 2, 3] -> 1+1 no, 1+2 yes -> [1, 3(merged), 3, null]
        val res4 = engine.shiftLine(listOf(t1, t1, t2, t3a))
        assertTrue(res4.moved)
        assertEquals(1, res4.newTiles[0]?.value)
        assertEquals(3, res4.newTiles[1]?.value)
        assertEquals(3, res4.newTiles[2]?.value)
        assertNull(res4.newTiles[3])

        // Case 6: [1, 1, 3, 3] -> 1+1 no, 1+3 no, 3+3 yes -> [1, 1, 6(merged), null]
        val res5 = engine.shiftLine(listOf(t1, t1, t3a, t3b))
        assertTrue(res5.moved)
        assertEquals(1, res5.newTiles[0]?.value)
        assertEquals(1, res5.newTiles[1]?.value)
        assertEquals(6, res5.newTiles[2]?.value)
        assertNull(res5.newTiles[3])

        // Case 7: [1, 1, 1, 1] cannot move left!
        val resBlocked = engine.shiftLine(listOf(t1, t1, t1, t1))
        assertFalse(resBlocked.moved)
        assertEquals(1, resBlocked.newTiles[0]?.value)
        assertEquals(1, resBlocked.newTiles[1]?.value)
        assertEquals(1, resBlocked.newTiles[2]?.value)
        assertEquals(1, resBlocked.newTiles[3]?.value)

        // Case 7: [1, 3, 6, 12] cannot move left!
        val t12 = Tile.create(12)
        val resBlocked2 = engine.shiftLine(listOf(t1, t3a, t6, t12))
        assertFalse(resBlocked2.moved)
    }

    @Test
    fun testUndoCapability() {
        val engine = GameEngine(Random(123))
        val initialScore = engine.score
        val canMoveLeft = engine.canMove(Direction.LEFT)
        if (canMoveLeft) {
            val moveRes = engine.move(Direction.LEFT)
            assertTrue(moveRes.moved)
            assertTrue(engine.canUndo())
            val undoSuccess = engine.undo()
            assertTrue(undoSuccess)
            assertEquals(initialScore, engine.score)
            assertFalse(engine.canUndo())
        }
    }
}
