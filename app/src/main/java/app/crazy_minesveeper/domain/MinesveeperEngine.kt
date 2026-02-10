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

    fun isWithinBounds(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    private fun getNextFlag(current: String): String {
        val cycle = listOf("", "r", "R", "g", "G", "b", "B")
        val idx = cycle.indexOf(current)
        var nextIdx = (idx + 1) % cycle.size
        while (nextIdx != idx) {
            val candidate = cycle[nextIdx]
            if (candidate == "") return ""
            val mineKey = when(candidate) { "r" -> "R"; "R" -> "RX"; "g" -> "G"; "G" -> "GX"; "b" -> "B"; "B" -> "BX"; else -> "" }
            if ((settings.mines[mineKey] ?: 0) > 0) return candidate
            nextIdx = (nextIdx + 1) % cycle.size
        }
        return ""
    }

    fun toggleFlag(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (!cell.exists || cell.isRevealed) return
        if (settings.day == 63) { cell.isRevealed = true; if (cell.isMine) { isGameOver = true; revealAllMines() }; return }
        cell.flagType = getNextFlag(cell.flagType)
        updateFlagsInRange(x, y)
        if (settings.day == 42) updateDay42Sums()
        postFlagHook?.invoke()
        notifyEntities(x, y, GameEvent.FLAG_OR_UNFLAG)
    }

    private fun updateFlagsInRange(x: Int, y: Int) {
        val range = settings.shape.small + settings.shape.large
        range.forEach { o ->
            val coords = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
            if (coords != null) {
                val n = cells[coords.second][coords.first]
                if (n.exists) n.surroundingFlags = accumulateSurrounding(coords.first, coords.second, true)
            }
        }
    }

    private fun accumulateSurrounding(x: Int, y: Int, isFlag: Boolean): String {
        var sum = ""
        val prop = if(isFlag) { c: GameCell -> c.flagType } else { c: GameCell -> c.mineType }
        val cellEffect = cells[y][x].effect
        
        if (cellEffect == "grayscale") {
            // JS: for(let d of shape.dogCount) sum += (n == n.toLowerCase()) ? n : "rr";
            settings.shape.dogCountActual.forEach { o ->
                val c = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
                if (c != null) {
                    val t = prop(cells[c.second][c.first])
                    if (t.isNotEmpty()) {
                        // Поляризация в режиме собаки не применяется (в JS так)
                        sum += if (t == t.lowercase()) t else "rr"
                    }
                }
            }
        } else {
            settings.shape.small.forEach { o ->
                val c = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
                if (c != null) {
                    val t = prop(cells[c.second][c.first])
                    if (t.isNotEmpty() && t == t.lowercase()) sum += applyPolarisation(t, o)
                }
            }
            settings.shape.large.forEach { o ->
                val c = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
                if (c != null) {
                    val t = prop(cells[c.second][c.first])
                    if (t.isNotEmpty() && t == t.uppercase()) sum += applyPolarisation(t, o)
                }
            }
        }
        return sum
    }

    fun applyDogEffect(x: Int, y: Int, on: Boolean) {
        // JS: for(let d of shape.dogEffect) t.effect = on ? "grayscale" : "";
        val effectArea = settings.shape.dogEffectActual
        effectArea.forEach { o ->
            val c = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
            if (c != null) {
                cells[c.second][c.first].effect = if (on) "grayscale" else ""
            }
        }
        // После изменения эффекта нужно пересчитать цифры во всей зоне влияния собаки + 1 клетка вокруг (updateRange в JS)
        // Для простоты пересчитываем все клетки, которые могут зависеть от изменившихся эффектов
        effectArea.forEach { o ->
            val c = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
            if (c != null) {
                val cell = cells[c.second][c.first]
                if (cell.exists) {
                    cell.surroundingMines = accumulateSurrounding(c.first, c.second, false)
                    cell.surroundingFlags = accumulateSurrounding(c.first, c.second, true)
                }
            }
        }
    }

    private fun applyPolarisation(type: String, offset: Offset): String {
        val filter = offset.forcedColor ?: return type
        if (filter is String) return filter
        if (filter is Map<*, *>) return (filter as Map<String, String>)[type] ?: type
        return type
    }

    private fun generateBoard(safeX: Int, safeY: Int) {
        if (settings.day == 55) { generateStripes(safeX, safeY); return }
        val types = listOf("r", "g", "b", "R", "G", "B")
        val excluded = mutableSetOf<Pair<Int, Int>>()
        excluded.add(safeX to safeY)
        settings.shape.small.forEach { o -> settings.topology.wrapCoordinates(safeX + o.dx, safeY + o.dy, width, height)?.let { excluded.add(it) } }

        types.forEach { t ->
            val mineKey = when(t) { "r" -> "R"; "g" -> "G"; "b" -> "B"; "R" -> "RX"; "G" -> "GX"; "B" -> "BX"; else -> "" }
            val count = settings.mines[mineKey] ?: 0
            var placed = 0; var attempts = 0
            while (placed < count && attempts < 40000) {
                attempts++
                val rx = Random.nextInt(width); val ry = Random.nextInt(height)
                val cell = cells[ry][rx]
                var valid = cell.exists && cell.mineType.isEmpty() && (rx != safeX || ry != safeY)
                if (valid && (Random.nextFloat() > cell.probability)) valid = false
                if (valid) {
                    for (exc in excluded) if (settings.topology.arePositionsEqual(exc.first, exc.second, rx, ry, width, height)) { valid = false; break }
                }
                if (valid) { cell.mineType = t; placed++ }
            }
        }
        calculateAllSurroundingMines()
    }

    fun revealCell(x: Int, y: Int) {
        if (isGameOver || isWin) return
        val cell = getCell(x, y) ?: return
        if (!cell.exists || cell.isFlagged) return
        if (isFirstClick) {
            if (settings.isFixedPattern && !cell.starter) return
            isFirstClick = false
            if (!settings.isFixedPattern) generateBoard(x, y)
            revealCell(x, y)
            notifyEntities(x, y, GameEvent.START_GAME)
            return
        }
        if (cell.isRevealed) { if (canChord(cell)) chord(x, y); return }
        cell.isRevealed = true
        if (settings.day == 65) { cell.clockTicks = 4; cell.isObfuscated = true }
        if (cell.isMine) { isGameOver = true; revealAllMines(); return }
        if (shouldAutoOpen(cell)) settings.shape.small.forEach { revealCell(x + it.dx, y + it.dy) }
        onTickHook?.invoke()
        notifyEntities(x, y, GameEvent.DIG_OR_CHORD)
        checkWin()
    }

    private fun chord(x: Int, y: Int) {
        settings.shape.small.forEach { o ->
            val coords = settings.topology.wrapCoordinates(x + o.dx, y + o.dy, width, height)
            if (coords != null) {
                val n = cells[coords.second][coords.first]
                if (n.exists && !n.isRevealed && !n.isFlagged) revealCell(x + o.dx, y + o.dy)
            }
        }
    }

    private fun canChord(cell: GameCell): Boolean {
        if (settings.disableChord) return false
        return when(settings.displayStyle) {
            DisplayStyle.COLORCHARGE -> { val (r, g, _) = getColorCharge(cell); r == 0 && g == 0 }
            else -> cell.surroundingMines.length == cell.surroundingFlags.length
        }
    }

    fun getColorCharge(cell: GameCell): Triple<Int, Int, Int> {
        val m = cell.surroundingMines.lowercase(); val f = cell.surroundingFlags.lowercase()
        var r = m.filter { it == 'r' }.length - f.filter { it == 'r' }.length
        var g = m.filter { it == 'g' }.length - f.filter { it == 'g' }.length
        var b = m.filter { it == 'b' }.length - f.filter { it == 'b' }.length
        r -= b; g -= b
        return Triple(r, g, 0)
    }

    private fun shouldAutoOpen(cell: GameCell): Boolean = cell.surroundingMines.isEmpty()

    private fun calculateAllSurroundingMines() {
        for (y in 0 until height) for (x in 0 until width) if(cells[y][x].exists) cells[y][x].surroundingMines = accumulateSurrounding(x, y, false)
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
        for (i in 2 until width) {
            if (!cells[0][i].exists) continue
            var sum = ""
            for (j in 2 until height) if (cells[j][i].exists && cells[j][i].isMine) sum += "r"
            cells[0][i].surroundingMines = sum
        }
        for (j in 2 until height) {
            if (!cells[j][0].exists) continue
            var sum = ""
            for (i in 2 until width) if (cells[j][i].exists && cells[j][i].isMine) sum += "r"
            cells[j][0].surroundingMines = sum
        }
    }

    private fun generateStripes(sx: Int, sy: Int) {
        settings.mines.forEach { (type, count) ->
            val stripe = when(type.take(1).lowercase()) { "r" -> 0; "g" -> 1; else -> 2 }
            var placed = 0; var attempts = 0
            while (placed < count && attempts < 10000) {
                attempts++
                val rx = (Random.nextInt(width / 3) * 3) + stripe; val ry = Random.nextInt(height)
                if (rx < width) {
                    val cell = cells[ry][rx]
                    if (cell.exists && cell.mineType.isEmpty() && (rx != sx || ry != sy)) { cell.mineType = type.lowercase(); placed++ }
                }
            }
        }
        calculateAllSurroundingMines()
    }

    private fun spawnInitialEntities() {
        settings.spawn.forEach { type ->
            when(type) {
                "sheep" -> activeEntities.add(SheepEntity(width / 2, height / 2))
                "dog" -> activeEntities.add(DogEntity(width - 2, height - 2))
                "horse" -> activeEntities.add(HorseEntity(width / 2, height / 2))
                "rat" -> activeEntities.add(RatEntity(0, 0))
                "cheese" -> activeEntities.add(CheeseEntity(width - 1, 0))
                "ball" -> activeEntities.add(BallEntity(0, height - 1))
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
        if (settings.day == 60 && event == GameEvent.DIG_OR_CHORD) {
            val dir = Random.nextInt(4); val px = if (dir == 0) 0 else if (dir == 2) width - 1 else Random.nextInt(width); val py = if (dir == 1) 0 else if (dir == 3) height - 1 else Random.nextInt(height)
            activeEntities.add(ProjectileEntity(px, py, dir))
        }
        activeEntities.removeAll { it.shouldDespawn }
    }
}
