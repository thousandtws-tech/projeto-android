# Contribuindo

## Setup

1. JDK 25 (o Gradle fixa `org.gradle.java.home` — ver `gradle.properties`).
2. Android SDK 37 em `local.properties` (`sdk.dir`).
3. `./gradlew :app:assembleDevDebug`.

## Convenções

- **Kotlin** em tudo; Java só se inevitável.
- **Kotlin puro** em `:core:common` e `:domain` — sem imports `android.*`
  ou de framework nesses módulos.
- Camadas: UI → UseCase → Repository(interface) → Remote/Local. UI nunca
  fala com Retrofit, DataStore ou repositório diretamente.
- Estado de tela em `StateFlow` imutável; eventos one-shot em `SharedFlow`.
- Erros via `AppError`/`AppResult` — nunca exceptions cruzando camadas.
- Design system: usar componentes `Sgp*` e tokens `Spacing`/`LayoutTokens`;
  nada de cor ou espaçamento hardcoded.
- Sem comentários novos salvo código genuinamente não-óbvio (regra do projeto).

## Antes de abrir PR

```bash
./gradlew :app:assembleDevDebug \
  :core:common:test :domain:test :data:testDebugUnitTest \
  :feature:auth:testDebugUnitTest :app:lintDevDebug
```

- Novo endpoint? Atualize `CentralAppApiService`, DTO, mapper, repositório, use case
  e `API_INTEGRATION.md` — com teste MockWebServer do contrato real.
- Nunca commitar credenciais, tokens ou URLs de produção sensíveis.
- Decisão arquitetural nova → ADR em `docs/adr/`.

## Estilo de commit

Mensagem curta em pt-BR descrevendo o "porquê" (`dominio: filtra faturas
vencidas pelo vencimento_atualizado`). Escopo opcional.
