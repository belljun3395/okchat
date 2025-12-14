package com.okestro.okchat.search.strategy

import com.okestro.okchat.search.client.SearchClient
import com.okestro.okchat.search.config.SearchFieldWeightConfig
import com.okestro.okchat.search.config.SearchWeightConfig
import org.springframework.ai.embedding.EmbeddingModel

class ContentSearchStrategy(
    searchClient: SearchClient,
    embeddingModel: EmbeddingModel,
    private val weightConfig: SearchWeightConfig,
    private val fieldConfig: SearchFieldWeightConfig
) : AbstractHybridSearchStrategy(searchClient, embeddingModel) {

    override fun getName() = "Content Hybrid Search"

    override fun getFieldWeights() = fieldConfig.content

    override fun getWeightSettings() = weightConfig.content
}
