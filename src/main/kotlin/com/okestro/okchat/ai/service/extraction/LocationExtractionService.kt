package com.okestro.okchat.ai.service.extraction

import com.okestro.okchat.ai.model.KeyWordExtractionPrompt
import com.okestro.okchat.ai.model.Prompt
import com.okestro.okchat.ai.model.PromptExample
import org.springframework.ai.chat.model.ChatModel
import org.springframework.stereotype.Service

/**
 * Extracts location-related keywords from queries mentioning spaces, paths, or locations.
 *
 * Purpose: Identify Confluence spaces, project names, team names, or file paths
 * Focus: Location indicators, space names, organizational units
 * Output: Confluence spaces, paths, project/team names (empty if no location mentioned)
 *
 * Example: execute("개발팀 스페이스에 있는 회의록")
 *          → ["개발팀 스페이스", "개발팀"]
 */
@Service
class LocationExtractionService(
    chatModel: ChatModel
) : BaseExtractionService(chatModel) {

    override fun buildPrompt(message: String): Prompt {
        val instruction = """
Extract any keywords that specify a location, such as a Confluence space, a project name, a team name, or a file path.
Order them from most important to least important (e.g., a specific path is more important than a general project name).
If no location is mentioned, return an empty string.

- Look for phrases like "in the ... space", "from the ... project", "... 폴더", "... 스페이스".
- Extract the names of teams, projects, or spaces.
        """.trimIndent()

        val examples = listOf(
            PromptExample(
                input = "Find the design document in the 'Mobile App' project folder.",
                output = "Mobile App project, Mobile App"
            ),
            PromptExample(
                input = "개발팀 스페이스에 있는 지난 주 회의록 찾아줘",
                output = "개발팀 스페이스, 개발팀"
            ),
            PromptExample(
                input = "Show me the latest architecture diagram.",
                output = ""
            ),
            PromptExample(
                input = "/docs/infra/networking/ 에서 VPN 관련 문서 찾아줘",
                output = "/docs/infra/networking/"
            )
        )

        return KeyWordExtractionPrompt(
            instruction = instruction,
            examples = examples,
            message = message
        )
    }
}
