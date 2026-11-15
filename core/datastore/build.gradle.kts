plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.sgp.cliente.core.datastore"
    compileSdk { version = release(37) }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        // Every class in this module is bound to Context/DataStore/Android
        // Keystore — nothing is unit-testable on the JVM today, so an empty
        // test source set must not fail the task.
        unitTests.all {
            it.failOnNoDiscoveredTests.set(false)
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
