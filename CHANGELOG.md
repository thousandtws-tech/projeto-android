# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [Unreleased]

### Adicionado
- Nova ação de fatura "Pagar online": `url_cobranca` da API agora chega ao
  domínio como `Invoice.paymentUrl` (página HTML de checkout do provedor)
  e vira CTA no card via `LocalUriHandler` — caminho real de pagamento em
  contas onde o PIX/boleto ainda não foi gerado pelo provedor.

### Alterado
- Login sem ícone de engrenagem: a configuração de servidor sai do header
  e continua acessível em Perfil → Preferências → "Servidor do provedor".
- URL oficial do projeto é `https://demo.sgp.net.br` em **todos** os
  flavors (dev/staging/prod, `DEFAULT_CENTRAL_ID = 3`, token via
  discovery) — antes só o dev apontava pro demo.
- Resposta PIX (`titulo/{id}/pix/`) parseada de forma robusta: `pix`
  aceita objeto (`codigo`, `copiaecola`, `emv`, `payload`, `qrcode`…),
  string EMV direta ou `codigo_pix` no topo; `JsonNull`/valores aninhados
  não vazam como código. `status=0`/vazio exibe a `msg` do provedor
  ("PIX não disponível.") em snackbar — nunca finge código copiado.
- `downloadBoleto` valida o payload antes de renderizar: corpos que não
  começam com `%PDF` (ex.: resposta "500" de boleto ainda não gerado)
  viram erro honesto "O boleto ainda não foi gerado…" em vez de quebrar
  o `PdfRenderer`. `boletoPath` volta a usar só `titulo.link` —
  `pagamento_boleto=false` não indica ausência de PDF (verificado na API).
- Cards de fatura na lista viram cards completos estilo referência: badge
  de status, botão outlined "Ver fatura", blocos Vencimento/Valor/Nº da
  fatura, e **ações de pagamento inline** — Copiar código (linha
  digitável), Copiar PIX e Abrir boleto (PDF) — sem abrir a tela de
  detalhes; cada ação só aparece quando a API traz dado real pra ela.
- Cards de fatura seguem o padrão dos apps de provedores: superfície
  cinza (`surfaceVariant`, mesmo tom do `SgpCard`) em todos os estados —
  atraso passa a ser sinalizado só pela pill "Vencida · Prioridade"
  (rose, compacta) em vez do card inteiro em `errorContainer`. O card de
  atenção "Fatura em atraso" também fica neutro: badge
  `SgpStatusBadge` OVERDUE + `Button` padrão M3 "Pagar agora" (cobalt),
  substituindo o pill custom vermelho.
- "Ver fatura" nos cards troca `OutlinedButton` por `Button` preenchido
  (azul cobalt + texto branco) — mesmo estilo do botão "Entrar" do login.
