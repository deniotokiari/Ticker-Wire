package pl.deniotokiari.tickerwire.common.data

import android.annotation.SuppressLint
import android.content.Context

/**
 * Singleton holder for Android application context.
 * Must be initialized in Application.onCreate() or MainActivity.onCreate().
 */
@SuppressLint("StaticFieldLeak")
object AndroidContextHolder {
    private var _context: Context? = null

    val context: Context
        get() = requireNotNull(_context) {
            "AndroidContextHolder not initialized. Call initialize(context) first."
        }

    fun initialize(context: Context) {
        _context = context.applicationContext
    }
}

