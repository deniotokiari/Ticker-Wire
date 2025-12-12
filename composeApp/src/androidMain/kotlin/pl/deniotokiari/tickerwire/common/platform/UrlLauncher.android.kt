package pl.deniotokiari.tickerwire.common.platform

import android.content.Intent
import androidx.core.net.toUri
import pl.deniotokiari.tickerwire.common.data.AndroidContextHolder

actual fun openUrl(url: String) {
    val context = AndroidContextHolder.context

    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // URL couldn't be opened
    }
}

