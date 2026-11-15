# Ambientes e Builds

## Flavors (`env` dimension)

| Flavor | Uso | `EnvConfig.DEFAULT_BASE_URL` |
|---|---|---|
| `dev` | Desenvolvimento diário | `https://demo.sgp.net.br` (demo público do SGP) |
| `staging` | Validação junto ao provedor | preencher com a instância de staging |
| `prod` | Distribuição | URL pública da instância SGP do provedor |

`EnvConfig` vive em `app/src/{flavor}/java/com/sgp/cliente/EnvConfig.kt`.
Dados do **assinante** (CPF/senha) nunca entram aqui — e nenhuma
credencial de escopo de aplicativo é embutida no APK: a Central é
descoberta em runtime via `GET /api/centralapp/list/`.

## Autenticação (Central App API)

O app consome `/api/centralapp/` — a mesma API da Central do Assinante
web oficial. Fluxo:

```
GET  /api/centralapp/list/                → centrais expostas pelo servidor
SettingsRepository.centralId/centralToken (DataStore, tela "Servidor do provedor")
        ↓
GET  /api/centralapp/captcha/challenge    → ALTCHA proof-of-work (quando habilitado)
POST /api/centralapp/login/               → login (CPF, CNPJ ou usuário) + senha
        ↓ jwt_token
X-TOKEN: <central token> + Authorization: Bearer <jwt>  em toda chamada
```

- O `X-TOKEN` é o token **da Central** descoberta — não existe credencial
  de app fixa no código.
- Um 401 dispara **um** re-login silencioso serializado (mutex) com as
  credenciais criptografadas (Keystore) e repete a chamada uma vez.
- `modo_autenticacao=1` (campo combinado): o mesmo input aceita CPF,
  CNPJ ou usuário — documentos são normalizados para dígitos.

## Contas de teste (demo.sgp.net.br)

| Login | Senha | Cliente | Contrato | Observação |
|---|---|---|---|---|
| `141.046.132-74` (CPF) | `123` | PEDRO HENRIQUE ANDRÉ LEVI GONÇALVES | 1539 (2mgas) | sem faturas |
| `07.387.503/0001-00` (CNPJ) | `123` | INTERJATO SERVIÇOS DE TELECOM LTDA | 1569 (100 mega) | sem faturas |
| `6zta35f0` (usuário) | `6zta35f0` | PEDRO | 566 (1GB Palacio Dedicado) | 13 faturas canceladas |
| `862.779.465-01` (CPF) | `86277946501` | ROBERTA SILVA SANTOS | 2448 | **7 faturas abertas** (1 atrasada R$625 + abertas até R$750), email+celular, `url_cobranca` |
| `651.278.070-17` (CPF) | `65127807017` | JORGE WESLEY OSODRAC | 2152 | 1 atrasada R$47.96 (7 no total), email |
| `005.800.521-84` (CPF) | `00580052184` | JOAO PE DE FEIJAO | 1445 | 1 atrasada R$100 |
| `548.266.508-92` (CPF) | `54826650892` | JENIFER SILVA DA MOTA | 2420 | 14 canceladas (com `url_cobranca`) |
| `383.245.376-86` (CPF) | `38324537686` | ARTHUR MARTIN MIGUEL BARROS | 2039 | 2 pagas |

A API aceita os documentos com ou sem máscara. Padrão de senha do servidor
demo: **dígitos do próprio CPF/CNPJ** (as contas `…/123` foram criadas pelo
Dev). Use ROBERTA (contrato 2448) para testar faturas abertas/vencidas,
detalhe e checkout; `titulo/{id}/pix/` responde "PIX não disponível" no
demo (gateway não configurado) e `promessapagamento` recusa o contrato —
ambas limitações do servidor, não do app. `situacao` observados reais:
`Aberta`, `Atrasada`, `Cancelada`, `Paga` — todos caem corretamente no
mapeador (`Aberta`/`Atrasada` → aberto, com `isOverdue` pela data).

## Build types

- `debug` — símbolos, logging HTTP sanitizado, sem otimização.
- `release` — otimização AGP (`packageScope` androidx/kotlin/com.sgp),
  `proguard-rules.pro`. Assinatura deve ser configurada fora do VCS
  (keystore + `signingConfigs` via propriedades ou CI).

## Resolução da base URL (runtime)

```
SettingsRepository.baseUrlOverride (DataStore, tela "Servidor do provedor")
        ↓ vazio
EnvConfig.DEFAULT_BASE_URL (flavor)
        ↓
BaseUrlProvider.update() → BaseUrlInterceptor reescreve scheme+host
```

`MainViewModel` mantém o provider sincronizado: mudar o servidor na tela
tem efeito imediato, sem rebuild — e dispara a descoberta da Central.

## Modo demo (somente `dev`)

O flavor `dev` inclui `DemoModeInterceptor`
(`app/src/dev/java/com/sgp/cliente/demo/`): quando o host configurado é
`demo`, toda chamada à API responde localmente com payloads fiéis ao
contrato `/api/centralapp/` — nenhum tráfego sai do aparelho.

Uso: **Login → engrenagem → digitar `demo` → Salvar** (o scheme `https://`
é adicionado automaticamente). Qualquer identificador e senha autenticam
— o interceptor ignora o body.

Coberto pelo mock: contratos, títulos (grupos `abertos`/`pagos`/
`canceladas`/`todas`), 2ª via, PIX, envio de via, promessa de pagamento,
verificação de acesso, extrato de uso e checkout de cartão. O interceptor
é contribuído via multibinding Dagger (`ExtraInterceptorsModule` em
`:core:network`); staging/prod não recebem contribuições e o código nem é
compilado nesses flavors.

## Variantes geradas

`./gradlew :app:assemble{Dev,Staging,Prod}{Debug,Release}`
ex.: `assembleDevDebug`, `bundleProdRelease`.

## CI/CD

Workflow em `.github/workflows/android-ci.yml`: build devDebug + testes +
lint. Releases/AAB e publicação exigem aprovação manual — nunca automático.
