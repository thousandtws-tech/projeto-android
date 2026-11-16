package com.sgp.cliente.core.designsystem.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sgp.cliente.core.designsystem.theme.LayoutTokens

private const val WIDTH_MEDIUM_MIN_DP = 600
private const val WIDTH_EXPANDED_MIN_DP = 840

@Composable
fun SgpContentFrame(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LayoutTokens.contentMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = maxWidth).fillMaxSize()) {
            content()
        }
    }
}

fun gridColumns(widthSizeClass: WindowWidthSizeClass): Int = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> 1
    WindowWidthSizeClass.Medium -> 2
    else -> 3
}

@Composable
fun currentWindowWidthSizeClass(): WindowWidthSizeClass {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < WIDTH_MEDIUM_MIN_DP -> WindowWidthSizeClass.Compact
        widthDp < WIDTH_EXPANDED_MIN_DP -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
}

val WindowSizeClass.isExpanded: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Expanded

val WindowSizeClass.isAtLeastMedium: Boolean
    get() = widthSizeClass != WindowWidthSizeClass.Compact
