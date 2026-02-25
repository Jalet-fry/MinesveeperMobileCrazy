package app.crazy_minesveeper.domain.model

data class GameCell(
    val x: Int,
    val y: Int,
    var mineValue: Int = 0,
    var isRevealed: Boolean = false,
    var revealedByPlayer: Boolean = false, // Новый флаг
    var flaggedValue: Int = 0,
    var adjacentSum: Int = 0,
    var exists: Boolean = true
) {
    val isMine: Boolean get() = mineValue != 0
    val isFlagged: Boolean get() = flaggedValue != 0
}
