import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    android {
       namespace = "de.shinz.rickandmortyshowcase.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.ktor.client.logging)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // api, not implementation: Room's generated code and the DAO return types
            // are part of this module's own surface.
            api(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.assertk)
            implementation(libs.turbine)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        // JUnit5 is JVM-only and must never reach commonMain, or iOS stops compiling.
        // ViewModel tests live here; everything pure lives in commonTest so iOS runs it too.
        // No typed accessor exists for this source set — it must be looked up by name.
        getByName("androidHostTest").dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.kotlin.test.junit5)
            runtimeOnly(libs.junit.platform.launcher)
        }
    }
}

room {
    // Not "$projectDir/schemas": an absolute path baked into a task input costs
    // build-cache relocatability.
    schemaDirectory(layout.projectDirectory.dir("schemas").asFile.path)
}

compose.resources {
    packageOfResClass = "de.shinz.rickandmortyshowcase.generated.resources"
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)

    // Per-target, deliberately. ksp(...) is deprecated for KMP and kspAndroidMain is
    // silently ignored — Room generates nothing and the build still succeeds.
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// The AGP KMP library plugin does not enable JUnit Platform discovery, and has no
// testOptions DSL to do it through. Without this, JUnit5 tests are silently skipped
// and the build still reports success. tasks.named(...) fails to resolve at
// configuration time, hence withType + a name check.
tasks.withType<Test>().configureEach {
    if (name == "testAndroidHostTest") {
        useJUnitPlatform()
    }
}
