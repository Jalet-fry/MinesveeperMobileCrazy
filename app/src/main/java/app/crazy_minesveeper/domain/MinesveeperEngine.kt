package app.crazy_minesveeper.domain

import app.crazy_minesveeper.domain.model.*
import app.crazy_minesveeper.data.util.Compression
import kotlin.random.Random

class MinesveeperEngine(val settings: LevelSettings) {
    
    val width: Int = settings.width
    val height: Int = settings.height
    val cells: Array<Array<GameCell>>
    val activeEntities = mutableListOf<GameEntity>()

    var isGameOver = false
    var isWin = false
    var isFirstClick = true

    // Коллбек для уведомления ViewModel об изменениях (для перерисовки)
    var onStateChanged: (() -> Unit)? = null

    var postFlagHook: (() -> Unit)? = null
    var preDigHook: (() -> Unit)? = null
    var onTickHook: (() -> Unit)? = null

    init {
        cells = Array(height) { y -> Array(width) { x -> GameCell(x, y) } }
        setupHooks()
        if (settings.boardData != null) loadBoardLayout()
        else { for (y in 0 until height) for (x in 0 until width) cells[y][x].exists = true }
        if (settings.isFixedPattern) {
            calculateAllSurroundingMines()
            if (settings.day == 42) applyDay42Logic()
        }
        spawnInitialEntities()
    }

