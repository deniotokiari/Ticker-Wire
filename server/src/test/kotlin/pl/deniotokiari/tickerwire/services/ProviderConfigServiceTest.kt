package pl.deniotokiari.tickerwire.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.KSerializer
import pl.deniotokiari.tickerwire.models.LimitConfig
import pl.deniotokiari.tickerwire.models.Provider
import pl.deniotokiari.tickerwire.models.ProviderConfig

class ProviderConfigServiceTest : BehaviorSpec({

    Given("ProviderConfigService initialization") {

        When("Firebase returns provider configurations") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>()
            val testConfigs = mapOf(
                "ALPHAVANTAGE" to ProviderConfig(
                    apiUri = "https://alphavantage.co/query",
                    apiKey = "test-key-1",
                    limit = LimitConfig(perDay = 25)
                ),
                "MARKETSTACK" to ProviderConfig(
                    apiUri = "https://api.marketstack.com/v2",
                    apiKey = "test-key-2",
                    limit = LimitConfig(perMonth = 100)
                )
            )

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns testConfigs

            val service = ProviderConfigService(mockFirebaseService)

            Then("should load configurations on init") {
                service.configs.size shouldBe 2
            }

            Then("should have ALPHAVANTAGE config") {
                service.configs.containsKey(Provider.ALPHAVANTAGE) shouldBe true
                service.configs[Provider.ALPHAVANTAGE]!!.apiKey shouldBe "test-key-1"
            }

            Then("should have MARKETSTACK config") {
                service.configs.containsKey(Provider.MARKETSTACK) shouldBe true
                service.configs[Provider.MARKETSTACK]!!.apiKey shouldBe "test-key-2"
            }
        }

        When("Firebase returns null") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>()

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns null

            val service = ProviderConfigService(mockFirebaseService)

            Then("should have empty configs") {
                service.configs.size shouldBe 0
            }
        }
    }

    Given("ProviderConfigService get function") {

        When("requesting an existing provider") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>()
            val testConfig = ProviderConfig(
                apiUri = "https://api.test.com",
                apiKey = "test-key",
                limit = LimitConfig(perDay = 50)
            )

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns mapOf("ALPHAVANTAGE" to testConfig)

            val service = ProviderConfigService(mockFirebaseService)
            val config = service.get(Provider.ALPHAVANTAGE)

            Then("should return the config") {
                config.apiUri shouldBe "https://api.test.com"
                config.apiKey shouldBe "test-key"
            }
        }

        When("requesting a non-existing provider") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>()

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns emptyMap()

            val service = ProviderConfigService(mockFirebaseService)

            Then("should throw IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> {
                    service.get(Provider.ALPHAVANTAGE)
                }
            }
        }
    }

    Given("ProviderConfigService refresh function") {

        When("refresh is called") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>()

            // Initial config
            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns mapOf(
                "ALPHAVANTAGE" to ProviderConfig(
                    apiUri = "https://api.old.com",
                    apiKey = "old-key",
                    limit = LimitConfig(perDay = 10)
                )
            ) andThen mapOf(
                "ALPHAVANTAGE" to ProviderConfig(
                    apiUri = "https://api.new.com",
                    apiKey = "new-key",
                    limit = LimitConfig(perDay = 100)
                )
            )

            val service = ProviderConfigService(mockFirebaseService)

            Then("should have initial config") {
                service.configs[Provider.ALPHAVANTAGE]!!.apiKey shouldBe "old-key"
            }

            service.refresh()

            Then("should have updated config after refresh") {
                service.configs[Provider.ALPHAVANTAGE]!!.apiKey shouldBe "new-key"
                service.configs[Provider.ALPHAVANTAGE]!!.limit.perDay shouldBe 100
            }
        }
    }

    Given("ProviderConfigService set function") {

        When("setting a new config") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>(relaxed = true)

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns emptyMap()

            val service = ProviderConfigService(mockFirebaseService)

            val newConfig = ProviderConfig(
                apiUri = "https://api.new.com",
                apiKey = "new-key",
                limit = LimitConfig(perDay = 50)
            )

            service.set(Provider.ALPHAVANTAGE, newConfig)

            Then("should update local config") {
                service.configs[Provider.ALPHAVANTAGE] shouldBe newConfig
            }

            Then("should persist to Firebase") {
                verify {
                    mockFirebaseService.set(
                        key = "providers",
                        value = any<Map<String, ProviderConfig>>(),
                        kSerializer = any()
                    )
                }
            }
        }
    }

    Given("ProviderConfigService setAsync function") {

        When("setting a config asynchronously") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>(relaxed = true)

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns emptyMap()

            val service = ProviderConfigService(mockFirebaseService)

            val newConfig = ProviderConfig(
                apiUri = "https://api.async.com",
                apiKey = "async-key",
                limit = LimitConfig(perMinute = 60)
            )

            service.setAsync(Provider.MARKETSTACK, newConfig)

            Then("should update local config immediately") {
                service.configs[Provider.MARKETSTACK] shouldBe newConfig
            }

            Then("should call setAsync on Firebase") {
                verify {
                    mockFirebaseService.setAsync(
                        key = "providers",
                        value = any<Map<String, ProviderConfig>>(),
                        kSerializer = any()
                    )
                }
            }
        }
    }

    Given("ProviderConfigService with multiple providers") {

        When("managing multiple providers") {
            val mockFirebaseService = mockk<FirebaseRemoteConfigService>(relaxed = true)

            every {
                mockFirebaseService.get(
                    key = "providers",
                    kSerializer = any<KSerializer<Map<String, ProviderConfig>>>()
                )
            } returns mapOf(
                "ALPHAVANTAGE" to ProviderConfig(
                    apiUri = "https://alphavantage.co",
                    apiKey = "av-key",
                    limit = LimitConfig(perDay = 25)
                )
            )

            val service = ProviderConfigService(mockFirebaseService)

            // Add another provider
            val newConfig = ProviderConfig(
                apiUri = "https://marketstack.com",
                apiKey = "ms-key",
                limit = LimitConfig(perMonth = 100)
            )
            service.set(Provider.MARKETSTACK, newConfig)

            Then("should have both providers") {
                service.configs.size shouldBe 2
                service.configs.containsKey(Provider.ALPHAVANTAGE) shouldBe true
                service.configs.containsKey(Provider.MARKETSTACK) shouldBe true
            }

            Then("should persist both providers") {
                verify {
                    mockFirebaseService.set(
                        key = "providers",
                        value = match<Map<String, ProviderConfig>> {
                            it.size == 2 &&
                                    it.containsKey("ALPHAVANTAGE") &&
                                    it.containsKey("MARKETSTACK")
                        },
                        kSerializer = any()
                    )
                }
            }
        }
    }
})

