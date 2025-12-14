package com.okestro.okchat.permission.model

enum class PermissionLevel {
    READ, // Can view and search
    WRITE, // Future: Can edit
    ADMIN, // Future: Can manage permissions
    DENY // Explicitly deny access (overrides path-based READ permissions)
}
