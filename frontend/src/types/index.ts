// Type definitions based on backend DTOs

export interface User {
    id: number;
    email: string;
    name: string;
    active: boolean;
    role: string;
    createdAt: string;
    updatedAt: string;
}

export type PermissionLevel = 'READ' | 'WRITE' | 'ADMIN' | 'DENY';

export interface DocumentPathPermission {
    id: number;
    userId: number;
    documentPath: string;
    spaceKey?: string;
    permissionLevel: PermissionLevel;
    grantedAt: string;
    grantedBy?: number;
}

export interface DocumentSearchResult {
    id: string;
    title: string;
    path: string;
    spaceKey: string;
    url: string;
}

export interface PathDetailResponse {
    path: string;
    documents: DocumentSearchResult[];
    usersWithAccess: User[];
    totalDocuments: number;
    totalUsers: number;
}

export interface UserPermissionsResponse {
    user: User;
    pathPermissions: DocumentPathPermission[];
    totalDocuments: number;
}

export interface KnowledgeBase {
    id: number;
    name: string;
    description: string;
    type: string;
    enabled: boolean;
    createdBy: number;
}

export type KnowledgeBaseUserRole = 'MEMBER' | 'ADMIN';

export interface KnowledgeBaseUser {
    userId: number;
    role: KnowledgeBaseUserRole;
    createdAt: string;
}
