package nadinee.passgenerator

import android.R.style.Theme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nadinee.passgenerator.data.Pass
import nadinee.passgenerator.data.Repository
import nadinee.passgenerator.ui.theme.PassGeneratorTheme // можешь переименовать в PassGeneratorTheme, если хочешь

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем репозиторий (загружаем сохранённые пароли)
        Repository.init(this)

        setContent {
            PassGeneratorTheme { // или твой собственный theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PasswordGeneratorScreen()
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen() {
    val context = LocalContext.current

    var generatedPassword by remember { mutableStateOf("") }
    var passwordLength by remember { mutableStateOf(12) }
    var showManualDialog by remember { mutableStateOf(false) }

    // Тригагер для обновления списка
    var updateTrigger by remember { mutableStateOf(0) }
    val forceUpdate = { updateTrigger++ }

    // Актуальный список паролей (всегда свежий при перерисовке)
    val passes = Repository.passes

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Генератор паролей",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // === Блок сгенерированного пароля ===
            if (generatedPassword.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = generatedPassword,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Галочка подтверждения
                var confirmed by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = { checked ->
                            confirmed = checked
                            if (checked) {
                                val now = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())

                                val newPass = Pass(
                                    password = generatedPassword,
                                    isCurrent = true,
                                    createdAt = now
                                )

                                val addedPass = Repository.addPassAndReturn(newPass, context)
                                Repository.setAsCurrent(addedPass.id, context)

                                generatedPassword = ""
                                confirmed = false
                                forceUpdate()  // Обновляем список
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Согласен — сохранить как текущий пароль")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // === Длина пароля ===
            Text("Длина пароля: $passwordLength", fontSize = 18.sp)
            Slider(
                value = passwordLength.toFloat(),
                onValueChange = { passwordLength = it.toInt() },
                valueRange = 6f..30f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // === Кнопка генерации ===
            Button(
                onClick = { generatedPassword = generateRandomPassword(passwordLength) },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Сгенерировать", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // === Кнопка ручного добавления ===
            OutlinedButton(
                onClick = { showManualDialog = true },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("+ Добавить свой пароль")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // === История паролей ===
            Text("История паролей", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (passes.isEmpty()) {
                Text("Нет сохранённых паролей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(passes, key = { it.id }) { pass ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = pass.password + if (pass.isCurrent) "  (текущий)" else "",
                                    fontWeight = if (pass.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pass.isCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            },
                            supportingContent = {
                                Text(pass.createdAt.toString())
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }

    // === Диалог ручного ввода ===
    if (showManualDialog) {
        var manualPassword by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Добавить свой пароль") },
            text = {
                OutlinedTextField(
                    value = manualPassword,
                    onValueChange = { manualPassword = it },
                    label = { Text("Введите пароль") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualPassword.isNotBlank()) {
                            val now = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())

                            val newPass = Pass(
                                password = manualPassword.trim(),
                                isCurrent = true,
                                createdAt = now
                            )
                            val added = Repository.addPassAndReturn(newPass, context)
                            Repository.setAsCurrent(added.id, context)
                            forceUpdate()  // Обновляем список!
                        }
                        showManualDialog = false
                        manualPassword = ""
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// === Функция генерации случайного пароля ===
private fun generateRandomPassword(length: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "abcdefghijklmnopqrstuvwxyz" +
            "0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}