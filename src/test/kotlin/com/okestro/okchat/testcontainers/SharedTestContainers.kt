package com.okestro.okchat.testcontainers

import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Centralized test container management
 * Provides singleton containers that are shared across all integration tests
 * to significantly improve test execution time.
 *
 * Usage example:
 * ```kotlin
 * @DataJpaTest
 * class YourTest {
 *     companion object {
 *         @JvmStatic
 *         @DynamicPropertySource
 *         fun properties(registry: DynamicPropertyRegistry) {
 *             SharedTestContainers.configureDatabase(registry)
 *         }
 *     }
 * }
 * ```
 */
object SharedTestContainers {

    /**
     * Shared MySQL container for all database integration tests
     */
    val mysqlContainer: MySQLContainer<*> by lazy {
        MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .apply {
                start()
                println("✅ MySQL container started: ${this.jdbcUrl}")
            }
    }

    /**
     * Configure database properties for Spring tests
     */
    fun configureMysql(registry: org.springframework.test.context.DynamicPropertyRegistry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl)
        registry.add("spring.datasource.username", mysqlContainer::getUsername)
        registry.add("spring.datasource.password", mysqlContainer::getPassword)
    }

    // Future containers can be added here:
    // val redisContainer: GenericContainer<*> by lazy { ... }
    // val kafkaContainer: KafkaContainer by lazy { ... }
    // val elasticsearchContainer: ElasticsearchContainer by lazy { ... }

    /**
     * Stop all containers (usually not needed due to reuse)
     */
    fun stopAll() {
        if (mysqlContainer.isRunning) {
            mysqlContainer.stop()
        }
    }
}
