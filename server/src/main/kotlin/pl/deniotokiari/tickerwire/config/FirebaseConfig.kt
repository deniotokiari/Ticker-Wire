package pl.deniotokiari.tickerwire.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File
import java.io.FileInputStream

object FirebaseConfig {
    fun initialize(credentialsPath: String? = null) {
        if (FirebaseApp.getApps().isEmpty()) {
            val options = when {
                // Option 1: Explicit path provided
                credentialsPath != null -> {
                    val file = File(credentialsPath)
                    if (!file.exists()) {
                        throw IllegalStateException("Firebase credentials file not found: $credentialsPath")
                    }
                    val serviceAccount = FileInputStream(file)
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build()
                }
                // Option 2: Look in project root
                else -> {
                    val projectRoot = File(System.getProperty("user.dir"))
                    val serviceAccountFile = File(projectRoot, "serviceAccountKey.json")

                    if (serviceAccountFile.exists()) {
                        val serviceAccount = FileInputStream(serviceAccountFile)
                        FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build()
                    } else {
                        // Option 3: Try environment variable (for GCP)
                        try {
                            FirebaseOptions.builder()
                                .setCredentials(GoogleCredentials.getApplicationDefault())
                                .build()
                        } catch (e: Exception) {
                            throw IllegalStateException(
                                "Firebase credentials not found. " +
                                        "Place serviceAccountKey.json in project root, " +
                                        "set FIREBASE_CREDENTIALS_PATH environment variable, " +
                                        "or set GOOGLE_APPLICATION_CREDENTIALS environment variable",
                                e
                            )
                        }
                    }
                }
            }

            FirebaseApp.initializeApp(options)
        }
    }
}
