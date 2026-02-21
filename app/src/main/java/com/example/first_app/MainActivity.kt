package com.example.first_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex)
)

private const val CHANNEL_ID = "pomodoro_timer_channel"

data class PomodoroTheme(
    val backgroundColor: Color,
    val textColor: Color,
    val buttonColor: Color,
    val buttonPressedColor: Color
)

// Данные для истории
data class PomodoroSession(
    val startTime: Long,
    val workMinutes: Int,
    val breakMinutes: Int
)

val appThemes = listOf(
    PomodoroTheme(
        backgroundColor = Color(0xFFFFF2F2),
        textColor = Color(0xFF471515),
        buttonColor = Color(0xFFFF4C4C).copy(alpha = 0.15f),
        buttonPressedColor = Color(0xFF6b4343)
    ),
    PomodoroTheme(
        backgroundColor = Color(0xFFF2F5FF),
        textColor = Color(0xFF152247),
        buttonColor = Color(0xFF4C7BFF).copy(alpha = 0.15f),
        buttonPressedColor = Color(0xFF43516B)
    ),
    PomodoroTheme(
        backgroundColor = Color(0xFFF2FFF5),
        textColor = Color(0xFF154723),
        buttonColor = Color(0xFF4CFF70).copy(alpha = 0.15f),
        buttonPressedColor = Color(0xFF436B4E)
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            PomodoroApp()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pomodoro Timer"
            val descriptionText = "Notifications for Pomodoro Timer completion"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

fun showNotification(context: Context) {
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.focus)
        .setContentTitle("Time's up!")
        .setContentText("Your session has finished. Take a break or start a new one.")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) {
            notify(1, builder.build())
        }
    }
}

@Composable
fun PomodoroApp() {
    val context = LocalContext.current
    var themeIndex by remember { mutableIntStateOf(0) }
    val currentTheme = appThemes[themeIndex]
    
    // Глобальное состояние истории и навигации
    val sessions = remember { mutableStateListOf<PomodoroSession>() }
    var showHistory by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AnimatedContent(targetState = showHistory, label = "ScreenTransition") { isHistoryVisible ->
        if (isHistoryVisible) {
            HistoryPage(
                sessions = sessions,
                theme = currentTheme,
                onBack = { showHistory = false }
            )
        } else {
            TimerPage(
                theme = currentTheme,
                themeIndex = themeIndex,
                onThemeChange = { themeIndex = it },
                onHistoryClick = { showHistory = true },
                onTimerStart = { work, rest ->
                    sessions.add(0, PomodoroSession(System.currentTimeMillis(), work, rest))
                },
                context = context
            )
        }
    }
}

