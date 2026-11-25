package com.sgp.cliente.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sgp.cliente.core.common.format.DateFormatter
import com.sgp.cliente.core.designsystem.components.SgpEmptyState
import com.sgp.cliente.core.designsystem.components.SgpPrimaryButton
import com.sgp.cliente.core.designsystem.components.SgpSecondaryButton
import com.sgp.cliente.core.designsystem.theme.Spacing
import com.sgp.cliente.domain.model.InAppNotification

/**
 * Sino com badge no header. `arrivalTick` incrementa a cada push
 * recebido em foreground — dispara uma animação sutil de balanço
 * (suprimida quando o usuário desativou animações no sistema).
 */
@Composable
fun NotificationBell(
    unreadCount: Int,
    arrivalTick: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(arrivalTick) {
        val animationsEnabled =
            android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()
        if (arrivalTick <= 0 || !animationsEnabled) return@LaunchedEffect
        rotation.animateTo(
            0f,
            keyframes {
                durationMillis = 650
                -14f at 90
                11f at 210
                -7f at 330
                4f at 450
                0f at 650
            },
        )
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    onClick = onClick,
                    onClickLabel = "Notificações",
                    role = Role.Button,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Notificações",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = rotation.value
                        transformOrigin = TransformOrigin(0.5f, 0.15f)
                    },
            )
        }
        if (unreadCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 2.dp),
            ) {
                Text(if (unreadCount > 9) "9+" else unreadCount.toString())
            }
        }
    }
}

/**
 * Central de notificações — sheet com a lista do inbox: ponto de
 * não-lida por item, "marcar todas" e estado vazio quando não há nada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    items: List<InAppNotification>,
    onDismiss: () -> Unit,
    onItemClick: (InAppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notificações", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onMarkAllRead, enabled = items.any { !it.read }) {
                    Text("Marcar todas como lidas")
                }
            }
            if (items.isEmpty()) {
                SgpEmptyState(
                    title = "Nenhuma notificação",
                    message = "Novos avisos do seu provedor aparecem aqui.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .padding(top = Spacing.xs),
                ) {
                    items(items, key = { it.id }) { item ->
                        NotificationRow(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: InAppNotification,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (item.read) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = notificationIcon(item),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp).padding(top = 2.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { "Notificação" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.read) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = DateFormatter.relative(item.receivedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!item.read) {
                Badge(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape),
                )
            }
        }
    }
}

/**
 * Modal para comunicados marcados como `modal` e para `app_update` —
 * que ganha o botão "Atualizar agora" abrindo o link enviado pelo CRM.
 */
@Composable
fun NotificationModal(
    item: InAppNotification,
    onDismiss: () -> Unit,
    onAction: (InAppNotification) -> Unit,
) {
    val openAction = { onAction(item) }

    AlertDialog(
        onDismissRequest = { if (!item.mandatory) onDismiss() },
        icon = {
            Icon(
                imageVector = notificationIcon(item),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                if (item.isAppUpdate) "Nova atualização disponível" else item.title,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (item.isAppUpdate && !item.appVersion.isNullOrBlank()) {
                    Text(
                        "Versão ${item.appVersion}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(item.body, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            if (item.isAppUpdate && item.actionUrl != null) {
                SgpPrimaryButton(text = "Atualizar agora", onClick = openAction)
            } else if (item.actionUrl != null) {
                SgpPrimaryButton(text = "Abrir", onClick = openAction)
            } else {
                TextButton(onClick = onDismiss) { Text("Entendi") }
            }
        },
        dismissButton = {
            if (!item.mandatory && (item.isAppUpdate || item.actionUrl != null)) {
                TextButton(onClick = onDismiss) { Text("Mais tarde") }
            }
        },
    )
}

private fun notificationIcon(item: InAppNotification): ImageVector = when (item.type) {
    "invoice" -> Icons.AutoMirrored.Filled.ReceiptLong
    "ticket" -> Icons.Filled.SupportAgent
    InAppNotification.TYPE_APP_UPDATE -> Icons.Filled.SystemUpdate
    "maintenance" -> Icons.Filled.Construction
    else -> Icons.Filled.Campaign
}
