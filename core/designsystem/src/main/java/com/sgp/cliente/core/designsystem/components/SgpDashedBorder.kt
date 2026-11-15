package com.sgp.cliente.core.designsystem.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dashed rounded-rect outline — used by the PIX copia-e-cola field, where
 * the dashed treatment is the established payment-code convention.
 */
fun Modifier.dashedBorder(
    color: Color,
    strokeWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 14.dp,
    dashOn: Dp = 8.dp,
    dashOff: Dp = 6.dp,
): Modifier = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashOn.toPx(), dashOff.toPx()),
        ),
    )
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke,
    )
}
