import apiClient from '../lib/api-client';

export type EmailStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SENT' | 'FAILED';
export type EmailProviderType = 'OUTLOOK' | 'GMAIL';

export interface PendingEmailReply {
    id: number;
    fromEmail: string;
    toEmail: string;
    originalSubject: string;
    originalContent: string;
    replyContent: string;
    providerType: EmailProviderType;
    messageId?: string;
    status: EmailStatus;
    createdAt: string;
    reviewedAt?: string;
    reviewedBy?: string;
    sentAt?: string;
    rejectionReason?: string;
}

export interface ReviewRequest {
    reviewedBy: string;
    rejectionReason?: string;
}

export interface EmailApiResponse {
    success: boolean;
    message: string;
    data?: unknown;
}

export interface PagePendingEmailReply {
    content: PendingEmailReply[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
    empty: boolean;
}

/**
 * Email Management Service
 *
 * APIs:
 * - GET /api/email/pending - Get pending emails (paginated)
 * - GET /api/email/pending/{id} - Get email by ID
 * - GET /api/email/pending/status/{status} - Get emails by status
 * - GET /api/email/pending/count - Get email counts by status
 * - POST /api/email/pending/{id}/approve - Approve and send email
 * - POST /api/email/pending/{id}/reject - Reject email
 * - DELETE /api/email/pending/{id} - Delete email
 */
export const emailService = {
    /**
     * Get pending emails (paginated)
     */
    getPendingReplies: (page = 0, size = 20) =>
        apiClient.get<PagePendingEmailReply>('/api/email/pending', {
            params: { page, size }
        }),

    /**
     * Get email by ID
     */
    getById: (id: number) =>
        apiClient.get<PendingEmailReply>(`/api/email/pending/${id}`),

    /**
     * Get emails by status
     */
    getByStatus: (status: EmailStatus) =>
        apiClient.get<PendingEmailReply[]>(`/api/email/pending/status/${status}`),

    /**
     * Get email counts by status
     */
    getCounts: () =>
        apiClient.get<Record<EmailStatus, number>>('/api/email/pending/count'),

    /**
     * Update email content
     */
    updateContent: (id: number, replyContent: string) =>
        apiClient.put<EmailApiResponse>(`/api/email/pending/${id}/content`, { replyContent }),

    /**
     * Approve email (move to APPROVED status)
     */
    approve: (id: number, reviewedBy: string) =>
        apiClient.post<EmailApiResponse>(`/api/email/pending/${id}/approve`, { reviewedBy }),

    /**
     * Send approved email
     */
    send: (id: number) =>
        apiClient.post<EmailApiResponse>(`/api/email/pending/${id}/send`, {}),

    /**
     * Reject email
     */
    reject: (id: number, data: ReviewRequest) =>
        apiClient.post<EmailApiResponse>(`/api/email/pending/${id}/reject`, data),

    /**
     * Delete email
     */
    delete: (id: number) =>
        apiClient.delete<EmailApiResponse>(`/api/email/pending/${id}`)
};
