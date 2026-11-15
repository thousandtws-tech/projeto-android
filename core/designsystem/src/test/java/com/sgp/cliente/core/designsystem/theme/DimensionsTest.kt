package com.sgp.cliente.core.designsystem.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the design-token contract: the spacing scale must stay strictly
 * increasing and the layout breakpoints must not drift silently.
 */
class DimensionsTest {

    @Test
    fun `spacing scale is strictly increasing`() {
        val scale = listOf(
            Spacing.xxs,
            Spacing.xs,
            Spacing.sm,
            Spacing.md,
            Spacing.lg,
            Spacing.xl,
            Spacing.xxl,
        )

        scale.zipWithNext().forEach { (smaller, larger) ->
            assertTrue("$larger should be > $smaller", larger > smaller)
        }
    }

    @Test
    fun `spacing anchors keep their documented values`() {
        assertEquals(4.dp, Spacing.xxs)
        assertEquals(8.dp, Spacing.xs)
        assertEquals(12.dp, Spacing.sm)
        assertEquals(16.dp, Spacing.md)
        assertEquals(24.dp, Spacing.lg)
        assertEquals(32.dp, Spacing.xl)
        assertEquals(48.dp, Spacing.xxl)
    }

    @Test
    fun `layout tokens keep the documented breakpoints`() {
        assertEquals(840.dp, LayoutTokens.contentMaxWidth)
        assertEquals(840.dp, LayoutTokens.twoPaneMinWidth)
        assertEquals(300.dp, LayoutTokens.gridItemMinWidth)
    }
}
