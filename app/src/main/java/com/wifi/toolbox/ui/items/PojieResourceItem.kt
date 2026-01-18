package com.wifi.toolbox.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.wifi.toolbox.structs.PojieResource

@Composable
fun PojieResourceItem(
    modifier: Modifier = Modifier,
    res: PojieResource,
    checkbox: Boolean? = null
) {
    ListItem(
        headlineContent = { Text(res.name ?: res.id) },
        supportingContent = {
            Text(
                text = res.description ?: "无描述",
                maxLines = 2,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Icon(
                if (res.type == 1) Icons.Default.Code else Icons.Default.Description,
                contentDescription = null
            )
        },
        trailingContent = {
            if (checkbox != null) {
                Checkbox(
                    checked = checkbox,
                    onCheckedChange = null
                )
            }
        },
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}