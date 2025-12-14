export interface CreatePromptRequest {
    type: string;
    content: string;
}

export interface UpdatePromptRequest {
    content: string;
}

export interface PromptResponse {
    id: number;
    type: string;
    version: number;
    content: string;
    isActive: boolean;
}

export interface PromptContentResponse {
    type: string;
    version: number;
    content: string;
}
