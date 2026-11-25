pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sgp-cliente"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:network")
include(":core:datastore")
include(":core:notifications")
// FCM opt-in: o módulo acoplado ao Firebase só entra quando o app tem
// google-services.json — espelha o `hasGoogleServices` do app/build.gradle.kts.
if (file("app/google-services.json").exists()) {
    include(":core:notifications-fcm")
}
include(":domain")
include(":data")
include(":feature:auth")
include(":feature:contracts")
include(":feature:dashboard")
include(":feature:bills")
include(":feature:billdetails")
include(":feature:paymentpromises")
include(":feature:profile")
include(":feature:onboarding")
include(":baselineprofile")
