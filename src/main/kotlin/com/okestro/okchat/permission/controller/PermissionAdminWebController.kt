package com.okestro.okchat.permission.controller

import com.okestro.okchat.permission.service.DocumentPermissionService
import com.okestro.okchat.permission.service.PermissionService
import com.okestro.okchat.user.service.UserService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

private val log = KotlinLogging.logger {}

/**
 * Web UI controller for permission management
 */
@Controller
@RequestMapping("/admin/permissions")
class PermissionAdminWebController(
    private val userService: UserService,
    private val permissionService: PermissionService,
    private val documentPermissionService: DocumentPermissionService
) {

    /**
     * Permission management home page
     */
    @GetMapping
    fun index(model: Model): String {
        log.info { "Loading permission management page" }

        val users = userService.getAllActiveUsers()
        val paths = documentPermissionService.searchAllPaths()

        model.addAttribute("users", users)
        model.addAttribute("paths", paths)
        model.addAttribute("totalUsers", users.size)
        model.addAttribute("totalPaths", paths.size)

        return "permissions/index"
    }

    /**
     * Advanced permission management page
     */
    @GetMapping("/manage")
    fun manage(model: Model): String {
        log.info { "Loading advanced permission management page" }

        val users = userService.getAllActiveUsers()
        val paths = documentPermissionService.searchAllPaths()

        model.addAttribute("users", users)
        model.addAttribute("paths", paths)

        return "permissions/manage"
    }

    /**
     * User detail page with permissions
     * Shows only path-based permissions (document-specific permissions are automatically covered by paths)
     */
    @GetMapping("/user/{email}")
    fun userDetail(@PathVariable email: String, model: Model): String {
        log.info { "Loading user detail page: $email" }

        val user = userService.findByEmail(email)
        if (user == null) {
            model.addAttribute("error", "User not found: $email")
            return "error"
        }

        val pathPermissions = permissionService.getUserPathPermissions(user.id!!)
        val paths = documentPermissionService.searchAllPaths()

        model.addAttribute("user", user)
        model.addAttribute("pathPermissions", pathPermissions)
        model.addAttribute("paths", paths)
        model.addAttribute("totalPermissions", pathPermissions.size)

        return "permissions/user-detail"
    }

    /**
     * Path detail page with accessible documents
     */
    @GetMapping("/path/{path:.+}")
    fun pathDetail(@PathVariable("path") path: String, model: Model): String {
        log.info { "Loading path detail page: $path" }

        val documents = documentPermissionService.searchAllByPath(path) // Efficiently get docs with titles
        val permissions = permissionService.getPathPermissions(path)

        // Get user details for each permission
        val usersWithAccess = permissions.mapNotNull { perm ->
            userService.getAllActiveUsers().find { it.id == perm.userId }
        }

        model.addAttribute("path", path)
        model.addAttribute("documents", documents)
        model.addAttribute("totalDocuments", documents.size)
        model.addAttribute("usersWithAccess", usersWithAccess)
        model.addAttribute("totalUsers", usersWithAccess.size)

        return "permissions/path-detail"
    }

    /**
     * All paths listing page
     */
    @GetMapping("/paths")
    fun paths(model: Model): String {
        log.info { "Loading all paths page" }

        val paths = documentPermissionService.searchAllPaths()

        // Group paths by hierarchy
        val groupedPaths = paths.groupBy { path ->
            val parts = path.split(" > ")
            if (parts.size > 1) parts[0] else "Root"
        }

        model.addAttribute("paths", paths)
        model.addAttribute("groupedPaths", groupedPaths)
        model.addAttribute("totalPaths", paths.size)

        return "permissions/paths"
    }

    /**
     * All users listing page
     */
    @GetMapping("/users")
    fun users(model: Model): String {
        log.info { "Loading all users page" }

        val users = userService.getAllActiveUsers()

        // Get permission count for each user (path-based only)
        val userPermissions = users.associateWith { user ->
            permissionService.getUserPathPermissions(user.id!!).size
        }

        model.addAttribute("users", users)
        model.addAttribute("userPermissions", userPermissions)
        model.addAttribute("totalUsers", users.size)

        return "permissions/users"
    }
}