    private fun loadBoardLayout() {
        if (settings.boardData == "P8") {
            for (y in 0 until height) for (x in 0 until width) cells[y][x].exists = true
            return
        }
        try {
            val data = Compression.decompress(settings.boardData!!, width, height)
            val fixedValues = mapOf(65 to "r", 86 to "R", 128 to "g", 170 to "G", 191 to "b", 223 to "B")
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val rawVal = data[x][y]
                    val cell = cells[y][x]
                    if (rawVal == 0) cell.exists = false
                    else {
                        cell.exists = true
                        cell.probability = (rawVal - 1) / 254.0f
                        if (settings.isFixedPattern) {
                            if (rawVal == 1) cell.starter = true
                            else if (fixedValues.containsKey(rawVal)) cell.mineType = fixedValues[rawVal]!!
                        }
                    }
                }
            }
        } catch (e: Exception) { for (y in 0 until height) for (x in 0 until width) cells[y][x].exists = true }
    }

    private fun getMineWeight(type: String): Int {
        return when(type.uppercase()) {
            "R", "G", "B" -> 2
            "RX", "GX", "BX" -> 3
            else -> if (type.isNotEmpty()) 1 else 0
        }
    }

    private fun accumulateSurrounding(x: Int, y: Int, isFlag: Boolean): String {
        var sum = ""
        val prop = if(isFlag) { c: GameCell -> c.flagType } else { c: GameCell -> c.mineType }
        val cellEffect = cells[y][x].effect
        
        val processCell = { nx: Int, ny: Int, isLarge: Boolean ->
            val t = prop(cells[ny][nx])
            if (t.isNotEmpty()) {
                val base = t.take(1).lowercase()
                val weight = if (isLarge) getMineWeight(t) else 1
                sum += base.repeat(weight)
            }
        }

        if (cellEffect == "grayscale") {
            settings.shape.dogCountActual.forEach { o ->
                settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)?.let {
                    processCell(it.first, it.second, false)
                }
            }
        } else {
            settings.shape.small.forEach { o ->
                settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)?.let {
                    processCell(it.first, it.second, false)
                }
            }
            settings.shape.large.forEach { o ->
                settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)?.let {
                    processCell(it.first, it.second, true)
                }
            }
        }
        return sum
    }

    fun toggleFlag(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (!cell.exists || cell.isRevealed) return
        
        val cycle = listOf("", "r", "R", "g", "G", "b", "B")
        val idx = cycle.indexOf(cell.flagType)
        cell.flagType = cycle[(idx + 1) % cycle.size]
        
        updateFlagsInRange(x, y)
        if (settings.day == 42) updateDay42Sums()
        postFlagHook?.invoke()
        notifyEntities(x, y, GameEvent.FLAG_OR_UNFLAG)
        onStateChanged?.invoke()
    }

    private fun updateFlagsInRange(x: Int, y: Int) {
        val range = settings.shape.small + settings.shape.large
        range.forEach { o ->
            settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)?.let { (nx, ny) ->
                val n = cells[ny][nx]
                if (n.exists) n.surroundingFlags = accumulateSurrounding(nx, ny, true)
            }
        }
    }

    fun revealCell(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (!cell.exists || cell.isFlagged) return
        
        if (isFirstClick) {
            isFirstClick = false
            if (!settings.isFixedPattern) generateBoard(x, y)
            revealCell(x, y)
            notifyEntities(x, y, GameEvent.START_GAME)
            onStateChanged?.invoke()
            return
        }

        if (cell.isRevealed) { 
            if (canChord(cell)) chord(x, y)
            onStateChanged?.invoke()
            return 
        }

        preDigHook?.invoke()
        cell.isRevealed = true
        
        if (cell.isMine) { 
            isGameOver = true
            revealAllMines() 
        } else {
            if (shouldAutoOpen(cell)) {
                settings.shape.small.forEach { revealCell(x + it.dx, y + it.dy) }
            }
        }
        
        onTickHook?.invoke()
        notifyEntities(x, y, GameEvent.DIG_OR_CHORD)
        checkWin()
        onStateChanged?.invoke()
    }

    private fun chord(x: Int, y: Int) {
        settings.shape.small.forEach { o ->
            val coords = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
            if (coords != null) {
                val n = cells[coords.second][coords.first]
                if (n.exists && !n.isRevealed && !n.isFlagged) revealCell(coords.first, coords.second)
            }
        }
    }

    private fun canChord(cell: GameCell): Boolean {
        if (settings.disableChord) return false
        return cell.surroundingMines.length == cell.surroundingFlags.length
    }

    private fun generateBoard(safeX: Int, safeY: Int) {
        val types = listOf("r", "g", "b", "R", "G", "B")
        val excluded = mutableSetOf<Pair<Int, Int>>()
        excluded.add(safeX to safeY)
        settings.shape.small.forEach { o -> 
            settings.topology.wrapCoordinates(safeX + o.dx, safeY + o.dy, width, height)?.let { excluded.add(it) } 
        }

        types.forEach { t ->
            val mineKey = when(t) { "r" -> "R"; "g" -> "G"; "b" -> "B"; "R" -> "RX"; "G" -> "GX"; "B" -> "BX"; else -> "" }
            val count = settings.mines[mineKey] ?: 0
            var placed = 0; var attempts = 0
            while (placed < count && attempts < 1000) {
                attempts++
                val rx = Random.nextInt(width); val ry = Random.nextInt(height)
                val cell = cells[ry][rx]
                if (cell.exists && cell.mineType.isEmpty() && !excluded.contains(rx to ry)) {
                    if (Random.nextFloat() <= cell.probability) {
                        cell.mineType = t
                        placed++
                    }
                }
            }
        }
        calculateAllSurroundingMines()
    }

    private fun shouldAutoOpen(cell: GameCell): Boolean = cell.surroundingMines.isEmpty()

    fun calculateAllSurroundingMines() {
        for (y in 0 until height) for (x in 0 until width) {
            if(cells[y][x].exists) {
                cells[y][x].surroundingMines = accumulateSurrounding(x, y, false)
                cells[y][x].surroundingFlags = accumulateSurrounding(x, y, true)
            }
        }
    }

    fun isWithinBounds(x: Int, y: Int): Boolean {
        return x in 0 until width && y in 0 until height
    }

    fun applyDogEffect(x: Int, y: Int, active: Boolean) {
        val effect = if (active) "grayscale" else ""
        settings.shape.dogEffectActual.forEach { o ->
            settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)?.let { (nx, ny) ->
                cells[ny][nx].effect = effect
            }
        }
        calculateAllSurroundingMines()
    }

    fun getColorCharge(cell: GameCell): Triple<Int, Int, Int> {
        val s = cell.surroundingMines
        return Triple(
            s.count { it == 'r' },
            s.count { it == 'g' },
            s.count { it == 'b' }
        )
    }

    private fun getCell(x: Int, y: Int): GameCell? {
        val c = settings.topology.wrapCoordinates(x, y, width, height) ?: return null
        return cells[c.second][c.first]
    }

    private fun setupHooks() {
        if (settings.day == 47) {
            val wipe = { cells.forEach { r -> r.forEach { if (it.isRevealed) it.isObfuscated = true } } }
            postFlagHook = wipe; preDigHook = wipe
        }
        if (settings.day == 65) {
            onTickHook = { cells.forEach { r -> r.forEach { if (it.isRevealed && it.clockTicks > 0) { it.clockTicks--; if (it.clockTicks == 0) it.isObfuscated = false } } } }
        }
    }

    private fun applyDay42Logic() {
        for (x in 0 until width) if(cells[0][x].exists) cells[0][x].isRevealed = true
        for (y in 1 until height) if(cells[y][0].exists) cells[y][0].isRevealed = true
        updateDay42Sums()
    }

    private fun updateDay42Sums() {
        if (settings.day != 42) return
        for (i in 0 until width) {
            if (!cells[0][i].exists) continue
            var sum = ""
            for (j in 0 until height) if (cells[j][i].exists && cells[j][i].isMine) sum += cells[j][i].mineType.lowercase()
            cells[0][i].surroundingMines = sum
        }
        // ... аналогично для строк
    }

    private fun spawnInitialEntities() {
        settings.spawn.forEach { type ->
            when(type) {
                "sheep" -> activeEntities.add(SheepEntity(width / 2, height / 2))
                "dog" -> activeEntities.add(DogEntity(width - 2, height - 2))
            }
        }
    }

    private fun revealAllMines() { cells.forEach { r -> r.forEach { if (it.isMine) it.isRevealed = true } } }
    
    private fun checkWin() {
        val total = cells.sumOf { r -> r.count { it.exists && !it.isMine } }
        val revealed = cells.sumOf { r -> r.count { it.exists && !it.isMine && it.isRevealed } }
        if (!isFirstClick && revealed == total && total > 0) isWin = true
    }

    private fun notifyEntities(x: Int, y: Int, event: GameEvent) {
        activeEntities.forEach { it.onUpdate(this, x, y, event) }
        activeEntities.removeAll { it.shouldDespawn }
    }
}
