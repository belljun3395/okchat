package com.okestro.okchat.prompt.application

import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseIn
import com.okestro.okchat.prompt.application.dto.CheckPromptExistsUseCaseOut
import com.okestro.okchat.prompt.repository.PromptRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service

@Service
class CheckPromptExistsUseCase(
    private val promptRepository: PromptRepository
) {
    suspend fun execute(useCaseIn: CheckPromptExistsUseCaseIn): CheckPromptExistsUseCaseOut =
        withContext(Dispatchers.IO) {
            val exists = promptRepository.findLatestByTypeAndActive(useCaseIn.type) != null
            CheckPromptExistsUseCaseOut(exists)
        }
}
