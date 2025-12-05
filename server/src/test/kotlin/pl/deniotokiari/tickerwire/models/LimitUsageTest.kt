package pl.deniotokiari.tickerwire.models

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class LimitUsageTest : BehaviorSpec({

    Given("a fresh LimitUsage with no prior usage") {
        val usage = LimitUsage()

        When("checking if it can be used with daily limit of 5") {
            val config = LimitConfig(perDay = 5)
            val canUse = usage.canUse(config)

            Then("it should be allowed") {
                canUse shouldBe true
            }
        }

        When("incrementing usage") {
            val config = LimitConfig(perDay = 5)
            val now = LocalDateTime.of(2024, 1, 15, 10, 30)
            val newUsage = usage.increment(config, now)

            Then("usedCount should be 1") {
                newUsage.usedCount shouldBe 1
            }

            Then("lastUsed should be set") {
                newUsage.lastUsed shouldBe now
            }
        }
    }

    Given("a LimitUsage with 4 uses and daily limit of 5") {
        val now = LocalDateTime.of(2024, 1, 15, 10, 30)
        val usage = LimitUsage(lastUsed = now, usedCount = 4)
        val config = LimitConfig(perDay = 5)

        When("checking if it can be used") {
            val canUse = usage.canUse(config, now)

            Then("it should be allowed (4 < 5)") {
                canUse shouldBe true
            }
        }

        When("incrementing to 5 uses") {
            val newUsage = usage.increment(config, now)

            Then("usedCount should be 5") {
                newUsage.usedCount shouldBe 5
            }
        }

        When("checking after increment to 5") {
            val newUsage = usage.increment(config, now)
            val canUse = newUsage.canUse(config, now)

            Then("it should NOT be allowed (5 >= 5)") {
                canUse shouldBe false
            }
        }
    }

    Given("a LimitUsage that was used yesterday at 23:59") {
        val yesterday = LocalDateTime.of(2024, 1, 14, 23, 59)
        val today = LocalDateTime.of(2024, 1, 15, 0, 1)
        val usage = LimitUsage(lastUsed = yesterday, usedCount = 5)
        val config = LimitConfig(perDay = 5)

        When("checking on the next day at 00:01") {
            val canUse = usage.canUse(config, today)

            Then("it should be allowed (new day resets count)") {
                canUse shouldBe true
            }
        }

        When("incrementing on the next day") {
            val newUsage = usage.increment(config, today)

            Then("usedCount should be 1 (reset then increment)") {
                newUsage.usedCount shouldBe 1
            }
        }
    }

    Given("a LimitUsage with monthly limit that was used last month") {
        val lastMonth = LocalDateTime.of(2024, 1, 31, 23, 59)
        val thisMonth = LocalDateTime.of(2024, 2, 1, 0, 0)
        val usage = LimitUsage(lastUsed = lastMonth, usedCount = 100)
        val config = LimitConfig(perMonth = 100)

        When("checking in the new month") {
            val canUse = usage.canUse(config, thisMonth)

            Then("it should be allowed (new month resets count)") {
                canUse shouldBe true
            }
        }

        When("incrementing in the new month") {
            val newUsage = usage.increment(config, thisMonth)

            Then("usedCount should be 1") {
                newUsage.usedCount shouldBe 1
            }
        }
    }

    Given("a LimitUsage with per-minute limit") {
        val baseTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        val sameMinute = LocalDateTime.of(2024, 1, 15, 10, 30, 45)
        val nextMinute = LocalDateTime.of(2024, 1, 15, 10, 31, 0)
        val usage = LimitUsage(lastUsed = baseTime, usedCount = 5)
        val config = LimitConfig(perMinute = 5)

        When("checking within the same minute at limit") {
            val canUse = usage.canUse(config, sameMinute)

            Then("it should NOT be allowed") {
                canUse shouldBe false
            }
        }

        When("checking in the next minute") {
            val canUse = usage.canUse(config, nextMinute)

            Then("it should be allowed (new minute resets)") {
                canUse shouldBe true
            }
        }
    }

    Given("a LimitUsage with combined daily and monthly limits") {
        val now = LocalDateTime.of(2024, 1, 15, 10, 30)
        val usage = LimitUsage(lastUsed = now, usedCount = 20)
        val config = LimitConfig(perDay = 25, perMonth = 500)

        When("checking if it can be used") {
            val canUse = usage.canUse(config, now)

            Then("it should be allowed (20 < 25 and 20 < 500)") {
                canUse shouldBe true
            }
        }

        When("usage reaches daily limit but not monthly") {
            val atDailyLimit = LimitUsage(lastUsed = now, usedCount = 25)
            val canUse = atDailyLimit.canUse(config, now)

            Then("it should NOT be allowed (daily limit reached)") {
                canUse shouldBe false
            }
        }
    }

    Given("getRemainingCapacity with various limits") {
        val now = LocalDateTime.of(2024, 1, 15, 10, 30)
        val usage = LimitUsage(lastUsed = now, usedCount = 15)

        When("checking remaining capacity for daily limit of 25") {
            val config = LimitConfig(perDay = 25)
            val remaining = usage.getRemainingCapacity(config, now)

            Then("remaining should be 10") {
                remaining shouldBe 10
            }
        }

        When("checking remaining capacity for per-minute limit of 60") {
            val config = LimitConfig(perMinute = 60)
            val remaining = usage.getRemainingCapacity(config, now)

            Then("remaining should be 45") {
                remaining shouldBe 45
            }
        }

        When("checking remaining capacity with no limits") {
            val config = LimitConfig()
            val remaining = usage.getRemainingCapacity(config, now)

            Then("remaining should be Int.MAX_VALUE") {
                remaining shouldBe Int.MAX_VALUE
            }
        }
    }

    Given("calendar boundary edge cases") {
        When("usage at 23:59:59 on Dec 31st, check at 00:00:00 on Jan 1st next year") {
            val lastYear = LocalDateTime.of(2024, 12, 31, 23, 59, 59)
            val newYear = LocalDateTime.of(2025, 1, 1, 0, 0, 0)
            val usage = LimitUsage(lastUsed = lastYear, usedCount = 100)
            val config = LimitConfig(perMonth = 100)

            val canUse = usage.canUse(config, newYear)

            Then("it should be allowed (new month AND new year)") {
                canUse shouldBe true
            }
        }

        When("usage at end of February, check in March (leap year boundary)") {
            val feb = LocalDateTime.of(2024, 2, 29, 23, 59)
            val march = LocalDateTime.of(2024, 3, 1, 0, 0)
            val usage = LimitUsage(lastUsed = feb, usedCount = 50)
            val config = LimitConfig(perMonth = 50)

            val canUse = usage.canUse(config, march)

            Then("it should be allowed (new month)") {
                canUse shouldBe true
            }
        }
    }

    Given("LimitUsage.isNewMonth companion function") {
        When("comparing same month") {
            val isNew = LimitUsage.isNewMonth(
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 31, 23, 59)
            )

            Then("should return false") {
                isNew shouldBe false
            }
        }

        When("comparing different months same year") {
            val isNew = LimitUsage.isNewMonth(
                LocalDateTime.of(2024, 1, 31, 23, 59),
                LocalDateTime.of(2024, 2, 1, 0, 0)
            )

            Then("should return true") {
                isNew shouldBe true
            }
        }

        When("comparing different years") {
            val isNew = LimitUsage.isNewMonth(
                LocalDateTime.of(2024, 12, 31, 23, 59),
                LocalDateTime.of(2025, 1, 1, 0, 0)
            )

            Then("should return true") {
                isNew shouldBe true
            }
        }
    }

    Given("LimitUsage.isNewDay companion function") {
        When("comparing same day") {
            val isNew = LimitUsage.isNewDay(
                LocalDateTime.of(2024, 1, 15, 0, 0),
                LocalDateTime.of(2024, 1, 15, 23, 59)
            )

            Then("should return false") {
                isNew shouldBe false
            }
        }

        When("comparing consecutive days at midnight boundary") {
            val isNew = LimitUsage.isNewDay(
                LocalDateTime.of(2024, 1, 15, 23, 59, 59),
                LocalDateTime.of(2024, 1, 16, 0, 0, 0)
            )

            Then("should return true") {
                isNew shouldBe true
            }
        }
    }

    Given("LimitUsage.isNewMinute companion function") {
        When("comparing same minute") {
            val isNew = LimitUsage.isNewMinute(
                LocalDateTime.of(2024, 1, 15, 10, 30, 0),
                LocalDateTime.of(2024, 1, 15, 10, 30, 59)
            )

            Then("should return false") {
                isNew shouldBe false
            }
        }

        When("comparing consecutive minutes") {
            val isNew = LimitUsage.isNewMinute(
                LocalDateTime.of(2024, 1, 15, 10, 30, 59),
                LocalDateTime.of(2024, 1, 15, 10, 31, 0)
            )

            Then("should return true") {
                isNew shouldBe true
            }
        }
    }
})

