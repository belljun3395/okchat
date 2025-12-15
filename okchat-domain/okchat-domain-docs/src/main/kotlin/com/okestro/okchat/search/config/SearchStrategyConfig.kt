package com.okestro.okchat.search.config

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.strategy.ContentSearchStrategy
import com.okestro.okchat.search.strategy.KeywordSearchStrategy
import com.okestro.okchat.search.strategy.TitleSearchStrategy
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.ObjectProvider
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
        searchClient: ObjectProvider<SearchClient>,
        embeddingModel: ObjectProvider<EmbeddingModel>,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): KeywordSearchStrategy {
        return KeywordSearchStrategy(searchClient.getIfAvailable()!!, embeddingModel.getIfAvailable()!!, weightConfig, fieldConfig)
    }

    @Bean
    fun titleSearchStrategy(
        searchClient: ObjectProvider<SearchClient>,
        embeddingModel: ObjectProvider<EmbeddingModel>,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): TitleSearchStrategy {
        return TitleSearchStrategy(searchClient.getIfAvailable()!!, embeddingModel.getIfAvailable()!!, weightConfig, fieldConfig)
    }

    @Bean
    fun contentSearchStrategy(
        searchClient: ObjectProvider<SearchClient>,
        embeddingModel: ObjectProvider<EmbeddingModel>,
        weightConfig: SearchWeightConfig,
        fieldConfig: SearchFieldWeightConfig
    ): ContentSearchStrategy {
        return ContentSearchStrategy(searchClient.getIfAvailable()!!,
            embeddingModel.getIfAvailable()!!, weightConfig, fieldConfig)
    }
}
