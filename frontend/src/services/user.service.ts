import apiClient from '../lib/api-client';
import type { User } from '../types';

export interface CreateUserRequest {
    email: string;
    name: string;
}

export interface UserResponse {
    success: boolean;
    message: string;
}

/**
 * User Management Service
 *
 * APIs:
 * - GET /admin/users - Get all active users
 * - GET /admin/users/{email} - Get user by email
 * - POST /admin/users - Create or update user
 * - DELETE /admin/users/{email} - Deactivate user
 */
export const userService = {
    /**
     * Get all active users
     */
    getAll: () => apiClient.get<User[]>('/admin/users'),

    /**
     * Get user by email
     */
    getByEmail: (email: string) => apiClient.get<User>(`/admin/users/${encodeURIComponent(email)}`),

    /**
     * Create or update user
     */
    create: (data: CreateUserRequest) => apiClient.post<User>('/admin/users', data),

    /**
     * Deactivate user
     */
    deactivate: (email: string) => apiClient.delete<UserResponse>(`/admin/users/${encodeURIComponent(email)}`)
};
