package com.app.pustakam.android.widgets

import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.OutlinedTextFieldDefaults

import androidx.compose.runtime.Composable


@Composable
fun POutLinedTextFieldColors( ) : TextFieldColors  {
   return OutlinedTextFieldDefaults.colors(
      focusedLabelColor = colorScheme.tertiary,
      unfocusedLabelColor = colorScheme.secondary,
      unfocusedTextColor = colorScheme.secondary,
      focusedTextColor = colorScheme.tertiary,
      focusedBorderColor = colorScheme.secondary,
      cursorColor = colorScheme.tertiary,)
}