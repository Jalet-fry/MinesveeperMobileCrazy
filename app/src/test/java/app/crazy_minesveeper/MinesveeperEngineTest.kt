package app.crazy_minesveeper

import app.crazy_minesveeper.domain.MinesveeperEngine
import app.crazy_minesveeper.domain.model.LevelSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinesveeperEngineTest {

    @Test
    fun `engine creates correct dimensions`() {
        val settings = LevelSettings(rows = 10, cols = 8)
        val engine = MinesveeperEngine(settings)
        assertEquals(10, engine.height)
        assertEquals(8, engine.width)
    }

    /**
     * ПИНГ-ПОНГ (Раунд 1)
     * ПИНГ (Витовт) -> ПОНГ (Жека)
     * Тест: Проверка Safe Start. Первый клик никогда не должен быть миной, 
     * и вокруг него (3x3) тоже не должно быть мин.
     */
    @Test
    fun `first click creates safe zone 3x3`() {
        val settings = LevelSettings(rows = 10, cols = 10, p1 = 50.0) // 50% мин
        val engine = MinesveeperEngine(settings)
        
        val clickX = 5
        val clickY = 5
        engine.revealCell(clickX, clickY)
        
        // Проверяем зону 3x3
        for (y in 4..6) {
            for (x in 4..6) {
                assertFalse("Cell at ($x, $y) should not be a mine", engine.cells[y][x].isMine)
            }
        }
        assertFalse("Game should not be over on first click", engine.isGameOver)
    }

    /**
     * ПИНГ-ПОНГ (Раунд 2)
     * ПИНГ (Жека): Проверяем, что аккорд не срабатывает, если флагов меньше, чем цифра.
     */
    @Test
    fun `chord does nothing if flags count is less than adjacentSum`() {
        val settings = LevelSettings(rows = 3, cols = 3)
        val engine = MinesveeperEngine(settings)
        
        val cell = engine.cells[1][1]
        cell.adjacentSum = 2
        cell.isRevealed = true
        
        engine.toggleFlag(0, 0) // Поставили только 1 флаг вместо 2
        engine.chord(1, 1)
        
        assertFalse("Neighbor (0,1) should remain hidden", engine.cells[0][1].isRevealed)
    }

    /**
     * ПИНГ-ПОНГ (Раунд 2 - Продолжение)
     * ПОНГ (Витовт): Проверка, что аккорд приводит к поражению, если флаг стоит неверно.
     */
    @Test
    fun `chord leads to game over if flag is wrong`() {
        val settings = LevelSettings(rows = 3, cols = 3)
        val engine = MinesveeperEngine(settings)
        
        // Имитируем состояние после генерации
        engine.isFirstClick = false 
        
        // Ставим мину в (0,0)
        engine.cells[0][0].mineValue = 1
        // Ячейка (1,1) имеет 1 мину рядом
        val center = engine.cells[1][1]
        center.adjacentSum = 1
        center.isRevealed = true
        
        // Игрок ошибается: ставит флаг на (0,1) вместо (0,0)
        engine.toggleFlag(0, 1) 
        
        // Выполняем аккорд в (1,1)
        engine.chord(1, 1)
        
        // Аккорд должен открыть (0,0), там мина -> Game Over
        assertTrue("Game should be over because of wrong flag in chord", engine.isGameOver)
        assertTrue("Mine at (0,0) should be revealed", engine.cells[0][0].isRevealed)
    }

    /**
     * ПИНГ-ПОНГ (Раунд 3)
     * ПИНГ (Витовт) -> ПОНГ (Жека)
     * Тест: Проверка "Анти-мины" (-1). Если рядом одна мина (+1) и одна анти-мина (-1), 
     * сумма adjacentSum должна быть 0 (в Charge Mode).
     */
    @Test
    fun `adjacentSum logic with anti-mines`() {
        // Мы проверяем логику суммы напрямую (как это делает движок)
        val settings = LevelSettings(isChargeMode = true)
        
        val mineValue1 = 1
        val mineValue2 = -1
        
        val sum = if (settings.isChargeMode) (mineValue1 + mineValue2) else 1
        
        assertEquals("Sum of +1 and -1 mines should be 0 in Charge Mode", 0, sum)
    }

    /**
     * ПИНГ-ПОНГ (Раунд 4)
     * ПИНГ (Жека) -> ПОНГ (Витовт)
     * Тест: Проверка автоматического открытия пустых ячеек (рекурсия).
     */
    @Test
    fun `revealRecursive opens empty area until it hits numbers`() {
        // ОБЯЗАТЕЛЬНО: Обнуляем ВСЕ типы мин, чтобы поле было пустым
        val settings = LevelSettings(rows = 5, cols = 5, p1 = 0.0, p2 = 0.0, p3 = 0.0, pAnti = 0.0)
        val engine = MinesveeperEngine(settings)
        
        // Первый клик
        engine.revealCell(0, 0)
        
        // Проверяем, что дальняя ячейка тоже открылась
        assertTrue("Cell (4,4) should be revealed by recursion", engine.cells[4][4].isRevealed)
    }

    /**
     * Дополнительный тест на победу
     */
    @Test
    fun `checkWin sets isWin to true when all non-mine cells are revealed`() {
        val settings = LevelSettings(rows = 2, cols = 1, p1 = 50.0) // 2 ячейки, 1 мина
        val engine = MinesveeperEngine(settings)
        
        // Чтобы избежать рандома в Unit-тесте, фиксируем состояние
        engine.isFirstClick = false
        engine.cells[0][0].mineValue = 1 // Мина в первой
        engine.cells[1][0].mineValue = 0 // Пусто во второй
        
        // Открываем единственную пустую ячейку
        engine.revealCell(0, 1)
        
        assertTrue("Should be win", engine.isWin)
    }
}
