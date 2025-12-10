package pl.deniotokiari.tickerwire.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream

object FirebaseConfig {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    fun initialize(credentialsPath: String? = null) {
        if (FirebaseApp.getApps().isNotEmpty()) {
            logger.info("Firebase already initialized")
            return
        }

        val options = tryInitialize(credentialsPath)
        FirebaseApp.initializeApp(options)
        logger.info("Firebase initialized successfully")
    }

    private fun tryInitialize(credentialsPath: String?): FirebaseOptions {
        // Option 1: Explicit path provided via environment variable
        if (!credentialsPath.isNullOrBlank()) {
            val file = File(credentialsPath)
            if (file.exists()) {
                logger.info("Using Firebase credentials from: $credentialsPath")
                return FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                    .build()
            }
            logger.warn("Credentials path provided but file not found: $credentialsPath")
        }

        // Option 2: Try Application Default Credentials (works on GCP - Cloud Run, GCE, etc.)
        try {
            logger.info("Trying Application Default Credentials...")
            val credentials = GoogleCredentials.getApplicationDefault()
            return FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId("ticker-wire")
                .build()
        } catch (e: Exception) {
            logger.warn("Application Default Credentials not available: ${e.message}")
        }

        // Option 3: Look in working directory
        val workingDir = File(System.getProperty("user.dir"))
        val localFile = File(workingDir, "serviceAccountKey.json")
        if (localFile.exists()) {
            logger.info("Using Firebase credentials from working directory: ${localFile.absolutePath}")
            return FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(localFile)))
                .build()
        }

        // Option 4: Look in /app directory (Docker)
        val dockerFile = File("/app/serviceAccountKey.json")
        if (dockerFile.exists()) {
            logger.info("Using Firebase credentials from Docker path: ${dockerFile.absolutePath}")
            return FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(dockerFile)))
                .build()
        }

        throw IllegalStateException(
            "Firebase credentials not found. Tried:\n" +
                "  1. FIREBASE_CONFIG_PATH: $credentialsPath\n" +
                "  2. Application Default Credentials\n" +
                "  3. Working directory: ${localFile.absolutePath}\n" +
                "  4. Docker path: ${dockerFile.absolutePath}"
        )
    }
}
