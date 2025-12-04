// Type definitions based on backend DTOs

export interface User {
    id: number;
    email: string;
    name: string;
    active: boolean;
    role?: string; // Added for frontend display
}

export interface DocumentPathPermission {
    id: number;
    userId: number;
    path: string;
    spaceKey?: string;
    permissionType: 'GRANT' | 'DENY';
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
