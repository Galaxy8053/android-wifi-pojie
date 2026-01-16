package com.wifi.toolbox.ui.items

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wifi.toolbox.R

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    content: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val customFont = FontFamily(
        Font(resId = R.font.mono, weight = FontWeight.Normal)
    )

    val commonTextStyle = TextStyle(
        fontFamily = customFont,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )

    Surface(
        modifier = modifier,
        shape = OutlinedTextFieldDefaults.shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            val lineCount = content.text.lines().size
            val lineNumbers = (1..lineCount).joinToString("\n")

            Text(
                text = lineNumbers,
                style = commonTextStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    textAlign = TextAlign.End
                ),
                modifier = Modifier
                    .padding(start = 8.dp, top = 12.dp, bottom = 12.dp, end = 8.dp)
                    .widthIn(min = 28.dp)
            )

            Box(Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth()) {
                BasicTextField(
                    value = content,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = commonTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}