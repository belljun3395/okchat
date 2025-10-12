package com.okestro.okchat.user.service

import com.okestro.okchat.user.model.User
import com.okestro.okchat.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
@DisplayName("UserService Integration Tests")
class UserServiceTest {

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
    private lateinit var userRepository: UserRepository

    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    @AfterEach
    fun tearDown() {
        userRepository.deleteAll()
    }

    @Test
    @DisplayName("findByEmail should return active user")
    fun `findByEmail should return active user`() {
        // given
        val email = "test@example.com"
        val savedUser = userRepository.save(User(email = email, name = "Test User", active = true))

        // when
        val result = userService.findByEmail(email)

        // then
        result.shouldNotBeNull()
        result.email shouldBe email
        result.id shouldBe savedUser.id
        result.active shouldBe true
    }

    @Test
    @DisplayName("findByEmail should return null for inactive user")
    fun `findByEmail should return null for inactive user`() {
        // given
        val email = "inactive@example.com"
        userRepository.save(User(email = email, name = "Inactive User", active = false))

        // when
        val result = userService.findByEmail(email)

        // then
        result.shouldBeNull()
    }

    @Test
    @DisplayName("findByEmail should return null for non-existing user")
    fun `findByEmail should return null for non-existing user`() {
        // when
        val result = userService.findByEmail("nonexistent@example.com")

        // then
        result.shouldBeNull()
    }

    @Test
    @DisplayName("findOrCreateUser should return existing user")
    fun `findOrCreateUser should return existing user`() {
        // given
        val email = "existing@example.com"
        val existingUser = userRepository.save(User(email = email, name = "Existing User"))

        // when
        val result = userService.findOrCreateUser(email, "New Name")

        // then
        result.id shouldBe existingUser.id
        result.name shouldBe existingUser.name // Should keep original name
    }

    @Test
    @DisplayName("findOrCreateUser should create new user with provided name")
    fun `findOrCreateUser should create new user with provided name`() {
        // given
        val email = "new@example.com"
        val name = "New User"

        // when
        val result = userService.findOrCreateUser(email, name)

        // then
        result.id.shouldNotBeNull()
        result.email shouldBe email
        result.name shouldBe name
        result.active shouldBe true

        // Verify user was saved to database
        val savedUser = userRepository.findByEmail(email)
        savedUser.shouldNotBeNull()
        savedUser.id shouldBe result.id
    }

    @Test
    @DisplayName("findOrCreateUser should create new user with email username when name not provided")
    fun `findOrCreateUser should create new user with email username when name not provided`() {
        // given
        val email = "newuser@example.com"

        // when
        val result = userService.findOrCreateUser(email)

        // then
        result.id.shouldNotBeNull()
        result.email shouldBe email
        result.name shouldBe "newuser" // Should use email prefix
        result.active shouldBe true
    }

    @Test
    @DisplayName("getAllActiveUsers should return only active users")
    fun `getAllActiveUsers should return only active users`() {
        // given
        userRepository.save(User(email = "active1@example.com", name = "Active 1", active = true))
        userRepository.save(User(email = "active2@example.com", name = "Active 2", active = true))
        userRepository.save(User(email = "inactive@example.com", name = "Inactive", active = false))

        // when
        val result = userService.getAllActiveUsers()

        // then
        result shouldHaveSize 2
        result.map { it.email } shouldContain "active1@example.com"
        result.map { it.email } shouldContain "active2@example.com"
        result.none { it.email == "inactive@example.com" } shouldBe true
    }

    @Test
    @DisplayName("deactivateUser should mark user as inactive")
    fun `deactivateUser should mark user as inactive`() {
        // given
        val user = userRepository.save(User(email = "active@example.com", name = "Active User", active = true))

        // when
        val userId = user.id!!
        userService.deactivateUser(userId)

        // then
        val deactivatedUser = userRepository.findById(userId).orElseThrow()
        deactivatedUser.active shouldBe false
    }

    @Test
    @DisplayName("deactivateUser should throw exception for non-existing user")
    fun `deactivateUser should throw exception for non-existing user`() {
        // when & then
        val exception = shouldThrow<IllegalArgumentException> {
            userService.deactivateUser(999L)
        }
        exception.message shouldContain "User not found"
    }

    @Test
    @DisplayName("getAllActiveUsers should return empty list when no active users")
    fun `getAllActiveUsers should return empty list when no active users`() {
        // given
        userRepository.save(User(email = "inactive@example.com", name = "Inactive", active = false))

        // when
        val result = userService.getAllActiveUsers()

        // then
        result shouldHaveSize 0
    }
}
