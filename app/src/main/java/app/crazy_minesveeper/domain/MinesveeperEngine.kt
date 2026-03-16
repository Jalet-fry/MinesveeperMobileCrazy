package app.crazy_minesveeper.domain

import app.crazy_minesveeper.domain.model.*
import kotlin.random.Random

class MinesveeperEngine(val settings: LevelSettings) {
    
    val width: Int = settings.width
    val height: Int = settings.height
    val cells: Array<Array<GameCell>>

    var isGameOver = false
    var isWin = false
    var isFirstClick = true

    var totalMines = mutableMapOf(1 to 0, 2 to 0, 3 to 0, -1 to 0)
    var remainingMines = mutableMapOf(1 to 0, 2 to 0, 3 to 0, -1 to 0)

    var onStateChanged: (() -> Unit)? = null

    private val activeFlagTypes: List<Int> by lazy {
        val list = mutableListOf<Int>()
        if (settings.p1 > 0) list.add(1)
        if (settings.p2 > 0) list.add(2)
        if (settings.p3 > 0) list.add(3)
        if (settings.pAnti > 0) list.add(-1)
        list
    }

    init {
        cells = Array(height) { y -> Array(width) { x -> GameCell(x, y) } }
    }

    fun toggleFlag(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (cell.isRevealed) return
        
        val current = cell.flaggedValue
        if (current == 0) {
            cell.flaggedValue = activeFlagTypes.firstOrNull() ?: 0
        } else {
            val idx = activeFlagTypes.indexOf(current)
            if (idx == -1 || idx == activeFlagTypes.size - 1) {
                cell.flaggedValue = 0
            } else {
                cell.flaggedValue = activeFlagTypes[idx + 1]
            }
        }
        
        updateRemainingCounts()
        onStateChanged?.invoke()
    }

    private fun updateRemainingCounts() {
        val currentFlags = mutableMapOf(1 to 0, 2 to 0, 3 to 0, -1 to 0)
        cells.forEach { row ->
            row.forEach { if (it.isFlagged) currentFlags[it.flaggedValue] = (currentFlags[it.flaggedValue] ?: 0) + 1 }
        }
        totalMines.keys.forEach { type ->
            remainingMines[type] = (totalMines[type] ?: 0) - (currentFlags[type] ?: 0)
        }
    }

    // Внешний метод для вызова из UI
    fun revealCell(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (cell.isFlagged || cell.isRevealed) return
        
        if (isFirstClick) {
            isFirstClick = false
            generateBoard(x, y)
        }

        revealRecursive(x, y)
        
        updateRemainingCounts()
        if (!isGameOver) checkWin()
        onStateChanged?.invoke()
    }

    // Внутренняя рекурсия без лишних уведомлений
    private fun revealRecursive(x: Int, y: Int) {
        val cell = getCell(x, y) ?: return
        if (cell.isFlagged || cell.isRevealed) return

        cell.revealedByPlayer = true
        cell.isRevealed = true

        /**
         * ПИНГ-ПОНГ (Раунд 2 - ПОНГ Витовта)
         * Тестируется тестом: `chord leads to game over if flag is wrong`
         * Проверка: Срабатывает ли Game Over при вскрытии мины через аккорд.
         */
        if (cell.isMine) { 
            isGameOver = true
            revealAllField()
            return
        }
        
        /**
         * ПИНГ-ПОНГ (Раунд 4 - ПОНГ Витовта)
         * Тестируется тестом: `revealRecursive opens empty area until it hits numbers`
         * Проверка: Рекурсивный вызов для всех соседей, если ячейка полностью безопасна.
         */
        if (isAreaAbsolutelySafe(x, y)) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i != 0 || j != 0) revealRecursive(x + i, y + j)
                }
            }
        }
    }

    /**
     * Задача Витовта (Backend): Механика Аккорда
     */
    fun chord(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (!cell.isRevealed || cell.adjacentSum == 0) return

        var flagsCount = 0
        for (i in -1..1) {
            for (j in -1..1) {
                if (getCell(x + i, y + j)?.isFlagged == true) flagsCount++
            }
        }

        /**
         * ПИНГ-ПОНГ (Раунд 2 - ПОНГ Витовта)
         * Тестируется тестом: `chord does nothing if flags count is less than adjacentSum`
         * Проверка: Аккорд срабатывает только при полном соответствии количества флагов цифре на клетке.
         */
        if (flagsCount == cell.adjacentSum) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i != 0 || j != 0) {
                        val neighbor = getCell(x + i, y + j)
                        if (neighbor != null && !neighbor.isFlagged && !neighbor.isRevealed) {
                            revealRecursive(x + i, y + j)
                        }
                    }
                }
            }
            updateRemainingCounts()
            if (!isGameOver) checkWin()
            onStateChanged?.invoke()
        }
    }

    private fun isAreaAbsolutelySafe(x: Int, y: Int): Boolean {
        for (i in -1..1) {
            for (j in -1..1) {
                val neighbor = getCell(x + i, y + j)
                if (neighbor != null && neighbor.isMine) return false
            }
        }
        return true
    }

    /**
     * Задача Евгения (Backend): Безопасный старт
     */
    private fun generateBoard(safeX: Int, safeY: Int) {
        val totalCells = width * height
        totalMines[1] = (totalCells * (settings.p1 / 100.0)).toInt()
        totalMines[2] = (totalCells * (settings.p2 / 100.0)).toInt()
        totalMines[3] = (totalCells * (settings.p3 / 100.0)).toInt()
        totalMines[-1] = (totalCells * (settings.pAnti / 100.0)).toInt()

        totalMines.forEach { (type, count) ->
            var placed = 0
            while (placed < count) {
                val rx = Random.nextInt(width)
                val ry = Random.nextInt(height)
                val cell = cells[ry][rx]
                
                // Гарантируем безопасность в радиусе 1 клетки (зона 3x3)
                val isSafeZone = Math.abs(rx - safeX) <= 1 && Math.abs(ry - safeY) <= 1
                
                /**
                 * ПИНГ-ПОНГ (Раунд 1 - ПОНГ Жеки)
                 * Тестируется тестом: `first click creates safe zone 3x3`
                 * Проверка: Генерация мин игнорирует область 3x3 вокруг первого клика.
                 */
                if (cell.mineValue == 0 && !isSafeZone) {
                    cell.mineValue = type
                    placed++
                }
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (cells[y][x].mineValue == 0) {
                    cells[y][x].adjacentSum = calculateSum(x, y)
                }
            }
        }
    }

    private fun calculateSum(x: Int, y: Int): Int {
        var result = 0
        for (i in -1..1) {
            for (j in -1..1) {
                val cell = getCell(x + i, y + j) ?: continue
                if (cell.isMine) {
                    /**
                     * ПИНГ-ПОНГ (Раунд 3 - ПОНГ Жеки)
                     * Тестируется тестом: `adjacentSum logic with anti-mines`
                     * Проверка: Учет веса мин (включая отрицательные) в режиме Charge Mode.
                     */
                    if (settings.isChargeMode) result += cell.mineValue else result += 1
                }
            }
        }
        return result
    }

    private fun getCell(x: Int, y: Int): GameCell? {
        if (x !in 0 until width || y !in 0 until height) return null
        return cells[y][x]
    }

    private fun revealAllField() {
        cells.forEach { row -> row.forEach { it.isRevealed = true } }
    }
    
    private fun checkWin() {
        if (isGameOver) return
        var revealedCount = 0
        var totalNonMines = 0
        cells.forEach { row ->
            row.forEach {
                if (it.exists && !it.isMine) {
                    totalNonMines++
                    if (it.isRevealed) revealedCount++
                }
            }
        }
        if (revealedCount == totalNonMines && totalNonMines > 0) isWin = true
    }
}
