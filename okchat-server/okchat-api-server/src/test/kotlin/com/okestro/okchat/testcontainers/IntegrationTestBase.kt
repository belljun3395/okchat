package com.okestro.okchat.testcontainers

/**
 * Base class for integration tests that require test containers
 * Provides a marker interface for future extensions
 *
 * Usage:
 * ```kotlin
 * @DataJpaTest
 * @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
 * @ActiveProfiles("test")
 * class YourIntegrationTest : IntegrationTestBase() {
 *     companion object {
 *         @JvmStatic
 *         @DynamicPropertySource
 *         fun properties(registry: DynamicPropertyRegistry) {
 *             SharedTestContainers.configureMysql(registry)
 *         }
 *     }
 * }
 * ```
 */
abstract class IntegrationTestBase {
    // Future common setup/teardown logic can be added here
}
