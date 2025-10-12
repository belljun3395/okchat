package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.DeletePendingReplyUseCaseIn
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify

class DeletePendingReplyUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = DeletePendingReplyUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    given("A pending reply is deleted") {
        every { pendingEmailReplyRepository.deleteById(42L) } just runs

        `when`("execute is invoked") {
            val result = useCase.execute(DeletePendingReplyUseCaseIn(42L))

            then("it should signal success and call the service") {
                result.success.shouldBeTrue()
                verify(exactly = 1) { pendingEmailReplyRepository.deleteById(42L) }
            }
        }
    }
})
