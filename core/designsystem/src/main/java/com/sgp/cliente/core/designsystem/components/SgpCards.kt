package com.sgp.cliente.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.core.designsystem.theme.StatusCancelled
import com.sgp.cliente.core.designsystem.theme.StatusCancelledContainer
import com.sgp.cliente.core.designsystem.theme.StatusOpen
import com.sgp.cliente.core.designsystem.theme.StatusOpenContainer
import com.sgp.cliente.core.designsystem.theme.StatusOverdue
import com.sgp.cliente.core.designsystem.theme.StatusOverdueContainer
import com.sgp.cliente.core.designsystem.theme.StatusPaid
import com.sgp.cliente.core.designsystem.theme.StatusPaidContainer

@Composable
fun SgpCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                content = content,
            )
        }
    }
}

/** Small colored status pill (invoice status, service status, etc.). */
@Composable
fun SgpStatusBadge(
    text: String,
    kind: SgpBadgeKind,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when (kind) {
        SgpBadgeKind.PAID -> StatusPaidContainer to StatusPaid
        SgpBadgeKind.OPEN -> StatusOpenContainer to StatusOpen
        SgpBadgeKind.OVERDUE -> StatusOverdueContainer to StatusOverdue
        SgpBadgeKind.NEUTRAL -> StatusCancelledContainer to StatusCancelled
    }
    Surface(
        color = background,
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs),
        )
    }
}

enum class SgpBadgeKind { PAID, OPEN, OVERDUE, NEUTRAL }

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun SgpCardPreview() {
    SgpTheme {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SgpCard {
                Text("Card estático", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Conteúdo de exemplo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SgpCard(onClick = {}) {
                Text("Card clicável", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SgpStatusBadgePreview() {
    SgpTheme {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            SgpStatusBadge(text = "Paga", kind = SgpBadgeKind.PAID)
            SgpStatusBadge(text = "Em aberto", kind = SgpBadgeKind.OPEN)
            SgpStatusBadge(text = "Vencida", kind = SgpBadgeKind.OVERDUE)
            SgpStatusBadge(text = "Cancelada", kind = SgpBadgeKind.NEUTRAL)
        }
    }
}
