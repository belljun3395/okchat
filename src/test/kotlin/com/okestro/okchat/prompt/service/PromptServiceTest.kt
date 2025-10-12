package com.okestro.okchat.prompt.service

import com.okestro.okchat.prompt.model.Prompt
import com.okestro.okchat.prompt.repository.PromptRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("PromptService 단위 테스트")
class PromptServiceTest {

    private lateinit var promptRepository: PromptRepository
    private lateinit var promptCacheService: PromptCacheService
    private lateinit var promptService: PromptService

    @BeforeEach
    fun setUp() {
        promptRepository = mockk()
        promptCacheService = mockk()
        promptService = PromptService(promptRepository, promptCacheService)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("getPrompt - 프롬프트 조회")
    inner class GetPromptTests {

        @Test
        @DisplayName("버전 지정 시 해당 버전 프롬프트 반환")
        fun `should return prompt with specific version`() = runTest {
            // given
            val prompt = createPrompt(type = "test", version = 2, content = "Version 2 content")
            every { promptRepository.findByTypeAndVersionAndActive("test", 2) } returns prompt

            // when
            val result = promptService.getPrompt("test", 2)

            // then
            result.shouldNotBeNull()
            result shouldBe "Version 2 content"
            verify(exactly = 1) { promptRepository.findByTypeAndVersionAndActive("test", 2) }
            coVerify(exactly = 0) { promptCacheService.getLatestPrompt(any()) }
        }

        @Test
        @DisplayName("버전 미지정 시 캐시에서 최신 버전 조회")
        fun `should return latest prompt from cache when version not specified`() = runTest {
            // given
            coEvery { promptCacheService.getLatestPrompt("test") } returns "Cached content"

            // when
            val result = promptService.getPrompt("test")

            // then
            result.shouldNotBeNull()
            result shouldBe "Cached content"
            coVerify(exactly = 1) { promptCacheService.getLatestPrompt("test") }
            verify(exactly = 0) { promptRepository.findLatestByTypeAndActive(any()) }
        }

        @Test
        @DisplayName("버전 미지정 + 캐시 미스 시 DB에서 조회 후 캐싱")
        fun `should fetch from DB and cache when cache miss`() = runTest {
            // given
            val prompt = createPrompt(type = "test", version = 3, content = "Latest content")
            coEvery { promptCacheService.getLatestPrompt("test") } returns null
            every { promptRepository.findLatestByTypeAndActive("test") } returns prompt
            coEvery { promptCacheService.cacheLatestPrompt("test", "Latest content") } just Runs

            // when
            val result = promptService.getPrompt("test")

            // then
            result.shouldNotBeNull()
            result shouldBe "Latest content"
            coVerify(exactly = 1) { promptCacheService.getLatestPrompt("test") }
            verify(exactly = 1) { promptRepository.findLatestByTypeAndActive("test") }
            coVerify(exactly = 1) { promptCacheService.cacheLatestPrompt("test", "Latest content") }
        }

        @Test
        @DisplayName("프롬프트가 존재하지 않으면 null 반환")
        fun `should return null when prompt not found`() = runTest {
            // given
            every { promptRepository.findByTypeAndVersionAndActive("nonexistent", 1) } returns null

            // when
            val result = promptService.getPrompt("nonexistent", 1)

            // then
            result.shouldBeNull()
        }
    }

    @Nested
    @DisplayName("createPrompt Tests")
    inner class CreatePromptTests {

        @Test
        @DisplayName("should create first prompt with version 1")
        fun `should create first prompt with version 1`() = runTest {
            // given
            every { promptRepository.findLatestByTypeAndActive("new") } returns null
            val savedPrompt = createPrompt(type = "new", version = 1, content = "New content")
            every { promptRepository.save(any()) } returns savedPrompt
            coEvery { promptCacheService.cacheLatestPrompt("new", "New content") } just Runs

            // when
            val result = promptService.createPrompt("new", "New content")

            // then
            result.version shouldBe 1
            result.type shouldBe "new"
            result.content shouldBe "New content"
            result.active shouldBe true
            verify(exactly = 1) { promptRepository.save(any()) }
            coVerify(exactly = 1) { promptCacheService.cacheLatestPrompt("new", "New content") }
        }

        @Test
        @DisplayName("should increment version and deactivate previous version")
        fun `should increment version and deactivate previous version`() = runTest {
            // given
            val existingPrompt = createPrompt(id = 1L, type = "existing", version = 2, content = "Old content")
            every { promptRepository.findLatestByTypeAndActive("existing") } returns existingPrompt
            every { promptRepository.deactivatePrompt(1L) } returns 1
            val newPrompt = createPrompt(id = 2L, type = "existing", version = 3, content = "New content")
            every { promptRepository.save(any()) } returns newPrompt
            coEvery { promptCacheService.cacheLatestPrompt("existing", "New content") } just Runs

            // when
            val result = promptService.createPrompt("existing", "New content")

            // then
            result.version shouldBe 3
            result.content shouldBe "New content"
            verify(exactly = 1) { promptRepository.deactivatePrompt(1L) }
            verify(exactly = 1) { promptRepository.save(any()) }
            coVerify(exactly = 1) { promptCacheService.cacheLatestPrompt("existing", "New content") }
        }
    }

    @Nested
    @DisplayName("updateLatestPrompt Tests")
    inner class UpdateLatestPromptTests {

        @Test
        @DisplayName("should create new version when updating latest prompt")
        fun `should create new version when updating latest prompt`() = runTest {
            // given
            val existingPrompt = createPrompt(id = 1L, type = "update", version = 5, content = "Old")
            every { promptRepository.findLatestByTypeAndActive("update") } returns existingPrompt
            every { promptRepository.deactivatePrompt(1L) } returns 1
            val newPrompt = createPrompt(id = 2L, type = "update", version = 6, content = "Updated")
            every { promptRepository.save(any()) } returns newPrompt
            coEvery { promptCacheService.cacheLatestPrompt("update", "Updated") } just Runs

            // when
            val result = promptService.updateLatestPrompt("update", "Updated")

            // then
            result.version shouldBe 6
            result.content shouldBe "Updated"
            verify(exactly = 1) { promptRepository.deactivatePrompt(1L) }
            coVerify(exactly = 1) { promptCacheService.cacheLatestPrompt("update", "Updated") }
        }

        @Test
        @DisplayName("should throw exception when updating non-existing prompt type")
        fun `should throw exception when updating non-existing prompt type`() = runTest {
            // given
            every { promptRepository.findLatestByTypeAndActive("nonexistent") } returns null

            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                promptService.updateLatestPrompt("nonexistent", "Content")
            }
            exception.message shouldContain "does not exist"
            exception.message shouldContain "Use createPrompt first"
        }
    }

