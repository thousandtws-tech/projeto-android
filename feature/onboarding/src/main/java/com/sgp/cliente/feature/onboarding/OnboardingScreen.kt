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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpTextButton
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @param:DrawableRes val illustration: Int,
    val illustrationDescription: String,
    val title: String,
    val body: String,
)

private val pages = listOf(
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
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    fun finish() {
        viewModel.complete()
        onFinish()
    }

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
                SgpTextButton(text = "Pular", onClick = ::finish)
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    count = pages.size,
                    current = page,
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
                            finish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
                    onClick = ::finish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
            Image(
                painter = painterResource(page.illustration),
                contentDescription = page.illustrationDescription,
                modifier = Modifier
                    .width(340.dp)
                    .height(264.dp),
                contentScale = ContentScale.Fit,
            )
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

/** Active dot: 28×6 primary pill; inactive: 8×6 outline-variant pill. */
@Composable
private fun OnboardingDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .height(8.dp)
            .semantics { contentDescription = "Passo ${current + 1} de $count" },
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

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun OnboardingPreview() {
    SgpTheme {
        OnboardingScreen(brandName = "SGP Cliente", onFinish = {})
    }
}
