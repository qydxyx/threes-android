package com.threes.game.model

enum class Direction {
    UP,
    DOWN,
    LEFT,
    RIGHT;

    val isHorizontal: Boolean
        get() = this == LEFT || this == RIGHT

    val isVertical: Boolean
        get() = this == UP || this == DOWN
}
