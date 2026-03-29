package app.crazy_minesveeper.domain.repository

import app.crazy_minesveeper.domain.model.LevelSettings

interface IGameRepository {
    fun getLevels(): List<LevelSettings>
    fun getLevelByTitle(title: String): LevelSettings?
    fun getDefaultLevel(): LevelSettings
}
