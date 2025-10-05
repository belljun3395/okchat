package com.okestro.okchat.search.config

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import com.okestro.okchat.search.strategy.KeywordSearchStrategy
import com.okestro.okchat.search.strategy.TitleSearchStrategy
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Search strategy configuration.
 * Strategies depend on SearchClient interface for engine independence.
 */
@Configuration
@EnableConfigurationProperties(SearchWeightConfig::class, SearchFieldWeightConfig::class)
class SearchStrategyConfig {

    @Bean
    fun keywordSearchStrategy(
        searchClient: SearchClient,
        embeddingModel: EmbeddingModel,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): KeywordSearchStrategy {
        return KeywordSearchStrategy(searchClient, embeddingModel, weightConfig, fieldConfig)
    }

    @Bean
    fun titleSearchStrategy(
        searchClient: SearchClient,
        embeddingModel: EmbeddingModel,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): TitleSearchStrategy {
        return TitleSearchStrategy(searchClient, embeddingModel, weightConfig, fieldConfig)
    }

    @Bean
    fun contentSearchStrategy(
        searchClient: SearchClient,
        embeddingModel: EmbeddingModel,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): ContentSearchStrategy {
        return ContentSearchStrategy(searchClient, embeddingModel, weightConfig, fieldConfig)
    }
}
