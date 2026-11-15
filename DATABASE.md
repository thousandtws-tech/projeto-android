# Persistência local

## Decisão: sem Room em v1

A API SGP é a fonte de verdade para dados financeiros. Cache offline de
faturas/valores criaria risco de exibir dados financeiros desatualizados como
oficiais — por isso Room **não** foi adicionado nesta versão (o catálogo não
contém a dependência de propósito).

Quando houver necessidade real (ex.: lista de contratos offline, extrato de
uso), introduzir Room em um módulo `:core:database` com TTL explícito e
marcação visual de "dados de cache".

## O que é persistido

| Store | Tecnologia | Conteúdo | Cifrado? |
|---|---|---|---|
| `sgp_session` | DataStore Preferences + Keystore | `login`, senha (para re-login), `jwt_token`, nome/CPF-CNPJ do cliente, contratos, `selectedContractId` | **sim** para a senha (AES/GCM via `CredentialCipher`) |
| `sgp_settings` | DataStore Preferences | `base_url_override`, `central_id`, `central_token`, `onboarding_seen` | não (URL pública; `central_token` identifica a Central, não o assinante) |

`SessionManager` serializa a sessão como JSON cifrado; a versão em claro só
existe em memória durante o uso.

## Invalidação

- **Logout** (`SessionManager.clear()`): apaga sessão cifrada.
- **Troca de contrato**: `selectContract(id)` persiste a escolha; a próxima
  leitura de `selectedContract()` a resolve.
- **Override de URL**: `setBaseUrlOverride(url?)`; `null` restaura o
  default do flavor.

## Regras

- Nunca persistir faturas, valores, linhas digitáveis ou códigos PIX.
- Nunca persistir senha fora da cifra do Keystore.
- Dados vindos da API expiram junto com a tela — pull-to-refresh re-busca.
