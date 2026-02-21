package com.example.first_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PomodoroApp()
        }
    }
}

@Composable
fun PomodoroApp() {
    // Состояние для времени, которое будет использоваться при сбросе
    var initialTime by remember { mutableIntStateOf(25 * 60) }
    // Текущее оставшееся время
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
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color(0xFFFFF2F2),
            title = {
                Text(
                    text = "Time's up!",
                    fontFamily = RobotoFlex,
                    color = Color(0xFF471515),
                    fontSize = 24.sp
                )
            },
            text = {
                Text(
                    text = "What would you like to do next?",
                    fontFamily = RobotoFlex,
                    color = Color(0xFF471515),
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
                    fontSize = 18.sp
                )
            },
            dismissButton = {
                PomodoroButton(
                    text = "Rest",
                    onClick = {
                        timeLeft = 5 * 60 // 5 минут отдыха
                        isRunning = true
                        showDialog = false
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 18.sp
                )
            }
        )
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF2F2)),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF2F2)),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.focus),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .background(Color(0xFFFFF2F2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "%02d".format(minutes),
                fontSize = 200.sp,
                fontFamily = RobotoFlex,
                color = Color(0xFF471515)
            )
            Text(
                text = "%02d".format(seconds),
                fontSize = 200.sp,
                fontFamily = RobotoFlex,
                color = Color(0xFF471515)
            )
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
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.width(40.dp))
            
            PomodoroButton(
                text = "+1 min",
                onClick = {
                    initialTime += 60
                    if (!isRunning) timeLeft = initialTime
                },
                modifier = Modifier.wrapContentSize(),
                fontSize = 18.sp
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF2F2)),
            horizontalArrangement = Arrangement.Center
        ) {
            PomodoroButton(
                text = "Start",
                onClick = { isRunning = true },
                modifier = Modifier.size(100.dp, 70.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Stop",
                onClick = { isRunning = false },
                modifier = Modifier.size(100.dp, 70.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            PomodoroButton(
                text = "Reset",
                onClick = {
                    isRunning = false
                    timeLeft = initialTime
                },
                modifier = Modifier.size(110.dp, 70.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PomodoroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 21.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = if (isPressed) Color(0xFF6b4343) else Color(0xFFFF4C4C).copy(alpha = 0.15f)
    val textColor = if (isPressed) Color.White else Color(0xFF471515)

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
