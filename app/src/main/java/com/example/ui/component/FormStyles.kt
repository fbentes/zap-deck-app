package com.example.ui.component

import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900
import com.example.ui.theme.ZapDeckPrimary

/**
 * Standard text field colors ensuring high accessibility, contrast, and clean layout:
 * - High contrast dark slate labels and input text (WCAG AAA compliant on white container)
 * - Distinct borders on focused and unfocused states
 * - Disabled state: Distinctively and intentionally dimmed with lower contrast only when actually disabled.
 */
@Composable
fun zapDeckTextFieldColors(): TextFieldColors {
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = Slate900,
        unfocusedTextColor = Slate900,
        disabledTextColor = Slate400,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Slate100,
        focusedBorderColor = ZapDeckPrimary,
        unfocusedBorderColor = Slate400,
        disabledBorderColor = Slate200,
        focusedLabelColor = ZapDeckPrimary,
        unfocusedLabelColor = Slate700,
        disabledLabelColor = Slate400,
        focusedLeadingIconColor = ZapDeckPrimary,
        unfocusedLeadingIconColor = Slate700,
        disabledLeadingIconColor = Slate400,
        focusedSupportingTextColor = Slate500,
        unfocusedSupportingTextColor = Slate500,
        disabledSupportingTextColor = Slate400
    )
}
