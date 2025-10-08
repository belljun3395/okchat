package com.okestro.okchat.ai.support.extraction

import com.okestro.okchat.ai.support.PromptTemplate
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Service for extracting location-related keywords (e.g., Confluence spaces, paths) from a user query using an LLM.
 * Extends BaseExtractionService to eliminate code duplication.
 *
 * Note: This class is named PathExtractionService but contains LocationExtractionService
 * to match the existing class structure while fixing the ktlint naming issue.
 */
@Service
class PathExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    /**
     * Extracts any mentioned paths, Confluence spaces, or team names that indicate a location.
     */
    suspend fun extractLocationKeywords(message: String): List<String> {
        return execute(message)
    }

    override fun buildPrompt(message: String): String {
        val instruction = """
Extract any keywords that specify a location, such as a Confluence space, a project name, a team name, or a file path.
Order them from most important to least important (e.g., a specific path is more important than a general project name).
If no location is mentioned, return an empty string.

- Look for phrases like "in the ... space", "from the ... project", "... 폴더", "... 스페이스".
- Extract the names of teams, projects, or spaces.
        """.trimIndent()

        val examples = PromptTemplate.formatExamples(
            "Find the design document in the 'Mobile App' project folder." to "Mobile App project, Mobile App",
            "개발팀 스페이스에 있는 지난 주 회의록 찾아줘" to "개발팀 스페이스, 개발팀",
            "Show me the latest architecture diagram." to "",
            "/docs/infra/networking/ 에서 VPN 관련 문서 찾아줘" to "/docs/infra/networking/"
        )

        return PromptTemplate.buildExtractionPrompt(instruction, examples, message)
    }
}
