package com.app.pustakam.android.screen.signup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.screen.Screen
import com.app.pustakam.android.widgets.IconLoad
import com.app.pustakam.android.widgets.POutLinedTextFieldColors
import com.app.pustakam.android.widgets.PrimaryFilledButton

@Composable
fun SignUpView(onNavigate: (Screen) -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    val password by remember { mutableStateOf(TextFieldValue("")) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    val confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    var phone by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 12.dp)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconLoad(
            placeHolderDrawable = R.drawable.avatar,
            url = "https://picsum.photos/seed/picsum/200/300",
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        ) {}

        OutlinedTextField(value = name,
            shape = RoundedCornerShape(12.dp),
            label = { Text("Enter your name.") },
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier, onValueChange = { text ->
                name = text
            })
        OutlinedTextField(value = phone,
            label = { Text("Enter your phone number.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier, onValueChange = { text ->
                phone = text
            })
        OutlinedTextField(value = email,
            label = { Text("Enter your email.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier, onValueChange = { text ->
                email = text
            })
        OutlinedTextField(value = password,
            colors = POutLinedTextFieldColors(),
            label = { Text("Enter a strong password.") },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = textFieldModifier, onValueChange = { text ->
                email = text
            })
        OutlinedTextField(value = confirmPassword,
            label = { Text("Confirm your password.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = textFieldModifier, onValueChange = { text ->
                email = text
            })

        PrimaryFilledButton(
            label = "Sign up",
            modifier = Modifier.padding(top = 12.dp)
        ) {
            onNavigate(Screen.LoginScreen)
        }
    }
}

@Preview
@Composable
private fun SignUp() {
    MyApplicationTheme {
        SignUpView { screen, ->

        }
    }
}