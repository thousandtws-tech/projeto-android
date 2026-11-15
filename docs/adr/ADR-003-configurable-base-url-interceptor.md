# ADR-003 — Base URL configurável via interceptor

## Contexto

Cada provedor roda sua própria instância SGP. A coleção usa `{{url}}` sem
valor padrão — não existe endpoint fixo para hardcodar. A URL pode precisar
mudar em runtime (provedor troca de domínio, white-label).

## Decisão

Retrofit aponta para um placeholder; `BaseUrlInterceptor` reescreve
scheme+host de cada request com o valor do `BaseUrlProvider`, alimentado por:

1. `SettingsRepository.baseUrlOverride` (DataStore — tela "Servidor do
   provedor");
2. `EnvConfig.DEFAULT_BASE_URL` do flavor.

`MainViewModel` sincroniza os dois → mudança vale na hora, sem rebuild nem
reinício.

Alternativas rejeitadas: `buildConfigField` por flavor (só configura em
build-time) e múltiplos Retrofits (memória + complexidade).

## Consequências

+ Uma só Retrofit/OkHttp para todas as instâncias.
+ O mesmo APK atende provedores diferentes (cenário white-label).
− Interceptor precisa ser cuidadoso para reescrever só scheme+host
  (path/query vêm do código).
