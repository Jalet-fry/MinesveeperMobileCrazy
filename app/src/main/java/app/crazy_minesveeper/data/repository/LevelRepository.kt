package app.crazy_minesveeper.data.repository

import app.crazy_minesveeper.domain.model.*

object LevelRepository {
    val levels = listOf(
        LevelSettings(
            title = "Easy (10x10)",
            width = 10,
            height = 10,
            p1 = 10.0, p2 = 2.0, p3 = 0.0, pAnti = 1.0
        ),
        LevelSettings(
            title = "Medium (15x15)",
            width = 15,
            height = 15,
            p1 = 12.0, p2 = 4.0, p3 = 1.0, pAnti = 2.0
        ),
        LevelSettings(
            title = "Hard (20x20)",
            width = 20,
            height = 20,
            p1 = 15.0, p2 = 6.0, p3 = 3.0, pAnti = 3.0
        ),
        LevelSettings(
            title = "Chaos (25x25)",
            width = 25,
            height = 25,
            p1 = 10.0, p2 = 10.0, p3 = 5.0, pAnti = 10.0
        )
    )
}
