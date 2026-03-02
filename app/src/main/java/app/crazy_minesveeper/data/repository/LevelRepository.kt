package app.crazy_minesveeper.data.repository

import app.crazy_minesveeper.domain.model.LevelSettings

/**
 * Репозиторий уровней сложности. 
 * Позволяет централизованно управлять настройками игры.
 */
object LevelRepository {
    val levels = listOf(
        LevelSettings(
            title = "Новичок",
            rows = 8,
            cols = 8,
            p1 = 10.0,
            p2 = 0.0,
            p3 = 0.0,
            pAnti = 0.0,
            isChargeMode = false
        ),
        LevelSettings(
            title = "Любитель",
            rows = 12,
            cols = 12,
            p1 = 12.0,
            p2 = 3.0,
            p3 = 0.0,
            pAnti = 2.0,
            isChargeMode = true
        ),
        LevelSettings(
            title = "Профи",
            rows = 16,
            cols = 20,
            p1 = 15.0,
            p2 = 5.0,
            p3 = 2.0,
            pAnti = 5.0,
            isChargeMode = true
        ),
        LevelSettings(
            title = "Безумие",
            rows = 20,
            cols = 30,
            p1 = 10.0,
            p2 = 10.0,
            p3 = 10.0,
            pAnti = 15.0,
            isChargeMode = true
        )
    )

    fun getLevelByTitle(title: String): LevelSettings? {
        return levels.find { it.title == title }
    }
    
    val defaultLevel = levels[0]
}
