# Conformidade legal — LGPD, Marco Civil, CDC e Anatel

Inventário de como o SGP Cliente trata dados pessoais e atende à legislação
brasileira. Referência normativa: Lei 13.709/2018 (LGPD), Lei 12.965/2014
(Marco Civil da Internet), Lei 8.078/1990 (CDC) e Resolução Anatel 632/2014
(RGC — direitos do consumidor de telecom).

## Papéis

| Papel | Quem |
| --- | --- |
| Controlador | O provedor de internet (detentor do contrato com o titular) |
| Operador | O desenvolvedor do app, nos limites do que processa |
| Titular | O assinante (usuário do app) |

## Inventário de dados pessoais

| Dado | Origem | Onde fica | Retenção | Base legal |
| --- | --- | --- | --- | --- |
| CPF/CNPJ + senha | Digitado pelo titular | DataStore `sgp_session`, **cifrado (Keystore AES/GCM)** | Até logout/desinstalar | Execução de contrato (art. 7º, V) |
| Nome, contratos, plano | API do provedor | `sgp_session` | Até logout | Execução de contrato |
| Faturas, PIX, linha digitável | API do provedor | **Somente memória** (cache TTL 60 s) | Volátil | Execução de contrato |
| URL do servidor | Digitado/padrão | `sgp_settings` (não pessoal) | Até reconfigurar | Interesse legítimo |
| Logs | Runtime | Logcat local | Volátil | Sanitizados: sem senha/token/documento |

**Não coletamos**: localização, contatos, identificadores de publicidade,
analytics, telemetria, cookies. **Nenhum SDK de terceiros** com acesso a
dados pessoais — a única saída de dados é HTTPS para o servidor do provedor.

## Medidas de segurança (art. 46 LGPD / art. 7º Marco Civil)

- Tráfego exclusivamente TLS — `usesCleartextTraffic="false"` no manifest.
- Senha persistida somente cifrada; chave protegida pelo Android Keystore
  (hardware-backed quando disponível) e **não exportável**.
- Sessão **excluída** de backup em nuvem e device-to-device
  (`backup_rules.xml` / `data_extraction_rules.xml`) — a chave não
  acompanha o backup e a minimização impede cópia fora do aparelho.
- Logging interceptor sanitizado (não registra bodies/credenciais).
- Exclusão imediata dos dados locais em "Sair da conta".

## Direitos do titular (art. 18 LGPD)

No app: acesso e correção do cadastro são feitos pelo titular junto ao
provedor (a Central é o canal oficial). Localmente, o titular pode:

- **Eliminação** — "Sair da conta" apaga sessão e credenciais na hora.
- **Acesso à informação** — "Privacidade e dados" (Perfil) e link no login
  abrem a política completa offline, sem precisar de rede.
- **Portabilidade/revogação** — via canais do provedor (controlador).

## Transparência de cobrança (CDC / RGC 632/2014)

- Segunda via de fatura disponível no detalhe (PIX/linha digitável via `titulo/{id}/detail/`).
- Linha digitável e código PIX exibidos ao titular sem cobrança adicional.
- Valores, vencimentos e status apresentados como retornados pela API —
  sem dark patterns, botões de recusa em destaque igual ao de aceite.

## Checklist

- [x] Política de privacidade acessível no app (login + perfil), offline
- [x] Minimização: sem trackers/analytics/SDKs de terceiros
- [x] Credenciais cifradas (Keystore) e fora de backup
- [x] Cleartext desabilitado
- [x] Logs sem dados sensíveis
- [x] Exclusão local imediata no logout
- [x] 2ª via / linha digitável / PIX acessíveis (CDC)
- [ ] Texto da política validado pelo jurídico do provedor (template atual
      é genérico — ajustar razão social, CNPJ e contato do encarregado/DPO
      na seção 1 da `PrivacyPolicyScreen`)
- [ ] URL pública da política quando houver hospedagem do provedor
- [ ] Avaliar Registro de Atividades de Tratamento (ROPA) do provedor