- "Copiar PIX" nos cards funciona de verdade: com `codigo_pix` inline na
  resposta da lista copia na hora; sem código mas com `pix_sob_demanda`,
  chama `GeneratePixUseCase` → `POST titulo/{id}/pix/` com spinner no
  card, copia o código gerado e mergea no estado (próximo toque copia
  card, copia o código gerado e mergea no estado (próximo toque copia
  direto); falha vira snackbar de erro. Legenda honesta: "Pague agora" /
  "Gerar agora" / "No boleto" (provider embute o PIX no boleto — toque
  abre o PDF) / "Indisponível". Confirmação de cópia via snackbar.
- Boleto abre dentro do app: `BoletoViewerDialog` baixa o PDF pelo
  cliente HTTP da app (`CentralAppApiService.downloadFile` com `@Url`,
  link pré-assinado do `titulo.link`) e renderiza as páginas com
  `PdfRenderer` — sem browser externo nem download manual.
  `GetBoletoPdfUseCase`/`InvoiceRepository.getBoletoPdf` com testes; modo
  demo responde `*.pdf` com um PDF mínimo renderizável.
- `Invoice.boletoPath` passa a chegar como URL absoluta (resolvida contra
  a base configurada no `InvoiceRepositoryImpl`) — a UI só abre, sem
  conhecer o host.
- Detalhes da fatura agora abrem em `ModalBottomSheet` (M3) sobre a tela
  atual em vez de uma rota dedicada: drag handle nativo, dismiss por swipe
  para baixo, gesto Back ou toque no scrim; `BillDetailsViewModel` passa a
  aceitar `show(invoiceId)` (reset + reload) para reutilização entre
  faturas; `Routes.BILL_DETAILS` removida.
- A sheet de detalhes abre na altura do conteúdo (lista permanece visível
  esmaecida acima): `LazyColumn` interna substituída por `Column` com
  `verticalScroll`, estados de loading/erro com altura fixa de 420dp.

### Corrigido
- "Abrir boleto" não aparece mais em faturas sem boleto: o mapper passa a
  respeitar `pagamento_boleto` — quando `false`, `boletoPath` fica nulo
  (o `link` da API responde um corpo `500` nesses títulos, e
  `url_cobranca` é página HTML de checkout, não PDF). Conta Pedro
  (141.046.132-74): cards mostram só "Copiar PIX / Gerar agora" em vez de
  abrir o viewer e cair em "Resposta inesperada do servidor".
- Filtros de fatura (Em aberto/Pagas/Canceladas) voltam a responder
  corretamente: a API `titulo/list/` não honra `grupo=canceladas` nem o
  campo `status` (caem num fallback sem filtro), e `grupo=abertos` cobre
  só o subconjunto "pagável agora" — então a listagem passa a paginar o
  stream `grupo=todas` e filtrar por `situacao` no cliente
  (`PagedResult.consumed` preserva a posição real no stream); aba "Em
  aberto" mostrava só 2 das 7 faturas da conta e "Canceladas" listava
  tudo.

### Adicionado
- Ações de pagamento do card de fatura são deduplicadas: só aparecem
  quando a API traz dado real para elas — "Copiar PIX" só quando um
  código pode ser copiado (inline ou sob demanda), sem o modo
  "No boleto" que duplicava "Abrir boleto"; ações sem dado deixam de
  ocupar espaço, e fatura sem nenhum caminho de pagamento mostra a
  legenda "Pagamento indisponível".
- Seletor de contratos filtra por boleto em aberto: cada contrato é
  sondado em paralelo via `GetContractOpenBillCountsUseCase`
  (`titulo/list` grupo=abertos, limit=1) e só os com boleto
  aberto/gerado aparecem, com legenda "N boletos em aberto"; sondagem
  falha não esconde o contrato (fail-open), e "Ver todos os contratos
  (N ocultos)" restaura suspensos/cancelados.
- Card de resumo na tela de Faturas (`OpenSummaryCard`): quantidade de
  faturas em aberto, valor total em aberto e vencimento mais próximo —
  visível apenas nos filtros Todas/Em aberto quando há faturas abertas;
  `BillsUiState.openSummary(today)` com testes.
- Modernização da tela de Faturas: card de atenção "Fatura em atraso"
  (errorContainer + pill "Pagar agora") quando há vencida nos filtros
  Todas/Em aberto; headers de competência com contagem e subtotal real;
  linhas de fatura com ícone tonal, "Paga em…" para pagas, contagem de
  dias para abertas e chevron de affordance; rodapé "Você viu todas as
  faturas" ao fim da paginação.
- `DateFormatter.dueWithRelative` (core:common) — "Vence em X · em N
  dias"/"Venceu em X · há N dias", compartilhado entre Dashboard e
  Faturas, com testes.
- "Minhas faturas" do Dashboard migrada para `HorizontalUncontainedCarousel`
  (M3 1.4.0): card hero full-color da fatura atual (status pill translúcido
  + ação "Pagar agora" em pill branca), cards tonais das próximas faturas
  com `maskClip`, e header com ação "Ver todas".
- Hero do Dashboard refinado ao padrão da referência: container arredondado
  com margens, plano exibido direto sobre o fundo da marca (sem card
  interno), círculos decorativos vazando no canto superior direito, pill de
  status tom-sobre-tom (`BrandDeep` novo token público) + pill branca
  "Trocar Contrato"; cards de ação rápida com borda sutil e ícone direto.
- Redesenho do Dashboard inspirado na referência do provedor: hero colorido
  com "Olá, {nome}." + "Sair", card "Meu plano" (plano, vencimento, pill de
  status online/offline via `rede/status`, "Trocar Contrato" quando há
  múltiplos), ações rápidas (Pagar com PIX / Segunda via / Meus dados),
  card destacado "Fatura atual" e CTAs "Histórico de pagamentos" /
  "Desbloqueio de confiança".
- Seção "Pagar com PIX" no detalhe da fatura com instruções e campo de
  código em borda tracejada (`Modifier.dashedBorder`).
- Modernização Material 3 (m3 1.4.0): filtros de fatura migrados de
  `FilterChip` para `SingleChoiceSegmentedButtonRow` conectada;
  `SgpTooltipButton` (TooltipBox + PlainTooltip) aplicado a botões
  somente-ícone (voltar, copiar, mostrar senha, engrenagem do login);
  `enableOnBackInvokedCallback` no manifest ativa o predictive back do
  Android 13+.

### Corrigido
- Integração real `/api/centralapp/` validada ponta a ponta em
  `demo.sgp.net.br` (descoberta → ALTCHA → login JWT → faturas):
  - `AltchaSolver` comparava hex formatando cada candidato (~40s de PoW no
    aparelho); agora decodifica o challenge uma vez e compara bytes —
    resolução abaixo de 1s.
  - `InvoiceGroup.ALL` enviava `grupo=todos`, um filtro reconhecido que
    retorna vazio; o valor real do servidor é `todas`.
  - Status "cancelada" mapeado para `grupo=canceladas` (antes caía em
    `todas`); filtro "Canceladas" da lista agora filtra de verdade.
  - `titulo/list/` responde envelope `{"total":N,"result":[…]}` quando
    paginado — `total` real passa a alimentar `PagedResult` em vez da
    estimativa por preenchimento de página.
- Crash `IllegalArgumentException: Key "header_março/2025" was already
  used` na lista de faturas: o agrupamento era por data exata mas a chave
  do LazyColumn usava a competência — duas faturas no mesmo mês colidiam.
  `BillsUiState.grouped` agora agrupa por competência (label do mês).
- `DemoModeInterceptor`: `grupo=pagos` não inclui mais fatura cancelada;
  adicionado grupo `canceladas` espelhando a API real.
- Documentação sincronizada com `/api/centralapp/`: `API_INTEGRATION.md`
  reescrito (descoberta, ALTCHA, JWT, contratos de endpoint verificados),
  `README.md`, `ARCHITECTURE.md`, `SECURITY.md` (modelo JWT + re-login),
  `DATABASE.md` (chaves `central_id`/`central_token`), `ENVIRONMENTS.md`
  (contas de teste CPF/CNPJ reais), `LGPD.md` e KDocs do domínio.

### Adicionado
- Identidade azul+branco do Figma em todo o app: paleta `SgpTheme` migrada
  para o cobalt `marca/blue #2458F4` (`Brand40`), `primaryContainer` =
  `marca/tint #E9EEFF`, `onPrimaryContainer` = `marca/ink #111B35`;
  secondary/tertiary retonalizadas na mesma família; dark scheme com tons
  espelhados. Badge "Em aberto" (`StatusOpen`) acompanha a marca.
- Tela Perfil redesenhada conforme Figma "Minha conta": headline "Do seu
  jeito.", card do titular em `primaryContainer`, opções em cards com borda
  `outlineVariant` (ícone em box tint + chevron), bottom sheet "Dados
  pessoais" e text button "Sair da conta".
- Nova `PreferencesScreen` (Figma "Preferências"): switches "Avisos de
  fatura" e "Atualizações de chamados" persistidos em `sgp_settings`
  (`notify_invoice_alerts`/`notify_ticket_updates`, default ativado),
  "Salvar preferências" e atalho "Servidor do provedor".
- `DocumentFormatter` em `core:common` — exibe CPF/CNPJ mascarados na UI.
- `:feature:onboarding` — telas de primeira execução (3 páginas swipeable)
  conforme o Figma "Onboarding / Conexão · Pagamentos · Atendimento":
  `HorizontalPager`, card de ilustração 284dp/radius 28, indicador de
  progresso (pill 28×6 ativa), CTA "Continuar" → "Acessar minha conta",
  "Pular" e "Já sou assinante". Persiste `onboarding_seen` em
  `sgp_settings` (exibido uma única vez; logout não reexibe). Marca no
  cabeçalho usa `app_name` (white-label). Ilustrações exportadas do Figma
  como PNG RGBA em `drawable-nodpi`. Validado no A55: 3 páginas, swipe,
  CTAs e pulo direto pro login no restart.
- Testes de regressão dos bugs expostos pela API real: apiValues de
  `InvoiceGroup` (`todas`/`canceladas`), status 3 → `CANCELLED`, `total`
  do envelope propagado a `PagedResult`, e `BillsUiStateTest` cobrindo
  agrupamento por competência (duas faturas no mesmo mês → uma chave).
- `SgpFloatingNavBar` (`:core:designsystem`) — barra de navegação flutuante
  animada no estilo "Modern Floating Navbar": card branco arredondado,
  indicador circular elevado com notch côncavo na borda superior (curvas
  Bézier fiéis ao vetor do Figma), transição por spring entre destinos.
  Usada em janelas compactas; rail adaptativo mantido em médio/expandido.
- Modo demo offline no flavor `dev` (ver ENVIRONMENTS.md).
- Sistema de skeleton loading em `:core:designsystem` (`SgpSkeleton.kt`) —
  equivalente Compose-nativo ao padrão SkeletonLayout: `Modifier.sgpSkeleton`
  com shimmer animado + blocos prontos (`SgpSkeletonDashboard`,
  `SgpSkeletonInvoiceList`, `SgpSkeletonBillDetails`, `SgpSkeletonRows`).
  Aplicado nos estados de carregamento de Dashboard, Faturas (lista e
  paginação), Detalhes e Promessas.
- Cache de páginas com TTL (60s) e coalescing por chave em
  `InvoiceRepositoryImpl` — telas que pedem a mesma fatia (Dashboard ↔
  Promessas) dividem uma única requisição; chamadas concorrentes idênticas
  compartilham o fetch em voo via `Mutex` por chave.
- `forceRefresh` propagado por `InvoiceRepository`, `GetInvoicesUseCase` e
  `GetOpenInvoicesUseCase` — pull-to-refresh força rede, entrada na aba
  consome cache.
- Módulo `:baselineprofile` (`androidx.baselineprofile` + generator via
  uiautomator) — jornada startup/login/navegação para AOT em release;
  `generateDevReleaseBaselineProfile` registrada.
- Conformidade legal (LGPD/Marco Civil/CDC/RGC 632): `PrivacyPolicyScreen`
  offline acessível no login e no Perfil; sessão excluída de backup em
  nuvem e device-to-device; `usesCleartextTraffic="false"`; inventário de
  dados e checklist em `LGPD.md`.
- `imePadding()` em Login e Configuração de Servidor — teclado empurra o
  formulário acima do IME (edge-to-edge exige consumir `WindowInsets.ime`).
- `SgpFloatingNavBar` compactada (56dp/44dp) com notch escalonado via
  `NOTCH_SCALE`; transições de navegação explícitas — crossfade curto nas
  abas (200ms + scale 0.98) e fade + slide leve (8%) nas telas raiz,
  substituindo o fade padrão de 700ms do Navigation Compose.
- Consumo da API real: `EnvConfig` por flavor passa a conter
  `DEFAULT_BASE_URL` + `DEFAULT_APP_NAME`/`DEFAULT_APP_TOKEN` (dev aponta
  para `demo.sgp.net.br`); seed das credenciais do app no 1º boot via
  `MainViewModel`; tela "Servidor do provedor" ganhou campos Nome do app e
  Token para ajuste em runtime sem rebuild.

### Alterado
- `BillsUiState.grouped` memoizado via `lazy` — agrupamento/sort por
  competência só recalcula quando o estado muda, não por recomposição.
- Indicador de pull-to-refresh não aparece sobre o skeleton (gated por
  `refreshing && !loading` em Dashboard e Faturas).
- Troca de abas sem `saveState`/`restoreState` — o `backStackMap` do
  Navigation acumulava estados salvos sem limite (~1 MB heap por par de
  trocas). ViewModels das abas agora são escopados ao entry `MAIN`
  (`LocalViewModelStoreOwner`): estado/dados sobrevivem às trocas de
  forma limitada (4 instâncias), heap estável medido em 68+ trocas.
- `ProfileViewModel.logout` e `LoginViewModel` sem `SharedFlow` de
  eventos — os callbacks `onLoggedOut`/`onLoggedIn` eram no-ops (a
  navegação já é dirigida pelo observer de `SessionState` no `SgpApp`)
  e `emit` em buffer cheio suspende na main thread.
- Desugaring (`coreLibraryDesugaring`) habilitado em `:data`,
  `:feature:bills`, `:feature:billdetails` e `:feature:dashboard` —
  uso de `java.time` com `minSdk 24` agora passa no lint por módulo.

### Corrigido
- **OOM + ANR no logout** (crash fatal no dispositivo): colisão de nome
  entre `private val logout: LogoutUseCase` e `fun logout()` — Kotlin
  resolvia `logout()` para a função membro, causando recursão infinita
  (`launch` → lambda → `logout()` → `launch`...), milhares de coroutines
  na fila unconfined da main (~240 MB em ~2 s → SIGABRT). Renomeada a
  propriedade para `logoutUseCase`.

## [0.1.0] — 2026-03-02

### Adicionado
- Projeto multi-módulo do zero (migração do template Views/Fragments para
  Compose-only, pacote `com.sgp.cliente`).
- `:core:common` — `AppResult`/`AppError`, validador CPF/CNPJ, formatters pt-BR.
- `:core:network` — Retrofit/OkHttp, `BaseUrlProvider`+interceptor com override
  em runtime, logging sanitizado, `safeApiCall`.
- `:core:datastore` — sessão cifrada (Keystore AES/GCM) + settings.
- `:core:designsystem` — tema M3, tokens, componentes, helpers adaptativos.
- `:domain` — models, contratos de repositório, use cases de auth/contrato/
  fatura/pagamento.
- `:data` — DTOs, mappers, `SgpApiService` (8 endpoints da Central [NOVA]),
  repositórios.
- Features: login, seleção de contrato, dashboard, faturas (filtro +
  paginação + agrupamento por competência), detalhes da fatura (linha
  digitável, PIX inline/sob demanda, 2ª via por e-mail, promessa de
  pagamento), promessas, perfil (troca de contrato, servidor, logout).
- `:app` — NavigationSuiteScaffold (barra/rail adaptativo), NavHost raiz com
  roteamento por sessão, flavors dev/staging/prod, edge-to-edge.
- Testes: validadores, formatters, `AppResult`, `GetOpenInvoicesUseCase`,
  `LoginViewModel`, mappers e contrato de fio via MockWebServer.
- Documentação completa + ADRs + workflow de CI.

### Limitações conhecidas
- `URA promessapagamento/list` fora de escopo (requer credencial administrativa).
- Endpoints `verificaacesso`/`extratouso`/`checkout-cartao` integrados na
  camada de dados, ainda sem UI.
- Sem cache offline (decisão — ver DATABASE.md).
