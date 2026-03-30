package app.crazy_minesveeper.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GameSkin(
    val id: String,
    val name: String,
    val mainBackground: Color,
    val panelBackground: Color,
    val gridBorder: Color,
    val cellClosed: Color,
    val cellOpened: Color,
    val numColors: List<Color>,
    val mineAnti: Color,
    val isDark: Boolean
)

object GameSkins {
    val ClassicRetro = GameSkin(
        id = "classic",
        name = "Classic Retro",
        mainBackground = Color(0xFFC0C0C0),
        panelBackground = Color(0xFFD0D0D0),
        gridBorder = Color(0xFF808080),
        cellClosed = Color(0xFFE0E0E0),
        cellOpened = Color(0xFFA0A0A0),
        numColors = listOf(
            Color(0xFF2979FF), Color(0xFF00C853), Color(0xFFFF1744),
            Color(0xFF311B92), Color(0xFFFF6D00), Color(0xFF00B8D4),
            Color(0xFF000000), Color(0xFF546E7A)
        ),
        mineAnti = Color(0xFFC2185B),
        isDark = false
    )

    val DeepDark = GameSkin(
        id = "dark",
        name = "Midnight Dark",
        mainBackground = Color(0xFF0A0A0A),
        panelBackground = Color(0xFF121212),
        gridBorder = Color(0xFF333333),
        cellClosed = Color(0xFF1E1E1E),
        cellOpened = Color(0xFF000000),
        numColors = listOf(
            Color(0xFF448AFF), Color(0xFF69F0AE), Color(0xFFFF5252),
            Color(0xFFE040FB), Color(0xFFFFAB40), Color(0xFF18FFFF),
            Color(0xFFFFFFFF), Color(0xFFCFD8DC)
        ),
        mineAnti = Color(0xFFFF4081),
        isDark = true
    )

    // Deus Ex Style: Amber/Gold on Dark Grey (High Contrast)
    val CyberAug = GameSkin(
        id = "cyber",
        name = "Cyber Aug",
        mainBackground = Color(0xFF080808), 
        panelBackground = Color(0xFF121212),
        gridBorder = Color(0xFFB8860B), 
        cellClosed = Color(0xFF1C1C12), // Subtle amber tint
        cellOpened = Color(0xFF020202), 
        numColors = listOf(
            Color(0xFFFFD700), Color(0xFFFFA000), Color(0xFFFFC107), 
            Color(0xFFFFB300), Color(0xFFFF8F00), Color(0xFFE6EE9C), 
            Color(0xFFFFF176), Color(0xFFBDBDBD)
        ),
        mineAnti = Color(0xFFFF3D00),
        isDark = true
    )

    // Gurren Lagann Style: Slate background with Blue/Red accents
    // UX Fix: Background is now Slate to let Blue cells stand out.
    val SpiralEnergy = GameSkin(
        id = "spiral",
        name = "Spiral Energy",
        mainBackground = Color(0xFF1A1C2C), // Slate Dark (Better contrast for blue cells)
        panelBackground = Color(0xFFB71C1C), // Red accent
        gridBorder = Color(0xFFFFD600),     // Golden Drill
        cellClosed = Color(0xFF2979FF),     // Bright Blue (Stand out)
        cellOpened = Color(0xFF102A43),     // Dark Navy
        numColors = listOf(
            Color(0xFFFFEB3B), Color(0xFF00E676), Color(0xFFFFFFFF), 
            Color(0xFF40C4FF), Color(0xFFFFAB40), Color(0xFFFF5252), 
            Color(0xFF1DE9B6), Color(0xFFAA00FF)
        ),
        mineAnti = Color(0xFFF44336),
        isDark = true
    )

    val allSkins = listOf(ClassicRetro, DeepDark, CyberAug, SpiralEnergy)
    
    fun getById(id: String): GameSkin = allSkins.find { it.id == id } ?: ClassicRetro
}

val LocalGameSkin = staticCompositionLocalOf { GameSkins.ClassicRetro }
