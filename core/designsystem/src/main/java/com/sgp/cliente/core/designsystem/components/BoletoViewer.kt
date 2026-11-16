package com.sgp.cliente.core.designsystem.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sgp.cliente.core.common.error.AppError
import com.sgp.cliente.core.designsystem.theme.Spacing
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Points → pixels multiplier for the rasterized boleto pages. */
private const val RENDER_SCALE = 2

/**
 * In-app boleto viewer: renders the API-downloaded PDF with the platform
 * `PdfRenderer` instead of handing the URL to an external browser.
 */
@Composable
fun BoletoViewerDialog(
    title: String,
    fileKey: String,
    pdf: ByteArray?,
    webUrl: String? = null,
    loading: Boolean,
    error: AppError?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                        Spacer(Modifier.size(Spacing.xs))
                        Text("Fechar")
                    }
                }
                when {
                    loading -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(Spacing.md))
                        Text("A baixar o boleto…", style = MaterialTheme.typography.bodyMedium)
                    }

                    error != null -> SgpErrorView(error = error, onRetry = onRetry)

                    webUrl != null -> BoletoWebPage(webUrl)

                    pdf != null -> BoletoPages(pdf, fileKey)
                }
            }
        }
    }
}

/** Bank-format boleto ("formato banco"): the provider `link` answers an
 * HTML boleto page rather than a local PDF — rendered in-app in a
 * WebView so the client never leaves the sheet. */
@Composable
private fun BoletoWebPage(url: String) {
    var pageLoading by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            pageLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
        )
        if (pageLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(Spacing.md))
                Text("A carregar o boleto…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BoletoPages(pdf: ByteArray, fileKey: String) {
    val context = LocalContext.current
    val pages by produceState<Result<List<ImageBitmap>>?>(initialValue = null, pdf) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val file = File.createTempFile("boleto-$fileKey-", ".pdf", context.cacheDir)
                try {
                    file.writeBytes(pdf)
                    renderPages(file)
                } finally {
                    file.delete()
                }
            }
        }
    }
    val result = pages
    when {
        result == null -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(Spacing.md))
            Text("A compor o boleto…", style = MaterialTheme.typography.bodyMedium)
        }

        result.isFailure || result.getOrNull().isNullOrEmpty() -> SgpErrorView(
            error = AppError.InvalidResponse,
            onRetry = null,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            itemsIndexed(result.getOrThrow()) { index, page ->
                Image(
                    bitmap = page,
                    contentDescription = "Página ${index + 1} do boleto",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

/** Rasterizes every page of [file] at [RENDER_SCALE]× into bitmaps. */
private fun renderPages(file: File): List<ImageBitmap> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        val renderer = PdfRenderer(descriptor)
        try {
            return (0 until renderer.pageCount).map { index ->
                val page = renderer.openPage(index)
                try {
                    val bitmap = Bitmap.createBitmap(
                        page.width * RENDER_SCALE,
                        page.height * RENDER_SCALE,
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    bitmap.asImageBitmap()
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
        }
    } finally {
        descriptor.close()
    }
}
