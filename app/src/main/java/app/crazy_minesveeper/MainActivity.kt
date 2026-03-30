package app.crazy_minesveeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.crazy_minesveeper.data.repository.GameRepositoryImpl
import app.crazy_minesveeper.domain.model.LevelSettings
import app.crazy_minesveeper.domain.repository.IGameRepository
import app.crazy_minesveeper.ui.MinesveeperScreen
import app.crazy_minesveeper.ui.theme.Crazy_minesveeperTheme
import app.crazy_minesveeper.ui.theme.GameSkin
import app.crazy_minesveeper.ui.theme.GameSkins
import app.crazy_minesveeper.ui.theme.LocalGameSkin

class MainActivity : ComponentActivity() {
    private val gameRepository: IGameRepository = GameRepositoryImpl()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var currentSkin by remember { mutableStateOf(GameSkins.ClassicRetro) }
            
            Crazy_minesveeperTheme(skin = currentSkin) {
                var screenState by remember { mutableStateOf("menu") }
                var customSettings by remember { mutableStateOf(gameRepository.getDefaultLevel()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).background(currentSkin.mainBackground)) {
                        when (screenState) {
                            "menu" -> {
                                MainMenu(
                                    currentSkin = currentSkin,
                                    onSkinChange = { currentSkin = it },
                                    onStartCustom = { screenState = "settings" }
                                )
                            }
                            "settings" -> {
                                CustomSettingsScreen(
                                    initialSettings = customSettings,
                                    repository = gameRepository,
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
fun MainMenu(
    currentSkin: GameSkin,
    onSkinChange: (GameSkin) -> Unit,
    onStartCustom: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = if (currentSkin.isDark) Color.White else Color.Black
        )
        
        Spacer(Modifier.height(48.dp))
        
        Text(
            "SELECT SKIN",
            style = MaterialTheme.typography.labelLarge,
            color = if (currentSkin.isDark) Color.Gray else Color.DarkGray
        )
        
        Spacer(Modifier.height(16.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(GameSkins.allSkins) { skin ->
                SkinThumbnail(
                    skin = skin,
                    isSelected = skin.id == currentSkin.id,
                    onClick = { onSkinChange(skin) }
                )
            }
        }

        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = onStartCustom,
            modifier = Modifier.width(240.dp).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = currentSkin.panelBackground,
                contentColor = if (currentSkin.isDark) Color.White else Color.Black
            )
        ) {
            Text(stringResource(R.string.new_game), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun SkinThumbnail(skin: GameSkin, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(skin.mainBackground)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) Color.Cyan else skin.gridBorder,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Preview of grid
            Column {
                Row {
                    Box(Modifier.size(15.dp).background(skin.cellClosed).border(0.5.dp, skin.gridBorder))
                    Box(Modifier.size(15.dp).background(skin.cellOpened).border(0.5.dp, skin.gridBorder))
                }
                Row {
                    Box(Modifier.size(15.dp).background(skin.cellOpened).border(0.5.dp, skin.gridBorder))
                    Box(Modifier.size(15.dp).background(skin.cellClosed).border(0.5.dp, skin.gridBorder))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            skin.name.split(" ").first(),
            fontSize = 10.sp,
            color = if (isSelected) Color.Cyan else Color.Gray
        )
    }
}

@Composable
fun CustomSettingsScreen(
    initialSettings: LevelSettings,
    repository: IGameRepository,
    onStart: (LevelSettings) -> Unit,
    onBack: () -> Unit
) {
    val skin = LocalGameSkin.current
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
        Text(
            stringResource(R.string.difficulty_presets),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (skin.isDark) Color.White else Color.Black
        )
        Spacer(Modifier.height(8.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(repository.getLevels()) { level ->
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
                    colors = if (title == level.title) ButtonDefaults.outlinedButtonColors(containerColor = skin.panelBackground.copy(alpha = 0.3f)) 
                             else ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(1.dp, if (title == level.title) Color.Cyan else skin.gridBorder)
                ) {
                    Text(level.title, color = if (skin.isDark) Color.White else Color.Black)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = skin.gridBorder)
        Spacer(Modifier.height(16.dp))
        
        Text(stringResource(R.string.customize_parameters), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = if (skin.isDark) Color.White else Color.Black)
        Spacer(Modifier.height(16.dp))

        SettingField(stringResource(R.string.rows_label), rows) { rows = it; title = "Custom" }
        SettingField(stringResource(R.string.cols_label), cols) { cols = it; title = "Custom" }
        SettingField(stringResource(R.string.mine_1x_label), p1) { p1 = it; title = "Custom" }
        SettingField(stringResource(R.string.mine_2x_label), p2) { p2 = it; title = "Custom" }
        SettingField(stringResource(R.string.mine_3x_label), p3) { p3 = it; title = "Custom" }
        SettingField(stringResource(R.string.mine_anti_label), pAnti) { pAnti = it; title = "Custom" }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text(stringResource(R.string.charge_mode_label), modifier = Modifier.weight(1f), color = if (skin.isDark) Color.LightGray else Color.DarkGray)
            Switch(checked = isChargeMode, onCheckedChange = { 
                isChargeMode = it
                title = "Custom"
            })
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.total_mines_label, String.format("%.1f", totalPercent)),
            color = if (!isPercentValid) Color.Red else (if (skin.isDark) Color.Cyan else Color.Blue),
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))
        Row {
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                Text(stringResource(R.string.back_button))
            }
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = {
                    onStart(LevelSettings(
                        title = if (title == "Custom") "Custom Game" else title,
                        rows = rowsValue,
                        cols = colsValue,
                        p1 = p1.toDoubleOrNull() ?: 10.0,
                        p2 = p2.toDoubleOrNull() ?: 5.0,
                        p3 = p3.toDoubleOrNull() ?: 2.0,
                        pAnti = pAnti.toDoubleOrNull() ?: 3.0,
                        isChargeMode = isChargeMode
                    ))
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = skin.panelBackground)
            ) {
                Text(stringResource(R.string.start_button), color = if (skin.isDark) Color.White else Color.Black)
            }
        }
    }
}

@Composable
fun SettingField(label: String, value: String, onValueChange: (String) -> Unit) {
    val skin = LocalGameSkin.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, modifier = Modifier.weight(1f), color = if (skin.isDark) Color.LightGray else Color.DarkGray)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.width(150.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (skin.isDark) Color.White else Color.Black,
                unfocusedTextColor = if (skin.isDark) Color.White else Color.Black,
                focusedBorderColor = Color.Cyan,
                unfocusedBorderColor = skin.gridBorder
            )
        )
    }
}

@Preview(showBackground = true, name = "Main Menu Wireframe")
@Composable
fun MainMenuPreview() {
    Crazy_minesveeperTheme(skin = GameSkins.ClassicRetro) {
        MainMenu(
            currentSkin = GameSkins.ClassicRetro,
            onSkinChange = {},
            onStartCustom = {}
        )
    }
}

@Preview(showBackground = true, name = "Custom Settings Wireframe")
@Composable
fun SettingsPreview() {
    val repo = GameRepositoryImpl()
    Crazy_minesveeperTheme(skin = GameSkins.DeepDark) {
        CustomSettingsScreen(
            initialSettings = repo.getDefaultLevel(),
            repository = repo,
            onStart = {},
            onBack = {}
        )
    }
}