    @Nested
    @DisplayName("deactivatePrompt - 프롬프트 비활성화")
    inner class DeactivatePromptTests {

        @Test
        @DisplayName("특정 버전 프롬프트 비활성화")
        fun `should deactivate specific version of prompt`() = runTest {
            // given
            val prompt = createPrompt(id = 1L, type = "test", version = 2, content = "Content")
            every { promptRepository.findByTypeAndVersionAndActive("test", 2) } returns prompt
            every { promptRepository.deactivatePrompt(1L) } returns 1
            every { promptRepository.findLatestByTypeAndActive("test") } returns null

            // when
            promptService.deactivatePrompt("test", 2)

            // then
            verify(exactly = 1) { promptRepository.deactivatePrompt(1L) }
        }

        @Test
        @DisplayName("최신 버전 비활성화 시 캐시 삭제")
        fun `should evict cache when deactivating latest version`() = runTest {
            // given
            val prompt = createPrompt(id = 1L, type = "test", version = 3, content = "Latest")
            every { promptRepository.findByTypeAndVersionAndActive("test", 3) } returns prompt
            every { promptRepository.deactivatePrompt(1L) } returns 1
            every { promptRepository.findLatestByTypeAndActive("test") } returns prompt
            coEvery { promptCacheService.evictLatestPromptCache("test") } just Runs

            // when
            promptService.deactivatePrompt("test", 3)

            // then
            verify(exactly = 1) { promptRepository.deactivatePrompt(1L) }
            coVerify(exactly = 1) { promptCacheService.evictLatestPromptCache("test") }
        }

        @Test
        @DisplayName("존재하지 않는 프롬프트 비활성화 시 예외 발생")
        fun `should throw exception when deactivating non-existing prompt`() = runTest {
            // given
            every { promptRepository.findByTypeAndVersionAndActive("test", 999) } returns null

            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                promptService.deactivatePrompt("test", 999)
            }
            exception.message shouldContain "Prompt not found"
        }
    }

    @Nested
    @DisplayName("Other Query Methods")
    inner class OtherQueryTests {

        @Test
        @DisplayName("getAllVersions should return all versions of prompt type")
        fun `should return all versions of prompt type`() = runTest {
            // given
            val prompts = listOf(
                createPrompt(type = "test", version = 3, content = "V3"),
                createPrompt(type = "test", version = 2, content = "V2"),
                createPrompt(type = "test", version = 1, content = "V1")
            )
            every { promptRepository.findAllByTypeAndActiveOrderByVersionDesc("test") } returns prompts

            // when
            val result = promptService.getAllVersions("test")

            // then
            result shouldHaveSize 3
            result[0].version shouldBe 3
            result[1].version shouldBe 2
            result[2].version shouldBe 1
        }

        @Test
        @DisplayName("getLatestVersion should return latest version number")
        fun `should return latest version number`() = runTest {
            // given
            every { promptRepository.findLatestVersionByType("test") } returns 7

            // when
            val result = promptService.getLatestVersion("test")

            // then
            result shouldBe 7
        }

        @Test
        @DisplayName("exists should check if prompt type exists")
        fun `should check if prompt type exists`() = runTest {
            // given
            every { promptRepository.findLatestByTypeAndActive("existing") } returns createPrompt()
            every { promptRepository.findLatestByTypeAndActive("nonexistent") } returns null

            // when
            val exists = promptService.exists("existing")
            val notExists = promptService.exists("nonexistent")

            // then
            exists shouldBe true
            notExists shouldBe false
        }

        @Test
        @DisplayName("getLatestPrompt should delegate to getPrompt")
        fun `should delegate to getPrompt for latest prompt`() = runTest {
            // given
            coEvery { promptCacheService.getLatestPrompt("test") } returns "Cached"

            // when
            val result = promptService.getLatestPrompt("test")

            // then
            result shouldBe "Cached"
        }

        @Test
        @DisplayName("getPromptByVersion should delegate to getPrompt")
        fun `should delegate to getPrompt for specific version`() = runTest {
            // given
            val prompt = createPrompt(version = 5, content = "V5")
            every { promptRepository.findByTypeAndVersionAndActive("test", 5) } returns prompt

            // when
            val result = promptService.getPromptByVersion("test", 5)

            // then
            result shouldBe "V5"
        }
    }

    private fun createPrompt(
        id: Long? = null,
        type: String = "test",
        version: Int = 1,
        content: String = "Test content",
        active: Boolean = true
    ): Prompt {
        return Prompt(
            id = id,
            type = type,
            version = version,
            content = content,
            active = active,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}
