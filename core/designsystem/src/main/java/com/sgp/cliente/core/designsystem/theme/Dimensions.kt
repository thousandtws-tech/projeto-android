package com.sgp.cliente.core.designsystem.theme

import androidx.compose.ui.unit.dp

/** Centralized spacing scale — never hardcode dp values in screens. */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
}

/** Layout constraints for responsive/adaptive UIs. */
object LayoutTokens {
    /** Max readable content width on tablets/foldables. */
    val contentMaxWidth = 840.dp

    /** Minimum width for a two-pane (list + detail) layout. */
    val twoPaneMinWidth = 840.dp

    /** Minimum card width inside adaptive grids. */
    val gridItemMinWidth = 300.dp
}
