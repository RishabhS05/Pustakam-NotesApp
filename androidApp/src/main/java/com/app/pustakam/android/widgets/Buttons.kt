package com.app.pustakam.android.widgets

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrimaryFilledButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(50.dp)
    ) {
        Text(label, style = TextStyle(color = MaterialTheme.colorScheme.primary, fontSize = 20.sp))
    }
}

@Composable
fun SecondaryTextButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(label, style = TextStyle(color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}