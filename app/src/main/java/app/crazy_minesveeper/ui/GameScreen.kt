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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.crazy_minesveeper.R
import app.crazy_minesveeper.domain.FeedbackType
import app.crazy_minesveeper.domain.MinesveeperEngine
import app.crazy_minesveeper.domain.model.*
import app.crazy_minesveeper.ui.theme.MineNumberStyle
import app.crazy_minesveeper.ui.theme.*
import app.crazy_minesveeper.ui.viewmodel.MinesveeperViewModel
import app.crazy_minesveeper.ui.viewmodel.Tool

@Composable
fun MinesveeperScreen(
    levelSettings: LevelSettings,
    onBack: () -> Unit,
    viewModel: MinesveeperViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val skin = LocalGameSkin.current

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

    LaunchedEffect(Unit) {
        viewModel.feedbackFlow.collect { type ->
            when (type) {
                FeedbackType.FLAG_SET -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                FeedbackType.MINE_EXPLODE -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                FeedbackType.CHORD_SUCCESS -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                FeedbackType.ERROR -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                FeedbackType.REVEAL_EMPTY -> {} 
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(skin.mainBackground)) {
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
                            viewModel.onCellLongClick(x, y)
                        },
                        modifier = Modifier.wrapContentSize(unbounded = true)
                    )
                }

                IconButton(
                    onClick = { scale = 1f; offset = Offset.Zero },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f), shape = CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_zoom_desc))
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

        if (engine.isGameOver || engine.isWin) {
            StatsDialog(
                isWin = engine.isWin,
                timeMs = viewModel.currentTime,
                clicks = viewModel.clickCount,
                efficiency = viewModel.getEfficiency(),
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
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.paused), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = viewModel::togglePause, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.resume))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsDialog(isWin: Boolean, timeMs: Long, clicks: Int, efficiency: Double, onRestart: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWin) stringResource(R.string.victory) else stringResource(R.string.game_over),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isWin) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(24.dp))
                
                StatRow(stringResource(R.string.stat_time), formatTime(timeMs))
                StatRow(stringResource(R.string.stat_clicks), clicks.toString())
                StatRow(stringResource(R.string.efficiency_label), String.format("%.2f", efficiency) + " " + stringResource(R.string.cells_per_click))

                Spacer(Modifier.height(32.dp))
                
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(stringResource(R.string.play_again))
                }
                TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.back_to_menu))
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MinesveeperHeader(engine: MinesveeperEngine, timeMs: Long, onPause: () -> Unit) {
    val skin = LocalGameSkin.current
    Surface(
        tonalElevation = 4.dp, 
        modifier = Modifier.fillMaxWidth(),
        color = skin.panelBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MineInfoItem(R.drawable.mine_r, engine.remainingMines[1] ?: 0, getNumberColor(3, skin))
                Spacer(Modifier.width(8.dp))
                MineInfoItem(R.drawable.mine_g, engine.remainingMines[2] ?: 0, getNumberColor(2, skin))
                Spacer(Modifier.width(8.dp))
                MineInfoItem(R.drawable.mine_b, engine.remainingMines[3] ?: 0, getNumberColor(1, skin))
            }
            
            Text(
                text = formatTime(timeMs), 
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                color = if (skin.isDark) Color.White else MaterialTheme.colorScheme.primary
            )
            
            IconButton(onClick = onPause) {
                Icon(Icons.Default.PlayArrow, "Pause", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.scale(1.2f))
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
fun MineInfoItem(iconRes: Int, count: Int, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = count.toString(), color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
    val cellSize = 28.dp 
    val skin = LocalGameSkin.current
    key(tick) {
        Column(modifier = modifier.border(1.dp, skin.gridBorder)) {
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
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "cellPress")
    val skin = LocalGameSkin.current

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .combinedClickable(
                onClick = onClick, 
                onLongClick = onLongClick,
                onLongClickLabel = "Flag"
            ),
        contentAlignment = Alignment.Center
    ) {
        val bgRes = if (cell.isRevealed) R.drawable.ground else R.drawable.closed
        Image(painterResource(bgRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        if (cell.isRevealed && !cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = Color.Black.copy(alpha = 0.2f)) }
        }

        if (cell.isRevealed && cell.isMine && cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = Color.Red.copy(alpha = 0.4f)) }
        }

        if (cell.isFlagged && !cell.isRevealed) {
            Image(painterResource(getFlagRes(cell.flaggedValue)), null, Modifier.fillMaxSize(0.75f))
        } else if (cell.isRevealed) {
            if (cell.isMine) {
                Image(painterResource(getMineRes(cell.mineValue)), null, Modifier.fillMaxSize(0.7f))
                if (settings.isChargeMode) {
                    Text(text = cell.mineValue.toString(), color = Color.White, style = MineNumberStyle.copy(fontSize = 10.sp))
                }
            } else if (cell.adjacentSum != 0) {
                val numRes = getNumRes(cell.adjacentSum)
                if (numRes != 0) {
                    Image(painterResource(numRes), null, Modifier.fillMaxSize(0.65f))
                } else {
                    Text(
                        text = cell.adjacentSum.toString(), 
                        style = MineNumberStyle,
                        color = getNumberColor(cell.adjacentSum, skin)
                    )
                }
            }
        }
    }
}

fun getMineRes(v: Int) = when(v) { 1 -> R.drawable.mine_r; 2 -> R.drawable.mine_g; 3 -> R.drawable.mine_b; -1 -> R.drawable.mine_rx; else -> R.drawable.mine_r }
fun getFlagRes(v: Int) = when(v) { 1 -> R.drawable.flag_r; 2 -> R.drawable.flag_final_2; 3 -> R.drawable.flag_final_3; -1 -> R.drawable.flag_bx_final; else -> R.drawable.flag_r }
fun getNumRes(n: Int): Int = when(n) { 1 -> R.drawable.numbers_num_1; 2 -> R.drawable.numbers_num_2; 3 -> R.drawable.numbers_num_3; 4 -> R.drawable.numbers_num_4; 5 -> R.drawable.numbers_num_5; 6 -> R.drawable.numbers_num_6; 7 -> R.drawable.numbers_num_7; 8 -> R.drawable.numbers_num_8; 9 -> R.drawable.numbers_num_9; else -> 0 }

fun getNumberColor(n: Int, skin: GameSkin): Color {
    if (n < 0) return skin.mineAnti
    val index = n - 1
    return if (index in skin.numColors.indices) {
        skin.numColors[index]
    } else {
        if (skin.isDark) Color.White else Color.Black
    }
}

@Composable
fun GameControls(onBack: () -> Unit, onRestart: () -> Unit, engine: MinesveeperEngine, currentTool: Tool, onToolChange: (Tool) -> Unit) {
    val skin = LocalGameSkin.current
    Surface(
        tonalElevation = 8.dp, 
        modifier = Modifier.fillMaxWidth(),
        color = skin.panelBackground
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.settings_title)) }
            
            FilledTonalButton(
                onClick = { onToolChange(if (currentTool == Tool.DIG) Tool.FLAG else Tool.DIG) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if(currentTool == Tool.FLAG) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(if(currentTool == Tool.DIG) stringResource(R.string.tool_dig) else stringResource(R.string.tool_flag))
            }
            
            if (engine.isGameOver || engine.isWin) {
                Text(if (engine.isWin) "🏆" else "💀", fontSize = 28.sp)
            }

            Button(onClick = onRestart) { Text(stringResource(R.string.restart)) }
        }
    }
}
