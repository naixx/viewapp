package com.github.naixx.viewapp.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VButton(
    text: String,
    imageVector: ImageVector? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onClick: suspend () -> Unit
) {
    Button(
        onClick = { coroutineScope.launch { onClick() } },
        colors = colors
    ) {
        imageVector?.let {
            Icon(
                imageVector = imageVector,
                contentDescription = text,
                modifier = Modifier.Companion.size(24.dp)
            )
            Spacer(modifier = Modifier.Companion.width(8.dp))
        }
        Text(text = text)
    }
}
