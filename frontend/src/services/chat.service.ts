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
        let buffer = ''; // Buffer to accumulate incomplete SSE events

        try {
            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    handler.onComplete();
                    break;
                }

                // Decode the chunk and append to buffer
                buffer += decoder.decode(value, { stream: true });

                // Process complete SSE events (separated by \n\n)
                const events = buffer.split('\n\n');

                // Keep the last incomplete event in the buffer
                buffer = events.pop() || '';

                // Process each complete event
                for (const event of events) {
                    if (!event.trim()) continue;

                    // Extract all data lines from the event
                    const lines = event.split('\n');
                    const dataLines: string[] = [];

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            let data = line.substring(5);
                            if (data.startsWith(' ')) {
                                data = data.substring(1);
                            }
                            dataLines.push(data);
                        }
                    }

                    // Join data lines with newlines and pass to handler
                    if (dataLines.length > 0) {
                        const eventData = dataLines.join('\n');
                        // Pass all data including whitespace/newlines to preserve formatting
                        if (eventData.length > 0) {
                            handler.onData(eventData);
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
