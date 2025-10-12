package com.okestro.okchat.permission.service

import com.okestro.okchat.permission.model.DocumentPathPermission
import com.okestro.okchat.permission.model.PermissionLevel
import com.okestro.okchat.permission.repository.DocumentPathPermissionRepository
import com.okestro.okchat.search.model.SearchResult
import com.okestro.okchat.search.model.SearchScore
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("PermissionService Integration Tests")
class PermissionServiceTest {

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
        }
    }

    @Autowired
    private lateinit var documentPathPermissionRepository: DocumentPathPermissionRepository

    private lateinit var permissionService: PermissionService

    private val userId = 1L

    @BeforeEach
    fun setUp() {
        permissionService = PermissionService(documentPathPermissionRepository)
    }

    @AfterEach
    fun tearDown() {
        documentPathPermissionRepository.deleteAll()
    }

    @Nested
    @DisplayName("filterSearchResults Tests")
    inner class FilterSearchResultsTests {

        @Test
        @DisplayName("should filter out documents without permissions")
        fun `should filter out documents without permissions`() {
            // given
            val results = listOf(
                createSearchResult("팀회의 > 2025 > 1월"),
                createSearchResult("프로젝트 > A")
            )

            // when
            val filtered = permissionService.filterSearchResults(results, userId)

            // then
            filtered.shouldBeEmpty()
        }

        @Test
        @DisplayName("should return documents with READ permission")
        fun `should return documents with READ permission`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의", permissionLevel = PermissionLevel.READ)
            )
            val results = listOf(
                createSearchResult("팀회의 > 2025 > 1월"),
                createSearchResult("프로젝트 > A")
            )

            // when
            val filtered = permissionService.filterSearchResults(results, userId)

            // then
            filtered shouldHaveSize 1
            filtered[0].path shouldBe "팀회의 > 2025 > 1월"
        }

        @Test
        @DisplayName("should filter out documents with DENY permission")
        fun `should filter out documents with DENY permission`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의", permissionLevel = PermissionLevel.DENY)
            )
            val results = listOf(
                createSearchResult("팀회의 > 2025 > 1월")
            )

            // when
            val filtered = permissionService.filterSearchResults(results, userId)

            // then
            filtered.shouldBeEmpty()
        }

        @Test
        @DisplayName("Most Specific Wins - child READ overrides parent DENY")
        fun `should use most specific permission - child READ overrides parent DENY`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "업무일지", permissionLevel = PermissionLevel.DENY)
            )
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "업무일지 > 김종준", permissionLevel = PermissionLevel.READ)
            )

            val results = listOf(
                createSearchResult("업무일지 > 김종준 > 2025"),
                createSearchResult("업무일지 > 다른사람")
            )

            // when
            val filtered = permissionService.filterSearchResults(results, userId)

            // then
            filtered shouldHaveSize 1
            filtered[0].path shouldBe "업무일지 > 김종준 > 2025"
        }

        @Test
        @DisplayName("Most Specific Wins - child DENY overrides parent READ")
        fun `should use most specific permission - child DENY overrides parent READ`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의", permissionLevel = PermissionLevel.READ)
            )
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의 > 비밀", permissionLevel = PermissionLevel.DENY)
            )

            val results = listOf(
                createSearchResult("팀회의 > 2025"),
                createSearchResult("팀회의 > 비밀 > 문서")
            )

            // when
            val filtered = permissionService.filterSearchResults(results, userId)

            // then
            filtered shouldHaveSize 1
            filtered[0].path shouldBe "팀회의 > 2025"
        }

        @Test
        @DisplayName("should return empty list for empty results")
        fun `should return empty list for empty results`() {
            // when
            val filtered = permissionService.filterSearchResults(emptyList(), userId)

            // then
            filtered.shouldBeEmpty()
        }
    }

    @Nested
    @DisplayName("grantPathPermission Tests")
    inner class GrantPathPermissionTests {

        @Test
        @DisplayName("should grant new READ permission")
        fun `should grant new READ permission`() {
            // when
            val permission = permissionService.grantPathPermission(userId, "팀회의", "TEAM", 999L)

            // then
            permission.userId shouldBe userId
            permission.documentPath shouldBe "팀회의"
            permission.spaceKey shouldBe "TEAM"
            permission.permissionLevel shouldBe PermissionLevel.READ
            permission.grantedBy shouldBe 999L
        }

        @Test
        @DisplayName("should return existing permission if already exists")
        fun `should return existing permission if already exists`() {
            // given
            val existing = documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의")
            )

            // when
            val result = permissionService.grantPathPermission(userId, "팀회의")

            // then
            result.id shouldBe existing.id
        }

        @Test
        @DisplayName("should clean up redundant child READ permissions when granting parent permission")
        fun `should clean up redundant child READ permissions when granting parent permission`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의 > 2025", permissionLevel = PermissionLevel.READ)
            )
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의 > 2025 > 1월", permissionLevel = PermissionLevel.READ)
            )
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의 > 2025 > 비밀", permissionLevel = PermissionLevel.DENY)
            )

            // when
            permissionService.grantPathPermission(userId, "팀회의")

            // then
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 2 // "팀회의" READ + "팀회의 > 2025 > 비밀" DENY
            permissions.map { it.documentPath } shouldContain "팀회의"
            permissions.map { it.documentPath } shouldContain "팀회의 > 2025 > 비밀"
            permissions.map { it.documentPath } shouldNotContain "팀회의 > 2025"
            permissions.map { it.documentPath } shouldNotContain "팀회의 > 2025 > 1월"
        }
    }

    @Nested
    @DisplayName("grantDenyPathPermission Tests")
    inner class GrantDenyPathPermissionTests {

        @Test
        @DisplayName("should grant new DENY permission")
        fun `should grant new DENY permission`() {
            // when
            val permission = permissionService.grantDenyPathPermission(userId, "비밀문서")

            // then
            permission.userId shouldBe userId
            permission.documentPath shouldBe "비밀문서"
            permission.permissionLevel shouldBe PermissionLevel.DENY
        }

        @Test
        @DisplayName("should replace READ with DENY for same path")
        fun `should replace READ with DENY for same path`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "팀회의", permissionLevel = PermissionLevel.READ)
            )

            // when
            permissionService.grantDenyPathPermission(userId, "팀회의")

            // then
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 1
            permissions[0].permissionLevel shouldBe PermissionLevel.DENY
        }

        @Test
        @DisplayName("should keep child READ permissions as exceptions to DENY")
        fun `should keep child READ permissions as exceptions to DENY`() {
            // given
            documentPathPermissionRepository.save(
                DocumentPathPermission(userId = userId, documentPath = "업무일지 > 김종준", permissionLevel = PermissionLevel.READ)
            )

            // when
            permissionService.grantDenyPathPermission(userId, "업무일지")

            // then
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 2
            permissions.map { it.documentPath } shouldContainExactlyInAnyOrder listOf("업무일지", "업무일지 > 김종준")
        }
    }

    @Nested
    @DisplayName("Permission Management Tests")
    inner class PermissionManagementTests {

        @Test
        @DisplayName("getUserPathPermissions should return all permissions for user")
        fun `getUserPathPermissions should return all permissions for user`() {
            // given
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "팀회의"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "프로젝트"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = 999L, documentPath = "다른사람"))

            // when
            val permissions = permissionService.getUserPathPermissions(userId)

            // then
            permissions shouldHaveSize 2
            permissions.map { it.documentPath } shouldContainExactlyInAnyOrder listOf("팀회의", "프로젝트")
        }

        @Test
        @DisplayName("getPathPermissions should return all permissions for path")
        fun `getPathPermissions should return all permissions for path`() {
            // given
            documentPathPermissionRepository.save(DocumentPathPermission(userId = 1L, documentPath = "팀회의"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = 2L, documentPath = "팀회의"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = 3L, documentPath = "프로젝트"))

            // when
            val permissions = permissionService.getPathPermissions("팀회의")

            // then
            permissions shouldHaveSize 2
            permissions.map { it.userId } shouldContainExactlyInAnyOrder listOf(1L, 2L)
        }

        @Test
        @DisplayName("revokeAllPermissionsForUser should delete all permissions")
        fun `revokeAllPermissionsForUser should delete all permissions`() {
            // given
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "팀회의"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "프로젝트"))

            // when
            permissionService.revokeAllPermissionsForUser(userId)

            // then
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions.shouldBeEmpty()
        }

        @Test
        @DisplayName("revokeBulkPathPermissions should delete multiple permissions")
        fun `revokeBulkPathPermissions should delete multiple permissions`() {
            // given
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "팀회의"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "프로젝트"))
            documentPathPermissionRepository.save(DocumentPathPermission(userId = userId, documentPath = "업무일지"))

            // when
            permissionService.revokeBulkPathPermissions(userId, listOf("팀회의", "프로젝트"))

            // then
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 1
            permissions[0].documentPath shouldBe "업무일지"
        }

        @Test
        @DisplayName("grantBulkPathPermissions should grant multiple permissions")
        fun `grantBulkPathPermissions should grant multiple permissions`() {
            // when
            val count = permissionService.grantBulkPathPermissions(
                userId,
                listOf("팀회의", "프로젝트", "업무일지"),
                "TEAM",
                999L
            )

            // then
            count shouldBe 3
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 3
            permissions.map { it.documentPath } shouldContainExactlyInAnyOrder listOf("팀회의", "프로젝트", "업무일지")
        }

        @Test
        @DisplayName("grantBulkDenyPathPermissions should grant multiple DENY permissions")
        fun `grantBulkDenyPathPermissions should grant multiple DENY permissions`() {
            // when
            val count = permissionService.grantBulkDenyPathPermissions(
                userId,
                listOf("비밀1", "비밀2")
            )

            // then
            count shouldBe 2
            val permissions = documentPathPermissionRepository.findByUserId(userId)
            permissions shouldHaveSize 2
            permissions.all { it.permissionLevel == PermissionLevel.DENY } shouldBe true
        }
    }

    private fun createSearchResult(path: String): SearchResult {
        return SearchResult(
            id = "id-${path.hashCode()}",
            title = "Title for $path",
            content = "Content for $path",
            path = path,
            spaceKey = "TEST",
            score = SearchScore.fromSimilarity(0.75)
        )
    }
}
