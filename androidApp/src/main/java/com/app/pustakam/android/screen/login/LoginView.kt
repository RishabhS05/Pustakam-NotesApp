package com.app.pustakam.android.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.POutLinedTextFieldColors
import com.app.pustakam.android.widgets.PrimaryFilledButton
import com.app.pustakam.android.widgets.SecondaryTextButton
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.extensions.isNotnull


@Composable
fun LoginView(onNavigateToHome: () -> Unit, onNavigateToSignUp: () -> Unit) {

    val loginViewModel: LoginViewModel = viewModel()
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    loginViewModel.loginUIState.collectAsState().value.apply {
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                loginViewModel.clearError()
            }
            isLogging -> onNavigateToHome()
            isLoading -> LoadingUI()
        }
    }
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 12.dp)
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = email,
            label = { Text("Enter your email or phone number.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier.focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction =  ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            onValueChange = { text ->
            email = text
        })
        OutlinedTextField(
            value = password,
            colors = POutLinedTextFieldColors(),
            label = { Text("Enter a strong password.") },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction =  ImeAction.Done),
            modifier = textFieldModifier,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    loginViewModel.login(email = email.text, password = password.text)
                }
            ),
            onValueChange = { text ->
                password = text
            })
        PrimaryFilledButton(label = "Login", modifier = Modifier.padding(top = 8.dp)) {
            loginViewModel.login(email = email.text, password = password.text)
        }
        SecondaryTextButton(label = "Sign up", onClick = { onNavigateToSignUp() })
    }
}

@Preview
@Composable
private fun LoginPre() {
    MyApplicationTheme {}

}