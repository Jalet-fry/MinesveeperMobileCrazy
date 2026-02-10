package app.crazy_minesveeper.domain.model

import app.crazy_minesveeper.domain.MinesveeperEngine
import kotlin.math.abs
import kotlin.random.Random

abstract class GameEntity(
    var x: Int,
    var y: Int,
    val type: String,
    val isPlaceable: Boolean = false
) {
    var shouldDespawn: Boolean = false
    var direction: Int = 0 // 0:R, 1:D, 2:L, 3:U

    abstract fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent)

    protected fun basicMove(engine: MinesveeperEngine, dx: Int, dy: Int): GameCell? {
        if (dx == 0 && dy == 0) return null
        val moveX = if (dx < 0) -1 else if (dx > 0) 1 else 0
        val moveY = if (dy < 0) -1 else if (dy > 0) 1 else 0
        
        var targetX = x
        var targetY = y

        if ((moveX == 0) != (moveY == 0)) { // Straight move
            targetX += moveX
            targetY += moveY
        } else { // Diagonal move
            if (engine.isWithinBounds(x + moveX, y + moveY)) {
                targetX += moveX
                targetY += moveY
            } else {
                // В JS: логика выбора между X и Y направлением (метод choose)
                if (Random.nextBoolean()) targetX += moveX else targetY += moveY
            }
        }

        if (engine.isWithinBounds(targetX, targetY)) {
            val cell = engine.cells[targetY][targetX]
            if (cell.exists) {
                x = targetX
                y = targetY
                return cell
            }
        }
        return null
    }
}

class SheepEntity(x: Int, y: Int) : GameEntity(x, y, "sheep") {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {
        if (event != GameEvent.DIG_OR_CHORD) return
        
        // JS: Sheep won't move if adjacent to dog
        val dog = engine.activeEntities.find { it.type == "dog" }
        if (dog != null && abs(x - dog.x) <= 1 && abs(y - dog.y) <= 1) return

        val t = basicMove(engine, clickX - x, clickY - y)
        if (t != null && !t.isRevealed) {
            engine.revealCell(x, y) // В JS овца открывает клетку, на которую наступила
        }
    }
}

class DogEntity(x: Int, y: Int) : GameEntity(x, y, "dog") {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {
        if (event == GameEvent.START_GAME) engine.applyDogEffect(x, y, true)
        if (event != GameEvent.DIG_OR_CHORD) return
        
        val ball = engine.activeEntities.find { it.type == "ball" } ?: return
        engine.applyDogEffect(x, y, false)
        basicMove(engine, ball.x - x, ball.y - y)
        engine.applyDogEffect(x, y, true)
    }
}

class RatEntity(x: Int, y: Int) : GameEntity(x, y, "rat") {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {
        if (event != GameEvent.DIG_OR_CHORD) return
        val cheese = engine.activeEntities.find { it.type == "cheese" } ?: return
        val t = basicMove(engine, cheese.x - x, cheese.y - y)
        if (t != null && !t.isRevealed) {
            if (t.mineType.isEmpty()) engine.revealCell(x, y)
            else if (t.mineType != t.flagType) engine.toggleFlag(x, y)
        }
    }
}

class HorseEntity(x: Int, y: Int) : GameEntity(x, y, "horse") {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {
        if (event != GameEvent.DIG_OR_CHORD) return
        val dx = clickX - x
        val dy = clickY - y
        if (dx == 0 || dy == 0 || abs(dx) == abs(dy)) return
        val jx = if (abs(dx) > abs(dy)) (if (dx > 0) 2 else -2) else (if (dx > 0) 1 else -1)
        val jy = if (abs(dx) > abs(dy)) (if (dy > 0) 1 else -1) else (if (dy > 0) 2 else -2)
        
        if (engine.isWithinBounds(x + jx, y + jy)) {
            x += jx; y += jy
            val t = engine.cells[y][x]
            if (t.exists && !t.isRevealed) engine.revealCell(x, y)
        }
    }
}

class ProjectileEntity(x: Int, y: Int, dir: Int) : GameEntity(x, y, "projectile") {
    init { direction = dir }
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {
        if (event != GameEvent.DIG_OR_CHORD) return
        when (direction) {
            0 -> x++; 1 -> y++; 2 -> x--; 3 -> y--
        }
        if (!engine.isWithinBounds(x, y)) { shouldDespawn = true; return }
        engine.activeEntities.forEach { 
            if (it != this && it.x == x && it.y == y && it.type != "projectile") engine.isGameOver = true
        }
    }
}

class CheeseEntity(x: Int, y: Int) : GameEntity(x, y, "cheese", isPlaceable = true) {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {}
}

class BallEntity(x: Int, y: Int) : GameEntity(x, y, "ball", isPlaceable = true) {
    override fun onUpdate(engine: MinesveeperEngine, clickX: Int, clickY: Int, event: GameEvent) {}
}
