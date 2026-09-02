package com.threes.game.model

data class LineShiftResult(
    val moved: Boolean,
    val newTiles: List<Tile?>,
    val mergedIndex: Int? = null,
    val mergedTile: Tile? = null
)
