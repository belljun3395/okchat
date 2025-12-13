import apiClient from '../lib/api-client';
import type { KnowledgeBase, KnowledgeBaseUserRole } from '../types';

export interface KnowledgeBaseMember {
    userId: number;
    email: string;
    name: string;
    role: KnowledgeBaseUserRole;
    createdAt: string;
    approvedBy?: string;
}

export interface AddMemberRequest {
    email: string;
    role: KnowledgeBaseUserRole;
}

/**
 * KnowledgeBase Service
 * 
 * APIs:
 * - GET /api/admin/knowledge-bases - List all KBs
 * - GET /api/admin/knowledge-bases/{id}/members - List members
 * - POST /api/admin/knowledge-bases/{id}/members - Add member
 * - DELETE /api/admin/knowledge-bases/{id}/members/{userId} - Remove member
 */
export const knowledgeBaseService = {
    /**
     * Get all Knowledge Bases
     */
    getAll: () => apiClient.get<KnowledgeBase[]>('/api/admin/knowledge-bases'),

    /**
     * Get members of a Knowledge Base
     */
    getMembers: (kbId: number) => apiClient.get<KnowledgeBaseMember[]>(`/api/admin/knowledge-bases/${kbId}/members?callerEmail=admin@okchat.com`),

    /**
     * Add member to Knowledge Base
     */
    addMember: (kbId: number, email: string, role: KnowledgeBaseUserRole) => 
        apiClient.post<void>(`/api/admin/knowledge-bases/${kbId}/members?callerEmail=admin@okchat.com`, { email, role }),

    /**
     * Create a new Knowledge Base
     */
    create: (data: CreateKnowledgeBasePayload) => 
        apiClient.post<KnowledgeBase>('/api/admin/knowledge-bases?callerEmail=admin@okchat.com', data),

    /**
     * Remove member from Knowledge Base
     */
    removeMember: (kbId: number, userId: number) => 
        apiClient.delete<void>(`/api/admin/knowledge-bases/${kbId}/members/${userId}?callerEmail=admin@okchat.com`)
};

export interface CreateKnowledgeBasePayload {
    name: string;
    description?: string;
    type: string;
    config?: Record<string, unknown>;
}
