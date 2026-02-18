package com.example.first_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily


val RobotoFlex = FontFamily(
    Font(R.font.roboto_flex)
)


// i like thinking this like main fun
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PomodoroApp()  // crazy that all app it's just function
        }
    }


}

@Composable
fun PomodoroApp() {
    var timeLeft by remember { mutableIntStateOf(25 * 60)}
    var isRunning by remember { mutableStateOf(false)}

    LaunchedEffect(isRunning) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) isRunning = false
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
        ){
            Image(
                painter = painterResource(R.drawable.focus),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )






//            Text(
//                text = "Pomodoro",
//                modifier = Modifier
//                    .background(
//                        color = Color.Red,
//                        shape = RoundedCornerShape(12.dp)
//                    )
//                    .border(
//                        2.dp,
//                        Color.White,
//                        RoundedCornerShape(12.dp)
//                    )
//                    .padding(16.dp)
//            )


        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .background(Color(0xFFFFF2F2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.1.dp)

        ){
            Text(
                text = "%02d".format(minutes),
                fontSize = 208.sp,
                fontFamily = RobotoFlex,
                color = Color(0xFF471515)
            )

            Text(
                text = "%02d".format(seconds),
                fontSize = 208.sp,
                fontFamily = RobotoFlex,
                color = Color(0xFF471515)
            )

        }





        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF2F2)),
            horizontalArrangement = Arrangement.Center
        ){
            Button(onClick = { isRunning = true },
                modifier = Modifier.size(100.dp, 70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4C4C).copy(alpha = 0.15f))



            ) {
                Text(text = "Start",
                    color = Color(0xFF471515),
                    fontSize = 21.sp

                )
            }

            Spacer(modifier = Modifier.width(16.dp))



            Button(onClick = {isRunning = false},
                modifier = Modifier.size(100.dp, 70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4C4C).copy(alpha = 0.15f)))
            {
                Text(text = "Stop",
                    color = Color(0xFF471515),
                    fontSize = 21.sp
                    )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    isRunning = false
                    timeLeft = 25 * 60 },
                modifier = Modifier.size(110.dp, 70.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4C4C).copy(alpha = 0.15f)
                )


// FF4C4C   FF4C4C

            ) {
                Text(text = "Reset",
                    color = Color(0xFF471515),
                    fontSize = 21.sp
                    )

            }


        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
