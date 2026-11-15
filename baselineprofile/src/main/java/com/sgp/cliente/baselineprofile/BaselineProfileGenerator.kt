package com.sgp.cliente.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

/**
 * Generates the Baseline Profile over the app's critical user journeys:
 * cold start, server setup, login, contract pick, dashboard and tab
 * navigation. Runs against the `dev` variant where the offline demo
 * interceptor makes the flows deterministic (no real provider needed).
 */
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.sgp.cliente",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        ensureLoggedIn()
        navigateTopLevelTabs()
    }
}

private const val TIMEOUT_MS = 10_000L
private const val DEMO_CPF = "52998224725"
private const val DEMO_PASSWORD = "demo123"
private const val DEMO_SERVER = "demo"

private fun MacrobenchmarkScope.ensureLoggedIn() {
    // Restored session lands straight on the dashboard.
    if (device.wait(Until.hasObject(By.text("Início")), 3_000)) return

    device.wait(Until.hasObject(By.desc("Configurar servidor")), TIMEOUT_MS)
    ensureDemoServer()
    login()
    pickContractIfAsked()
    device.wait(Until.hasObject(By.text("Início")), 15_000)
}

private fun MacrobenchmarkScope.ensureDemoServer() {
    device.findObject(By.desc("Configurar servidor"))?.click() ?: return
    val urlField = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 5_000)
    urlField?.text = DEMO_SERVER
    device.findObject(By.text("Salvar"))?.click()
    device.wait(Until.hasObject(By.desc("Configurar servidor")), TIMEOUT_MS)
}

private fun MacrobenchmarkScope.login() {
    val fields = device.findObjects(By.clazz("android.widget.EditText"))
    fields.getOrNull(0)?.text = DEMO_CPF
    fields.getOrNull(1)?.text = DEMO_PASSWORD
    device.findObject(By.text("Entrar"))?.click()
}

private fun MacrobenchmarkScope.pickContractIfAsked() {
    if (device.wait(Until.hasObject(By.textContains("Contrato 101")), 8_000)) {
        device.findObject(By.textContains("Contrato 101"))?.click()
    }
}

private fun MacrobenchmarkScope.navigateTopLevelTabs() {
    for (tab in listOf("Faturas", "Promessa", "Perfil", "Início")) {
        device.findObject(By.text(tab))?.click()
        device.waitForIdle()
    }
}
