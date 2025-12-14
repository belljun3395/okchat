package com.okestro.okchat.user.repository

import com.okestro.okchat.user.model.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    /**
     * Find user by email address
     */
    fun findByEmail(email: String): User?

    /**
     * Find active users by email
     */
    fun findByEmailAndActive(email: String, active: Boolean = true): User?

    /**
     * Find active users by id
     */
    fun findByIdAndActive(id: Long, active: Boolean = true): User?
}
