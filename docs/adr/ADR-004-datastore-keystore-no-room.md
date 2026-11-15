# ADR-004 — DataStore + Keystore, sem Room

## Contexto

Requisito: sessão segura + preferências. Tentação: Room para "cache offline".

## Decisão

- `SessionManager` (DataStore + JSON cifrado por `CredentialCipher`/Keystore)
  para sessão e contratos.
- `SettingsManager` (DataStore) para override de URL e credenciais de app.
- **Nenhuma persistência de dados financeiros** — faturas, valores, PIX e
  linhas digitáveis existem só em memória; a API é a fonte de verdade.

Motivo: exibir boleto/valor cacheado como oficial é risco financeiro real.
Quando cache offline for requisito, introduzir Room com TTL + marcação visual.

## Consequências

+ Zero risco de dado financeiro defasado.
+ Superfície de ataque pequena (pouco dado sensível em disco, e o que há é
  cifrado).
− Sem offline-first: sem rede, não há consulta — aceito para v1.
