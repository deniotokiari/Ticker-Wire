plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.shadow)
    application
}

group = "pl.deniotokiari.tickerwire"
version = "1.0.3"
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

// Configure Shadow plugin for fat JAR
tasks.shadowJar {
    archiveBaseName.set("server")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
    
    // Merge service files (for SLF4J, gRPC LoadBalancerProvider, etc.)
    // This is critical for gRPC load balancer providers used by Firebase Admin SDK
    mergeServiceFiles()
    
    // Exclude signature files to avoid conflicts
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    
    // Workaround: Post-process JAR to ensure gRPC LoadBalancerProvider includes all providers
    // The mergeServiceFiles() sometimes misses entries from grpc-core
    // Note: Using ProcessBuilder instead of exec() for configuration cache compatibility
    doLast {
        val jarFile = archiveFile.get().asFile
        val tempDir = File.createTempFile("shadow-temp", "").apply {
            delete()
            mkdirs()
        }
        
        try {
            // Extract the service file using ProcessBuilder (configuration cache compatible)
            val extractProcess = ProcessBuilder(
                "jar", "xf", jarFile.absolutePath, "META-INF/services/io.grpc.LoadBalancerProvider"
            )
                .directory(tempDir)
                .redirectErrorStream(true)
                .start()
            val extractExitCode = extractProcess.waitFor()
            // Ignore exit code - file might not exist yet
            
            val serviceFile = File(tempDir, "META-INF/services/io.grpc.LoadBalancerProvider")
            val existingProviders = if (serviceFile.exists()) {
                serviceFile.readLines().filter { line -> line.isNotBlank() }.toMutableSet()
            } else {
                mutableSetOf<String>()
            }
            
            // Add critical providers that might be missing
            existingProviders.add("io.grpc.internal.PickFirstLoadBalancerProvider")
            existingProviders.add("io.grpc.util.SecretRoundRobinLoadBalancerProvider\$Provider")
            existingProviders.add("io.grpc.util.OutlierDetectionLoadBalancerProvider")
            existingProviders.add("io.grpc.grpclb.GrpclbLoadBalancerProvider")
            
            // Write back
            serviceFile.parentFile.mkdirs()
            serviceFile.writeText(existingProviders.joinToString("\n") + "\n")
            
            // Update JAR using ProcessBuilder
            val updateProcess = ProcessBuilder(
                "jar", "uf", jarFile.absolutePath, "-C", tempDir.absolutePath, "META-INF/services/io.grpc.LoadBalancerProvider"
            )
                .redirectErrorStream(true)
                .start()
            val updateExitCode = updateProcess.waitFor()
            if (updateExitCode != 0) {
                throw RuntimeException("Failed to update JAR with merged service file (exit code: $updateExitCode)")
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

// Make shadowJar the default JAR task
tasks.build {
    dependsOn(tasks.shadowJar)
}
