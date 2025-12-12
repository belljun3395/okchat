package com.okestro.okchat.email.application

import com.okestro.okchat.email.application.dto.SavePendingReplyUseCaseIn
import com.okestro.okchat.email.application.dto.SavePendingReplyUseCaseOut
import com.okestro.okchat.email.model.entity.PendingEmailReply
import com.okestro.okchat.email.model.entity.ReviewStatus
import com.okestro.okchat.email.repository.PendingEmailReplyRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Service
class SavePendingReplyUseCase(
    private val pendingEmailReplyRepository: PendingEmailReplyRepository
) {
    @Transactional("transactionManager")
    suspend fun execute(useCaseIn: SavePendingReplyUseCaseIn): SavePendingReplyUseCaseOut =
        withContext(Dispatchers.IO + MDCContext()) {
            val messageId = useCaseIn.originalMessage.rawMessage.getHeader("Message-ID")?.firstOrNull()

            val pendingReply = PendingEmailReply(
                fromEmail = useCaseIn.originalMessage.from,
                toEmail = useCaseIn.toEmail,
                originalSubject = useCaseIn.originalMessage.subject,
                originalContent = useCaseIn.originalMessage.content.take(2000),
                replyContent = useCaseIn.replyContent,
                providerType = useCaseIn.providerType,
                messageId = messageId,
                status = ReviewStatus.PENDING,
                createdAt = Instant.now()
            )

            val saved = pendingEmailReplyRepository.save(pendingReply)
            logger.info {
                "Saved pending email reply: id=${saved.id}, from=${saved.fromEmail}, subject=${saved.originalSubject}"
            }
            SavePendingReplyUseCaseOut(saved)
        }
}
