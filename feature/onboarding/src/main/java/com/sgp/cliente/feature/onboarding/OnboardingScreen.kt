package com.sgp.cliente.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpTextButton
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.domain.model.OnboardingSlide
import kotlinx.coroutines.launch

internal data class OnboardingPage(
    /** Ilustração embutida (`R.drawable`) ou mídia do CMS — só uma vale
     * por vez; [videoUrl] tem precedência sobre a imagem. */
    @param:DrawableRes val illustration: Int? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val illustrationDescription: String,
    val title: String,
    val body: String,
)

internal val defaultPages = listOf(
    OnboardingPage(
        illustration = R.drawable.onboarding_connection,
        illustrationDescription = "Ilustração de uma órbita azul conectada",
        title = "Sua conexão.\nSeu controle.",
        body = "Sua internet, suas faturas e seu atendimento. Tudo em um só lugar.",
    ),
    OnboardingPage(
        illustration = R.drawable.onboarding_payments,
        illustrationDescription = "Ilustração de cartão de pagamento confirmado",
        title = "Menos burocracia.\nMais tempo pra você.",
        body = "Pague com Pix ou boleto e acompanhe suas faturas sem complicar.",
    ),
    OnboardingPage(
        illustration = R.drawable.onboarding_support,
        illustrationDescription = "Ilustração de balões de conversa de atendimento",
        title = "Perto de você.\nSempre que precisar.",
        body = "Resolva dúvidas, teste sua conexão e acompanhe cada atendimento.",
    ),
)

/** Tags used by Compose UI tests to locate onboarding elements. */
internal object OnboardingTestTags {
    const val Pager = "onboarding_pager"
    const val Dots = "onboarding_dots"
    const val SkipButton = "onboarding_skip_button"
    const val CtaButton = "onboarding_cta_button"
    const val SubscriberButton = "onboarding_subscriber_button"
}

/**
 * First-run onboarding (3 swipeable pages) matching the Figma
 * "Onboarding / Conexão · Pagamentos · Atendimento" flow.
 * [onFinish] runs when the user finishes, skips, or taps "Já sou assinante".
 */
@Composable
fun OnboardingScreen(
    brandName: String,
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val slides by viewModel.slides.collectAsStateWithLifecycle()
    val pages = remember(slides) {
        if (slides.isEmpty()) {
            defaultPages
        } else {
            slides.sortedBy(OnboardingSlide::position).map { slide ->
                OnboardingPage(
                    imageUrl = slide.imageUrl,
                    videoUrl = slide.videoUrl,
                    illustrationDescription = slide.title,
                    title = slide.title,
                    body = slide.subtitle,
                )
            }
        }
    }
    OnboardingContent(
        brandName = brandName,
        onFinish = {
            viewModel.complete()
            onFinish()
        },
        pages = pages,
    )
}

/**
 * Stateless onboarding UI; [OnboardingScreen] wires the finish action to
 * [OnboardingViewModel.complete]. [initialPage] exists for previews/tests.
 */
@Composable
internal fun OnboardingContent(
    brandName: String,
    onFinish: () -> Unit,
    initialPage: Int = 0,
    pages: List<OnboardingPage> = defaultPages,
) {
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pages.size },
    )
    val scope = rememberCoroutineScope()

    SgpContentFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Cabeçalho 64dp: marca + "Pular"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = brandName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                SgpTextButton(
                    text = "Pular",
                    onClick = onFinish,
                    modifier = Modifier.testTag(OnboardingTestTags.SkipButton),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .testTag(OnboardingTestTags.Pager),
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    count = pages.size,
                    current = page,
                    isCurrentPage = pagerState.currentPage == page,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Ações do onboarding: CTA pill 52dp + text button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(OnboardingTestTags.CtaButton),
                    shape = CircleShape,
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.lastIndex) {
                            "Continuar"
                        } else {
                            "Acessar minha conta"
                        },
                    )
                }
                SgpTextButton(
                    text = "Já sou assinante",
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(OnboardingTestTags.SubscriberButton),
                )
            }
        }
    }
}

/**
 * Área principal do Figma (justify-between): card 284dp no topo e
 * bloco de apresentação (dots + headline + body) centrado no restante.
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    count: Int,
    current: Int,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(284.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            when {
                page.videoUrl != null -> OnboardingVideo(
                    url = page.videoUrl,
                    isCurrentPage = isCurrentPage,
                    contentDescription = page.illustrationDescription,
                )
                page.illustration != null -> Image(
                    painter = painterResource(page.illustration),
                    contentDescription = page.illustrationDescription,
                    modifier = Modifier
                        .width(340.dp)
                        .height(264.dp),
                    contentScale = ContentScale.Fit,
                )
                else -> AsyncImage(
                    model = page.imageUrl,
                    contentDescription = page.illustrationDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            OnboardingDots(count = count, current = current)
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Vídeo do slide (CMS): ExoPlayer mudo em loop, sem controles, crop
 * igual à imagem. Só toca na página atual e em foreground — release
 * ao sair da composição libera o decoder.
 */
@Composable
private fun OnboardingVideo(
    url: String,
    isCurrentPage: Boolean,
    contentDescription: String?,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
    }
    LaunchedEffect(isCurrentPage) { player.playWhenReady = isCurrentPage }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isCurrentPage) player.play()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .semantics { this.contentDescription = contentDescription.orEmpty() },
    )
}

/** Active dot: 28×6 primary pill; inactive: 8×6 outline-variant pill. */
@Composable
private fun OnboardingDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .height(8.dp)
            .semantics { contentDescription = "Passo ${current + 1} de $count" }
            .testTag(OnboardingTestTags.Dots),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .width(if (index == current) 28.dp else 8.dp)
                    .height(6.dp)
                    .align(Alignment.CenterVertically)
                    .clip(CircleShape)
                    .background(
                        if (index == current) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Onboarding — Página 1")
@Composable
private fun OnboardingFirstPagePreview() {
    SgpTheme {
        OnboardingContent(brandName = "SGP Cliente", onFinish = {})
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Onboarding — Página 2")
@Composable
private fun OnboardingSecondPagePreview() {
    SgpTheme {
        OnboardingContent(brandName = "SGP Cliente", onFinish = {}, initialPage = 1)
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915, name = "Onboarding — Página 3")
@Composable
private fun OnboardingLastPagePreview() {
    SgpTheme {
        OnboardingContent(brandName = "SGP Cliente", onFinish = {}, initialPage = 2)
    }
}
