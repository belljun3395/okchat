package com.okestro.okchat.search.config

import com.okestro.okchat.search.model.MetadataFields
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "search.hybrid")
data class SearchWeightConfig(
    var keyword: WeightSettings = WeightSettings(textWeight = 0.7, vectorWeight = 0.3),
    var title: WeightSettings = WeightSettings(textWeight = 0.7, vectorWeight = 0.3),
    var content: WeightSettings = WeightSettings(textWeight = 0.4, vectorWeight = 0.6)
) {
    data class WeightSettings(
        var textWeight: Double = 0.5,
        var vectorWeight: Double = 0.5
    ) {
        init {
            require(textWeight >= 0.0 && textWeight <= 1.0) {
                "textWeight must be between 0.0 and 1.0"
            }
            require(vectorWeight >= 0.0 && vectorWeight <= 1.0) {
                "vectorWeight must be between 0.0 and 1.0"
            }
        }

        fun combine(textScore: Double, vectorScore: Double): Double {
            return (textScore * textWeight) + (vectorScore * vectorWeight)
        }
    }
}

@ConfigurationProperties(prefix = "search.fields")
data class SearchFieldWeightConfig(
    var keyword: FieldWeights = FieldWeights(
        queryBy = "${MetadataFields.KEYWORDS},${MetadataFields.TITLE},content",
        weights = "10,5,1"
    ),
    var title: FieldWeights = FieldWeights(
        queryBy = "${MetadataFields.TITLE},content,${MetadataFields.KEYWORDS}",
        weights = "10,3,1"
    ),
    var content: FieldWeights = FieldWeights(
        queryBy = "content,${MetadataFields.TITLE},${MetadataFields.KEYWORDS}",
        weights = "10,5,3"
    ),
    var path: FieldWeights = FieldWeights(
        queryBy = MetadataFields.PATH,
        weights = "10"
    )
) {
    data class FieldWeights(
        var queryBy: String,
        var weights: String
    ) {
        fun queryByList(): List<String> {
            return queryBy.split(",").map { it.trim() }
        }

        fun weightsList(): List<Int> {
            return weights.split(",").map { it.trim().toInt() }
        }
    }
}
