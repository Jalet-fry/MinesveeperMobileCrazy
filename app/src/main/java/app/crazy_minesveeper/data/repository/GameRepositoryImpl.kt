package app.crazy_minesveeper.data.repository

import app.crazy_minesveeper.domain.model.LevelSettings
import app.crazy_minesveeper.domain.repository.IGameRepository

class GameRepositoryImpl : IGameRepository {
    override fun getLevels(): List<LevelSettings> {
        return LevelRepository.levels
    }

    override fun getLevelByTitle(title: String): LevelSettings? {
        return LevelRepository.getLevelByTitle(title)
    }

    override fun getDefaultLevel(): LevelSettings {
        return LevelRepository.defaultLevel
    }
}
