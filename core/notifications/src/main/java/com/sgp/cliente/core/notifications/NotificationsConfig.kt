package com.sgp.cliente.core.notifications

/**
 * Configuração do módulo, fornecida pelo app (EnvConfig por flavor).
 * [crmApiUrl] vazio/nulo desativa o registro de token no CRM.
 */
interface NotificationsConfig {
    val crmApiUrl: String?

    /**
     * Slug do tenant no CRM multi-tenant — vai no header `X-Tenant`
     * das rotas públicas (`/banners`, `/onboarding`, `/devices`).
     * Cada build white-label conhece o seu (não é segredo). Nulo/vazio
     * = sem header → backend cai no tenant platform (builds antigos e
     * o demo seguem funcionando).
     */
    val crmTenantSlug: String? get() = null
}
