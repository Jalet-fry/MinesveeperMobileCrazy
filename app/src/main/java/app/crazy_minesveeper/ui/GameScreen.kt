package app.crazy_minesveeper.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
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
import java.util.*

@Composable
fun MinesveeperScreen(
    levelSettings: LevelSettings,
    onBack: () -> Unit,
    viewModel: MinesveeperViewModel = viewModel()
) {
    LaunchedEffect(levelSettings) {
        viewModel.startLevel(levelSettings)
    }

    val engine = viewModel.engine ?: return
    val tick = viewModel.tick
    val currentTime = viewModel.currentTime

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val cellSize = 32.dp

    LaunchedEffect(engine, boxSize) {
        if (boxSize == IntSize.Zero) return@LaunchedEffect
        val cellSizePx = with(density) { cellSize.toPx() }
        val gridWidth = engine.width * cellSizePx
        val gridHeight = engine.height * cellSizePx
        if (gridWidth > 0 && gridHeight > 0) {
            val scaleX = (boxSize.width * 0.9f) / gridWidth
            val scaleY = (boxSize.height * 0.9f) / gridHeight
            scale = minOf(scaleX, scaleY, 1f).coerceIn(0.05f, 5f)
            offset = Offset.Zero
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFE3F2FD)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MinesveeperHeader(engine, currentTime, levelSettings.title)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { boxSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        scale = (scale * zoom).coerceIn(0.05f, 5f)
                        val newOffset = offset + pan
                        val zoomChange = scale / oldScale
                        offset = (newOffset - centroid) * zoomChange + centroid
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
                Box(modifier = Modifier.wrapContentSize(unbounded = true)) {
                    // 1. Слой сетки
                    MinesveeperGrid(engine, tick, viewModel::onCellClick, viewModel::onCellLongClick)
                    
                    // 2. Слой сущностей (Овцы, собаки и т.д.)
                    EntityLayer(engine, tick, cellSize)
                }
            }

            IconButton(
                onClick = {
                    val cellSizePx = with(density) { cellSize.toPx() }
                    val gridWidth = engine.width * cellSizePx
                    val gridHeight = engine.height * cellSizePx
                    if (boxSize != IntSize.Zero && gridWidth > 0) {
                        scale = minOf(boxSize.width * 0.9f / gridWidth, boxSize.height * 0.9f / gridHeight, 1f)
                        offset = Offset.Zero
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    .background(Color.White.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom")
            }
        }

        GameControls(
            onBack = onBack,
            onRestart = viewModel::restart,
            engine = engine,
            currentTool = viewModel.currentTool,
            onToolChange = viewModel::setTool
        )
    }
}

@Composable
fun EntityLayer(engine: MinesveeperEngine, tick: Long, cellSize: androidx.compose.ui.unit.Dp) {
    key(tick) {
        Box(modifier = Modifier.size(cellSize * engine.width, cellSize * engine.height)) {
            engine.activeEntities.forEach { entity ->
                val imageRes = when (entity) {
                    is SheepEntity -> R.drawable.entities_sheep
                    is DogEntity -> R.drawable.entities_dog
                    is HorseEntity -> R.drawable.entities_horse
                    is RatEntity -> R.drawable.entities_rat
                    is CheeseEntity -> R.drawable.entities_cheese
                    is BallEntity -> R.drawable.entities_ball
                    is ProjectileEntity -> when(entity.direction) {
                        0 -> R.drawable.entities_projectile_up
                        1 -> R.drawable.entities_projectile_right
                        2 -> R.drawable.entities_projectile_down
                        else -> R.drawable.entities_projectile_left
                    }
                    else -> 0
                }
                if (imageRes != 0) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(cellSize)
                            .offset(cellSize * entity.x, cellSize * entity.y)
                    )
                }
            }
        }
    }
}

