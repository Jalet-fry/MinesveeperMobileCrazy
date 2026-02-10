package app.crazy_minesveeper.domain.model

data class GameCell(
    val x: Int,
    val y: Int,
    var mineType: String = "",
    var isRevealed: Boolean = false,
    var flagType: String = "",
    var surroundingMines: String = "",
    var surroundingFlags: String = "",
    var isObfuscated: Boolean = false,
    var clockTicks: Int = 0,
    var effect: String = "",
    var starter: Boolean = false,
    var exists: Boolean = true,
    var probability: Float = 1.0f,
    var isCheckpoint: Boolean = false,
    var isPowerButton: Boolean = false
) {
    val isMine: Boolean get() = mineType.isNotEmpty()
    val isFlagged: Boolean get() = flagType.isNotEmpty()
    val neighboringMinesCount: Int get() = surroundingMines.length
}
