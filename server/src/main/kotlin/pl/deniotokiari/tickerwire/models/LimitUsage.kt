package pl.deniotokiari.tickerwire.models

import java.time.LocalDateTime

/**
 * Limit usage state - mutable, stored in Firestore
 * Tracks when a provider was last used and how many times
 */
data class LimitUsage(
    val lastUsed: LocalDateTime? = null,
    val usedCount: Int = 0,
) {
    /**
     * Check if the limit can be used given the config and current time
     */
    fun canUse(config: LimitConfig, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val resetUsage = resetIfNeeded(config, now)

        return when {
            config.perMonth != null && resetUsage.usedCount >= config.perMonth -> false
            config.perDay != null && resetUsage.usedCount >= config.perDay -> false
            config.perMinute != null && resetUsage.usedCount >= config.perMinute -> false
            else -> true
        }
    }

    /**
     * Increment the usage count
     */
    fun increment(config: LimitConfig, now: LocalDateTime = LocalDateTime.now()): LimitUsage {
        val resetUsage = resetIfNeeded(config, now)

        return LimitUsage(
            lastUsed = now,
            usedCount = resetUsage.usedCount + 1
        )
    }

    /**
     * Reset usage counters if we're in a new period
     */
    fun resetIfNeeded(config: LimitConfig, now: LocalDateTime = LocalDateTime.now()): LimitUsage {
        if (lastUsed == null) return this

        var newUsedCount = usedCount

        // Check if we're in a new calendar month
        if (config.perMonth != null && isNewMonth(lastUsed, now)) {
            newUsedCount = 0
        }

        // Check if we're in a new calendar day
        if (config.perDay != null && isNewDay(lastUsed, now)) {
            newUsedCount = 0
        }

        // Check if we're in a new minute
        if (config.perMinute != null && isNewMinute(lastUsed, now)) {
            newUsedCount = 0
        }

        return if (newUsedCount != usedCount) {
            copy(usedCount = newUsedCount)
        } else {
            this
        }
    }

    /**
     * Get remaining capacity for the given config
     */
    fun getRemainingCapacity(config: LimitConfig, now: LocalDateTime = LocalDateTime.now()): Int {
        val resetUsage = resetIfNeeded(config, now)

        return when {
            config.perMinute != null -> config.perMinute - resetUsage.usedCount
            config.perDay != null -> config.perDay - resetUsage.usedCount
            config.perMonth != null -> config.perMonth - resetUsage.usedCount
            else -> Int.MAX_VALUE
        }
    }

    companion object {
        /**
         * Check if now is in a different calendar month than lastTime
         */
        fun isNewMonth(lastTime: LocalDateTime, now: LocalDateTime): Boolean {
            return lastTime.year != now.year || lastTime.monthValue != now.monthValue
        }

        /**
         * Check if now is in a different calendar day than lastTime
         */
        fun isNewDay(lastTime: LocalDateTime, now: LocalDateTime): Boolean {
            return lastTime.toLocalDate() != now.toLocalDate()
        }

        /**
         * Check if now is in a different minute than lastTime
         */
        fun isNewMinute(lastTime: LocalDateTime, now: LocalDateTime): Boolean {
            return lastTime.withSecond(0).withNano(0) != now.withSecond(0).withNano(0)
        }
    }
}

