/**
 * Chat Service
 *
 * Note: Chat uses Server-Sent Events (SSE) streaming,
 * so we use fetch API directly instead of axios
 */

export interface ChatRequest {
    message: string;
    keywords?: string[];
    sessionId?: string;
    isDeepThink?: boolean;
    userEmail?: string;
}

export interface ChatStreamHandler {
    onData: (chunk: string) => void;
    onError: (error: Error) => void;
    onComplete: () => void;
}

/**
 * Send chat message and handle SSE stream
 */
export const sendChatMessage = async (
    request: ChatRequest,
    handler: ChatStreamHandler
): Promise<void> => {
    try {
        const response = await fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'text/event-stream'
            },
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        if (!response.body) {
            throw new Error('ReadableStream not supported');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        try {
            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    handler.onComplete();
                    break;
                }

                const chunk = decoder.decode(value, { stream: true });
                const lines = chunk.split('\n');

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        let data = line.substring(5);
                        if (data.startsWith(' ')) {
                            data = data.substring(1);
                        }

                        if (data.trim()) {
                            handler.onData(data);
                        }
                    }
                }
            }
        } finally {
            reader.releaseLock();
        }
    } catch (error) {
        handler.onError(error instanceof Error ? error : new Error('Unknown error occurred'));
    }
};
