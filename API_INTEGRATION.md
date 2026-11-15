# Integração com a API SGP

O app consome **`/api/centralapp/`** — a mesma API usada pela Central do
Assinante web oficial (verificada inspecionando o bundle da Central e
testando contra `demo.sgp.net.br`). A coleção Postman
(`SGP.postman_collection.json`, pasta "Central Assinante [NOVA]") documenta
a API legada `/api/central/`, cujo login (`contratos`) rejeita credenciais
no servidor — por isso a camada de dados usa `centralapp`.

## Base URL

URL pública da instância SGP do provedor (ex.: `https://demo.sgp.net.br`).
Configurável via **Perfil → Servidor do provedor** ou
`EnvConfig.DEFAULT_BASE_URL` no flavor — sem endpoint fixo.

## Autenticação

```
GET  /api/centralapp/list/                     → [{id, nome, token, logo}]
GET  /api/centralapp/config/{id}/              → flags da Central (X-TOKEN: token dela)
GET  /api/centralapp/captcha/challenge?token=  → {algorithm, challenge, salt, signature, maxnumber}
POST /api/centralapp/login/                    → login, password[, altcha] → {status, jwt_token, contrato, nome, cpfcnpj, usuario}
```

- **`X-TOKEN`** — token da Central descoberta em `list/` (persistido em
  `sgp_settings` como `central_id`+`central_token`). Escolha: preferir a
  Central chamada "Central do Assinante", senão a primeira com token.
- **`Authorization: Bearer <jwt>`** — emitido por `login/`; o interceptor
  omite nos endpoints de bootstrap (`list/`, `config`, `captcha`, `login`).
- **ALTCHA** — quando `habilitar_captcha_login` está ativo, o login exige o
  payload do proof-of-work (`SHA-256(salt + n) == challenge`, resolvido em
  `Dispatchers.Default` comparando bytes).
- **401** → um re-login silencioso serializado (mutex) com as credenciais
  cifradas (Keystore) e a chamada é repetida uma vez.
- `modo_autenticacao=1`: campo único aceita CPF, CNPJ **ou** usuário —
  documentos vão como dígitos, usuários passam intocados.

## Endpoints autenticados

Todos `POST` `application/x-www-form-urlencoded` com `contrato` no body
(+ headers acima).

### `clientecontrato/list/` — contratos do cliente
Resposta: array `[{id, descricao, cliente_nome, cliente_cpfcnpj, situacao,
vencimento, valor, servicos: [{id, plano, grupo, valor}]}]`.

### `titulo/list/` — faturas
Campos: `contrato`, `grupo` (**`abertos`|`pagos`|`canceladas`|`todas`** —
atenção: são plurais femininos; `todos` é um filtro reconhecido que retorna
vazio), `pesquisa`, `limit`, `offset`.
Com paginação responde envelope `{"total":N,"result":[…]}`; sem, array nu.
Cada fatura: `id`, `numero_documento`, `situacao` ("Em aberto"/"Paga"/
"Cancelada"/"Vencida"), `situacao_atrasada_dias`, `data_vencimento`
(`dd/MM/yyyy`), `data_pagamento`, `valor`, `valortotal`, `linha_digitavel`,
`codigo_pix`, `link`, `url_cobranca`, `pagamento_pix`, `pix_sob_demanda`,
`emails[]`, `celulares[]`.

### `titulo/{id}/detail/` — detalhe da fatura
Mesmo shape do item da listagem (usado p/ `boletoPath`/checkout).

### `titulo/{id}/pix/` — PIX sob demanda
Resposta: `{status, msg, pix: {…}}` — o copia-e-cola vem sob chaves
variáveis por gateway (`codigo`, `copiaecola`, `emv`, `qrcode`…).

### `titulo/{id}/{tipo}/send/` — envio da via
`tipo` = `email`|`sms`. Resposta `{status, msg}`.

### `promessapagamento/create/` — promessa de pagamento
Campos: `contrato`, `observacao`, `termo`. Resposta `{status, protocolo, msg}`.

### `servico/list` — serviços do contrato
`[{id, plano, login, situacao, tipo, grupo, up, down}]`.

### `extrato/list/` — extrato de tráfego
Campos: `contrato`, `servico`, `mes`, `ano` → `{extrato: {itens, total}}`.

### `rede/status/` — status do serviço
`{status, disponivel, msg}`.

### `info/` — feature flags do contrato
`{titulo_2via_email, titulo_2via_sms, permite_add_pixrec, contrato_status}`.

## Erros

- Transporte → `AppError.NetworkUnavailable`/`Timeout`.
- HTTP 401 → re-login + retry (uma vez); persistindo → `AppError.Unauthorized`.
- `status:0`/`msg` em payload 2xx → `AppError.Api(msg)` do servidor.
- Payload fora do contrato → `AppError.InvalidResponse`.

## Observações verificadas no demo

- `grupo=vencidos` pode responder **HTML com HTTP 200** em contratos sem
  faturas — o app não usa esse grupo; se usar, tratar como resposta vazia.
- PIX indisponível no `demo.sgp.net.br` (gateway não configurado) —
  contrato confirmado, comportamento em produção depende do gateway.
- Respostas podem vir como array nu ou envelopadas (`result`/`itens`) —
  `unwrapArray()` tolera ambas.

## Fora de escopo (v1)

- `centralapp/` extras (`avisos`, `alterarsenha`, `assinaturas`, `cartoes`,
  `ocorrencia/*`) — sem UI em v1.
- `/api/central/chamado/*` — fase de chamados de suporte.
