package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.BorderColor
import com.example.myapplication.ui.theme.CyanAccent
import com.example.myapplication.ui.theme.ElevatedSurface
import com.example.myapplication.ui.theme.PanelBackground
import com.example.myapplication.ui.theme.PrimaryText
import com.example.myapplication.ui.theme.SecondaryText

/**
 * Premium Dark Panel/Card Container:
 * - Panel background: #121925
 * - Thin border: #273449
 * - Corner radii: 12-16 dp
 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = PrimaryText,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelBackground)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            action?.invoke()
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/**
 * StatMini card with tabular monospaced digits & explanation label.
 */
@Composable
fun StatMini(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    explanation: String? = null
) {
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(ElevatedSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        if (explanation != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = explanation,
                color = SecondaryText.copy(alpha = 0.7f),
                fontSize = 8.sp,
                lineHeight = 10.sp
            )
        }
    }
}

/**
 * Standard input field with minimum 48 dp touch target compliance.
 */
@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = SecondaryText, fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                focusedBorderColor = CyanAccent,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = ElevatedSurface,
                unfocusedContainerColor = PanelBackground
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
        )
    }
}

/**
 * Status Badge overlay with subtle glass styling.
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    backgroundColor: Color = ElevatedSurface
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
