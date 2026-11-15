package com.sgp.cliente.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing

private const val SHIMMER_DURATION_MS = 1100
private const val SHIMMER_SWEEP_RATIO = 0.4f

/**
 * Shimmering placeholder — the Compose-native equivalent of a SkeletonLayout
 * mask. Draws an animated shine sweeping left→right over a [shape] block.
 * Use in loading states that mirror the real layout instead of a spinner.
 */
fun Modifier.sgpSkeleton(shape: Shape = RoundedCornerShape(10.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "sgpShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SHIMMER_DURATION_MS, easing = LinearEasing),
        ),
        label = "sgpShimmerProgress",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val shine = MaterialTheme.colorScheme.surface

    clip(shape).drawBehind {
        val sweep = size.width * SHIMMER_SWEEP_RATIO
        val x = progress * (size.width + sweep) - sweep
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base, shine, base),
                start = Offset(x, 0f),
                end = Offset(x + sweep, size.height),
            ),
        )
    }
}

@Composable
fun SgpSkeletonBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(10.dp),
) {
    Spacer(
        modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .sgpSkeleton(shape),
    )
}

/** One shimmering card row matching the invoice-list item proportions. */
@Composable
fun SgpSkeletonInvoiceCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                SgpSkeletonBox(width = 110.dp, height = 20.dp)
                SgpSkeletonBox(width = 150.dp, height = 14.dp)
            }
            SgpSkeletonBox(width = 74.dp, height = 24.dp, shape = RoundedCornerShape(50))
        }
    }
}

/** Skeleton mirroring the dashboard layout: contract card, banner, invoice rows. */
@Composable
fun SgpSkeletonDashboard(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        item {
            SkeletonCard {
                SgpSkeletonBox(width = 160.dp, height = 22.dp)
                SgpSkeletonBox(width = 220.dp, height = 15.dp)
            }
        }
        item { SgpSkeletonBox(height = 52.dp, shape = RoundedCornerShape(14.dp)) }
        item { SgpSkeletonBox(width = 140.dp, height = 16.dp) }
        items(5) { SgpSkeletonInvoiceCard() }
    }
}

/** Skeleton mirroring a grouped invoice list (headers + card rows). */
@Composable
fun SgpSkeletonInvoiceList(
    modifier: Modifier = Modifier,
    groups: Int = 3,
    itemsPerGroup: Int = 2,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        repeat(groups) {
            item { SgpSkeletonBox(width = 130.dp, height = 15.dp) }
            items(itemsPerGroup) { SgpSkeletonInvoiceCard() }
        }
    }
}

/** Skeleton mirroring the invoice-details screen (summary + copyable + actions). */
@Composable
fun SgpSkeletonBillDetails(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SkeletonCard {
            SgpSkeletonBox(width = 70.dp, height = 22.dp, shape = RoundedCornerShape(50))
            SgpSkeletonBox(width = 140.dp, height = 32.dp)
            SgpSkeletonBox(width = 200.dp, height = 15.dp)
        }
        SkeletonCard {
            SgpSkeletonBox(width = 90.dp, height = 13.dp)
            SgpSkeletonBox(height = 20.dp)
        }
        Spacer(Modifier.height(Spacing.xs))
        repeat(2) {
            SgpSkeletonBox(height = 48.dp, shape = RoundedCornerShape(50))
        }
    }
}

/** Generic skeleton for simple screens (intro text + rows + action button). */
@Composable
fun SgpSkeletonRows(
    modifier: Modifier = Modifier,
    rows: Int = 4,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        SgpSkeletonBox(height = 34.dp)
        repeat(rows) { SgpSkeletonInvoiceCard() }
        Spacer(Modifier.height(Spacing.xs))
        SgpSkeletonBox(height = 48.dp, shape = RoundedCornerShape(50))
    }
}

@Composable
private fun SkeletonCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun SgpSkeletonInvoiceCardPreview() {
    SgpTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SgpSkeletonInvoiceCard()
            SgpSkeletonInvoiceCard()
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun SgpSkeletonRowsPreview() {
    SgpTheme {
        SgpSkeletonRows(rows = 2)
    }
}
