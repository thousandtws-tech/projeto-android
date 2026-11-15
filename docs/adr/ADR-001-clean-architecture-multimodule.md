# ADR-001 — Clean Architecture em módulos Gradle

## Contexto

App Android que evoluirá comercialmente: precisa de fronteiras físicas que
impeçam acoplamento entre UI, regras de negócio e infraestrutura.

## Decisão

Clean Architecture de 3 camadas materializada em módulos Gradle:
`feature/*` (presentation) → `domain` → `data`/`core:*` (infra).
`domain` e `core:common` são **Kotlin puro** — sem Android, Retrofit ou Hilt
em dependências de produção (Hilt apenas na borda, via `@Inject` nos
construtores, que é `javax.inject` e não quebra a pureza).

## Consequências

+ Dependência cíclica impossível (o Gradle a rejeita).
+ Testes de domínio rodam na JVM sem emulator.
− Mais cerimônia de configuração (12 `build.gradle.kts`).
