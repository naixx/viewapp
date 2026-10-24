package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VSmallButton(
    text: String,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onClick: suspend () -> Unit = {}
) {
    OutlinedCard(
        modifier = modifier,
        onClick = { coroutineScope.launch { onClick() } },
        border = border ?: CardDefaults.outlinedCardBorder()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
