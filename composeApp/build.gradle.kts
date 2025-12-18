import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.screenshot)
}

// Read local.properties
val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

val apiUri = localProperties.getProperty("api.uri") ?: "http://localhost:8080"

kotlin {
    // Suppress expect/actual classes Beta warning
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    js {
        browser()
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            // Ktor Android engine
            implementation(libs.ktor.client.android)
        }

        iosMain.dependencies {
            // Ktor Darwin engine for iOS
            implementation(libs.ktor.client.darwin)
        }

        jsMain.dependencies {
            // Ktor JS engine
            implementation(libs.ktor.client.js)
        }

        val wasmJsMain by getting {
            dependencies {
                // Ktor JS engine for WasmJS
                implementation(libs.ktor.client.js)
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.annotations)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(projects.shared)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.multiplatform.settings.serialization)

            // Ktor Client
            implementation(libs.ktor.client.core.multiplatform)
            implementation(libs.ktor.client.content.negotiation.multiplatform)
            implementation(libs.ktor.client.serialization.kotlinx.json.multiplatform)
            implementation(libs.ktor.client.logging)

            implementation(libs.kermit)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        kotlin.srcDir("build/generated/kotlin")
    }
}

// Generate ApiConfig.kt from local.properties
tasks.register("generateApiConfig") {
    val outputDir = file("build/generated/kotlin/pl/deniotokiari/tickerwire/common/config")
    val outputFile = file("$outputDir/ApiConfig.kt")
    val localPropertiesFile = rootProject.file("local.properties")
    
    inputs.file(localPropertiesFile).optional()
    outputs.file(outputFile)
    
    // Mark as not compatible with configuration cache
    notCompatibleWithConfigurationCache("Reading local.properties at runtime")
    
    doFirst {
        val apiUriValue = apiUri
        outputDir.mkdirs()
        outputFile.writeText("""
            package pl.deniotokiari.tickerwire.common.config
            
            internal const val API_BASE_URL = "$apiUriValue"
        """.trimIndent())
    }
}

// Make compilation depend on config generation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn("generateApiConfig")
}

android {
    namespace = "pl.deniotokiari.tickerwire"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pl.deniotokiari.tickerwire"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing config will be set via environment variables or gradle.properties
            // See signingConfigs block below
        }
    }
    
    signingConfigs {
        // Release signing configuration
        // In production, use environment variables or gradle.properties:
        // storeFile=file("path/to/keystore.jks")
        // storePassword=your_store_password
        // keyAlias=your_key_alias
        // keyPassword=your_key_password
        create("release") {
            val keystoreFile = project.findProperty("RELEASE_STORE_FILE") as String?
            val keystorePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            val keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            val keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            
            if (keystoreFile != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
    
    buildTypes.getByName("release") {
        val releaseSigningConfig = signingConfigs.getByName("release")
        val storeFile = releaseSigningConfig.storeFile
        signingConfig = releaseSigningConfig.takeIf {
            storeFile != null && storeFile.exists()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    debugImplementation(compose.uiTooling)

    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)

    add("kspCommonMainMetadata", libs.koin.ksp.compiler)
    add("kspAndroid", libs.koin.ksp.compiler)
    add("kspIosArm64", libs.koin.ksp.compiler)
    add("kspIosSimulatorArm64", libs.koin.ksp.compiler)
    
    // Firebase Analytics for Android
    implementation(libs.firebase.analytics)
}

// Make KSP tasks depend on API config generation
tasks.matching { it.name.startsWith("ksp") }
    .configureEach {
        dependsOn("generateApiConfig")
    }

tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }
    .configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }

