package com.sgp.cliente.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/** One destination shown in [SgpFloatingNavBar]. */
data class SgpFloatingNavItem(
    val icon: ImageVector,
    val label: String,
)

private val BarHeight = 56.dp
private val BarCornerRadius = 14.dp
private val BarHorizontalMargin = 16.dp
private val BarBottomMargin = 8.dp
private val IndicatorSize = 44.dp
private val IndicatorIconSize = 22.dp
private val ItemIconSize = 18.dp
private val ItemGap = 3.dp
private val LabelSize = 10.sp

// Concave scoop geometry lifted from the Figma "Subtract" vector (design px == dp).
// The source geometry fits a 50dp indicator; NOTCH_SCALE keeps proportions
// for the current IndicatorSize.
private const val NOTCH_SCALE = 44f / 50f
private const val NOTCH_HALF = 46f
private const val NOTCH_DEPTH = 30f
private const val NOTCH_SHOULDER_Y = 15.2538f
private const val NOTCH_SHOULDER_C2Y = 7.6486f
private const val NOTCH_BOTTOM_C2Y = 24.0807f
private const val NOTCH_INNER_X = 25.838f
private const val NOTCH_INNER_C1X = 37.1634f
private const val NOTCH_INNER_C2X = 30.3376f
private const val NOTCH_BOTTOM_C1X = 20.6157f
private const val NOTCH_BOTTOM_C2X = 11f

/**
 * Floating bottom navigation with an animated raised indicator, based on the
 * "Modern Floating Navbar" design: a white rounded bar whose top edge scoops
 * down around a white circle carrying the selected item's icon. The circle
 * (and its scoop) springs to the newly selected slot on selection change.
 */
@Composable
fun SgpFloatingNavBar(
    items: List<SgpFloatingNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    require(items.size >= 2) { "SgpFloatingNavBar needs at least 2 items" }
    val indicatorLift = IndicatorSize / 2 // circle center sits on the bar's top edge

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.coerceIn(0, items.lastIndex).toFloat(),
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "navIndicator",
    )

    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { BarCornerRadius.toPx() }
    val notchDepthPx = with(density) { (NOTCH_DEPTH * NOTCH_SCALE).dp.toPx() }
    val notchShoulderYPx = with(density) { (NOTCH_SHOULDER_Y * NOTCH_SCALE).dp.toPx() }
    val notchShoulderC2YPx = with(density) { (NOTCH_SHOULDER_C2Y * NOTCH_SCALE).dp.toPx() }
    val notchBottomC2YPx = with(density) { (NOTCH_BOTTOM_C2Y * NOTCH_SCALE).dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = BarHorizontalMargin)
            .padding(bottom = BarBottomMargin)
            .height(indicatorLift + BarHeight),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val slotWidthPx = widthPx / items.size
        val notchCenterPx = (animatedIndex + 0.5f) * slotWidthPx

        val barShape = GenericShape { size, _ ->
            addPath(
                floatingBarPath(
                    width = size.width,
                    height = size.height,
                    cornerRadius = cornerRadiusPx,
                    notchCenter = notchCenterPx,
                    notchDepth = notchDepthPx,
                    notchShoulderY = notchShoulderYPx,
                    notchShoulderC2Y = notchShoulderC2YPx,
                    notchBottomC2Y = notchBottomC2YPx,
                    density = density,
                ),
            )
        }

        // The bar card fills the bottom BarHeight of the container.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BarHeight)
                .shadow(elevation = 8.dp, shape = barShape, clip = false)
                .background(color = containerColor, shape = barShape),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    // 1 as the indicator arrives over this slot, 0 when away.
                    val progress = (1f - abs(animatedIndex - index)).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(BarCornerRadius))
                            .selectable(
                                selected = index == selectedIndex,
                                role = Role.Tab,
                                onClick = { onItemSelected(index) },
                            )
                            .semantics { contentDescription = item.label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(ItemIconSize)
                                .graphicsLayer { alpha = 1f - progress },
                        )
                        Spacer(Modifier.height(ItemGap))
                        Text(
                            text = item.label,
                            fontSize = LabelSize,
                            fontWeight =
                                if (progress > 0.5f) FontWeight.SemiBold
                                else FontWeight.Medium,
                            color = lerp(contentColor, selectedContentColor, progress),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Raised indicator circle sliding along the bar's top edge.
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (notchCenterPx - IndicatorSize.toPx() / 2f).roundToInt(),
                        y = 0,
                    )
                }
                .size(IndicatorSize)
                .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
                .background(color = containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val shownIndex = animatedIndex.roundToInt().coerceIn(items.indices)
            Icon(
                imageVector = items[shownIndex].icon,
                contentDescription = null,
                tint = selectedContentColor,
                modifier = Modifier.size(IndicatorIconSize),
            )
        }
    }
}

/**
 * Rounded-rect bar with a concave scoop on its top edge, centered at
 * [notchCenter] (all values in px). The scoop's cubic control points
 * reproduce the Figma "Subtract" path: flat top until `cx - 46`, two cubic
 * segments curving down to depth 30 at `cx`, mirrored back up to `cx + 46`.
 * Near the edges the scoop merges into the corner, as in the design.
 */
private fun floatingBarPath(
    width: Float,
    height: Float,
    cornerRadius: Float,
    notchCenter: Float,
    notchDepth: Float,
    notchShoulderY: Float,
    notchShoulderC2Y: Float,
    notchBottomC2Y: Float,
    density: Density,
): Path {
    fun Float.designPx(): Float = with(density) { (this@designPx * NOTCH_SCALE).dp.toPx() }

    val x0 = notchCenter - NOTCH_HALF.designPx()
    val x1 = notchCenter - NOTCH_INNER_C1X.designPx()
    val x2 = notchCenter - NOTCH_INNER_C2X.designPx()
    val x3 = notchCenter - NOTCH_INNER_X.designPx()
    val x4 = notchCenter - NOTCH_BOTTOM_C1X.designPx()
    val x5 = notchCenter - NOTCH_BOTTOM_C2X.designPx()
    val x6 = notchCenter + NOTCH_BOTTOM_C2X.designPx()
    val x7 = notchCenter + NOTCH_BOTTOM_C1X.designPx()
    val x8 = notchCenter + NOTCH_INNER_X.designPx()
    val x9 = notchCenter + NOTCH_INNER_C2X.designPx()
    val x10 = notchCenter + NOTCH_INNER_C1X.designPx()
    val x11 = notchCenter + NOTCH_HALF.designPx()

    return Path().apply {
        // Top edge: left corner -> flat -> scoop -> flat -> right corner.
        moveTo(cornerRadius, 0f)
        lineTo(x0.coerceAtLeast(0f), 0f)
        cubicTo(x1, 0f, x2, notchShoulderC2Y, x3, notchShoulderY)
        cubicTo(x4, notchBottomC2Y, x5, notchDepth, notchCenter, notchDepth)
        cubicTo(x6, notchDepth, x7, notchBottomC2Y, x8, notchShoulderY)
        cubicTo(x9, notchShoulderC2Y, x10, 0f, x11.coerceAtMost(width), 0f)
        lineTo(width - cornerRadius, 0f)

        // Right edge + corners, bottom edge, left edge + corners.
        quadraticTo(width, 0f, width, cornerRadius)
        lineTo(width, height - cornerRadius)
        quadraticTo(width, height, width - cornerRadius, height)
        lineTo(cornerRadius, height)
        quadraticTo(0f, height, 0f, height - cornerRadius)
        lineTo(0f, cornerRadius)
        quadraticTo(0f, 0f, cornerRadius, 0f)
        close()
    }
}
