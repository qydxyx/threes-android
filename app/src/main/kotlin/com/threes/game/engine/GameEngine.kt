package com.threes.game.engine

import com.threes.game.model.Direction
import com.threes.game.model.LineShiftResult
import com.threes.game.model.Tile
import kotlin.random.Random

data class MoveResult(
    val moved: Boolean,
    val movedLineIndices: List<Int>,
    val mergedTiles: List<Tile>,
    val spawnedTile: Tile?,
    val spawnedPosition: Pair<Int, Int>?
)

data class GameSnapshot(
    val grid: List<List<Tile?>>,
    val score: Int,
    val nextTile: Tile,
    val deck: List<Int>,
    val maxTileValue: Int,
    val moveCount: Int
)

class GameEngine(
    private val random: Random = Random.Default
) {
    val size = 4
    var grid: Array<Array<Tile?>> = Array(size) { Array(size) { null } }
        private set

    val deck: MutableList<Int> = mutableListOf()
    var nextTile: Tile = Tile.create(1)
        private set

    var maxTileValue: Int = 3
        private set

    var score: Int = 0
        private set

    var moveCount: Int = 0
        private set

    var isGameOver: Boolean = false
        private set

    private var previousSnapshot: GameSnapshot? = null

    init {
        startNewGame()
    }

    fun startNewGame() {
        grid = Array(size) { Array(size) { null } }
        deck.clear()
        refillDeck()
        maxTileValue = 3
        moveCount = 0
        previousSnapshot = null

        // Choose 9 random positions out of 16
        val allPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                allPositions.add(r to c)
            }
        }
        allPositions.shuffle(random)

        // Deal 9 cards from deck onto board
        for (i in 0 until 9) {
            val (r, c) = allPositions[i]
            val value = drawFromDeck()
            grid[r][c] = Tile.create(value, isNew = true)
            if (value > maxTileValue) {
                maxTileValue = value
            }
        }

        // Draw the preview card
        nextTile = drawNextTile()
        recalculateScore()
        isGameOver = checkGameOver()
    }

    private fun refillDeck() {
        val base = mutableListOf<Int>()
        repeat(4) { base.add(1) }
        repeat(4) { base.add(2) }
        repeat(4) { base.add(3) }
        base.shuffle(random)
        deck.addAll(base)
    }

    private fun drawFromDeck(): Int {
        if (deck.isEmpty()) {
            refillDeck()
        }
        return deck.removeAt(0)
    }

    private fun drawNextTile(): Tile {
        // Check for bonus high-card if max tile >= 48
        if (maxTileValue >= 48) {
            // Approx 1 in 21 chance (~4.76%)
            if (random.nextInt(21) == 0) {
                val bonusCandidates = mutableListOf<Int>()
                var valCandidate = 6
                val limit = maxTileValue / 8
                while (valCandidate <= limit) {
                    bonusCandidates.add(valCandidate)
                    valCandidate *= 2
                }
                if (bonusCandidates.isNotEmpty()) {
                    val chosenIdx = random.nextInt(bonusCandidates.size)
                    val bonusVal = bonusCandidates[chosenIdx]
                    val hintWindow = if (bonusCandidates.size <= 3) {
                        bonusCandidates.toList()
                    } else {
                        val start = (chosenIdx - 1).coerceIn(0, bonusCandidates.size - 3)
                        bonusCandidates.subList(start, start + 3).toList()
                    }
                    return Tile.create(
                        value = bonusVal,
                        isBonus = true,
                        bonusCandidates = hintWindow
                    )
                }
            }
        }

        val baseVal = drawFromDeck()
        return Tile.create(baseVal, isBonus = false)
    }

    fun canMove(direction: Direction): Boolean {
        return getMovableLines(direction).isNotEmpty()
    }

    fun getMovableLines(direction: Direction): List<Int> {
        val movable = mutableListOf<Int>()
        for (i in 0 until size) {
            val line = getLine(direction, i)
            val shiftResult = shiftLine(line)
            if (shiftResult.moved) {
                movable.add(i)
            }
        }
        return movable
    }

    /**
     * Returns the exact set of board coordinates (row, col) whose tiles actually slide
     * when swiping in [direction]. Stationary tiles at the front of a line that are blocked
     * by the wall or serve as the stationary target of a merge are excluded.
     */
    fun getMovableCells(direction: Direction): Set<Pair<Int, Int>> {
        val cells = mutableSetOf<Pair<Int, Int>>()
        for (lineIdx in 0 until size) {
            val line = getLine(direction, lineIdx)
            val shiftResult = shiftLine(line)
            val firstMoved = shiftResult.firstMovedIndex
            if (shiftResult.moved && firstMoved != null) {
                for (posInLine in firstMoved until size) {
                    if (line[posInLine] != null) {
                        val coord = when (direction) {
                            Direction.LEFT -> lineIdx to posInLine
                            Direction.RIGHT -> lineIdx to (size - 1 - posInLine)
                            Direction.UP -> posInLine to lineIdx
                            Direction.DOWN -> (size - 1 - posInLine) to lineIdx
                        }
                        cells.add(coord)
                    }
                }
            }
        }
        return cells
    }

    fun move(direction: Direction): MoveResult {
        if (isGameOver) {
            return MoveResult(false, emptyList(), emptyList(), null, null)
        }

        val movableLines = getMovableLines(direction)
        if (movableLines.isEmpty()) {
            return MoveResult(false, emptyList(), emptyList(), null, null)
        }

        // Save snapshot for undo
        saveSnapshot()

        val mergedTiles = mutableListOf<Tile>()

        // Clear previous animation flags
        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = grid[r][c]
                if (tile != null && (tile.isMerged || tile.isNew)) {
                    grid[r][c] = tile.copy(isMerged = false, isNew = false)
                }
            }
        }

        // Apply shift to movable lines
        for (lineIdx in movableLines) {
            val line = getLine(direction, lineIdx)
            val result = shiftLine(line)
            setLine(direction, lineIdx, result.newTiles)
            result.mergedTile?.let {
                mergedTiles.add(it)
                if (it.value > maxTileValue) {
                    maxTileValue = it.value
                }
            }
        }

        // Determine spawn candidates on the edge of moved lines
        val spawnCandidates = mutableListOf<Pair<Int, Int>>()
        for (lineIdx in movableLines) {
            val pos = when (direction) {
                Direction.LEFT -> lineIdx to (size - 1)
                Direction.RIGHT -> lineIdx to 0
                Direction.UP -> (size - 1) to lineIdx
                Direction.DOWN -> 0 to lineIdx
            }
            if (grid[pos.first][pos.second] == null) {
                spawnCandidates.add(pos)
            }
        }

        val spawnedTile: Tile?
        val spawnedPos: Pair<Int, Int>?
        if (spawnCandidates.isNotEmpty()) {
            spawnedPos = spawnCandidates[random.nextInt(spawnCandidates.size)]
            val newSpawn = nextTile.copy(isNew = true, bonusCandidates = emptyList())
            grid[spawnedPos.first][spawnedPos.second] = newSpawn
            spawnedTile = newSpawn
            if (newSpawn.value > maxTileValue) {
                maxTileValue = newSpawn.value
            }
            nextTile = drawNextTile()
        } else {
            spawnedTile = null
            spawnedPos = null
        }

        moveCount++
        recalculateScore()
        isGameOver = checkGameOver()

        return MoveResult(
            moved = true,
            movedLineIndices = movableLines,
            mergedTiles = mergedTiles,
            spawnedTile = spawnedTile,
            spawnedPosition = spawnedPos
        )
    }

    private fun getLine(direction: Direction, lineIndex: Int): List<Tile?> {
        return when (direction) {
            Direction.LEFT -> (0 until size).map { c -> grid[lineIndex][c] }
            Direction.RIGHT -> (size - 1 downTo 0).map { c -> grid[lineIndex][c] }
            Direction.UP -> (0 until size).map { r -> grid[r][lineIndex] }
            Direction.DOWN -> (size - 1 downTo 0).map { r -> grid[r][lineIndex] }
        }
    }

    private fun setLine(direction: Direction, lineIndex: Int, tiles: List<Tile?>) {
        when (direction) {
            Direction.LEFT -> {
                for (c in 0 until size) grid[lineIndex][c] = tiles[c]
            }
            Direction.RIGHT -> {
                for (c in 0 until size) grid[lineIndex][size - 1 - c] = tiles[c]
            }
            Direction.UP -> {
                for (r in 0 until size) grid[r][lineIndex] = tiles[r]
            }
            Direction.DOWN -> {
                for (r in 0 until size) grid[size - 1 - r][lineIndex] = tiles[r]
            }
        }
    }

    /**
     * Shifts a 4-tile line towards index 0 by at most 1 step.
     */
    fun shiftLine(line: List<Tile?>): LineShiftResult {
        val c0 = line[0]
        val c1 = line[1]
        val c2 = line[2]
        val c3 = line[3]

        // Case 1: Slot 0 is empty
        if (c0 == null) {
            if (c1 != null || c2 != null || c3 != null) {
                return LineShiftResult(
                    moved = true,
                    newTiles = listOf(c1, c2, c3, null),
                    firstMovedIndex = 1
                )
            }
            return LineShiftResult(moved = false, newTiles = line)
        }

        // Case 2: Slot 0 & 1 can merge
        if (Tile.canMerge(c0, c1)) {
            val merged = Tile.merge(c0, c1!!)
            return LineShiftResult(
                moved = true,
                newTiles = listOf(merged, c2, c3, null),
                firstMovedIndex = 1,
                mergedIndex = 0,
                mergedTile = merged
            )
        }

        // Case 3: Slot 1 is empty
        if (c1 == null) {
            if (c2 != null || c3 != null) {
                return LineShiftResult(
                    moved = true,
                    newTiles = listOf(c0, c2, c3, null),
                    firstMovedIndex = 2
                )
            }
            return LineShiftResult(moved = false, newTiles = line)
        }

        // Case 4: Slot 1 & 2 can merge
        if (Tile.canMerge(c1, c2)) {
            val merged = Tile.merge(c1, c2!!)
            return LineShiftResult(
                moved = true,
                newTiles = listOf(c0, merged, c3, null),
                firstMovedIndex = 2,
                mergedIndex = 1,
                mergedTile = merged
            )
        }

        // Case 5: Slot 2 is empty
        if (c2 == null) {
            if (c3 != null) {
                return LineShiftResult(
                    moved = true,
                    newTiles = listOf(c0, c1, c3, null),
                    firstMovedIndex = 3
                )
            }
            return LineShiftResult(moved = false, newTiles = line)
        }

        // Case 6: Slot 2 & 3 can merge
        if (Tile.canMerge(c2, c3)) {
            val merged = Tile.merge(c2, c3!!)
            return LineShiftResult(
                moved = true,
                newTiles = listOf(c0, c1, merged, null),
                firstMovedIndex = 3,
                mergedIndex = 2,
                mergedTile = merged
            )
        }

        // Case 7: Line cannot move
        return LineShiftResult(moved = false, newTiles = line)
    }

    private fun recalculateScore() {
        var total = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                grid[r][c]?.let { total += it.score }
            }
        }
        score = total
    }

    private fun checkGameOver(): Boolean {
        for (dir in Direction.values()) {
            if (canMove(dir)) return false
        }
        return true
    }

    fun canUndo(): Boolean = previousSnapshot != null

    fun undo(): Boolean {
        val snapshot = previousSnapshot ?: return false
        for (r in 0 until size) {
            for (c in 0 until size) {
                grid[r][c] = snapshot.grid[r][c]
            }
        }
        score = snapshot.score
        nextTile = snapshot.nextTile
        deck.clear()
        deck.addAll(snapshot.deck)
        maxTileValue = snapshot.maxTileValue
        moveCount = snapshot.moveCount
        previousSnapshot = null
        isGameOver = checkGameOver()
        return true
    }

    fun getGridSnapshot(): List<List<Tile?>> {
        return (0 until size).map { r ->
            (0 until size).map { c -> grid[r][c]?.copy() }
        }
    }

    private fun saveSnapshot() {
        previousSnapshot = GameSnapshot(
            grid = getGridSnapshot(),
            score = score,
            nextTile = nextTile.copy(),
            deck = deck.toList(),
            maxTileValue = maxTileValue,
            moveCount = moveCount
        )
    }

    fun restoreFromState(
        loadedGrid: List<List<Tile?>>,
        loadedNextTile: Tile,
        loadedDeck: List<Int>,
        loadedScore: Int,
        loadedMaxTile: Int,
        loadedMoveCount: Int
    ) {
        var maxId = loadedNextTile.id
        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = loadedGrid.getOrNull(r)?.getOrNull(c)
                grid[r][c] = tile
                if (tile != null && tile.id > maxId) {
                    maxId = tile.id
                }
            }
        }
        Tile.syncMaxId(maxId)
        nextTile = loadedNextTile
        deck.clear()
        deck.addAll(loadedDeck)
        score = loadedScore
        maxTileValue = loadedMaxTile
        moveCount = loadedMoveCount
        previousSnapshot = null
        isGameOver = checkGameOver()
    }
}
