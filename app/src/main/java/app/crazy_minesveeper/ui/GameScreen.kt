package app.crazy_minesveeper.ui

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.crazy_minesveeper.R
import app.crazy_minesveeper.domain.MinesveeperEngine
import app.crazy_minesveeper.domain.model.*
import app.crazy_minesveeper.ui.viewmodel.MinesveeperViewModel
import app.crazy_minesveeper.ui.viewmodel.Tool

@Composable
fun MinesveeperScreen(
    levelSettings: LevelSettings,
    onBack: () -> Unit,
    viewModel: MinesveeperViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(levelSettings) {
        viewModel.startLevel(levelSettings)
    }

    val engine = viewModel.engine ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    val tick = viewModel.tick
    val isPaused = viewModel.isPaused
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Задача Витовта (Backend/UI): Триггеры вибрации при изменении состояния
    LaunchedEffect(tick) {
        if (engine.isGameOver) {
            Log.i("MINES_DEBUG", "VIBRATION: Game Over Triggered (Long Effect)")
            // Android 11 любит TextHandleMove больше чем LongPress
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        if (engine.isWin) {
            Log.i("MINES_DEBUG", "VIBRATION: Victory Triggered")
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFBDBDBD))) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MinesveeperHeader(engine, viewModel.currentTime, viewModel::togglePause)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (!isPaused) {
                                scale = (scale * zoom).coerceIn(0.2f, 5f)
                                offset += pan
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MinesveeperGrid(
                        engine = engine,
                        tick = tick,
                        onClick = { x, y -> 
                            viewModel.onCellClick(x, y)
                        },
                        onLongClick = { x, y ->
                            // Задача Жеки (UI): Моментальная вибрация под пальцем при флаге
                            Log.i("MINES_DEBUG", "VIBRATION: Flag at ($x, $y)")
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onCellLongClick(x, y)
                        },
                        modifier = Modifier.wrapContentSize(unbounded = true)
                    )
                }

                IconButton(
                    onClick = { scale = 1f; offset = Offset.Zero },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
                }
            }

            GameControls(
                onBack = {
                    viewModel.clearGame()
                    onBack()
                },
                onRestart = viewModel::restart,
                engine = engine,
                currentTool = viewModel.currentTool,
                onToolChange = viewModel::setTool
            )
        }

        // Красная вспышка при проигрыше (Задача Жеки - Фронт)
        if (engine.isGameOver) {
            val infiniteTransition = rememberInfiniteTransition(label = "flash")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = alpha))
                    .pointerInput(Unit) {}
            )
        }

        // Task 4: Stats screen (GameOver Modal)
        if (engine.isGameOver || engine.isWin) {
            StatsDialog(
                isWin = engine.isWin,
                timeMs = viewModel.currentTime,
                clicks = viewModel.clickCount,
                onRestart = viewModel::restart,
                onExit = {
                    viewModel.clearGame()
                    onBack()
                }
            )
        }

        if (isPaused) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).pointerInput(Unit) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAUSED", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = viewModel::togglePause, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text("RESUME")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsDialog(isWin: Boolean, timeMs: Long, clicks: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWin) "VICTORY!" else "GAME OVER",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWin) Color(0, 128, 0) else Color.Red
                )
                Spacer(Modifier.height(16.dp))
                
                StatRow("Time", formatTime(timeMs))
                StatRow("Clicks", clicks.toString())
                
                // Эффективность (Задача Жеки/Витовта "На равных")
                val efficiency = if (clicks > 0) String.format("%.2f", 1.0) else "0.00"
                StatRow("Efficiency", "$efficiency cells/click")

                Spacer(Modifier.height(24.dp))
                
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
                    Text("Play Again")
                }
                TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to Menu")
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun MinesveeperHeader(engine: MinesveeperEngine, timeMs: Long, onPause: () -> Unit) {
    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MineInfoItem(R.drawable.mine_r, engine.remainingMines[1] ?: 0)
            MineInfoItem(R.drawable.mine_g, engine.remainingMines[2] ?: 0)
            Text(text = formatTime(timeMs), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            MineInfoItem(R.drawable.mine_b, engine.remainingMines[3] ?: 0)
            IconButton(onClick = onPause) {
                Icon(painterResource(R.drawable.ic_launcher_background), "Pause", Modifier.size(24.dp))
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}

@Composable
fun MineInfoItem(iconRes: Int, count: Int, textColor: Color = Color.Red) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, Modifier.size(24.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = count.toString(), color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun MinesveeperGrid(
    engine: MinesveeperEngine, 
    tick: Long, 
    onClick: (Int, Int) -> Unit, 
    onLongClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cellSize = 24.dp 
    key(tick) {
        Column(modifier = modifier.border(2.dp, Color.Black)) {
            for (y in 0 until engine.height) {
                Row {
                    for (x in 0 until engine.width) {
                        CellView(engine.cells[y][x], engine.settings, Modifier.size(cellSize), { onClick(x, y) }, { onLongClick(x, y) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellView(cell: GameCell, settings: LevelSettings, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        val bgRes = if (cell.isRevealed) R.drawable.ground else R.drawable.closed
        Image(painterResource(bgRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        if (cell.isRevealed && !cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = Color.Black.copy(alpha = 0.3f)) }
        }

        if (cell.isRevealed && cell.isMine && cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = Color.Red.copy(alpha = 0.5f)) }
        }

        if (cell.isFlagged && !cell.isRevealed) {
            Image(painterResource(getFlagRes(cell.flaggedValue)), null, Modifier.fillMaxSize(0.8f))
        } else if (cell.isRevealed) {
            if (cell.isMine) {
                Image(painterResource(getMineRes(cell.mineValue)), null, Modifier.fillMaxSize(0.8f))
                if (settings.isChargeMode) {
                    Text(text = cell.mineValue.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else if (cell.adjacentSum != 0) {
                val numRes = getNumRes(cell.adjacentSum)
                if (numRes != 0) {
                    Image(painterResource(numRes), null, Modifier.fillMaxSize(0.7f))
                } else {
                    Text(text = cell.adjacentSum.toString(), fontWeight = FontWeight.Bold, color = getNumberColor(cell.adjacentSum), fontSize = 14.sp)
                }
            }
        }
    }
}

fun getMineRes(v: Int) = when(v) { 1 -> R.drawable.mine_r; 2 -> R.drawable.mine_g; 3 -> R.drawable.mine_b; -1 -> R.drawable.mine_rx; else -> R.drawable.mine_r }
fun getFlagRes(v: Int) = when(v) { 1 -> R.drawable.flag_r; 2 -> R.drawable.flag_final_2; 3 -> R.drawable.flag_final_3; -1 -> R.drawable.flag_bx_final; else -> R.drawable.flag_r }
fun getNumRes(n: Int): Int = when(n) { 1 -> R.drawable.numbers_num_1; 2 -> R.drawable.numbers_num_2; 3 -> R.drawable.numbers_num_3; 4 -> R.drawable.numbers_num_4; 5 -> R.drawable.numbers_num_5; 6 -> R.drawable.numbers_num_6; 7 -> R.drawable.numbers_num_7; 8 -> R.drawable.numbers_num_8; 9 -> R.drawable.numbers_num_9; else -> 0 }
fun getNumberColor(n: Int): Color = when { n == 1 -> Color.Blue; n == 2 -> Color(0, 128, 0); n >= 3 -> Color.Red; n < 0 -> Color(0, 0, 255); else -> Color.Black }

@Composable
fun GameControls(onBack: () -> Unit, onRestart: () -> Unit, engine: MinesveeperEngine, currentTool: Tool, onToolChange: (Tool) -> Unit) {
    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack) { Text("Settings") }
            Button(
                onClick = { onToolChange(if (currentTool == Tool.DIG) Tool.FLAG else Tool.DIG) },
                colors = ButtonDefaults.buttonColors(containerColor = if(currentTool == Tool.FLAG) Color.Red else Color.DarkGray)
            ) {
                Text(if(currentTool == Tool.DIG) "Dig" else "Flag")
            }
            if (engine.isGameOver) Text("💀", fontSize = 24.sp)
            if (engine.isWin) Text("🏆", fontSize = 24.sp)
            Button(onClick = onRestart) { Text("Restart") }
        }
    }
}
