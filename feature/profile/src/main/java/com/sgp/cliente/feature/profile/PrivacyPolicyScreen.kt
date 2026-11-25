package com.sgp.cliente.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.sgp.cliente.core.designsystem.adaptive.SgpContentFrame
import com.sgp.cliente.core.designsystem.components.SgpTooltipButton
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import com.sgp.cliente.core.designsystem.theme.Spacing

private data class PolicySection(val title: String, val body: String)

private val sections = listOf(
    PolicySection(
        "1. Quem trata seus dados",
        "Este aplicativo é o canal oficial da JD FIBRA, seu provedor de " +
            "internet. O controlador dos seus dados pessoais é o " +
            "próprio provedor, com quem você mantém o contrato de serviço. " +
            "O app apenas exibe, no seu dispositivo, as informações que a API " +
            "oficial do provedor retorna.",
    ),
    PolicySection(
        "2. Dados coletados",
        "• CPF ou CNPJ e a senha do app JD FIBRA — usados somente " +
            "para autenticar junto ao provedor.\n" +
            "• Dados do contrato, faturas, códigos de pagamento e promessas — " +
            "são lidos da API e exibidos a você; não são armazenados além de " +
            "cache temporário em memória.\n" +
            "• Endereço do servidor do provedor — configuração salva no " +
            "aparelho para localizar a instância correta.",
    ),
    PolicySection(
        "3. Finalidade e base legal",
        "Os dados são usados exclusivamente para executar o contrato entre " +
            "você e o provedor (art. 7º, V, da LGPD — Lei 13.709/2018): " +
            "consultar faturas, gerar códigos PIX e linha digitável, solicitar " +
            "segunda via e registrar promessas de pagamento.",
    ),
    PolicySection(
        "4. Onde os dados ficam",
        "Suas credenciais são gravadas apenas neste aparelho, criptografadas " +
            "com o Android Keystore (AES/GCM). A sessão não é incluída em " +
            "backup em nuvem nem em transferência entre dispositivos. Ao " +
            "sair da conta, todos os dados locais são apagados.",
    ),
    PolicySection(
        "5. Compartilhamento",
        "O app não possui analytics, publicidade nem SDKs de terceiros. " +
            "Nenhum dado pessoal é enviado a terceiros: a única comunicação " +
            "é com o servidor do provedor, via conexão segura (HTTPS/TLS).",
    ),
    PolicySection(
        "6. Retenção",
        "Sessão e credenciais permanecem no aparelho até você sair da conta " +
            "ou desinstalar o app. Registros de log locais não contêm senhas, " +
            "tokens nem documentos.",
    ),
    PolicySection(
        "7. Seus direitos",
        "Nos termos do art. 18 da LGPD, você pode solicitar ao provedor " +
            "(controlador): confirmação de tratamento, acesso, correção, " +
            "anonimização, bloqueio, eliminação, portabilidade e informação " +
            "sobre compartilhamento. No app, a exclusão dos dados locais é " +
            "imediata em \"Sair da conta\".",
    ),
    PolicySection(
        "8. Segurança",
        "Tráfego exclusivamente criptografado (sem cleartext), senha cifrada " +
            "com chave protegida por hardware, sessão fora de backups e logs " +
            "sanitizados, conforme exigências do art. 46 da LGPD e do Marco " +
            "Civil da Internet (Lei 12.965/2014).",
    ),
    PolicySection(
        "9. Encarregado de dados (DPO)",
        "Dúvidas e solicitações sobre dados pessoais devem ser dirigidas ao " +
            "encarregado indicado pelo seu provedor de internet, pelos canais " +
            "oficiais de atendimento (app JD FIBRA, site ou telefone).",
    ),
    PolicySection(
        "10. Legislação aplicável",
        "Lei 13.709/2018 (LGPD), Lei 12.965/2014 (Marco Civil da Internet), " +
            "Lei 8.078/1990 (Código de Defesa do Consumidor) e Resolução " +
            "Anatel 632/2014 (direitos do consumidor de telecom).",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidade") },
                navigationIcon = {
                    SgpTooltipButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        description = "Voltar",
                        onClick = onBack,
                    )
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        SgpContentFrame(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(
                            text = "Política de Privacidade",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Como este app coleta, usa e protege seus dados " +
                                "pessoais, em conformidade com a legislação brasileira.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = section.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun PrivacyPolicyScreenPreview() {
    SgpTheme {
        PrivacyPolicyScreen(onBack = {})
    }
}
