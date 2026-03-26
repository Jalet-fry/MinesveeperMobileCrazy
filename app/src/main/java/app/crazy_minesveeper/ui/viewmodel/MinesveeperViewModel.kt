package app.crazy_minesveeper.ui.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.crazy_minesveeper.domain.MinesveeperEngine
import app.crazy_minesveeper.domain.model.LevelSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Tool { DIG, FLAG }

class MinesveeperViewModel : ViewModel() {
    var engine by mutableStateOf<MinesveeperEngine?>(null)
        private set
    
    var tick by mutableLongStateOf(0L)
        private set

    var currentTime by mutableLongStateOf(0L)
        private set

    var clickCount by mutableIntStateOf(0)
        private set

    var currentTool by mutableStateOf(Tool.DIG)
        private set

    var isPaused by mutableStateOf(false)
        private set

    private var timerJob: Job? = null
    private var lastPausedTime: Long = 0

    fun startLevel(settings: LevelSettings) {
        engine = null 
        stopTimer()
        currentTime = 0
        lastPausedTime = 0
        clickCount = 0
        isPaused = false
        
        val newEngine = MinesveeperEngine(settings)
        newEngine.onStateChanged = {
            tick++ 
        }
        
        engine = newEngine
        tick++
    }

    fun togglePause() {
        if (engine?.isGameOver == true || engine?.isWin == true) return
        isPaused = !isPaused
        if (isPaused) {
            stopTimer()
        } else {
            startTimer()
        }
    }

    fun clearGame() {
        stopTimer()
        engine = null
        isPaused = false
        currentTime = 0
        lastPausedTime = 0
        clickCount = 0
    }

    fun setTool(tool: Tool) {
        currentTool = tool
    }

    fun onCellClick(x: Int, y: Int) {
        if (isPaused) return
        val eng = engine ?: return
        if (eng.isGameOver || eng.isWin) return

        if (eng.isFirstClick) {
            startTimer()
        }

        if (currentTool == Tool.DIG) {
            val cell = eng.cells[y][x]
            if (cell.isRevealed) {
                // Если ячейка уже открыта — пробуем Аккорд (Chord)
                eng.chord(x, y)
            } else {
                eng.revealCell(x, y)
            }
            clickCount++
        } else {
            eng.toggleFlag(x, y)
        }

        if (eng.isGameOver || eng.isWin) {
            stopTimer()
        }
    }

    fun onCellLongClick(x: Int, y: Int) {
        if (isPaused) return
        val eng = engine ?: return
        if (eng.isGameOver || eng.isWin) return
        eng.toggleFlag(x, y)
    }

    fun restart() {
        val settings = engine?.settings ?: return
        startLevel(settings)
    }

    fun startTimer() {
        if (timerJob != null) return
        val startTime = System.currentTimeMillis() - lastPausedTime
        timerJob = viewModelScope.launch {
            while (true) {
                currentTime = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    fun stopTimer() {
        lastPausedTime = currentTime
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
