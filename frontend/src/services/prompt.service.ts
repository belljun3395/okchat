import apiClient from '../lib/api-client';
import type { 
    CreatePromptRequest, 
    UpdatePromptRequest, 
    PromptResponse, 
    PromptContentResponse 
} from '../types/prompt';

export const promptService = {
    /**
     * Get a specific prompt by type and optional version.
     * If version is omitted, returns the latest version.
     */
    getPrompt: (type: string, version?: number) => 
        apiClient.get<PromptContentResponse>(`/api/prompts/${type}`, { params: { version } }),

    /**
     * Get all versions of a specific prompt type.
     */
    getAllVersions: (type: string) => 
        apiClient.get<PromptResponse[]>(`/api/prompts/${type}/versions`),

    /**
     * Get all distinct prompt types.
     */
    getAllTypes: () => 
        apiClient.get<string[]>('/api/prompts/types'),

    /**
     * Create a new prompt (starts at version 1).
     */
    createPrompt: (data: CreatePromptRequest) => 
        apiClient.post<PromptResponse>('/api/prompts', data),

    /**
     * Update an existing prompt (creates a new version).
     */
    updatePrompt: (type: string, data: UpdatePromptRequest) => 
        apiClient.put<PromptResponse>(`/api/prompts/${type}`, data),

    /**
     * Deactivate a specific prompt version.
     */
    deactivatePrompt: (type: string, version: number) => 
        apiClient.delete<void>(`/api/prompts/${type}/versions/${version}`),
};
