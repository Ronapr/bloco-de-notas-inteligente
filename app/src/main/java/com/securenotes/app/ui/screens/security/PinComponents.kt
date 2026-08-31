package com.securenotes.app.ui.screens.security

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
internal fun PinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
                .take(com.securenotes.app.security.SecurityManager.MAX_PIN_LENGTH)
            if (digits != value) onValueChange(digits)
        },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(),
        modifier = modifier.testTag(testTag)
    )
}