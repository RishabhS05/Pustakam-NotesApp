package com.app.pustakam.android.extension

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue


fun TextFieldValue.applyBold(current: TextFieldValue): TextFieldValue {
    if (selection.collapsed) return this
    val newAnnotatedString = buildAnnotatedString {
        append(annotatedString)
        addStyle(
            style = SpanStyle(fontWeight = FontWeight.Bold),
            start = selection.start,
            end = selection.end
        )
    }
    return this.copy(annotatedString = newAnnotatedString)
}