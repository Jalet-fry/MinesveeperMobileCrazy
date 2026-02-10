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

    var currentTool by mutableStateOf(Tool.DIG)
        private set

    private var timerJob: Job? = null

    fun startLevel(settings: LevelSettings) {
        stopTimer()
        currentTime = 0
        engine = MinesveeperEngine(settings)
        tick++
    }

    fun setTool(tool: Tool) {
        currentTool = tool
    }

    fun onCellClick(x: Int, y: Int) {
        val eng = engine ?: return
        if (eng.isGameOver || eng.isWin) return

        if (eng.isFirstClick) {
            startTimer()
        }

        if (currentTool == Tool.DIG) {
            eng.revealCell(x, y)
        } else {
            eng.toggleFlag(x, y)
        }

        if (eng.isGameOver || eng.isWin) {
            stopTimer()
        }
        tick++
    }

    fun onCellLongClick(x: Int, y: Int) {
        val eng = engine ?: return
        if (eng.isGameOver || eng.isWin) return
        
        eng.toggleFlag(x, y)
        tick++
    }

    fun restart() {
        val settings = engine?.settings ?: return
        startLevel(settings)
    }

    private fun startTimer() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (true) {
                currentTime = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
