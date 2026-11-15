import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.baselineprofile)
    // Classpath only (`apply false`): applied below, conditionally, so a
    // checkout without google-services.json still configures and builds.
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.firebase.crashlytics.plugin) apply false
}

// Firebase/Crashlytics opt-in: drop google-services.json into app/ (gitignored)
// to enable the plugins and dependencies. Without the file the build stays
// green with no Firebase on the classpath.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = libs.plugins.gms.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.plugin.get().pluginId)
}

// Optional release signing: credentials live in root keystore.properties
// (gitignored — see keystore.properties.template). Missing file = unsigned
// release build, which keeps CI and dev machines working.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

android {
    namespace = "com.sgp.cliente"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "com.sgp.cliente"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") { dimension = "env" }
        create("staging") { dimension = "env" }
        create("prod") { dimension = "env" }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**", "com.sgp.**")
            }
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// The baseline-profile plugin disables unit tests on the nonMinifiedRelease
// variants it generates, but AGP's Compose Preview tasks require them —
// re-enable for every variant (debug already has them on).
androidComponents {
    beforeVariants { variant ->
        variant.hostTests[
            com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE,
        ]?.enable = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:contracts"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:bills"))
    implementation(project(":feature:billdetails"))
    implementation(project(":feature:paymentpromises"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:onboarding"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navsuite)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    implementation(libs.timber)
    if (hasGoogleServices) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
    }

    // Dev flavor only: offline demo interceptor (okhttp types on classpath).
    "devImplementation"(libs.okhttp)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
}
