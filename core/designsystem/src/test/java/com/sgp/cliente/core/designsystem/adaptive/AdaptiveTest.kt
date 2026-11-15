package com.sgp.cliente.core.designsystem.adaptive

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure adaptive helpers in `Adaptive.kt`.
 * `WindowSizeClass` has a private constructor, so instances are built via
 * [WindowSizeClass.calculateFromSize] over [DpSize] breakpoints.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class AdaptiveTest {

    private fun windowClass(widthDp: Int): WindowSizeClass =
        WindowSizeClass.calculateFromSize(DpSize(widthDp.dp, 800.dp))

    @Test
    fun `grid columns follow the width size class contract`() {
        assertEquals(1, gridColumns(WindowWidthSizeClass.Compact))
        assertEquals(2, gridColumns(WindowWidthSizeClass.Medium))
        assertEquals(3, gridColumns(WindowWidthSizeClass.Expanded))
    }

    @Test
    fun `isExpanded only above the 840dp width breakpoint`() {
        assertTrue(windowClass(840).isExpanded)
        assertTrue(windowClass(1200).isExpanded)
        assertFalse(windowClass(839).isExpanded)
        assertFalse(windowClass(360).isExpanded)
    }

    @Test
    fun `isAtLeastMedium only at or above the 600dp width breakpoint`() {
        assertTrue(windowClass(600).isAtLeastMedium)
        assertTrue(windowClass(840).isAtLeastMedium)
        assertFalse(windowClass(599).isAtLeastMedium)
        assertFalse(windowClass(360).isAtLeastMedium)
    }
}
