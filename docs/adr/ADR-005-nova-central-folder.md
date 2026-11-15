# ADR-005 — Usar a pasta "Central Assinante [NOVA]" da coleção

## Contexto

A coleção SGP contém duas pastas com os mesmos endpoints: "Central Assinante"
e "Central Assinante [NOVA]".

## Decisão

Integrar apenas a versão **[NOVA]** — é a revisão atual (mesmos paths,
payloads e exemplos mais completos: `vencimento_atualizado`,
`valorcorrigido`, `gerarpix`, `paginacao`).

## Consequências

+ Contrato único e atual; DTOs mapeiam os campos novos (data corrigida,
  valor corrigido, flag de PIX sob demanda).
+ A pasta antiga fica documentada como legada — não integrar.
− Divergências futuras entre instalações SGP antigas/novas devem ser tratadas
  com tolerância nos DTOs (`ignoreUnknownKeys`, campos opcionais, `staus`
  já tolerado).