@Composable
fun TimerPage(
    theme: PomodoroTheme,
    themeIndex: Int,
    onThemeChange: (Int) -> Unit,
    onHistoryClick: () -> Unit,
    onTimerStart: (Int, Int) -> Unit,
    context: Context
) {
    var initialTime by remember { mutableIntStateOf(25 * 60) }
    var breakTime by remember { mutableIntStateOf(5 * 60) }
    var timeLeft by remember { mutableIntStateOf(initialTime) }
    var isRunning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            if (timeLeft == 0) {
                isRunning = false
                showDialog = true
                showNotification(context)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = theme.backgroundColor,
            title = {
                Text(
                    text = "Time's up!",
                    fontFamily = RobotoFlex,
                    color = theme.textColor,
                    fontSize = 24.sp
                )
            },
            text = {
                Text(
                    text = "What would you like to do next?",
                    fontFamily = RobotoFlex,
                    color = theme.textColor,
                    fontSize = 18.sp
                )
            },
            confirmButton = {
                PomodoroButton(
                    text = "Work",
                    onClick = {
                        timeLeft = initialTime
                        isRunning = true
                        showDialog = false
                        onTimerStart(initialTime / 60, breakTime / 60)
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 18.sp,
                    theme = theme
                )
            },
            dismissButton = {
                PomodoroButton(
                    text = "Rest",
                    onClick = {
                        timeLeft = breakTime
                        isRunning = true
                        showDialog = false
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 18.sp,
                    theme = theme
                )
            }
        )
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка Истории слева
            IconButton(onClick = onHistoryClick) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "History",
                    tint = theme.textColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Выбор темы справа
            Row {
                appThemes.forEachIndexed { index, themeItem ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(if (index == themeIndex) themeItem.textColor else themeItem.buttonPressedColor.copy(alpha = 0.5f))
                            .clickable { onThemeChange(index) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Изображение удалено по просьбе пользователя

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp), // Немного увеличил высоту из-за удаления картинки
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%02d".format(minutes),
                fontSize = 170.sp,
                fontFamily = RobotoFlex,
                color = theme.textColor
            )
            Text(
                text = "%02d".format(seconds),
                fontSize = 170.sp,
                fontFamily = RobotoFlex,
                color = theme.textColor
            )
        }

        // Кнопка выбора режима с выпадающим меню (одна кнопка вместо нескольких)
        var expanded by remember { mutableStateOf(false) }
        val modes = listOf("25/5" to (25 to 5), "35/10" to (35 to 10), "50/10" to (50 to 10))
        
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .wrapContentSize(Alignment.TopCenter)
        ) {
            PomodoroButton(
                text = "Mode: ${initialTime/60}/${breakTime/60}",
                onClick = { expanded = true },
                modifier = Modifier.wrapContentSize(),
                fontSize = 16.sp,
                theme = theme
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(theme.backgroundColor)
            ) {
                modes.forEach { (label, times) ->
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = label, 
                                fontFamily = RobotoFlex, 
                                color = theme.textColor 
                            ) 
                        },
                        onClick = {
                            initialTime = times.first * 60
                            breakTime = times.second * 60
                            if (!isRunning) timeLeft = initialTime
                            expanded = false
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PomodoroButton(
                text = "-1 min",
                onClick = {
                    if (initialTime >= 60) {
                        initialTime -= 60
                        if (!isRunning) timeLeft = initialTime
                    }
                },
                modifier = Modifier.wrapContentSize(),
                fontSize = 18.sp,
                theme = theme
            )
            
            Spacer(modifier = Modifier.width(40.dp))
            
            PomodoroButton(
                text = "+1 min",
                onClick = {
                    initialTime += 60
                    if (!isRunning) timeLeft = initialTime
                },
                modifier = Modifier.wrapContentSize(),
                fontSize = 18.sp,
                theme = theme
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PomodoroButton(
                text = "Start",
                onClick = { 
                    isRunning = true 
                    onTimerStart(initialTime / 60, breakTime / 60)
                },
                modifier = Modifier.size(100.dp, 70.dp),
                theme = theme
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Stop",
                onClick = { isRunning = false },
                modifier = Modifier.size(100.dp, 70.dp),
                theme = theme
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Reset",
                onClick = {
                    isRunning = false
                    timeLeft = initialTime
                },
                modifier = Modifier.size(110.dp, 70.dp),
                theme = theme
            )
        }
    }
}

@Composable
fun HistoryPage(
    sessions: List<PomodoroSession>,
    theme: PomodoroTheme,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    
    val dateFormat = remember { SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.backgroundColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back", color = theme.textColor, fontFamily = RobotoFlex, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "History",
                fontFamily = RobotoFlex,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No sessions yet",
                    fontFamily = RobotoFlex,
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = 18.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sessions) { session ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = theme.buttonColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Mode: ${session.workMinutes}/${session.breakMinutes}",
                                    fontFamily = RobotoFlex,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textColor
                                )
                                Text(
                                    text = dateFormat.format(Date(session.startTime)),
                                    fontFamily = RobotoFlex,
                                    fontSize = 14.sp,
                                    color = theme.textColor.copy(alpha = 0.7f)
                                )
                            }
                            Text(
                                text = "Started",
                                fontFamily = RobotoFlex,
                                color = theme.textColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PomodoroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp,
    theme: PomodoroTheme
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed) theme.buttonPressedColor else theme.buttonColor
    val textColor = if (isPressed) Color.White else theme.textColor

    Button(
        onClick = onClick,
        modifier = modifier,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        )
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = RobotoFlex
        )
    }
}
