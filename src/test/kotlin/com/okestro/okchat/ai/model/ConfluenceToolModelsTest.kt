package com.okestro.okchat.ai.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ConfluenceToolModelsTest {

    @Test
    fun `GetAllChildPagesInput should validate maxDepth within range`() {
        // Given
        val validInput = GetAllChildPagesInput(pageId = "123", maxDepth = 5)
        val tooSmallInput = GetAllChildPagesInput(pageId = "123", maxDepth = 0)
        val tooLargeInput = GetAllChildPagesInput(pageId = "123", maxDepth = 100)

        // Then
        assertEquals(5, validInput.getValidatedMaxDepth())
        assertEquals(1, tooSmallInput.getValidatedMaxDepth()) // coerced to minimum
        assertEquals(20, tooLargeInput.getValidatedMaxDepth()) // coerced to maximum
    }

    @Test
    fun `GetAllChildPagesInput should use default maxDepth`() {
        // Given
        val input = GetAllChildPagesInput(pageId = "123")

        // Then
        assertEquals(10, input.maxDepth)
    }

    @Test
    fun `GetPagesBySpaceIdInput should validate limit within range`() {
        // Given
        val validInput = GetPagesBySpaceIdInput(spaceId = "456", limit = 25)
        val tooSmallInput = GetPagesBySpaceIdInput(spaceId = "456", limit = 0)
        val tooLargeInput = GetPagesBySpaceIdInput(spaceId = "456", limit = 200)

        // Then
        assertEquals(25, validInput.getValidatedLimit())
        assertEquals(1, tooSmallInput.getValidatedLimit()) // coerced to minimum
        assertEquals(100, tooLargeInput.getValidatedLimit()) // coerced to maximum
    }

    @Test
    fun `GetPagesBySpaceIdInput should use default limit`() {
        // Given
        val input = GetPagesBySpaceIdInput(spaceId = "456")

        // Then
        assertEquals(25, input.limit)
    }
}
