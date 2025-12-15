package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.CountByStatusUseCaseIn
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class CountByStatusUseCaseTest : BehaviorSpec({

    val pendingEmailReplyRepository = mockk<PendingEmailReplyRepository>()
    val useCase = CountByStatusUseCase(pendingEmailReplyRepository)

    afterEach {
        clearAllMocks()
    }

    given("Replies exist for a specific status") {
        every { pendingEmailReplyRepository.countByStatus(ReviewStatus.PENDING) } returns 4L

        `when`("execute is called with the status") {
            val result = useCase.execute(CountByStatusUseCaseIn(ReviewStatus.PENDING))

            then("it should return the same count as the service") {
                result.count shouldBe 4L
                verify(exactly = 1) { pendingEmailReplyRepository.countByStatus(ReviewStatus.PENDING) }
            }
        }
    }

    given("No replies exist for another status") {
        every { pendingEmailReplyRepository.countByStatus(ReviewStatus.REJECTED) } returns 0L

        `when`("the use case runs") {
            val result = useCase.execute(CountByStatusUseCaseIn(ReviewStatus.REJECTED))

            then("the count should be zero") {
                result.count shouldBe 0L
                verify(exactly = 1) { pendingEmailReplyRepository.countByStatus(ReviewStatus.REJECTED) }
            }
        }
    }
})
