import React, { useState, useEffect, useRef } from 'react';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { chatService } from '../services';

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
    // Constants for LocalStorage
    const STORAGE_KEY_SESSIONS = 'okchat_sessions';
    const STORAGE_KEY_MESSAGES_PREFIX = 'okchat_messages_';

    // Legacy keys (for migration)
    const LEGACY_STORAGE_KEY_SESSION = 'okchat_session_id';
    const LEGACY_STORAGE_KEY_MESSAGES = 'okchat_messages';

    // State for sessions list
    const [sessions, setSessions] = useState<Array<{ id: string; title: string; timestamp: number }>>([]);
    const [sessionId, setSessionId] = useState<string>('');

    // State for chat messages
    const [messages, setMessages] = useState<Array<{ content: string; isUser: boolean; timestamp: string }>>([]);

    // Initialize state on mount (handle migration and load)
    useEffect(() => {
        // 1. Load Sessions List
        const savedSessions = localStorage.getItem(STORAGE_KEY_SESSIONS);
        let parsedSessions: Array<{ id: string; title: string; timestamp: number }> = [];

        if (savedSessions) {
            try {
                parsedSessions = JSON.parse(savedSessions);
            } catch (e) { console.error('Failed to parse sessions', e); }
        }

        // 2. Check for legacy data and migrate if needed
        const legacySessionId = localStorage.getItem(LEGACY_STORAGE_KEY_SESSION);
        const legacyMessages = localStorage.getItem(LEGACY_STORAGE_KEY_MESSAGES);

        if (legacySessionId && legacyMessages && !parsedSessions.find(s => s.id === legacySessionId)) {
            // Migrate
            const newSession = {
                id: legacySessionId,
                title: 'Restored Chat',
                timestamp: Date.now()
            };
            parsedSessions.unshift(newSession);

            // Save to new format
            localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(parsedSessions));
            localStorage.setItem(STORAGE_KEY_MESSAGES_PREFIX + legacySessionId, legacyMessages);

            // Clear legacy
            localStorage.removeItem(LEGACY_STORAGE_KEY_SESSION);
            localStorage.removeItem(LEGACY_STORAGE_KEY_MESSAGES);
        }

        setSessions(parsedSessions);

        // 3. Determine current Session ID
        let currentId = sessionId;
        if (!currentId) {
            if (parsedSessions.length > 0) {
                currentId = parsedSessions[0].id;
            } else {
                // Create first session
                currentId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
                const newSession = { id: currentId, title: 'New Chat', timestamp: Date.now() };
                parsedSessions = [newSession];
                setSessions(parsedSessions);
                localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(parsedSessions));
            }
        }
        setSessionId(currentId);

        // 4. Load messages for this session
        const savedMessages = localStorage.getItem(STORAGE_KEY_MESSAGES_PREFIX + currentId);
        if (savedMessages) {
            try {
                setMessages(JSON.parse(savedMessages));
            } catch (e) {
                console.error('Failed to parse messages', e);
                setDefaultWelcomeMessage();
            }
        } else {
            setDefaultWelcomeMessage();
        }
    }, [sessionId]); // Re-run when sessionId changes (for loading messages) but NOT when sessions list changes

    const setDefaultWelcomeMessage = () => {
        setMessages([{
            content: "**Hello! I'm OKChat.**\n\nI can help you search documents, summarize meetings, and answer questions.\n\nTry asking: \"What was discussed in the last marketing meeting?\"",
            isUser: false,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }]);
    };

    // State for input field
    const [inputValue, setInputValue] = useState('');

    // State for loading/typing status
    const [isTyping, setIsTyping] = useState(false);

    // State for analysis options (Deep Think, Keywords)
    const [isOptionsOpen, setIsOptionsOpen] = useState(false);
    const [isDeepThink, setIsDeepThink] = useState(false);
    const [keywords, setKeywords] = useState('');



    // Ref for auto-scrolling to the bottom of the chat
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Configure marked options once on mount
    useEffect(() => {
        marked.setOptions({
            breaks: true, // Convert line breaks to <br>
            gfm: true // GitHub Flavored Markdown
        });
    }, []);

    // Persist messages when they change
    useEffect(() => {
        if (!sessionId) return;

        // Save messages
        localStorage.setItem(STORAGE_KEY_MESSAGES_PREFIX + sessionId, JSON.stringify(messages));

        // Update session list title if it's 'New Chat' and we have user messages
        const firstUserMsg = messages.find(m => m.isUser);
        if (firstUserMsg) {
            setSessions(prev => {
                const sessionIndex = prev.findIndex(s => s.id === sessionId);
                if (sessionIndex !== -1 && prev[sessionIndex].title === 'New Chat') {
                    const newTitle = firstUserMsg.content.slice(0, 30) + (firstUserMsg.content.length > 30 ? '...' : '');
                    const newSessions = [...prev];
                    newSessions[sessionIndex] = { ...newSessions[sessionIndex], title: newTitle };
                    localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(newSessions));
                    return newSessions;
                }
                return prev;
            });
        }
    }, [messages, sessionId]);

    // Auto-scroll to bottom when messages change
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages, isTyping]);

    /**
     * Handles starting a new chat session
     */

    const handleNewChat = () => {
        const newSessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
        const newSession = { id: newSessionId, title: 'New Chat', timestamp: Date.now() };

        // Add to top of list
        const newSessions = [newSession, ...sessions];
        setSessions(newSessions);
        setSessionId(newSessionId);

        // Save
        localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(newSessions));

        setMessages([{
            content: "**Hello! I'm OKChat.**\n\nI can help you search documents, summarize meetings, and answer questions.\n\nTry asking: \"What was discussed in the last marketing meeting?\"",
            isUser: false,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }]);
        setInputValue('');
        setIsDeepThink(false);
        setKeywords('');
    };

    /**
     * Handles deleting a chat session
     */
    const handleDeleteChat = (e: React.MouseEvent, idToDelete: string) => {
        e.stopPropagation(); // Prevent clicking the session itself

        if (!window.confirm('정말 이 채팅방을 삭제하시겠습니까?')) {
            return;
        }

        // Remove from sessions list
        const newSessions = sessions.filter(s => s.id !== idToDelete);
        setSessions(newSessions);

        // Update storage
        localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(newSessions));
        localStorage.removeItem(STORAGE_KEY_MESSAGES_PREFIX + idToDelete);

        // If we deleted the current session, switch to another one
        if (idToDelete === sessionId) {
            if (newSessions.length > 0) {
                // Switch to the first available session
                setSessionId(newSessions[0].id);
            } else {
                // No sessions left, create a new one (effectively same as New Chat)
                handleNewChat();
            }
        }
    };

    // State for renaming sessions
    const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
    const [editTitle, setEditTitle] = useState('');

    /**
     * Handles starting the edit mode for a session title
     */
    const handleStartEdit = (e: React.MouseEvent, session: { id: string; title: string }) => {
        e.stopPropagation();
        setEditingSessionId(session.id);
        setEditTitle(session.title);
    };

    /**
     * Handles saving the edited session title
     */
    const handleSaveEdit = (e?: React.FormEvent) => {
        if (e) e.preventDefault();

        if (editingSessionId && editTitle.trim()) {
            const newSessions = sessions.map(s =>
                s.id === editingSessionId ? { ...s, title: editTitle.trim() } : s
            );
            setSessions(newSessions);
            localStorage.setItem(STORAGE_KEY_SESSIONS, JSON.stringify(newSessions));
        }

        setEditingSessionId(null);
        setEditTitle('');
    };

    /**
     * Handles key press in edit input
     */
    const handleEditKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSaveEdit();
        } else if (e.key === 'Escape') {
            setEditingSessionId(null); // Cancel
        }
    };

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
        await chatService.sendChatMessage(requestData, {
            onData: (chunk: string) => {
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
            onError: (error: any) => {
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
                {/* Sidebar */}
                <aside className="chat-sidebar" style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>

                    <div className="mb-4">
                        <button
                            onClick={handleNewChat}
                            style={{
                                width: '100%',
                                padding: '10px 12px',
                                fontSize: '13px',
                                border: '1px solid #e5e7eb',
                                background: '#1f2937',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                color: 'white',
                                transition: 'all 0.2s',
                                fontWeight: 500,
                                display: 'flex',
                                alignItems: 'center',
                                gap: '8px'
                            }}
                            onMouseOver={(e) => e.currentTarget.style.background = '#374151'}
                            onMouseOut={(e) => e.currentTarget.style.background = '#1f2937'}
                        >
                            <span style={{ fontSize: '16px' }}>+</span> New Chat
                        </button>
                    </div>

                    <div style={{ flex: 1, overflowY: 'auto', marginBottom: '16px' }}>
                        <h3 style={{
                            fontSize: '12px',
                            textTransform: 'uppercase',
                            color: '#6b7280',
                            fontWeight: 600,
                            marginBottom: '8px',
                            letterSpacing: '0.05em'
                        }}>Recent</h3>

                        <div className="space-y-1">
                            {sessions.map(session => (
                                <div
                                    key={session.id}
                                    onClick={() => setSessionId(session.id)}
                                    title={session.title}
                                    style={{
                                        width: '100%',
                                        padding: '8px 12px',
                                        fontSize: '13px',
                                        borderRadius: '6px',
                                        cursor: 'pointer',
                                        color: sessionId === session.id ? '#111827' : '#4b5563',
                                        background: sessionId === session.id ? '#e5e7eb' : 'transparent',
                                        transition: 'all 0.2s',
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'space-between',
                                        gap: '8px',
                                        position: 'relative',
                                        border: '1px solid transparent'
                                    }}
                                    onMouseOver={(e) => {
                                        if (sessionId !== session.id) {
                                            e.currentTarget.style.background = '#f3f4f6';
                                        }
                                        const delBtn = e.currentTarget.querySelector('.delete-btn') as HTMLElement;
                                        if (delBtn) delBtn.style.opacity = '1';
                                        const editBtn = e.currentTarget.querySelector('.edit-btn') as HTMLElement;
                                        if (editBtn) editBtn.style.opacity = '1';
                                    }}
                                    onMouseOut={(e) => {
                                        if (sessionId !== session.id) {
                                            e.currentTarget.style.background = 'transparent';
                                        }
                                        const delBtn = e.currentTarget.querySelector('.delete-btn') as HTMLElement;
                                        if (delBtn) delBtn.style.opacity = '0';
                                        const editBtn = e.currentTarget.querySelector('.edit-btn') as HTMLElement;
                                        if (editBtn) editBtn.style.opacity = '0';
                                    }}
                                >
                                    {editingSessionId === session.id ? (
                                        <input
                                            type="text"
                                            value={editTitle}
                                            onChange={(e) => setEditTitle(e.target.value)}
                                            onKeyDown={handleEditKeyDown}
                                            onBlur={handleSaveEdit}
                                            autoFocus
                                            onClick={(e) => e.stopPropagation()}
                                            style={{
                                                flex: 1,
                                                minWidth: 0,
                                                background: 'white',
                                                border: '1px solid #3b82f6',
                                                borderRadius: '4px',
                                                padding: '2px 4px',
                                                fontSize: '13px',
                                                outline: 'none'
                                            }}
                                        />
                                    ) : (
                                        <>
                                            <span style={{
                                                whiteSpace: 'nowrap',
                                                overflow: 'hidden',
                                                textOverflow: 'ellipsis',
                                                flex: 1
                                            }}>
                                                {session.title}
                                            </span>

                                            <button
                                                className="edit-btn"
                                                onClick={(e) => handleStartEdit(e, session)}
                                                title="Rename chat"
                                                style={{
                                                    opacity: 0,
                                                    border: 'none',
                                                    background: 'none',
                                                    color: '#9ca3af',
                                                    cursor: 'pointer',
                                                    padding: '2px',
                                                    borderRadius: '4px',
                                                    fontSize: '14px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    transition: 'opacity 0.2s, color 0.2s'
                                                }}
                                                onMouseOver={(e) => e.currentTarget.style.color = '#3b82f6'}
                                                onMouseOut={(e) => e.currentTarget.style.color = '#9ca3af'}
                                            >
                                                ✎
                                            </button>

                                            <button
                                                className="delete-btn"
                                                onClick={(e) => handleDeleteChat(e, session.id)}
                                                title="Delete chat"
                                                style={{
                                                    opacity: 0,
                                                    border: 'none',
                                                    background: 'none',
                                                    color: '#9ca3af',
                                                    cursor: 'pointer',
                                                    padding: '2px',
                                                    borderRadius: '4px',
                                                    fontSize: '14px',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    transition: 'opacity 0.2s, color 0.2s'
                                                }}
                                                onMouseOver={(e) => e.currentTarget.style.color = '#ef4444'}
                                                onMouseOut={(e) => e.currentTarget.style.color = '#9ca3af'}
                                            >
                                                ×
                                            </button>
                                        </>
                                    )}
                                </div>
                            ))}
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
                </main >
            </div >
        </div >
    );
};

export default ChatPage;
