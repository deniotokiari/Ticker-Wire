package pl.deniotokiari.tickerwire.models

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json

class LimitConfigTest : BehaviorSpec({

    val json = Json { ignoreUnknownKeys = true }

    Given("LimitConfig serialization") {

        When("serializing a config with all limits") {
            val config = LimitConfig(
                perMonth = 500,
                perDay = 25,
                perMinute = 5
            )

            val jsonString = json.encodeToString(config)

            Then("should contain all fields with snake_case") {
                jsonString shouldBe """{"per_month":500,"per_day":25,"per_minute":5}"""
            }
        }

        When("serializing a config with only daily limit") {
            val config = LimitConfig(perDay = 100)

            val jsonString = json.encodeToString(config)

            Then("should contain per_day field") {
                jsonString shouldContain """"per_day":100"""
            }
        }

        When("serializing an empty config") {
            val config = LimitConfig()

            val jsonString = json.encodeToString(config)

            Then("should serialize to valid JSON") {
                // Empty config may or may not include null values depending on serializer settings
                val decoded = json.decodeFromString<LimitConfig>(jsonString)
                decoded.perMonth shouldBe null
                decoded.perDay shouldBe null
                decoded.perMinute shouldBe null
            }
        }
    }

    Given("LimitConfig deserialization") {

        When("deserializing a full config") {
            val jsonString = """{"per_month":1000,"per_day":50,"per_minute":10}"""

            val config = json.decodeFromString<LimitConfig>(jsonString)

            Then("should parse all limits correctly") {
                config.perMonth shouldBe 1000
                config.perDay shouldBe 50
                config.perMinute shouldBe 10
            }
        }

        When("deserializing a partial config") {
            val jsonString = """{"per_day":25}"""

            val config = json.decodeFromString<LimitConfig>(jsonString)

            Then("should parse available limits and default others to null") {
                config.perMonth shouldBe null
                config.perDay shouldBe 25
                config.perMinute shouldBe null
            }
        }

        When("deserializing an empty object") {
            val jsonString = """{}"""

            val config = json.decodeFromString<LimitConfig>(jsonString)

            Then("should have all null values") {
                config.perMonth shouldBe null
                config.perDay shouldBe null
                config.perMinute shouldBe null
            }
        }

        When("deserializing with extra unknown fields") {
            val jsonString = """{"per_day":25,"unknown_field":"ignored"}"""

            val config = json.decodeFromString<LimitConfig>(jsonString)

            Then("should ignore unknown fields") {
                config.perDay shouldBe 25
            }
        }
    }

    Given("LimitConfig data class operations") {

        When("copying a config with modified values") {
            val original = LimitConfig(perMonth = 100, perDay = 10)
            val copied = original.copy(perDay = 20)

            Then("original should be unchanged") {
                original.perDay shouldBe 10
            }

            Then("copy should have new value") {
                copied.perDay shouldBe 20
                copied.perMonth shouldBe 100
            }
        }

        When("comparing equal configs") {
            val config1 = LimitConfig(perDay = 25)
            val config2 = LimitConfig(perDay = 25)

            Then("should be equal") {
                config1 shouldBe config2
            }
        }
    }
})

