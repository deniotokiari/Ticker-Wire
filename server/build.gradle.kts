plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    application
}

group = "pl.deniotokiari.tickerwire"
version = "1.0.0"
application {
    mainClass.set("pl.deniotokiari.tickerwire.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverCors)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.koinKtor)
    implementation(libs.koinLoggerSlf4j)
    // Ktor Client for external API calls
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlin.testJunit)
    // Kotest dependencies
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    // MockK for mocking
    testImplementation(libs.mockk)
    // JUnit Platform dependencies for IDE support
    testRuntimeOnly(libs.junit.platform.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.firebase.admin)
}

// Configure JUnit 5 for Kotest
tasks.test {
    useJUnitPlatform()
}
