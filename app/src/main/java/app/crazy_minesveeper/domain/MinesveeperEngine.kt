package app.crazy_minesveeper.domain

import app.crazy_minesveeper.domain.model.*
import kotlin.random.Random

enum class FeedbackType {
    REVEAL_EMPTY, MINE_EXPLODE, FLAG_SET, CHORD_SUCCESS, ERROR
}

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
    var onFeedbackRequired: ((FeedbackType) -> Unit)? = null

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
        
        // UX Improvement: Если тип мины один, переключаем мгновенно (0 -> Type -> 0)
        if (activeFlagTypes.size == 1) {
            cell.flaggedValue = if (current == 0) activeFlagTypes[0] else 0
        } else {
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
        }
        
        if (cell.isFlagged) onFeedbackRequired?.invoke(FeedbackType.FLAG_SET)
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

    fun revealCell(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (cell.isFlagged || cell.isRevealed) return
        
        if (isFirstClick) {
            isFirstClick = false
            generateBoard(x, y)
        }

        revealRecursive(x, y)
        
        if (isGameOver) {
            onFeedbackRequired?.invoke(FeedbackType.MINE_EXPLODE)
        } else {
            onFeedbackRequired?.invoke(FeedbackType.REVEAL_EMPTY)
        }

        updateRemainingCounts()
        if (!isGameOver) checkWin()
        onStateChanged?.invoke()
    }

    private fun revealRecursive(x: Int, y: Int) {
        val cell = getCell(x, y) ?: return
        if (cell.isFlagged || cell.isRevealed) return

        cell.revealedByPlayer = true
        cell.isRevealed = true

        if (cell.isMine) {
            isGameOver = true
            revealAllField()
            return
        }
        
        if (isAreaAbsolutelySafe(x, y)) {
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i != 0 || j != 0) revealRecursive(x + i, y + j)
                }
            }
        }
    }

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

        if (flagsCount == cell.adjacentSum) {
            var revealedAny = false
            for (i in -1..1) {
                for (j in -1..1) {
                    if (i != 0 || j != 0) {
                        val neighbor = getCell(x + i, y + j)
                        if (neighbor != null && !neighbor.isFlagged && !neighbor.isRevealed) {
                            revealRecursive(x + i, y + j)
                            revealedAny = true
                        }
                    }
                }
            }
            if (revealedAny) {
                if (isGameOver) {
                    onFeedbackRequired?.invoke(FeedbackType.MINE_EXPLODE)
                } else {
                    onFeedbackRequired?.invoke(FeedbackType.CHORD_SUCCESS)
                }
            }
            updateRemainingCounts()
            if (!isGameOver) checkWin()
            onStateChanged?.invoke()
        } else {
            onFeedbackRequired?.invoke(FeedbackType.ERROR)
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
                
                val isSafeZone = Math.abs(rx - safeX) <= 1 && Math.abs(ry - safeY) <= 1
                
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

    // Вспомогательный метод для подсчета вскрытых ячеек (для статистики)
    fun getRevealedCount(): Int {
        var count = 0
        cells.forEach { row -> row.forEach { if (it.isRevealed && !it.isMine) count++ } }
        return count
    }
}
