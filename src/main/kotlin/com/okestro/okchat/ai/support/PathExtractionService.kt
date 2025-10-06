package com.okestro.okchat.ai.support

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for extracting location-related keywords (e.g., Confluence spaces, paths) from a user query using an LLM.
 */
@Service
class LocationExtractionService(
    private val chatModel: ChatModel
) {

    /**
     * Extracts any mentioned paths, Confluence spaces, or team names that indicate a location.
     */
    suspend fun extractLocationKeywords(message: String): List<String> {
        val locationPrompt = """
            From the user's query, extract any keywords that specify a location, such as a Confluence space, a project name, a team name, or a file path.
            Order them from most important to least important (e.g., a specific path is more important than a general project name).
            If no location is mentioned, return an empty string.

            - Look for phrases like "in the ... space", "from the ... project", "... 폴더", "... 스페이스".
            - Extract the names of teams, projects, or spaces.

            FORMAT: Comma-separated list, ordered from most important to least important.

            EXAMPLES:
            - User Query: "Find the design document in the 'Mobile App' project folder."
              Output: "Mobile App project, Mobile App"
            - User Query: "개발팀 스페이스에 있는 지난 주 회의록 찾아줘"
              Output: "개발팀 스페이스, 개발팀"
            - User Query: "Show me the latest architecture diagram."
              Output: ""
            - User Query: "/docs/infra/networking/ 에서 VPN 관련 문서 찾아줘"
              Output: "/docs/infra/networking/"

            User query: "$message"

            Location Keywords (comma-separated, most important first):
        """.trimIndent()

        return try {
            val options = OpenAiChatOptions.builder()
                .temperature(0.2)
                .maxTokens(100)
                .build()

            val response = chatModel.call(Prompt(locationPrompt, options))
            val keywordsText = response.result.output.text?.trim()
            log.debug { "Extracted location keywords: $keywordsText" }

            if (keywordsText.isNullOrBlank()) {
                emptyList()
            } else {
                keywordsText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
            }
        } catch (e: Exception) {
            log.warn { "Failed to extract location keywords: ${e.message}. Returning empty list." }
            emptyList()
        }
    }
}
