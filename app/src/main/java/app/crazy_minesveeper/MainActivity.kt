package app.crazy_minesveeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                var screenState by remember { mutableStateOf("menu") }
                var customSettings by remember { mutableStateOf(LevelSettings()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (screenState) {
                            "menu" -> {
                                MainMenu(
                                    onStartCustom = { screenState = "settings" }
                                )
                            }
                            "settings" -> {
                                CustomSettingsScreen(
                                    initialSettings = customSettings,
                                    onStart = {
                                        customSettings = it
                                        screenState = "game"
                                    },
                                    onBack = { screenState = "menu" }
                                )
                            }
                            "game" -> {
                                MinesveeperScreen(
                                    levelSettings = customSettings,
                                    onBack = { screenState = "settings" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenu(onStartCustom: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
//        Text("Crazy Minesweeper", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Сапер",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Black
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStartCustom, modifier = Modifier.width(200.dp)) {
            Text("NEW GAME")
        }
    }
}

@Composable
fun CustomSettingsScreen(
    initialSettings: LevelSettings,
    onStart: (LevelSettings) -> Unit,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialSettings.title) }
    var rows by remember { mutableStateOf(initialSettings.rows.toString()) }
    var cols by remember { mutableStateOf(initialSettings.cols.toString()) }
    var p1 by remember { mutableStateOf(initialSettings.p1.toString()) }
    var p2 by remember { mutableStateOf(initialSettings.p2.toString()) }
    var p3 by remember { mutableStateOf(initialSettings.p3.toString()) }
    var pAnti by remember { mutableStateOf(initialSettings.pAnti.toString()) }
    var isChargeMode by remember { mutableStateOf(initialSettings.isChargeMode) }

    val totalPercent = (p1.toDoubleOrNull() ?: 0.0) + 
                       (p2.toDoubleOrNull() ?: 0.0) + 
                       (p3.toDoubleOrNull() ?: 0.0) + 
                       (pAnti.toDoubleOrNull() ?: 0.0)

    val rowsValue = rows.toIntOrNull() ?: -1
    val colsValue = cols.toIntOrNull() ?: -1
    
    val isRowsValid = rowsValue in 5..50
    val isColsValid = colsValue in 5..50
    val isPercentValid = totalPercent <= 80.0
    val isValid = isRowsValid && isColsValid && isPercentValid

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("DIFFICULTY PRESETS", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        
        // Автоматически подхватывает все уровни из репозитория
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(LevelRepository.levels) { level ->
                OutlinedButton(
                    onClick = {
                        title = level.title
                        rows = level.rows.toString()
                        cols = level.cols.toString()
                        p1 = level.p1.toString()
                        p2 = level.p2.toString()
                        p3 = level.p3.toString()
                        pAnti = level.pAnti.toString()
                        isChargeMode = level.isChargeMode
                    },
                    colors = if (title == level.title) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer) 
                             else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(level.title)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))
        
        Text("CUSTOMIZE PARAMETERS", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))

        SettingField("Rows (5-50)", rows) { 
            rows = it
            title = "Custom Game" // Сбрасываем название пресета при ручном вводе
        }
        SettingField("Cols (5-50)", cols) { 
            cols = it
            title = "Custom Game"
        }
        SettingField("Mines 1x % (Red)", p1) { p1 = it; title = "Custom Game" }
        SettingField("Mines 2x % (Green)", p2) { p2 = it; title = "Custom Game" }
        SettingField("Mines 3x % (Blue)", p3) { p3 = it; title = "Custom Game" }
        SettingField("Mines -1x % (Anti)", pAnti) { pAnti = it; title = "Custom Game" }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Charge Mode (Math logic)", modifier = Modifier.weight(1f))
            Switch(checked = isChargeMode, onCheckedChange = { 
                isChargeMode = it
                title = "Custom Game"
            })
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Total Mines: ${String.format("%.1f", totalPercent)}%",
            color = if (!isPercentValid) Color.Red else Color.DarkGray,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))
        Row {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text("BACK")
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    onStart(LevelSettings(
                        title = title,
                        rows = rowsValue,
                        cols = colsValue,
                        p1 = p1.toDoubleOrNull() ?: 10.0,
                        p2 = p2.toDoubleOrNull() ?: 5.0,
                        p3 = p3.toDoubleOrNull() ?: 2.0,
                        pAnti = pAnti.toDoubleOrNull() ?: 3.0,
                        isChargeMode = isChargeMode
                    ))
                },
                enabled = isValid
            ) {
                Text("START")
            }
        }
    }
}

@Composable
fun SettingField(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(150.dp),
            singleLine = true
        )
    }
}
