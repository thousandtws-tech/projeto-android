# Segurança

## Modelo de credenciais

A Central App emite **JWT** em `login/` — as credenciais do assinante
(`login`+`senha`) viajam **somente** no login e no re-login silencioso pós-401.
As chamadas autenticadas levam `X-TOKEN` (token da Central descoberta) +
`Authorization: Bearer <jwt>`. Por isso:

- Credenciais persistem **cifradas** — `CredentialCipher` usa Android Keystore
  (AES/GCM, chave não exportável) antes de gravar no DataStore privado do app,
  exclusivamente para re-emissão do JWT expirado.
- JWT e dados de sessão ficam no mesmo DataStore privado; nunca em logs ou
  backups compartilhados.
- `SessionManager.clear()` (logout) remove sessão, JWT e credenciais cifradas.

## O que NÃO entra no APK

- Credenciais de escopo de aplicativo (a Central é descoberta via `list/`).
- Basic Auth administrativo, URLs ou credenciais de exemplo da coleção Postman.
- Qualquer segredo em `BuildConfig`, `local.properties` versionado ou código.

## Rede

- HTTPS por padrão; a tela de configuração valida o esquema (`http://` só é
  aceito para cenários controlados — preferir sempre `https://`).
- `BaseUrlInterceptor` reescreve apenas scheme+host: paths e campos vêm do
  código, nunca do servidor configurado.
- `SanitizedLoggingInterceptor` registra apenas método+path+status — bodies
  (senha, login, tokens, PIX, linha digitável) nunca são logados; logging só
  em builds debug.
- Timeouts curtos (20s conexão / 30s leitura-escrita).

## Dados financeiros

- A API é a única fonte de verdade. Não há cache local de faturas/valores.
- Nenhuma ação local marca fatura como paga — PIX/código de barras são apenas
  copiados para a área de transferência.
- `promessapagamento` exibe `liberado` exatamente como retornado.

## Logging estruturado

- Timber é o backend (apenas `:app`); `SgpApplication` planta `DebugTree` em
  `BuildConfig.DEBUG` e `ReleaseTree` em release — só **WARN/ERROR** chegam ao
  Logcat em produção, e R8 remove os call sites de `v/d/i`.
- Módulos Kotlin-puros e features logam via `AppLog` (`:core:common`), ligado
  ao Timber em `SgpApplication.onCreate` — nenhum módulo fora de `:app`
  importa `timber.*` ou `android.util.Log`.
- Regra absoluta: nunca logar senha, JWT, X-TOKEN, CPF/CNPJ, PIX, linha
  digitável nem dados pessoais. Loga-se apenas tipo de erro
  (`error::class.simpleName`) e transições de estado.

## Firebase Crashlytics (opt-in)

Desativado por padrão — o build não exige conta Firebase. Para ativar:

1. Baixe `google-services.json` do projeto Firebase (Crashlytics habilitado).
2. Coloque em `app/google-services.json` — o arquivo está no `.gitignore`,
   nunca commitado.
3. `app/build.gradle.kts` detecta o arquivo e aplica automaticamente
   `com.google.gms.google-services` + `com.google.firebase.crashlytics` e
   adiciona `firebase-bom`/`firebase-crashlytics` ao classpath.
4. Rebuild: `./gradlew :app:assembleProdRelease`.

Sem o arquivo, os plugins não são aplicados e nenhuma dependência Firebase
entra no APK — build permanece verde.

## Assinatura de release

APK/AAB de release só é assinado quando `keystore.properties` existe na raiz
(gitignored — ver `keystore.properties.template`). Sem ele, `assemble*Release`
gera binário não assinado, mantendo CI/devs funcionando.

Setup:

```bash
# 1. Gerar o keystore (guardar fora do repositório ou na raiz, gitignored)
keytool -genkeypair -v -keystore sgp-cliente-release.jks \
  -alias sgp-cliente -keyalg RSA -keysize 4096 -validity 10950

# 2. Copiar o template e preencher
cp keystore.properties.template keystore.properties
```

`storeFile` aceita caminho absoluto ou relativo à raiz. Senhas ficam apenas
em `keystore.properties` local — nunca em código, `gradle.properties`
versionado ou CI logs.

## Superfícies restantes (hardening futuro)

- `sgp_session` é excluído de cloud-backup e device-transfer
  (`backup_rules.xml`/`data_extraction_rules.xml`); `sgp_settings` (só URL
  e central) permanece — não é dado pessoal.
- Certificate pinning: avaliar quando a base URL de produção for conhecida
  (pinning por instância não é prático com URL configurável).
- Detecção de root/Play Integrity: fora de escopo em v1.

## Como reportar vulnerabilidades

Abrir issue privada ou contatar o mantenedor — não publicar PoC com dados
reais de clientes.
