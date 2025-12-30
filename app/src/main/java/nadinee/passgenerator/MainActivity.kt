package nadinee.passgenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import nadinee.passgenerator.data.Pass
import nadinee.passgenerator.data.Repository
import nadinee.passgenerator.ui.theme.PassGeneratorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Repository.init(this)

        setContent {
            PassGeneratorTheme {
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
    val passes = Repository.passes

    val listState = rememberLazyListState() // 👈 состояние списка

    var generatedPassword by remember { mutableStateOf("") }
    var passwordLength by remember { mutableStateOf(12) }
    var showManualDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Генератор паролей",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ===== СГЕНЕРИРОВАННЫЙ ПАРОЛЬ =====
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

                var confirmed by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmed,
                        onCheckedChange = { checked ->
                            confirmed = checked
                            if (!checked) return@Checkbox

                            if (passes.any { it.password == generatedPassword }) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Этот пароль уже был сохранён ранее"
                                    )
                                }
                                confirmed = false
                                return@Checkbox
                            }

                            val now = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())

                            val newPass = Pass(
                                password = generatedPassword,
                                isCurrent = true,
                                createdAt = now
                            )

                            val added = Repository.addPassAndReturn(newPass, context)
                            Repository.setAsCurrent(added.id, context)

                            generatedPassword = ""
                            confirmed = false

                            scope.launch {
                                listState.animateScrollToItem(0) // 👈 автопрокрутка
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохранить как текущий пароль")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ===== ДЛИНА ПАРОЛЯ =====
            Text("Длина пароля: $passwordLength", fontSize = 18.sp)

            Slider(
                value = passwordLength.toFloat(),
                onValueChange = { passwordLength = it.toInt() },
                valueRange = 6f..30f,
                steps = 23,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    generatedPassword = generateRandomPassword(passwordLength)
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Сгенерировать", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showManualDialog = true },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("+ Добавить свой пароль")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ===== ИСТОРИЯ =====
            Text(
                text = "История паролей",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (passes.isEmpty()) {
                Text(
                    text = "Нет сохранённых паролей",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    state = listState // 👈 подключили состояние
                ) {
                    items(passes, key = { it.id }) { pass ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = pass.password +
                                            if (pass.isCurrent) "  (текущий)" else "",
                                    fontWeight = if (pass.isCurrent)
                                        FontWeight.Bold else FontWeight.Normal,
                                    color = if (pass.isCurrent)
                                        MaterialTheme.colorScheme.primary
                                    else LocalContentColor.current
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

    // ===== ДИАЛОГ РУЧНОГО ВВОДА =====
    if (showManualDialog) {

        var manualPassword by remember { mutableStateOf("") }
        var makeCurrent by remember { mutableStateOf(true) }
        var useCurrentDate by remember { mutableStateOf(true) }
        var manualDateTime by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Добавить пароль") },
            text = {
                Column {

                    OutlinedTextField(
                        value = manualPassword,
                        onValueChange = { manualPassword = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = makeCurrent,
                            onCheckedChange = { makeCurrent = it }
                        )
                        Text("Сделать текущим паролем")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = useCurrentDate,
                            onCheckedChange = { useCurrentDate = it }
                        )
                        Text("Использовать текущую дату и время")
                    }

                    if (!useCurrentDate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = manualDateTime,
                            onValueChange = { manualDateTime = it },
                            label = { Text("Дата и время (гггг-мм-дд чч:мм)") },
                            placeholder = { Text("2025-12-30 14:30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val password = manualPassword.trim()
                        if (password.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Введите пароль")
                            }
                            return@TextButton
                        }

                        if (passes.any { it.password == password }) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Этот пароль уже был сохранён ранее"
                                )
                            }
                            return@TextButton
                        }

                        val createdAt = if (useCurrentDate) {
                            Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        } else {
                            if (manualDateTime.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Введите дату и время")
                                }
                                return@TextButton
                            }
                            try {
                                LocalDateTime.parse(
                                    manualDateTime.replace(" ", "T")
                                )
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Неверный формат даты. Используйте гггг-мм-дд чч:мм"
                                    )
                                }
                                return@TextButton
                            }
                        }

                        val newPass = Pass(
                            password = password,
                            isCurrent = makeCurrent,
                            createdAt = createdAt
                        )

                        val added = Repository.addPassAndReturn(newPass, context)
                        if (makeCurrent) {
                            Repository.setAsCurrent(added.id, context)
                        }

                        scope.launch {
                            listState.animateScrollToItem(0)
                        }

                        showManualDialog = false
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

// ===== ГЕНЕРАЦИЯ ПАРОЛЯ =====
private fun generateRandomPassword(length: Int): String {
    val chars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { chars.random() }
        .joinToString("")
}
