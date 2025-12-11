import React, { useState, useEffect, useRef } from 'react';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { sendChatMessage } from '../services/chat.service';

/**
 * ChatPage Component
 *
 * This is the main chat interface. It handles:
 * 1. Sending messages to the backend via POST /api/chat
 * 2. Receiving streaming responses (SSE) from the backend
 * 3. Rendering markdown content safely
 * 4. Managing chat state (messages, input, settings)
 */
const ChatPage: React.FC = () => {
    // State for chat messages
    // We use a simple array of objects. 'isUser' determines if it's a user or bot message.
    const [messages, setMessages] = useState<Array<{ content: string; isUser: boolean; timestamp: string }>>([]);

    // State for input field
    const [inputValue, setInputValue] = useState('');

    // State for loading/typing status
    const [isTyping, setIsTyping] = useState(false);

    // State for analysis options (Deep Think, Keywords)
    const [isOptionsOpen, setIsOptionsOpen] = useState(false);
    const [isDeepThink, setIsDeepThink] = useState(false);
    const [keywords, setKeywords] = useState('');

    // Session ID state - generated once on component mount
    const [sessionId, setSessionId] = useState('');

    // Ref for auto-scrolling to the bottom of the chat
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Configure marked options once on mount
    useEffect(() => {
        marked.setOptions({
            breaks: true, // Convert line breaks to <br>
            gfm: true // GitHub Flavored Markdown
        });
    }, []);

    // Initialize session ID on mount
    useEffect(() => {
        const newSessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
        setSessionId(newSessionId);

        // Add welcome message
        setMessages([{
            content: "**Hello! I'm OKChat.**\n\nI can help you search documents, summarize meetings, and answer questions.\n\nTry asking: \"What was discussed in the last marketing meeting?\"",
            isUser: false,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }]);
    }, []);

    // Auto-scroll to bottom when messages change
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isTyping]);

    /**
     * Handles sending a message
     */
    const handleSendMessage = async () => {
        if (!inputValue.trim()) return;

        const userMessage = inputValue.trim();
        setInputValue(''); // Clear input

        // Add user message to state
        const newMessage = {
            content: userMessage,
            isUser: true,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        };

        setMessages(prev => [...prev, newMessage]);
        setIsTyping(true);

        // Prepare request data
        const requestData = {
            message: userMessage,
            sessionId: sessionId,
            isDeepThink: isDeepThink,
            keywords: keywords ? keywords.split(',').map(k => k.trim()).filter(k => k) : []
        };

        // Initialize bot response state
        let botResponse = '';
        let isFirstChunk = true;

        // Use the chat service with enhanced error handling
        await sendChatMessage(requestData, {
            onData: (chunk) => {
                // Skip request ID metadata
                if (chunk && !chunk.startsWith('__REQUEST_ID__:')) {
                    botResponse += chunk;

                    if (isFirstChunk) {
                        setIsTyping(false);
                        // Add the bot message for the first time
                        setMessages(prev => [...prev, {
                            content: botResponse,
                            isUser: false,
                            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                        }]);
                        isFirstChunk = false;
                    } else {
                        // Update the existing last message
                        setMessages(prev => {
                            const newMessages = [...prev];
                            const lastMsg = newMessages[newMessages.length - 1];
                            lastMsg.content = botResponse;
                            return newMessages;
                        });
                    }
                }
            },
            onError: (error) => {
                console.error('Error sending message:', error);
                setIsTyping(false);

                const errorMessage = error.message || 'An unknown error occurred';
                setMessages(prev => [...prev, {
                    content: `**Error**: ${errorMessage}\n\nPlease try again or contact support if the problem persists.`,
                    isUser: false,
                    timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                }]);
            },
            onComplete: () => {
                setIsTyping(false);
                console.log('Chat stream completed');
            }
        });
    };

    /**
     * Renders markdown content safely
     * Note: Markdown syntax normalization is now handled by the backend
     */
    const renderMarkdown = (content: string) => {
        try {
            // Ensure content is a string
            if (typeof content !== 'string') return { __html: '' };

            // DEBUG: Log markdown content
            console.log('=== MARKDOWN DEBUG ===');
            console.log('Input markdown (first 500 chars):', content.substring(0, 500));

            // Parse markdown (marked.parse is synchronous, options set on mount)
            // Backend already normalizes markdown syntax before streaming
            const rawHtml = marked.parse(content) as string;

            console.log('Parsed HTML (first 500 chars):', rawHtml.substring(0, 500));

            // Sanitize HTML while allowing markdown-generated tags
            const cleanHtml = DOMPurify.sanitize(rawHtml, {
                ALLOWED_TAGS: [
                    'p', 'br', 'strong', 'em', 'u', 's', 'del', 'strike',
                    'code', 'pre', 'a', 'ul', 'ol', 'li',
                    'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                    'blockquote', 'hr', 'table', 'thead', 'tbody', 'tr', 'th', 'td',
                    'span', 'div', 'img'
                ],
                ALLOWED_ATTR: ['href', 'target', 'rel', 'class', 'src', 'alt', 'title'],
                ALLOW_DATA_ATTR: false,
                KEEP_CONTENT: true
            });

            console.log('Sanitized HTML (first 500 chars):', cleanHtml.substring(0, 500));
            console.log('=== END DEBUG ===');

            return { __html: cleanHtml };
        } catch (e) {
            console.error('Markdown parsing error:', e);
            // Fallback: escape HTML and preserve line breaks
            const escaped = content
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/\n/g, '<br>');
            return { __html: escaped };
        }
    };

    return (
        <div className="flex flex-col h-screen">
            {/* Navigation */}
            <nav className="navbar">
                <div className="flex items-center gap-lg">
                    <a href="/" className="nav-brand">
                        OKChat
                    </a>
                    <span className="text-sm text-secondary" style={{ fontWeight: 400 }}>Admin Dashboard</span>
                </div>
                <div className="flex items-center gap-md">
                    <a href="/admin/permissions" className="btn btn-secondary btn-sm">
                        Admin Dashboard
                    </a>
                </div>
            </nav>

            <div className="chat-layout flex-1 overflow-hidden">
                {/* Sidebar */}
                <aside className="chat-sidebar">
                    <h3>Settings</h3>

                    <div className="input-group">
                        <label className="input-label">Session ID</label>
                        <div className="text-xs font-mono text-muted bg-gray-50 p-2 rounded border border-gray-200 truncate" style={{ 
                            fontFamily: 'Monaco, Menlo, monospace',
                            fontSize: '11px',
                            padding: '8px 12px',
                            borderRadius: '6px'
                        }}>
                            {sessionId || 'Initializing...'}
                        </div>
                    </div>

                    {/* Analysis Options */}
                    <div className="input-group" style={{ marginTop: '24px' }}>
                        <label className="input-label">Analysis Options</label>
                        <div className="border border-gray-200 rounded-lg overflow-hidden bg-white">
                            <button
                                onClick={() => setIsOptionsOpen(!isOptionsOpen)}
                                className="w-full px-4 py-2 bg-gray-50 hover:bg-gray-100 transition-colors flex items-center justify-between text-sm font-medium text-secondary"
                                style={{ 
                                    width: '100%',
                                    padding: '10px 16px',
                                    background: isOptionsOpen ? '#f3f4f6' : '#f9fafb',
                                    border: 'none',
                                    cursor: 'pointer',
                                    borderRadius: isOptionsOpen ? '8px 8px 0 0' : '8px',
                                    transition: 'all 0.2s'
                                }}
                            >
                                <span>Advanced Options</span>
                                <span>{isOptionsOpen ? '↑' : '↓'}</span>
                            </button>

                            {isOptionsOpen && (
                                <div className="p-4 space-y-3 border-t border-gray-200" style={{ padding: '16px' }}>
                                    <label className="flex items-center gap-sm p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50 transition-colors" style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '8px',
                                        padding: '12px',
                                        border: '1px solid #e5e7eb',
                                        borderRadius: '8px',
                                        cursor: 'pointer',
                                        transition: 'background-color 0.2s'
                                    }}>
                                        <input
                                            type="checkbox"
                                            checked={isDeepThink}
                                            onChange={(e) => setIsDeepThink(e.target.checked)}
                                            style={{
                                                width: '18px',
                                                height: '18px',
                                                cursor: 'pointer',
                                                accentColor: '#2563eb'
                                            }}
                                        />
                                        <span className="text-sm font-medium" style={{ fontSize: '14px', fontWeight: 500 }}>Enable Deep Think</span>
                                    </label>
                                    <div>
                                        <label className="input-label text-sm" style={{ display: 'block', marginBottom: '6px', fontSize: '14px', fontWeight: 500 }}>Context Keywords</label>
                                        <input
                                            type="text"
                                            className="form-control"
                                            placeholder="e.g. meeting, 2025, project"
                                            value={keywords}
                                            onChange={(e) => setKeywords(e.target.value)}
                                            style={{
                                                width: '100%',
                                                padding: '8px 12px',
                                                border: '1px solid #d1d5db',
                                                borderRadius: '6px',
                                                fontSize: '14px'
                                            }}
                                        />
                                        <small className="text-muted mt-1 block text-xs" style={{ 
                                            display: 'block',
                                            marginTop: '6px',
                                            fontSize: '12px',
                                            color: '#6b7280'
                                        }}>Comma separated keywords</small>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>

                    <div style={{ marginTop: 'auto', paddingTop: '24px', borderTop: '1px solid #e5e7eb' }}>
                        <div className="flex items-center justify-between text-sm text-secondary" style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            fontSize: '14px',
                            color: '#6b7280'
                        }}>
                            <span>Messages</span>
                            <span className="font-medium text-main" style={{ fontWeight: 600, color: '#1f2937' }}>{messages.length}</span>
                        </div>
                    </div>
                </aside>

                {/* Main Chat Area */}
                <main className="chat-main">
                    <div className="chat-messages">
                        {messages.length === 0 ? (
                            <div className="chat-empty-state">
                                <h3>Welcome to OKChat</h3>
                                <p>Start a conversation by typing a message below. I can help you search documents, summarize meetings, and answer questions.</p>
                            </div>
                        ) : (
                            <>
                                {messages.map((msg, index) => (
                                    <div key={index} className={`message ${msg.isUser ? 'user' : 'bot'}`}>
                                        <div className="message-avatar">
                                            {msg.isUser ? 'U' : 'B'}
                                        </div>
                                        <div className="message-content-wrapper">
                                            <div
                                                className={`message-content ${!msg.isUser ? 'markdown-body' : ''}`}
                                                dangerouslySetInnerHTML={!msg.isUser ? renderMarkdown(msg.content) : undefined}
                                            >
                                                {msg.isUser ? msg.content : undefined}
                                            </div>
                                            <span className="message-timestamp">{msg.timestamp}</span>
                                        </div>
                                    </div>
                                ))}

                                {isTyping && (
                                    <div className="message bot">
                                        <div className="message-avatar">
                                            B
                                        </div>
                                        <div className="typing-indicator">
                                            <span></span><span></span><span></span>
                                        </div>
                                    </div>
                                )}
                            </>
                        )}

                        <div ref={messagesEndRef} />
                    </div>

                    {/* Input Area */}
                    <div className="chat-input-area">
                        <div className="max-w-4xl mx-auto">
                            <div className="chat-input-wrapper">
                                <input
                                    type="text"
                                    className="chat-input"
                                    placeholder="Type your message here..."
                                    value={inputValue}
                                    onChange={(e) => setInputValue(e.target.value)}
                                    onKeyPress={(e) => {
                                        if (e.key === 'Enter' && !e.shiftKey) {
                                            e.preventDefault();
                                            handleSendMessage();
                                        }
                                    }}
                                    disabled={isTyping}
                                />
                                <button
                                    className="chat-send-btn"
                                    onClick={handleSendMessage}
                                    disabled={isTyping || !inputValue.trim()}
                                    title="Send message (Enter)"
                                >
                                    →
                                </button>
                            </div>
                        </div>
                        <div className="text-center mt-2">
                            <span className="text-xs text-muted">AI can make mistakes. Please verify important information.</span>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default ChatPage;
