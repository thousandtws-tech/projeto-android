# SGP Cliente — Android

Aplicativo Android nativo da **Central do Assinante** para clientes de provedores
de internet que usam o sistema **SGP**. Interface 100% Jetpack Compose + Material 3,
arquitetura multi-módulo Clean Architecture + MVVM.

## Funcionalidades

| Feature | Endpoint SGP (`/api/centralapp/`) | Status |
|---|---|---|
| Login (CPF, CNPJ ou usuário + senha) | `login/` + `captcha/challenge` (ALTCHA) | Implementado |
| Descoberta da Central | `list/` + `config/{id}/` | Implementado |
| Seleção de contrato | `clientecontrato/list/` | Implementado |
| Dashboard (faturas em aberto/vencidas) | `titulo/list/` | Implementado |
| Listagem de faturas com filtros e paginação | `titulo/list/` | Implementado |
| Detalhes da fatura (valor, vencimento, linha digitável) | `titulo/{id}/detail/` | Implementado |
| PIX copia-e-cola (inline ou sob demanda) | `titulo/{id}/pix/` | Implementado |
| Envio de 2ª via por e-mail/SMS | `titulo/{id}/{email|sms}/send/` | Implementado |
| Promessa de pagamento ("Liberação por Confiança") | `promessapagamento/create/` | Implementado |
| Configuração do servidor do provedor | — (persistida local) | Implementado |
| Status do serviço / extrato de uso / cartão | `rede/status/`, `extrato/list/`, `info/` | Camada de dados pronta, sem UI |

## Arquitetura

```
app                  — ponto de entrada, navegação, flavors (dev/staging/prod)
core/common          — AppResult, AppError, validadores, formatters (Kotlin puro)
core/network         — Retrofit/OkHttp, BaseUrlProvider, interceptors, safeApiCall
core/datastore       — DataStore (sessão/settings) + criptografia via Keystore
core/designsystem    — tema M3, spacing, componentes, helpers adaptativos
domain               — models, interfaces de repositório, use cases (Kotlin puro)
data                 — DTOs, mappers, CentralAppApiService,
                       CentralAppRemoteDataSource, repos
feature/*            — auth, contracts, dashboard, bills, billdetails,
                       paymentpromises, profile (Compose + ViewModel + UDF)
```

Fluxo: `Screen → ViewModel → UseCase → Repository (interface) → RemoteDataSource → CentralAppApiService`.
Detalhes em [ARCHITECTURE.md](ARCHITECTURE.md).

## Como compilar

Pré-requisitos: JDK 21 (Temurin; JDK 25+ quebra o detekt — ver CONTRIBUTING.md),
Android SDK 37 (`sdk.dir` em `local.properties`).

```bash
# APK de desenvolvimento
./gradlew :app:assembleDevDebug

# Testes unitários
./gradlew :core:common:test :domain:test :data:testDebugUnitTest :feature:auth:testDebugUnitTest

# Lint
./gradlew :app:lintDevDebug

# Qualidade (ktlint + detekt + cobertura Kover agregada)
./gradlew ktlintCheck detekt koverHtmlReport
```

Saída: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`

## Configuração do servidor

A instância SGP é específica de cada provedor. A URL base é resolvida em:

1. Override persistido — tela **Login → engrenagem** ou **Perfil → Servidor do provedor**;
2. `EnvConfig.DEFAULT_BASE_URL` do flavor (`app/src/{dev,staging,prod}/java/.../EnvConfig.kt`).

Toda requisição passa pelo `BaseUrlInterceptor`, que reescreve o host — trocar
de servidor não exige rebuild nem reinício.

## Segurança

- Sessão JWT + credenciais cifradas (Android Keystore → AES/GCM) em DataStore privado.
- Nenhuma credencial de app embutida no APK — a Central é descoberta via `list/`.
- Logs HTTP sanitizados (método+path+status, nunca bodies).
- Ver [SECURITY.md](SECURITY.md).

## Documentação

- [ARCHITECTURE.md](ARCHITECTURE.md) — módulos, camadas, decisões
- [API_INTEGRATION.md](API_INTEGRATION.md) — contratos dos endpoints usados
- [SECURITY.md](SECURITY.md) — modelo de segurança
- [ENVIRONMENTS.md](ENVIRONMENTS.md) — flavors, variants, URL base
- [DATABASE.md](DATABASE.md) — persistência local (DataStore; sem Room em v1)
- [CONTRIBUTING.md](CONTRIBUTING.md) — convenções e fluxo de contribuição
- [CHANGELOG.md](CHANGELOG.md) — histórico de versões
- [docs/adr/](docs/adr/) — ADRs das decisões arquiteturais
