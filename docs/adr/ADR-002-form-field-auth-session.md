# ADR-002 — Sessão por credenciais reenviadas (sem JWT)

## Contexto

A Central do Assinante SGP não tem endpoint de login que emita token: cada
request POST leva `cpfcnpj`+`senha` como campos de formulário. "Login" é o
`POST /api/central/contratos` responder `auth:true`.

## Decisão

Persistir as credenciais **cifradas** (Android Keystore → `CredentialCipher`)
e reenviá-las por request via `SgpFieldMapFactory`. `SessionState` no
`SessionRepository` (DataStore) dirige o roteamento raiz
(Login ↔ área autenticada).

Alternativas rejeitadas:
- Não persistir nada e pedir senha a cada request — UX inviável.
- Guardar em `SharedPreferences` em claro — inseguro.
- Assumir um JWT inexistente — violaria "não inventar contrato da API".

## Consequências

+ Compatível com o contrato real da API.
+ Logout = `clear()` local; a API não tem conceito de revogação.
− Credenciais precisam existir no dispositivo enquanto logado — mitigado por
  cifra + armazenamento privado + `allowBackup` sob revisão.
