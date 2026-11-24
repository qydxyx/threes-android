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

    var hasUsedShuffle: Boolean = false
        private set

    var lastMoveDirection: Direction? = null
        private set

    private var previousSnapshot: GameSnapshot? = null

    private val cellHabitWeights = FloatArray(size * size)

    init {
        startNewGame()
    }

    fun getCellHabitWeights(): List<Float> = cellHabitWeights.toList()

    fun setCellHabitWeights(weights: List<Float>) {
        for (i in 0 until (size * size)) {
            val w = weights.getOrNull(i) ?: 0f
            cellHabitWeights[i] = if (w.isFinite() && w >= 0f) w else 0f
        }
    }

    /**
     * Captures the current board's large tile positions so the next Fast Mode game places its
     * starting large tiles in the exact corner/cluster where the user stacked them in the previous game.
     */
    fun observeCurrentBoardHabits() {
        val boardMax = (0 until size).maxOf { r ->
            (0 until size).maxOf { c -> grid[r][c]?.value ?: 0 }
        }
        val effectiveMax = maxOf(maxTileValue, boardMax)
        if (effectiveMax < 6) return

        // Reset weights to reflect the previous game's actual layout directly
        for (i in cellHabitWeights.indices) {
            cellHabitWeights[i] = 0f
        }

        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = grid[r][c] ?: continue
                if (tile.value < 6) continue
                val tierScore = when {
                    tile.value >= effectiveMax -> 100f
                    tile.value >= effectiveMax / 2 -> 60f
                    tile.value >= effectiveMax / 4 -> 35f
                    else -> 15f
                }
                val idx = r * size + c
                cellHabitWeights[idx] = tierScore + tile.value * 0.05f
            }
        }
    }

    fun startNewGame(
        fastMode: Boolean = false,
        maxUnlockedTile: Int = 3
    ) {
        if (moveCount > 0 && maxTileValue >= 6) {
            observeCurrentBoardHabits()
        }
        grid = Array(size) { Array(size) { null } }
        deck.clear()
        refillDeck()
        maxTileValue = 3
        moveCount = 0
        hasUsedShuffle = false
        previousSnapshot = null
        lastMoveDirection = null

        val allPositions = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                allPositions.add(r to c)
            }
        }

        if (fastMode) {
            val largeValues = buildFastModeLargeTiles(maxUnlockedTile)
            val largePositions = selectFastModeLargeTilePositions(largeValues.size)
            for (i in largePositions.indices) {
                val (r, c) = largePositions[i]
                val value = largeValues[i]
                grid[r][c] = Tile.create(value, isBonus = true, isNew = true)
                if (value > maxTileValue) {
                    maxTileValue = value
                }
            }

            val remainingPositions = allPositions.filter { it !in largePositions }.toMutableList()
            remainingPositions.shuffle(random)
            val baseDealCount = (9 - largePositions.size).coerceAtLeast(0)
            for (i in 0 until baseDealCount) {
                val (r, c) = remainingPositions[i]
                val value = drawFromDeck()
                grid[r][c] = Tile.create(value, isNew = true)
                if (value > maxTileValue) {
                    maxTileValue = value
                }
            }
        } else {
            // Choose 9 random positions out of 16
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
        }

        // Draw the preview card
        nextTile = drawNextTile()
        recalculateScore()
        isGameOver = checkGameOver()
    }

    private fun buildFastModeLargeTiles(maxUnlockedTile: Int): List<Int> {
        val targetLimit = maxOf(48, maxUnlockedTile / 8).coerceAtMost(768)
        var leadValue = 6
        while (leadValue * 2 <= targetLimit) {
            leadValue *= 2
        }
        val secondValue = (leadValue / 2).coerceAtLeast(6)
        val thirdValue = (secondValue / 2).coerceAtLeast(6)
        return listOf(leadValue, secondValue, thirdValue)
    }

    private fun selectFastModeLargeTilePositions(count: Int): List<Pair<Int, Int>> {
        if (count <= 0) return emptyList()
        val hasHabitHistory = cellHabitWeights.any { it > 0.001f }

        val anchorPos: Pair<Int, Int> = if (hasHabitHistory) {
            var bestPos = (size - 1) to (size - 1)
            var bestScore = Float.NEGATIVE_INFINITY
            var bestDirect = Float.NEGATIVE_INFINITY
            for (r in 0 until size) {
                for (c in 0 until size) {
                    val direct = cellHabitWeights[r * size + c]
                    var neighborSum = 0f
                    for ((nr, nc) in getOrthogonalNeighbors(r, c)) {
                        neighborSum += cellHabitWeights[nr * size + nc]
                    }
                    val structuralFactor = when {
                        isCorner(r, c) -> 1.15f
                        isEdge(r, c) -> 1.05f
                        else -> 0.85f
                    }
                    val score = (direct + 0.18f * neighborSum) * structuralFactor +
                        when {
                            isCorner(r, c) -> 0.002f
                            isEdge(r, c) -> 0.001f
                            else -> 0f
                        }
                    if (score > bestScore || (score == bestScore && direct > bestDirect)) {
                        bestScore = score
                        bestDirect = direct
                        bestPos = r to c
                    }
                }
            }
            bestPos
        } else {
            val corners = listOf(0 to 0, 0 to (size - 1), (size - 1) to 0, (size - 1) to (size - 1))
            corners[random.nextInt(corners.size)]
        }

        val placed = mutableListOf(anchorPos)
        while (placed.size < count) {
            val candidates = mutableSetOf<Pair<Int, Int>>()
            for ((pr, pc) in placed) {
                for (neighbor in getOrthogonalNeighbors(pr, pc)) {
                    if (neighbor !in placed) {
                        candidates.add(neighbor)
                    }
                }
            }
            if (candidates.isEmpty()) break

            val nextPos = candidates.maxWithOrNull(
                Comparator { a, b ->
                    val wa = cellHabitWeights[a.first * size + a.second]
                    val wb = cellHabitWeights[b.first * size + b.second]
                    val structuralA = when {
                        isCorner(a.first, a.second) -> 0.02f
                        isEdge(a.first, a.second) -> 0.01f
                        else -> 0f
                    }
                    val structuralB = when {
                        isCorner(b.first, b.second) -> 0.02f
                        isEdge(b.first, b.second) -> 0.01f
                        else -> 0f
                    }
                    val distA = kotlin.math.abs(a.first - anchorPos.first) + kotlin.math.abs(a.second - anchorPos.second)
                    val distB = kotlin.math.abs(b.first - anchorPos.first) + kotlin.math.abs(b.second - anchorPos.second)
                    val scoreA = wa + structuralA - distA * 0.001f
                    val scoreB = wb + structuralB - distB * 0.001f
                    scoreA.compareTo(scoreB)
                }
            ) ?: break
            placed.add(nextPos)
        }
        return placed
    }

    private fun isCorner(r: Int, c: Int): Boolean {
        return (r == 0 || r == size - 1) && (c == 0 || c == size - 1)
    }

    private fun isEdge(r: Int, c: Int): Boolean {
        return r == 0 || r == size - 1 || c == 0 || c == size - 1
    }

    private fun getOrthogonalNeighbors(r: Int, c: Int): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        if (r > 0) result.add((r - 1) to c)
        if (r < size - 1) result.add((r + 1) to c)
        if (c > 0) result.add(r to (c - 1))
        if (c < size - 1) result.add(r to (c + 1))
        return result
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

    /**
     * Returns the coordinates (row, col) of both tiles (the stationary target and the incoming
     * mover) that will merge when swiping in [direction].
     */
    fun getMergingCells(direction: Direction): Set<Pair<Int, Int>> {
        val cells = mutableSetOf<Pair<Int, Int>>()
        for (lineIdx in 0 until size) {
            val line = getLine(direction, lineIdx)
            val shiftResult = shiftLine(line)
            val mergedIdx = shiftResult.mergedIndex
            if (shiftResult.moved && mergedIdx != null) {
                for (posInLine in mergedIdx..(mergedIdx + 1)) {
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

        val movableCells = getMovableCells(direction)
        val stationaryBeforeMove = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (grid[r][c] != null && (r to c) !in movableCells) {
                    stationaryBeforeMove.add(r to c)
                }
            }
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

        recordMoveHabit(stationaryBeforeMove)

        moveCount++
        lastMoveDirection = direction
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

    private fun recordMoveHabit(stationaryCells: Set<Pair<Int, Int>>) {
        if (maxTileValue < 6) return
        for (i in cellHabitWeights.indices) {
            cellHabitWeights[i] *= 0.75f
        }
        val threshold = maxOf(6, maxTileValue / 4)
        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = grid[r][c] ?: continue
                if (tile.value < threshold) continue
                val tierWeight = when {
                    tile.value >= maxTileValue -> 30.0f
                    tile.value >= maxTileValue / 2 -> 16.0f
                    else -> 8.0f
                }
                val stationaryMultiplier = if ((r to c) in stationaryCells) 2.5f else 1.0f
                val idx = r * size + c
                cellHabitWeights[idx] = (
                    cellHabitWeights[idx] + tierWeight * stationaryMultiplier
                ).coerceAtMost(1000f)
            }
        }
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

    fun canShuffle(): Boolean {
        if (hasUsedShuffle) return false
        return hasPlayableShuffleArrangement()
    }

    fun getLockedShuffleTileValues(maxLockedCount: Int = 3): List<Int> {
        return findAnchorTopTilePositions(maxLockedCount).mapNotNull { (r, c) ->
            grid[r][c]?.value
        }
    }

    private fun hasPlayableShuffleArrangement(): Boolean {
        val anchorPositions = findAnchorTopTilePositions(3)
        if (anchorPositions.isEmpty()) return false
        val anchorSet = anchorPositions.toSet()
        val anchorTiles = anchorPositions.mapNotNull { (r, c) ->
            grid[r][c]?.let { (r to c) to it }
        }
        val otherCoords = mutableListOf<Pair<Int, Int>>()
        val otherTiles = mutableListOf<Tile>()
        var hasEmpty = false
        for (r in 0 until size) {
            for (c in 0 until size) {
                val pos = r to c
                if (pos !in anchorSet) {
                    otherCoords.add(pos)
                    val t = grid[r][c]
                    if (t == null) {
                        hasEmpty = true
                    } else {
                        otherTiles.add(t)
                    }
                }
            }
        }
        if (otherCoords.isEmpty()) return false
        if (hasEmpty && (otherTiles.isNotEmpty() || anchorTiles.isNotEmpty())) return true

        val hasAdjacentOtherPair = otherCoords.indices.any { i ->
            val (r1, c1) = otherCoords[i]
            (i + 1 until otherCoords.size).any { j ->
                val (r2, c2) = otherCoords[j]
                kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) == 1
            }
        }
        if (hasAdjacentOtherPair) {
            for (i in 0 until otherTiles.size) {
                for (j in (i + 1) until otherTiles.size) {
                    if (Tile.canMerge(otherTiles[i], otherTiles[j])) {
                        return true
                    }
                }
            }
        }

        for ((anchorPos, anchorTile) in anchorTiles) {
            val hasFreeNeighbor = getOrthogonalNeighbors(anchorPos.first, anchorPos.second)
                .any { it in otherCoords }
            if (hasFreeNeighbor && otherTiles.any { Tile.canMerge(anchorTile, it) }) {
                return true
            }
        }
        return false
    }

    private fun findAnchorTopTilePositions(maxCount: Int = 3): List<Pair<Int, Int>> {
        val allTiles = mutableListOf<Pair<Pair<Int, Int>, Tile>>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                val tile = grid[r][c] ?: continue
                allTiles.add((r to c) to tile)
            }
        }
        val lockCount = minOf(maxCount, (allTiles.size - 2).coerceAtLeast(0))
        if (lockCount <= 0) return emptyList()

        return allTiles
            .sortedWith(
                compareByDescending<Pair<Pair<Int, Int>, Tile>> { it.second.value }
                    .thenByDescending { (pos, _) -> cellHabitWeights[pos.first * size + pos.second] }
                    .thenByDescending { (pos, _) ->
                        when {
                            isCorner(pos.first, pos.second) -> 2
                            isEdge(pos.first, pos.second) -> 1
                            else -> 0
                        }
                    }
                    .thenBy { (pos, _) -> pos.first * size + pos.second }
            )
            .take(lockCount)
            .map { it.first }
    }

    /**
     * Randomly shuffles all tiles on the board except the 3 largest tiles (which remain
     * locked at their current positions), guarantees at least one valid move post-shuffle,
     * consumes the 1-per-game shuffle, and clears the undo snapshot.
     */
    fun shuffleBoard(): Boolean {
        if (!canShuffle()) return false
        val anchorPositions = findAnchorTopTilePositions(3)
        if (anchorPositions.isEmpty()) return false
        val anchorSet = anchorPositions.toSet()
        val anchorTiles = anchorPositions.associateWith { (r, c) ->
            grid[r][c]!!.copy(isMerged = false, isNew = false)
        }

        val otherCoords = mutableListOf<Pair<Int, Int>>()
        val originalOtherTiles = mutableListOf<Tile?>()
        for (r in 0 until size) {
            for (c in 0 until size) {
                val pos = r to c
                if (pos !in anchorSet) {
                    otherCoords.add(pos)
                    originalOtherTiles.add(grid[r][c])
                }
            }
        }

        val candidateTiles = originalOtherTiles.toMutableList()
        var foundPlayable = false

        for (attempt in 0 until 80) {
            candidateTiles.shuffle(random)
            for ((pos, tile) in anchorTiles) {
                grid[pos.first][pos.second] = tile
            }
            for (i in otherCoords.indices) {
                val (r, c) = otherCoords[i]
                grid[r][c] = candidateTiles[i]
            }
            if (!checkGameOver()) {
                val changed = candidateTiles.indices.any { idx ->
                    candidateTiles[idx]?.value != originalOtherTiles[idx]?.value
                }
                if (changed || attempt >= 20) {
                    foundPlayable = true
                    break
                }
            }
        }

        if (!foundPlayable) {
            var pairIdxA = -1
            var pairIdxB = -1
            outer@ for (i in 0 until candidateTiles.size) {
                for (j in (i + 1) until candidateTiles.size) {
                    if (Tile.canMerge(candidateTiles[i], candidateTiles[j])) {
                        pairIdxA = i
                        pairIdxB = j
                        break@outer
                    }
                }
            }

            val adjacentCoordPairs = mutableListOf<Pair<Int, Int>>()
            for (i in otherCoords.indices) {
                val (r1, c1) = otherCoords[i]
                for (j in (i + 1) until otherCoords.size) {
                    val (r2, c2) = otherCoords[j]
                    if (kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) == 1) {
                        adjacentCoordPairs.add(i to j)
                    }
                }
            }

            if (pairIdxA >= 0 && pairIdxB >= 0 && adjacentCoordPairs.isNotEmpty()) {
                val tileB = candidateTiles.removeAt(pairIdxB)
                val tileA = candidateTiles.removeAt(pairIdxA)
                candidateTiles.shuffle(random)

                val (slotA, slotB) = adjacentCoordPairs[random.nextInt(adjacentCoordPairs.size)]
                val firstSlot = minOf(slotA, slotB)
                val secondSlot = maxOf(slotA, slotB)
                candidateTiles.add(firstSlot, tileA)
                candidateTiles.add(secondSlot, tileB)
            } else {
                for ((anchorPos, anchorTile) in anchorTiles) {
                    val mergeIdx = candidateTiles.indexOfFirst { Tile.canMerge(anchorTile, it) }
                    if (mergeIdx >= 0) {
                        val freeNeighbors = getOrthogonalNeighbors(anchorPos.first, anchorPos.second)
                            .filter { it in otherCoords }
                        if (freeNeighbors.isNotEmpty()) {
                            val neighborCoord = freeNeighbors[random.nextInt(freeNeighbors.size)]
                            val neighborSlotIdx = otherCoords.indexOf(neighborCoord)
                            if (neighborSlotIdx >= 0) {
                                val matchedTile = candidateTiles.removeAt(mergeIdx)
                                candidateTiles.shuffle(random)
                                candidateTiles.add(neighborSlotIdx, matchedTile)
                                break
                            }
                        }
                    }
                }
            }
        }

        for ((pos, tile) in anchorTiles) {
            grid[pos.first][pos.second] = tile
        }
        for (i in otherCoords.indices) {
            val (r, c) = otherCoords[i]
            val t = candidateTiles[i]
            grid[r][c] = t?.let {
                Tile.create(
                    value = it.value,
                    isBonus = it.isBonus,
                    isNew = true
                )
            }
        }

        hasUsedShuffle = true
        previousSnapshot = null
        lastMoveDirection = null
        recalculateScore()
        isGameOver = checkGameOver()
        return !isGameOver
    }

    fun canUndo(): Boolean = previousSnapshot != null

    fun getUndoSnapshot(): GameSnapshot? = previousSnapshot?.let { snap ->
        snap.copy(
            grid = snap.grid.map { row -> row.map { it?.copy() } },
            nextTile = snap.nextTile.copy(),
            deck = snap.deck.toList()
        )
    }

    fun undo(): Boolean {
        val snapshot = previousSnapshot ?: return false
        for (r in 0 until size) {
            for (c in 0 until size) {
                grid[r][c] = snapshot.grid[r][c]?.copy(isMerged = false, isNew = false)
            }
        }
        score = snapshot.score
        nextTile = snapshot.nextTile
        deck.clear()
        deck.addAll(snapshot.deck)
        maxTileValue = snapshot.maxTileValue
        moveCount = snapshot.moveCount
        previousSnapshot = null
        lastMoveDirection = null
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
            grid = (0 until size).map { r ->
                (0 until size).map { c -> grid[r][c]?.copy(isMerged = false, isNew = false) }
            },
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
        loadedMoveCount: Int,
        loadedPreviousSnapshot: GameSnapshot? = null,
        loadedHasUsedShuffle: Boolean = false
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
        loadedPreviousSnapshot?.let { snap ->
            if (snap.nextTile.id > maxId) maxId = snap.nextTile.id
            snap.grid.forEach { row ->
                row.forEach { tile ->
                    if (tile != null && tile.id > maxId) {
                        maxId = tile.id
                    }
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
        hasUsedShuffle = loadedHasUsedShuffle
        previousSnapshot = loadedPreviousSnapshot
        lastMoveDirection = null
        isGameOver = checkGameOver()
    }
}
