package com.okestro.okchat.search.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DocumentUtils Extension Function Tests")
class DocumentUtilsTest {

    @Test
    @DisplayName("extractChunk should remove chunk suffix from document ID")
    fun `extractChunk should remove chunk suffix from document ID`() {
        // given
        val docIdWithChunk = "doc123_chunk_1"

        // when
        val result = docIdWithChunk.extractChunk()

        // then
        result shouldBe "doc123"
    }

    @Test
    @DisplayName("extractChunk should handle multiple chunk patterns")
    fun `extractChunk should handle multiple chunk patterns`() {
        // given
        val docId1 = "doc456_chunk_5"
        val docId2 = "doc789_chunk_10"

        // when
        val result1 = docId1.extractChunk()
        val result2 = docId2.extractChunk()

        // then
        result1 shouldBe "doc456"
        result2 shouldBe "doc789"
    }

    @Test
    @DisplayName("extractChunk should return original string when no chunk suffix")
    fun `extractChunk should return original string when no chunk suffix`() {
        // given
        val normalDocId = "doc123"

        // when
        val result = normalDocId.extractChunk()

        // then
        result shouldBe "doc123"
    }

    @Test
    @DisplayName("extractChunk should handle empty string")
    fun `extractChunk should handle empty string`() {
        // given
        val emptyString = ""

        // when
        val result = emptyString.extractChunk()

        // then
        result shouldBe ""
    }

    @Test
    @DisplayName("extractChunk should handle string with chunk in middle")
    fun `extractChunk should handle string with chunk in middle`() {
        // given
        val complexId = "prefix_chunk_suffix_chunk_end"

        // when
        val result = complexId.extractChunk()

        // then
        result shouldBe "prefix"
    }
}
