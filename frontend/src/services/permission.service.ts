import apiClient from '../lib/api-client';
import type { UserPermissionsResponse, PathDetailResponse, User } from '../types';

export interface BulkGrantPathPermissionRequest {
    userEmail: string;
    documentPaths: string[];
    spaceKey?: string;
}

export interface RevokeBulkPathPermissionRequest {
    userEmail: string;
    documentPaths: string[];
}

export interface PermissionResponse {
    success: boolean;
    message: string;
}

export interface BulkPermissionResponse {
    success: boolean;
    message: string;
    grantedCount?: number;
    totalRequested?: number;
}

export interface UserPermissionStat {
    user: User;
    permissionCount: number;
}

/**
 * Permission Management Service
 *
 * APIs:
 * - GET /api/admin/permissions/paths - Get all document paths
 * - GET /api/admin/permissions/user/{email} - Get user permissions
 * - POST /api/admin/permissions/path/bulk - Grant permissions
 * - DELETE /api/admin/permissions/path/bulk - Revoke permissions
 * - POST /api/admin/permissions/path/bulk/deny - Deny permissions
 * - GET /api/admin/permissions/users - Get all users with permissions
 * - DELETE /api/admin/permissions/user/{email} - Revoke all permissions for user
 * - GET /api/admin/permissions/path/detail - Get path details
 */
export const permissionService = {
    /**
     * Get all document paths
     */
    getAllPaths: (knowledgeBaseId?: number) => apiClient.get<string[]>('/api/admin/permissions/paths', { params: { knowledgeBaseId } }),

    /**
     * Get user permissions
     */
    getUserPermissions: (email: string) =>
        apiClient.get<UserPermissionsResponse>(`/api/admin/permissions/user/${encodeURIComponent(email)}`),

    /**
     * Grant permissions (bulk)
     */
    grantBulk: (data: BulkGrantPathPermissionRequest) =>
        apiClient.post<BulkPermissionResponse>('/api/admin/permissions/path/bulk', data),

    /**
     * Revoke permissions (bulk)
     */
    revokeBulk: (data: RevokeBulkPathPermissionRequest) =>
        apiClient.delete<PermissionResponse>('/api/admin/permissions/path/bulk', { data }),

    /**
     * Deny permissions (bulk)
     */
    denyBulk: (data: BulkGrantPathPermissionRequest) =>
        apiClient.post<BulkPermissionResponse>('/api/admin/permissions/path/bulk/deny', data),

    /**
     * Get all users with permission statistics
     */
    getAllUsersWithPermissions: () =>
        apiClient.get<UserPermissionStat[]>('/api/admin/permissions/users'),

    /**
     * Revoke all permissions for a user
     */
    revokeAllForUser: (email: string) =>
        apiClient.delete<PermissionResponse>(`/api/admin/permissions/user/${encodeURIComponent(email)}`),

    /**
     * Get path details
     */
    getPathDetail: (path: string) =>
        apiClient.get<PathDetailResponse>('/api/admin/permissions/path/detail', {
            params: { path }
        })
};
