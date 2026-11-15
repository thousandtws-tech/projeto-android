# Arquitetura

## Visão geral

Clean Architecture de 3 camadas sobre módulos Gradle, MVVM na apresentação,
UDF com StateFlow, DI com Hilt, Kotlin Coroutines em tudo que é assíncrono.

```
┌─────────────────────────────────────────────────────────┐
│ Presentation (feature/*)                                │
│   Composable Screen → @HiltViewModel → UiState(StateFlow)│
├─────────────────────────────────────────────────────────┤
│ Domain (domain — Kotlin puro, sem Android)              │
│   UseCase → Repository (interface) → Model              │
├─────────────────────────────────────────────────────────┤
│ Data (data, core/network, core/datastore)               │
│   RepositoryImpl → RemoteDataSource → Retrofit Service  │
│   SessionManager/SettingsManager → DataStore + Keystore │
└─────────────────────────────────────────────────────────┘
```

Regra de dependência: aponta sempre para dentro. `domain` não conhece Retrofit,
DataStore ou o framework Android. `feature/*` não conhece `data`.

## Módulos

| Módulo | Tipo | Responsabilidade |
|---|---|---|
| `:app` | application | Hilt app, MainActivity, NavHost raiz, navegação adaptativa (`SgpFloatingNavBar` compacto / rail), flavors |
| `:core:common` | Kotlin puro | `AppResult`, `AppError`, `CpfCnpjValidator`, `MoneyFormatter`, `DateFormatter`, `DispatcherProvider` |
| `:core:network` | Android lib | Retrofit/OkHttp wiring, `BaseUrlProvider`/`BaseUrlInterceptor`, logging sanitizado, `safeApiCall`, `ConnectivityObserver` |
| `:core:datastore` | Android lib | `SessionManager` (sessão + JWT + senha cifrada), `SettingsManager` (URL, `central_id`/`central_token`), `CredentialCipher` (Keystore) |
| `:core:designsystem` | Android lib | `SgpTheme` (M3), `Spacing`/`LayoutTokens`, componentes `Sgp*`, `SgpContentFrame`, `gridColumns` |
| `:domain` | Kotlin puro | Models (`Contract`, `Invoice`, `CustomerSession`, `PagedResult`, `PixCharge`, `PaymentPromise`, `SecondCopy`), interfaces de repositório, use cases |
| `:data` | Android lib | DTOs `@Serializable`, mappers `toDomain()`, `CentralAppApiService`, `CentralAppRemoteDataSource`, `CentralAppAuthInterceptor`, `AltchaSolver`, implementações de repositório, `DataModule` |
| `:feature:*` | Android lib | Screens + ViewModels por funcionalidade |

## Decisões chave (ADRs em docs/adr/)

1. **Multi-módulo por camada + feature** — build paralelo, fronteiras de
   dependência impostas pelo Gradle.
2. **JWT + token de Central** — `/api/centralapp/` emite JWT em `login/`
   (com ALTCHA proof-of-work quando habilitado). Chamadas autenticadas levam
   `X-TOKEN` (Central descoberta em `list/`) + `Bearer`; 401 dispara um
   re-login silencioso serializado com as credenciais cifradas no Keystore.
3. **Base URL reescrita por interceptor** — a instância é do provedor, não um
   endpoint fixo. Override persistido > default do flavor; troca em runtime.
4. **`AppResult`/`AppError` tipados** — nada de exceptions vazando para a UI;
   `safeApiCall` mapeia HTTP/IO para `AppError`.
5. **DataStore + Keystore, sem Room** — não há cache offline em v1; a API é a
   fonte de verdade (ver DATABASE.md).
6. **Navegação adaptativa** — `SgpFloatingNavBar` (barra flutuante animada com
   indicador elevado e notch) em compacto, rail via `NavigationSuiteScaffold`
   em médio/expandido; conteúdo centrado com `SgpContentFrame` (max 840dp).
   As abas NÃO usam `saveState`/`restoreState` do Navigation (o
   `backStackMap` cresce sem limite a cada troca — medido ~1 MB heap por
   par de trocas até OOM). Em vez disso, os ViewModels das abas são
   resolvidos contra o `NavBackStackEntry` da rota `MAIN`
   (`LocalViewModelStoreOwner`) — quatro instâncias limitadas que mantêm
   dados carregados e tornam revisita instantânea sem estado de navegação
   acumulado.
7. **Cache de página com TTL + coalescing** — `InvoiceRepositoryImpl` guarda
   as últimas páginas por chave (contrato, filtro, offset, limit) por 60s;
   telas que pedem a mesma fatia dividem o resultado. Um `Mutex` por chave
   faz chamadas concorrentes idênticas compartilharem o fetch em voo.
   `forceRefresh` (pull-to-refresh) furta o cache e o regrava.
8. **Skeleton loading nativo** — `SgpSkeleton.kt` (shimmer sobre blocos que
   espelham o layout real) em vez de spinner genérico; pull-to-refresh não
   sobrepõe o skeleton.

## Estados de tela

Cada ViewModel expõe `StateFlow<UiState>` imutável (`loading`, dados, `error`)
e, quando a ação não deriva de estado, `SharedFlow` de eventos one-shot
(ex.: seleção de contrato). Navegação de sessão é dirigida por
`SessionState` no `SgpApp` — ViewModels nunca emitem eventos de
login/logout (um `emit` suspenso sobre SharedFlow sem coletor chega a
livelockar o event loop da main). Erros chegam como `AppError` e viram
mensagem pt-BR via `AppError.toUserMessage()`.

## Testes

- `core:common` — validadores, formatters, `AppResult` (JUnit puro).
- `domain` — `GetOpenInvoicesUseCase` com fakes + `Clock` fixo.
- `data` — MockWebServer: campos de formulário no fio + parsing dos DTOs; mappers.
- `feature:auth` — `LoginViewModel` com MockK + Turbine + dispatcher de teste.
