import apiClient from '../lib/api-client';

export interface DailyUsageStats {
    totalInteractions: number;
    averageResponseTime: number;
}

export interface QueryTypeStat {
    queryType: string;
    count: number;
    averageRating: number;
    averageResponseTime: number;
}

export interface DateRange {
    startDate: string;
    endDate: string;
}

export interface QualityTrendStats {
    averageRating: number;
    helpfulPercentage: number;
    totalInteractions: number;
    dateRange: DateRange;
}

export interface PerformanceMetrics {
    averageResponseTimeMs: number;
    errorRate: number;
    dateRange: DateRange;
}

export interface FeedbackRequest {
    requestId: string;
    rating?: number;
    wasHelpful?: boolean;
    feedback?: string;
}

/**
 * Analytics Service
 *
 * APIs:
 * - GET /api/admin/chat/analytics/usage/daily - Get daily usage stats
 * - GET /api/admin/chat/analytics/quality/trend - Get quality trend
 * - GET /api/admin/chat/analytics/query-types - Get query type stats
 * - GET /api/admin/chat/analytics/performance - Get performance metrics
 * - POST /api/admin/chat/analytics/feedback - Submit feedback
 */
export const analyticsService = {
    /**
     * Get daily usage statistics
     */
    getDailyUsage: (startDate: string, endDate: string) =>
        apiClient.get<DailyUsageStats>('/api/admin/chat/analytics/usage/daily', {
            params: { startDate, endDate }
        }),

    /**
     * Get quality trend statistics
     */
    getQualityTrend: (startDate: string, endDate: string) =>
        apiClient.get<QualityTrendStats>('/api/admin/chat/analytics/quality/trend', {
            params: { startDate, endDate }
        }),

    /**
     * Get query type statistics
     */
    getQueryTypeStats: (startDate: string, endDate: string) =>
        apiClient.get<QueryTypeStat[]>('/api/admin/chat/analytics/query-types', {
            params: { startDate, endDate }
        }),

    /**
     * Get performance metrics
     */
    getPerformanceMetrics: (startDate: string, endDate: string) =>
        apiClient.get<PerformanceMetrics>('/api/admin/chat/analytics/performance', {
            params: { startDate, endDate }
        }),

    /**
     * Submit feedback
     */
    submitFeedback: (data: FeedbackRequest) =>
        apiClient.post<void>('/api/admin/chat/analytics/feedback', data)
};
