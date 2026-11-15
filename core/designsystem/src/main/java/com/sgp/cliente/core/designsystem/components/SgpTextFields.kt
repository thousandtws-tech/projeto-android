package com.sgp.cliente.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun SgpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { msg -> { Text(msg) } },
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
    )
}

/** CPF (000.000.000-00) or CNPJ (00.000.000/0000-00) input with live mask. */
@Composable
fun SgpCpfCnpjField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "CPF ou CNPJ",
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    SgpTextField(
        value = value,
        onValueChange = { input ->
            onValueChange(input.filter(Char::isDigit).take(MAX_DIGITS))
        },
        label = label,
        placeholder = "Somente números",
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = CpfCnpjVisualTransformation(),
        modifier = modifier,
    )
}

@Composable
fun SgpPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Senha",
    errorMessage: String? = null,
    enabled: Boolean = true,
) {
    var visible by remember { mutableStateOf(false) }
    SgpTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation =
            if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            SgpTooltipButton(
                icon = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                description = if (visible) "Ocultar senha" else "Mostrar senha",
                onClick = { visible = !visible },
            )
        },
        modifier = modifier,
    )
}

private const val MAX_DIGITS = 14

/** Applies CPF mask up to 11 digits, CNPJ mask above that. */
private class CpfCnpjVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(MAX_DIGITS)
        val masked = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                when {
                    digits.length <= 11 -> when (index) {
                        2, 5 -> append('.')
                        8 -> append('-')
                    }
                    else -> when (index) {
                        1, 4 -> append('.')
                        7 -> append('/')
                        11 -> append('-')
                    }
                }
            }
        }

        // Mask separator positions in ORIGINAL index space:
        // CPF  → after digits 3, 6, 9  (000.000.000-00)
        // CNPJ → after digits 2, 5, 8 and 12 (00.000.000/0000-00)
        val isCnpj = digits.length > 11
        val separators = if (isCnpj) intArrayOf(2, 5, 8, 12) else intArrayOf(3, 6, 9)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + separators.count { offset > it }).coerceAtMost(masked.length)

            override fun transformedToOriginal(offset: Int): Int =
                (offset - separators.count { it + separators.indexOf(it) < offset })
                    .coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(masked), offsetMapping)
    }
}
