package com.app.pustakam.android.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.app.pustakam.android.screen.navigation.Screen

@Composable
fun NotificationView(onNavigate : ()->Unit) {
    Column(modifier = Modifier
        .fillMaxSize()
        .background(color = Color.DarkGray), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Notification", style = TextStyle(color = Color.White, fontSize = 30.sp))
    }
}