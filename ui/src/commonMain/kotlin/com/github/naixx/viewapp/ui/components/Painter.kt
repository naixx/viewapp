package com.github.naixx.viewapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/** An icon as a [Painter] (e.g. for Voyager's `TabOptions`); what kmp-libs' Android-only `painter` did. */
val ImageVector.painter: Painter
    @Composable get() = rememberVectorPainter(this)
