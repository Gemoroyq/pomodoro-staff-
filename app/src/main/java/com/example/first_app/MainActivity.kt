package com.example.first_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex)
)

private const val CHANNEL_ID = "pomodoro_timer_channel"

// 1. Класс для хранения твоих ЛИЧНЫХ настроек темы
data class PomodoroTheme(
    val backgroundColor: Color,
    val textColor: Color,
    val buttonColor: Color,
    val buttonPressedColor: Color
)

// 2. Список твоих тем. Ты можешь менять цвета здесь на любые другие!
val appThemes = listOf(
    PomodoroTheme( // Розовый (оригинал)
        backgroundColor = Color(0xFFFFF2F2),
        textColor = Color(0xFF471515),
        buttonColor = Color(0xFFFF4C4C).copy(alpha = 0.15f),
        buttonPressedColor = Color(0xFF6b4343)
    ),
    PomodoroTheme( // Голубой
        backgroundColor = Color(0xFFF2F5FF),
        textColor = Color(0xFF152247),
        buttonColor = Color(0xFF4C7BFF).copy(alpha = 0.15f),
        buttonPressedColor = Color(0xFF43516B)
    ),
    PomodoroTheme( // Зеленый
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
    
    // 3. Состояние текущей темы
    var themeIndex by remember { mutableIntStateOf(0) }
    val currentTheme = appThemes[themeIndex]

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
            containerColor = currentTheme.backgroundColor,
            title = {
                Text(
                    text = "Time's up!",
                    fontFamily = RobotoFlex,
                    color = currentTheme.textColor,
                    fontSize = 24.sp
                )
            },
            text = {
                Text(
                    text = "What would you like to do next?",
                    fontFamily = RobotoFlex,
                    color = currentTheme.textColor,
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
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 18.sp,
                    theme = currentTheme
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
                    theme = currentTheme
                )
            }
        )
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.backgroundColor),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ряд для выбора темы (маленькие кружочки)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            appThemes.forEachIndexed { index, theme ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(if (index == themeIndex) theme.textColor else theme.buttonPressedColor.copy(alpha = 0.5f))
                        .clickable { themeIndex = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%02d".format(minutes),
                fontSize = 170.sp,
                fontFamily = RobotoFlex,
                color = currentTheme.textColor
            )
            Text(
                text = "%02d".format(seconds),
                fontSize = 170.sp,
                fontFamily = RobotoFlex,
                color = currentTheme.textColor
            )
        }

        // Кнопки режимов (Пресеты)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val modes = listOf("25/5" to (25 to 5), "35/10" to (35 to 10), "50/10" to (50 to 10))
            modes.forEach { (label, times) ->
                PomodoroButton(
                    text = label,
                    onClick = {
                        initialTime = times.first * 60
                        breakTime = times.second * 60
                        if (!isRunning) timeLeft = initialTime
                    },
                    modifier = Modifier.wrapContentSize().padding(horizontal = 4.dp),
                    fontSize = 16.sp,
                    theme = currentTheme
                )
            }
        }



        // Кнопки для изменения времени таймера
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
                theme = currentTheme
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
                theme = currentTheme
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PomodoroButton(
                text = "Start",
                onClick = { isRunning = true },
                modifier = Modifier.size(100.dp, 70.dp),
                theme = currentTheme
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Stop",
                onClick = { isRunning = false },
                modifier = Modifier.size(100.dp, 70.dp),
                theme = currentTheme
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Reset",
                onClick = {
                    isRunning = false
                    timeLeft = initialTime
                },
                modifier = Modifier.size(110.dp, 70.dp),
                theme = currentTheme
            )
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
