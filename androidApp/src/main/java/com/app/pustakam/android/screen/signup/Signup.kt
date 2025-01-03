package com.app.pustakam.android.screen.signup

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import com.app.pustakam.android.widgets.CircleIconLoad
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.POutLinedTextFieldColors
import com.app.pustakam.android.widgets.PrimaryFilledButton
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.extensions.isNotnull

@Composable
fun SignUpView(onNavigate: () -> Unit) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    var phone by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }
    val registerViewModel: RegisterViewModel = viewModel()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    registerViewModel.signupUIState.collectAsStateWithLifecycle().value.apply {
        when {
            error.isNotnull() -> SnackBarUi(error = error!!) {
                registerViewModel.clearError()
            }
            isRegistered -> onNavigate()
            isLoading -> LoadingUI()
        }
    }
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp, horizontal = 12.dp)
    Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircleIconLoad(
            placeHolderDrawable = R.drawable.avatar, url = "https://picsum.photos/seed/picsum/200/300", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        ) {}

        OutlinedTextField(value = name, shape = RoundedCornerShape(12.dp),
            label = { Text("Enter your name.") },
            colors = POutLinedTextFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction =  ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            modifier = textFieldModifier.focusRequester(focusRequester),
            onValueChange = { text ->
            name = text
        })

        OutlinedTextField(value = email,
            label = { Text("Enter your email.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier,
            keyboardOptions = KeyboardOptions(imeAction =  ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            onValueChange = { text ->
            email = text
        })
        OutlinedTextField(value = phone,
            label = { Text("Enter your phone number.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            modifier = textFieldModifier,
            keyboardOptions = KeyboardOptions(imeAction =  ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            onValueChange = { text ->
            phone = text
        })
        OutlinedTextField(value = password,
            colors = POutLinedTextFieldColors(),
            label = { Text("Enter a strong password.") },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password,imeAction =  ImeAction.Next),
            modifier = textFieldModifier,
            keyboardActions = KeyboardActions(onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }),
            onValueChange = { text ->
                password = text
            })
        OutlinedTextField(value = confirmPassword,
            label = { Text("Confirm your password.") },
            shape = RoundedCornerShape(12.dp),
            colors = POutLinedTextFieldColors(),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
                registerViewModel.registerUser(
                    email = email.text,
                    password = password.text,
                    confirmPassword = confirmPassword.text,
                    name = name.text,
                    phoneNumber = phone.text
                )
            }),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = textFieldModifier,
            onValueChange = { text ->
                confirmPassword = text
            })

        PrimaryFilledButton(
            label = "Sign up", modifier = Modifier.padding(top = 12.dp)
        ) {
            registerViewModel.registerUser(
                email = email.text,
                password = password.text,
                confirmPassword = confirmPassword.text,
                name = name.text,
                phoneNumber = phone.text
            )
        }
    }
}

@Preview
@Composable
private fun SignUp() {
    MyApplicationTheme {
        SignUpView {}
    }
}