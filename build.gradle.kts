import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    // Catalog pins 0.9.1, which calls KotlinCompilation.compileKotlinTask —
    // removed in Kotlin 2.4 and crashes coverage of the kotlin-jvm modules
    // (:domain, :core:common). 0.9.8 is KGP-2.4 compatible; applying it here
    // propagates the version to every `apply(plugin = ...)` below.
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
}

// Code quality — ktlint, detekt and Kover are applied to every module.
// Rules live in .editorconfig (ktlint) and config/detekt/detekt.yml.
// See CONTRIBUTING.md#qualidade.
apply(plugin = "org.jlleitschuh.gradle.ktlint") // also lints this script

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.kotlinx.kover")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml")))
        // One baseline per module under config/detekt/baseline/ — the
        // baseline path is per-subproject, so a single shared file is not
        // viable. Modules with zero findings get no file (detekt treats a
        // missing baseline as empty).
        baseline = rootProject.layout.projectDirectory
            .file("config/detekt/baseline/${project.name}.xml")
            .asFile
        // Scan every source set (main, test, androidTest, flavors like
        // src/dev) — the default covers only src/{main,test}/{java,kotlin}.
        source.setFrom("src")
        parallel = true
    }

    // Pin --jvm-target to the project's bytecode target (17); detekt would
    // otherwise pass the daemon's JDK version, which it rejects above 22.
    // NOTE: detekt also runs its Kotlin frontend inside the daemon JVM and
    // crashes on JDKs it can't parse (e.g. Microsoft OpenJDK "25.0.4.1") —
    // run Gradle on JDK 21 (CI does; see CONTRIBUTING.md#qualidade).
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "17"
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "17"
    }

    extensions.configure<KtlintExtension> {
        ignoreFailures.set(false)
    }

    // AGP's Compose Preview tasks fail to configure on library variants
    // unless unit tests can see Android resources and are enabled (only :app
    // sets both — it re-enables them in androidComponents.beforeVariants).
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            testOptions.unitTests.isIncludeAndroidResources = true
        }
        extensions.configure<com.android.build.api.variant.LibraryAndroidComponentsExtension> {
            beforeVariants { variant ->
                variant.hostTests[
                    com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE,
                ]?.enable = true
            }
        }
    }
}

// Coverage aggregation: the root project is the merging module. The merged
// `koverHtmlReport`/`koverXmlReport` tasks aggregate every module's unit-test
// coverage (all build variants; per-module `createVariant` would be needed
// for variant-scoped reports). :baselineprofile is an instrumented-test
// module — no JVM tests to cover.
kover {
    merge {
        subprojects { it.name != "baselineprofile" }
    }
    reports {
        filters {
            excludes {
                // R, BuildConfig, databinding/viewbinding and friends.
                androidGeneratedClasses()
                classes(
                    // Dagger/Hilt generated artifacts.
                    "dagger.**",
                    "dagger.hilt.**",
                    "hilt_aggregated_deps",
                    "**Hilt_*",
                    "**.*_Factory",
                    "**.*_Factory\$*",
                    "**.*_MembersInjector",
                    "**.*_HiltModules*",
                    "**.*Module_*",
                    // Compose compiler generated singletons.
                    "**ComposableSingletons**",
                )
            }
        }
    }
}
