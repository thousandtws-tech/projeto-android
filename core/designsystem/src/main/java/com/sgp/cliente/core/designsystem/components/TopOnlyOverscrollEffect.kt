package com.sgp.cliente.core.designsystem.components

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Overscroll só na borda superior: o efeito de plataforma (glow/stretch)
 * aparece nas duas pontas da lista — na base ele cobre o fim do conteúdo
 * durante o scroll, parecendo esconder itens atrás da barra flutuante.
 *
 * Deltas de topo (`delta.y > 0`, puxando para baixo) seguem para o efeito
 * real; deltas de base rolam direto para o scroll, sem visual nenhum.
 */
class TopOnlyOverscrollEffect(
    private val delegate: OverscrollEffect,
) : OverscrollEffect {

    override val isInProgress: Boolean get() = delegate.isInProgress
    override val node: DelegatableNode get() = delegate.node

    override fun applyToScroll(
        delta: Offset,
        source: NestedScrollSource,
        performScroll: (Offset) -> Offset,
    ): Offset =
        if (delta.y > 0f) {
            delegate.applyToScroll(delta, source, performScroll)
        } else {
            performScroll(delta)
        }

    override suspend fun applyToFling(
        velocity: Velocity,
        performFling: suspend (Velocity) -> Velocity,
    ) {
        if (velocity.y > 0f) {
            delegate.applyToFling(velocity, performFling)
        } else {
            performFling(velocity)
        }
    }
}
