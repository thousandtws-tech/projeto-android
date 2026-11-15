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