@Composable
fun MinesveeperHeader(engine: MinesveeperEngine, timeMs: Long, title: String) {
    val seconds = (timeMs / 1000) % 60
    val minutes = (timeMs / 60000)
    val ms = (timeMs % 1000)
    val timeStr = String.format(Locale.getDefault(), "%d:%02d.%03d", minutes, seconds, ms)

    Surface(tonalElevation = 4.dp, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = timeStr, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                val totalMines = engine.settings.mines.values.sum()
                val flags = engine.cells.sumOf { r -> r.count { it.isFlagged } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.flag_r), contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(text = " $flags / $totalMines", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (flags > totalMines) Color.Red else Color.Unspecified)
                }
            }
        }
    }
}

@Composable
fun MinesveeperGrid(engine: MinesveeperEngine, tick: Long, onClick: (Int, Int) -> Unit, onLongClick: (Int, Int) -> Unit) {
    val cellSize = 32.dp
    val totalWidth = cellSize * engine.width
    val totalHeight = cellSize * engine.height

    key(tick) {
        Column(
            modifier = Modifier
                .requiredSize(totalWidth, totalHeight)
                .border(1.dp, Color.Gray)
                .background(Color.DarkGray)
        ) {
            for (y in 0 until engine.height) {
                Row(modifier = Modifier.requiredHeight(cellSize)) {
                    for (x in 0 until engine.width) {
                        val cell = engine.cells[y][x]
                        if (cell.exists) {
                            CellView(cell, engine, Modifier.size(cellSize), { onClick(x, y) }, { onLongClick(x, y) })
                        } else {
                            Box(modifier = Modifier.size(cellSize))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellView(cell: GameCell, engine: MinesveeperEngine, modifier: Modifier, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        val baseImage = if (cell.isRevealed) {
            if (cell.effect == "grayscale") R.drawable.grayscale_ground else R.drawable.ground
        } else R.drawable.closed
        
        Image(painter = painterResource(id = baseImage), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

        if (cell.flagType.isNotEmpty()) {
            Image(painter = painterResource(id = getFlagResId(cell.flagType)), contentDescription = null, modifier = Modifier.fillMaxSize(0.8f))
        } 
        else if (cell.isRevealed) {
            if (cell.isMine) {
                Image(painter = painterResource(id = getMineResId(cell.mineType)), contentDescription = null, modifier = Modifier.fillMaxSize(0.8f))
            } else if (cell.isObfuscated) {
                val clockImage = when(cell.clockTicks) {
                    4 -> R.drawable.misc_clock_4
                    3 -> R.drawable.misc_clock_3
                    2 -> R.drawable.misc_clock_2
                    else -> R.drawable.misc_clock_1
                }
                Image(painter = painterResource(id = clockImage), contentDescription = null, modifier = Modifier.fillMaxSize(0.6f))
            } else {
                when (engine.settings.displayStyle) {
                    DisplayStyle.COLORCHARGE -> ColorchargeIndicator(engine.getColorCharge(cell))
                    else -> {
                        val count = if (cell.effect == "grayscale") cell.neighboringMinesCount else cell.neighboringMinesCount
                        if (count > 0 || count < 0) {
                            val res = if (cell.effect == "grayscale") getGrayscaleNumResId(count) else getNumResId(count)
                            if (res != 0) Image(painter = painterResource(id = res), contentDescription = null, modifier = Modifier.fillMaxSize(0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorchargeIndicator(charge: Triple<Int, Int, Int>) {
    val (r, g, b) = charge
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        if (r > 0) Image(painterResource(R.drawable.count_count_r), null, Modifier.size(8.dp))
        if (g > 0) Image(painterResource(R.drawable.count_count_g), null, Modifier.size(8.dp))
        if (b > 0) Image(painterResource(R.drawable.count_count_b), null, Modifier.size(8.dp))
    }
}

@Composable
fun GameControls(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    engine: MinesveeperEngine,
    currentTool: Tool,
    onToolChange: (Tool) -> Unit
) {
    Surface(tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolButton(
                    icon = R.drawable.ground,
                    isSelected = currentTool == Tool.DIG,
                    onClick = { onToolChange(Tool.DIG) },
                    label = "Dig"
                )
                Spacer(modifier = Modifier.width(16.dp))
                ToolButton(
                    icon = R.drawable.flag_r,
                    isSelected = currentTool == Tool.FLAG,
                    onClick = { onToolChange(Tool.FLAG) },
                    label = "Flag"
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) { Text("Menu") }

                if (engine.isGameOver || engine.isWin) {
                    Text(
                        text = if (engine.isWin) "🏆 WIN" else "💀 BOOM",
                        color = if (engine.isWin) Color(0xFF2E7D32) else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Button(onClick = onRestart) { Text("Restart") }
            }
        }
    }
}

@Composable
fun ToolButton(icon: Int, isSelected: Boolean, onClick: () -> Unit, label: String) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.size(width = 80.dp, height = 50.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text = label, fontSize = 10.sp, color = contentColor)
        }
    }
}

fun getFlagResId(t: String) = when(t.lowercase()) { "r" -> R.drawable.flag_r; "g" -> R.drawable.flag_g; "b" -> R.drawable.flag_b; "rx" -> R.drawable.flag_rx; "gx" -> R.drawable.flag_gx; "bx" -> R.drawable.flag_bx; else -> R.drawable.flag_r }
fun getMineResId(t: String) = when(t.lowercase()) { "g" -> R.drawable.mine_g; "b" -> R.drawable.mine_b; "rx" -> R.drawable.mine_rx; "gx" -> R.drawable.mine_gx; "bx" -> R.drawable.mine_bx; else -> R.drawable.mine_r }

fun getNumResId(n: Int) = when {
    n > 0 -> when(n) { 1 -> R.drawable.numbers_num_1; 2 -> R.drawable.numbers_num_2; 3 -> R.drawable.numbers_num_3; 4 -> R.drawable.numbers_num_4; 5 -> R.drawable.numbers_num_5; 6 -> R.drawable.numbers_num_6; 7 -> R.drawable.numbers_num_7; 8 -> R.drawable.numbers_num_8; 9 -> R.drawable.numbers_num_9; else -> R.drawable.numbers_num_10 }
    n < 0 -> when(n) { -1 -> R.drawable.numbers_num_negative_1; -2 -> R.drawable.numbers_num_negative_2; -3 -> R.drawable.numbers_num_negative_3; -4 -> R.drawable.numbers_num_negative_4; -5 -> R.drawable.numbers_num_negative_5; -6 -> R.drawable.numbers_num_negative_6; -7 -> R.drawable.numbers_num_negative_7; -8 -> R.drawable.numbers_num_negative_8; else -> R.drawable.numbers_num_negative_9 }
    else -> 0
}

fun getGrayscaleNumResId(n: Int) = when {
    n > 0 -> when(n) { 1 -> R.drawable.grayscale_num_1; 2 -> R.drawable.grayscale_num_2; 3 -> R.drawable.grayscale_num_3; 4 -> R.drawable.grayscale_num_4; 5 -> R.drawable.grayscale_num_5; 6 -> R.drawable.grayscale_num_6; 7 -> R.drawable.grayscale_num_7; 8 -> R.drawable.grayscale_num_8; 9 -> R.drawable.grayscale_num_9; else -> R.drawable.grayscale_num_10 }
    n < 0 -> when(n) { -1 -> R.drawable.grayscale_num_negative_1; -2 -> R.drawable.grayscale_num_negative_2; -3 -> R.drawable.grayscale_num_negative_3; -4 -> R.drawable.grayscale_num_negative_4; -5 -> R.drawable.grayscale_num_negative_5; -6 -> R.drawable.grayscale_num_negative_6; -7 -> R.drawable.grayscale_num_negative_7; -8 -> R.drawable.grayscale_num_negative_8; else -> R.drawable.grayscale_num_negative_9 }
    else -> 0
}
