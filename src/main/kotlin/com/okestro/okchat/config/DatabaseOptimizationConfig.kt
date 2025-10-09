package com.okestro.okchat.config

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cfg.AvailableSettings
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean

private val log = KotlinLogging.logger {}

/**
 * Database optimization configuration
 * Configures Hibernate for better performance
 */
@Configuration
class DatabaseOptimizationConfig {

    /**
     * Customize Hibernate properties for performance
     */
    @Bean
    fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
        return HibernatePropertiesCustomizer { hibernateProperties ->
            // Enable batch processing
            hibernateProperties[AvailableSettings.STATEMENT_BATCH_SIZE] = "25"
            hibernateProperties[AvailableSettings.ORDER_INSERTS] = "true"
            hibernateProperties[AvailableSettings.ORDER_UPDATES] = "true"
            hibernateProperties[AvailableSettings.BATCH_VERSIONED_DATA] = "true"
            
            // Enable second-level cache
            hibernateProperties[AvailableSettings.USE_SECOND_LEVEL_CACHE] = "true"
            hibernateProperties[AvailableSettings.USE_QUERY_CACHE] = "true"
            hibernateProperties[AvailableSettings.CACHE_REGION_FACTORY] = "org.hibernate.cache.jcache.internal.JCacheRegionFactory"
            
            // Optimize JDBC fetching
            hibernateProperties[AvailableSettings.DEFAULT_BATCH_FETCH_SIZE] = "16"
            hibernateProperties[AvailableSettings.USE_STREAMS_FOR_BINARY] = "true"
            
            // Connection handling
            hibernateProperties[AvailableSettings.CONNECTION_HANDLING] = "DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION"
            
            // Statistics (disable in production for performance)
            hibernateProperties[AvailableSettings.GENERATE_STATISTICS] = "false"
            
            log.info { "Configured Hibernate optimizations: batch_size=25, 2nd-level-cache=enabled, batch_fetch=16" }
        }
    }
}