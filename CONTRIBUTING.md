# Contribuindo

## Setup

1. JDK 21 recomendado (Temurin). JDK 25+ funciona para build/testes, mas
   quebra o detekt (ver "JDK conhecido" em Qualidade). Se a máquina aponta
   para outra JDK via `org.gradle.java.home` no `~/.gradle/gradle.properties`
   global, sobrescreva por comando: `-Dorg.gradle.java.home=<jdk-21>`.
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
- Logging: use `AppLog` (`core.common.logging`) em qualquer módulo —
  `AppLog.d/i/w/e(tag, msg, throwable?)`. Timber só existe em `:app`
  (`:core:common` e `:domain` são JVM puros; Timber é AAR e não resolve
  neles). Tag por subsistema (`app.lifecycle`, `feature.auth`, ...); nunca
  logar senha, token, CPF/CNPJ, PIX ou linha digitável.
- Crashlytics é opt-in: `app/google-services.json` (gitignored) ativa os
  plugins gms/crashlytics e as dependências Firebase automaticamente.
- Assinatura release: `keystore.properties` na raiz (gitignored, template em
  `keystore.properties.template`) ativa o `signingConfig`; sem ele o release
  sai não assinado.
- Sem comentários novos salvo código genuinamente não-óbvio (regra do projeto).

## Qualidade

Ferramentas aplicadas a todos os módulos pelo `build.gradle.kts` raiz:

```bash
./gradlew ktlintCheck      # ktlint — regras em .editorconfig
./gradlew ktlintFormat     # autocorrige a maioria das violações
./gradlew detekt           # detekt — regras em config/detekt/detekt.yml
./gradlew detektBaseline   # regenera baselines (ver abaixo)
./gradlew koverHtmlReport  # cobertura agregada → build/reports/kover/
./gradlew koverXmlReport   # mesmo relatório em XML (usado pelo CI)
```

- **detekt**: as dívidas atuais vivem em `config/detekt/baseline/<modulo>.xml`
  (o plugin deriva `<modulo>-<variante>.xml` por variante Android — por isso um
  baseline único não é viável). Código novo com finding quebra o build; ao
  corrigir dívida, rode `./gradlew detektBaseline` e commite o diff do XML.
- **ktlint**: layout rules do `ktlint_official` que conflitavam com o estilo
  legado estão desativadas no `.editorconfig` — reative progressivamente.
- **Strings pt-BR inline** nas telas são decisão consciente para a v1;
  internacionalização é backlog documentado — não criar regras/findings contra.
- **Pre-commit hook** (opcional): `cp scripts/pre-commit.sh .git/hooks/pre-commit`
  e `chmod +x` (roda `ktlintCheck detekt`). Alternativa: `git config
  core.hooksPath <dir>` apontando para um diretório com o hook nomeado
  `pre-commit`.
- **JDK conhecido**: o detekt 1.23.8 roda o frontend Kotlin dentro da JVM do
  Gradle e falha em JDKs novas demais ou cujo `java.version` tem 4 partes
  (ex.: Microsoft OpenJDK `25.0.4.1` → `IllegalArgumentException`). O CI roda
  em Temurin 21 por isso. Localmente, se `org.gradle.java.home` apontar para
  uma JDK incompatível, sobrescreva por comando:
  `./gradlew "-Dorg.gradle.java.home=<caminho-jdk-21>" detekt` —
  ou ajuste o `~/.gradle/gradle.properties` global.

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
