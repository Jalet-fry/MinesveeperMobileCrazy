package app.crazy_minesveeper.domain.model

data class LevelSettings(
    val title: String = "Custom Game",
    val rows: Int = 10,
    val cols: Int = 10,
    val p1: Double = 10.0,
    val p2: Double = 5.0,
    val p3: Double = 2.0,
    val pAnti: Double = 3.0,
    val isChargeMode: Boolean = true, // Новая опция: считать сумму зарядов вместо количества
    
    val width: Int = cols,
    val height: Int = rows
)
