package com.sgp.cliente.domain.model

/**
 * Identidade visual white-label do tenant — vem do `GET /branding`
 * público do CRM (escopo `X-Tenant` do build). Campos nulos/vazios
 * significam "usar a identidade embutida" — o app nunca quebra sem
 * branding configurado.
 */
data class TenantBranding(
    /** Nome da empresa — a11y da logo e futura personalização de textos. */
    val name: String? = null,
    /** URL absoluta da logo (o repo resolve `/media/…` contra a origem da API). */
    val logoUrl: String? = null,
    /** Cor principal hex `#rrggbb` — botões, destaques, tinta da logo. */
    val primaryColor: String? = null,
    /** Cor de apoio hex `#rrggbb` — elementos secundários/terciários. */
    val accentColor: String? = null,
)
