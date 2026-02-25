package app.crazy_minesveeper.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFBDBDBD)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MinesveeperHeader(engine)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
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
                MinesveeperGrid(engine, tick, viewModel::onCellClick, viewModel::onCellLongClick)
            }

            IconButton(
                onClick = { scale = 1f; offset = Offset.Zero },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
            }
        }

        GameControls(onBack, viewModel::restart, engine, viewModel.currentTool, viewModel::setTool)
    }
}

@Composable
fun MinesveeperHeader(engine: MinesveeperEngine) {
    Surface(tonalElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MineInfoItem(R.drawable.mine_r, engine.remainingMines[1] ?: 0)
            MineInfoItem(R.drawable.mine_g, engine.remainingMines[2] ?: 0)
            MineInfoItem(R.drawable.mine_b, engine.remainingMines[3] ?: 0)
            MineInfoItem(R.drawable.mine_rx, engine.remainingMines[-1] ?: 0, Color.Blue)
        }
    }
}

@Composable
fun MineInfoItem(iconRes: Int, count: Int, textColor: Color = Color.Red) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(iconRes), null, Modifier.size(24.dp))
        Spacer(Modifier.width(4.dp))
        Text(text = count.toString(), color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun MinesveeperGrid(engine: MinesveeperEngine, tick: Long, onClick: (Int, Int) -> Unit, onLongClick: (Int, Int) -> Unit) {
    val cellSize = 32.dp
    key(tick) {
        Column(modifier = Modifier.border(2.dp, Color.Black)) {
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
        // Фоновая картинка ячейки
        val bgRes = if (cell.isRevealed) R.drawable.ground else R.drawable.closed
        Image(painterResource(bgRes), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        // Подсветка для "автоматически открытых" клеток при проигрыше
        if (cell.isRevealed && !cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Black.copy(alpha = 0.3f))
            }
        }

        // Подсветка взорванной мины (на которую нажали)
        if (cell.isRevealed && cell.isMine && cell.revealedByPlayer) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = Color.Red.copy(alpha = 0.5f))
            }
        }

        if (cell.isFlagged && !cell.isRevealed) {
            Image(painterResource(getFlagRes(cell.flaggedValue)), null, Modifier.fillMaxSize(0.8f))
        } else if (cell.isRevealed) {
            if (cell.isMine) {
                Image(painterResource(getMineRes(cell.mineValue)), null, Modifier.fillMaxSize(0.8f))
                if (settings.isChargeMode) {
                    Text(
                        text = cell.mineValue.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            } else if (cell.adjacentSum != 0) {
                val numRes = getNumRes(cell.adjacentSum)
                if (numRes != 0) {
                    Image(painterResource(numRes), null, Modifier.fillMaxSize(0.7f))
                } else {
                    Text(
                        text = cell.adjacentSum.toString(),
                        fontWeight = FontWeight.Bold,
                        color = getNumberColor(cell.adjacentSum),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

fun getMineRes(v: Int) = when(v) {
    1 -> R.drawable.mine_r
    2 -> R.drawable.mine_g
    3 -> R.drawable.mine_b
    -1 -> R.drawable.mine_rx
    else -> R.drawable.mine_r
}

fun getFlagRes(v: Int) = when(v) {
    1 -> R.drawable.flag_r
    2 -> R.drawable.flag_final_2
    3 -> R.drawable.flag_final_3
    -1 -> R.drawable.flag_bx_final
    else -> R.drawable.flag_r
}

fun getNumRes(n: Int): Int = when(n) {
    1 -> R.drawable.numbers_num_1
    2 -> R.drawable.numbers_num_2
    3 -> R.drawable.numbers_num_3
    4 -> R.drawable.numbers_num_4
    5 -> R.drawable.numbers_num_5
    6 -> R.drawable.numbers_num_6
    7 -> R.drawable.numbers_num_7
    8 -> R.drawable.numbers_num_8
    9 -> R.drawable.numbers_num_9
    else -> 0
}

fun getNumberColor(n: Int): Color = when {
    n == 1 -> Color.Blue
    n == 2 -> Color(0, 128, 0)
    n >= 3 -> Color.Red
    n < 0 -> Color(0, 0, 255)
    else -> Color.Black
}

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
