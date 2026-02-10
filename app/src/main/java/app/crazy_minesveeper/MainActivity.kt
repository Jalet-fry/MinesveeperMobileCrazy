package app.crazy_minesveeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crazy_minesveeper.data.repository.LevelRepository
import app.crazy_minesveeper.domain.model.LevelSettings
import app.crazy_minesveeper.ui.MinesveeperScreen
import app.crazy_minesveeper.ui.theme.Crazy_minesveeperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Crazy_minesveeperTheme {
                var currentLevel by remember { mutableStateOf<LevelSettings?>(null) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (currentLevel == null) {
                            LevelSelectScreen {
                                currentLevel = it
                            }
                        } else {
                            MinesveeperScreen(
                                levelSettings = currentLevel!!,
                                onBack = { currentLevel = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LevelSelectScreen(onLevelSelected: (LevelSettings) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Minesveeper 2023",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 48.dp, bottom = 24.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(LevelRepository.levels.size) { index ->
                val level = LevelRepository.levels[index]
                Button(
                    onClick = { onLevelSelected(level) },
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    Text(text = level.day.toString(), fontSize = 20.sp)
                }
            }
        }
    }
}
