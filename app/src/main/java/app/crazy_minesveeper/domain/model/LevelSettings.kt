package app.crazy_minesveeper.domain.model

enum class DisplayStyle {
    DEFAULT, COLORCHARGE, MINUSGB, GRAYSCALE
}

data class LevelSettings(
    val day: Int,
    val title: String,
    val description: String,
    val width: Int,
    val height: Int,
    val mines: Map<String, Int> = emptyMap(),
    val shape: NeighborShape = NeighborShape.DEFAULT,
    val boardData: String? = null,
    val grayMines: Boolean = false,
    val isFixedPattern: Boolean = false,
    val topology: Topology = Topology(),
    val displayStyle: DisplayStyle = DisplayStyle.DEFAULT,
    val isDecrementing: Boolean = false,
    val disableChord: Boolean = false,
    val spawn: List<String> = emptyList(),
    
    // Новые поля для спец-уровней 60-68
    val isConfusionMode: Boolean = false, // Day 63
    val checkpoints: List<Pair<Int, Int>> = emptyList(), // Day 66
    val letters: Map<Pair<Int, Int>, String> = emptyMap() // Day 68
)
