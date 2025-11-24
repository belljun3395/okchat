document.addEventListener('DOMContentLoaded', () => {
    const chatMessages = document.getElementById('chatMessages');
    const messageInput = document.getElementById('messageInput');
    const sendButton = document.getElementById('sendButton');
    const deepThinkCheckbox = document.getElementById('deepThinkCheckbox');
    const keywordsInput = document.getElementById('keywordsInput');
    const sessionIdElement = document.getElementById('sessionId');
    const messageCountElement = document.getElementById('messageCount');
    
    // Debug mode toggle (set to false in production)
    const DEBUG_MODE = true;

    // Configure marked.js for better markdown rendering
    marked.setOptions({
        breaks: true,        // Convert \n to <br>
        gfm: true,          // GitHub Flavored Markdown
        headerIds: false,   // Don't add IDs to headers
        mangle: false,      // Don't escape autolinked email addresses
        pedantic: false     // Don't conform to obscure parts of markdown.pl
    });

    // Generate a session ID
    const sessionId = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    if (sessionIdElement) {
        sessionIdElement.textContent = `Session: ${sessionId}`;
    }
    
    let messageCount = 0;

    function updateMessageCount() {
        messageCount++;
        if (messageCountElement) {
            messageCountElement.textContent = `${messageCount} messages`;
        }
    }
    
    /**
     * Normalize text by adding line breaks where markdown syntax indicates they should be.
     */
    function normalizeMarkdown(text) {
        let normalized = text;
        
        // Check if already well-formatted (multiple line breaks exist)
        const lineCount = (text.match(/\n/g) || []).length;
        const doubleLineBreaks = (text.match(/\n\n/g) || []).length;
        
        if (DEBUG_MODE) {
            console.log(`📊 Markdown analysis: ${text.length} chars, ${lineCount} newlines, ${doubleLineBreaks} double newlines`);
        }
        
        // Check if markdown looks properly formatted
        const hasProperHeaderFormat = text.match(/#{1,6}\s+[^\n]+\n\n/);  // Space after #
        const hasProperSectionSpacing = doubleLineBreaks >= 3;
        
        // Only skip normalization if BOTH conditions are met
        if (hasProperHeaderFormat && hasProperSectionSpacing) {
            if (DEBUG_MODE) console.log('✅ Already well-formatted, skipping normalization');
            return text;
        }
        
        if (DEBUG_MODE) {
            console.log('🔧 Normalizing markdown text without proper formatting...');
        }
        
        // Step 1: Fix markdown headers - ensure space after # (###제목 → ### 제목)
        normalized = normalized.replace(/(^|\n)(#{1,6})([^\s#\n])/gm, '$1$2 $3');
        
        // Step 2: Ensure double newline after headers (### 제목\n → ### 제목\n\n)
        normalized = normalized.replace(/(^|\n)(#{1,6}\s+.+?)(\n)(?!\n)/gm, '$1$2$3\n\n');
        
        // Step 3: Clean up any double spaces created
        normalized = normalized.replace(/  +/g, ' ');
        
        // Step 4: Add line breaks before headers if needed
        normalized = normalized.replace(/([^\n])\n(#{1,6}\s)/g, '$1\n\n$2');
        
        // Step 5: Fix "**text**:" patterns - ensure space after colon
        normalized = normalized.replace(/(\*\*[^*]+\*\*):(?!\s)/g, '$1: ');
        
        // Step 6: Fix numbered lists - ensure space after number (1.항목 → 1. 항목)
        normalized = normalized.replace(/(^|\n)(\d+\.)([^\s\n])/gm, '$1$2 $3');
        
        // Step 7: Fix bullet lists - ensure space after dash (-항목 → - 항목)  
        normalized = normalized.replace(/(^|\n)([\s]*-)([^\s\n])/gm, '$1$2 $3');
        
        // Step 8: Remove stray code blocks at the end
        normalized = normalized.replace(/```\s*$/g, '');
        
        // Step 9: Ensure proper spacing around lists
        normalized = normalized.replace(/(\n[^\n]+)\n(\s*[-*+]|\s*\d+\.)/g, '$1\n\n$2');
        
        return normalized;
    }

    /**
     * Format markdown text to HTML using marked.js with XSS protection.
     */
    function formatMarkdown(text) {
        try {
            // Step 1: Normalize markdown (add line breaks if needed)
            const normalizedText = normalizeMarkdown(text);
            
            // Step 2: Parse markdown with marked.js
            const rawHtml = marked.parse(normalizedText);
            
            // Step 3: Sanitize HTML to prevent XSS attacks
            const cleanHtml = DOMPurify.sanitize(rawHtml, {
                ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 'code', 'pre', 'a', 'ul', 'ol', 'li', 
                               'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'blockquote', 'hr', 'del', 'table', 
                               'thead', 'tbody', 'tr', 'th', 'td', 'span', 'div'],
                ALLOWED_ATTR: ['href', 'target', 'rel', 'class'],
                ALLOW_DATA_ATTR: false
            });
            
            return cleanHtml;
        } catch (error) {
            console.error('❌ Markdown parsing error:', error);
            // Fallback: return safely escaped text with line breaks
            return text.replace(/&/g, '&amp;')
                      .replace(/</g, '&lt;')
                      .replace(/>/g, '&gt;')
                      .replace(/"/g, '&quot;')
                      .replace(/\n/g, '<br>');
        }
    }

    function addMessage(content, isUser = false) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${isUser ? 'user' : 'bot'} animate-fade-in`;
        
        const contentDiv = document.createElement('div');
        contentDiv.className = 'message-content';
        
        if (isUser) {
            contentDiv.textContent = content;
        } else {
            contentDiv.innerHTML = formatMarkdown(content);
            
            // Add copy buttons to code blocks
            contentDiv.querySelectorAll('pre').forEach(pre => {
                const wrapper = document.createElement('div');
                wrapper.className = 'code-block-wrapper';
                pre.parentNode.insertBefore(wrapper, pre);
                wrapper.appendChild(pre);
                
                const copyBtn = document.createElement('button');
                copyBtn.className = 'code-copy-button';
                copyBtn.textContent = 'Copy';
                copyBtn.onclick = () => {
                    navigator.clipboard.writeText(pre.textContent).then(() => {
                        copyBtn.textContent = 'Copied!';
                        copyBtn.classList.add('copied');
                        setTimeout(() => {
                            copyBtn.textContent = 'Copy';
                            copyBtn.classList.remove('copied');
                        }, 2000);
                    });
                };
                wrapper.appendChild(copyBtn);
            });
        }
        
        const timeSpan = document.createElement('span');
        timeSpan.className = 'message-timestamp';
        const now = new Date();
        timeSpan.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        messageDiv.appendChild(contentDiv);
        contentDiv.appendChild(timeSpan);
        
        chatMessages.appendChild(messageDiv);
        
        // Smooth scroll to bottom
        requestAnimationFrame(() => {
            chatMessages.scrollTo({
                top: chatMessages.scrollHeight,
                behavior: 'smooth'
            });
        });
        
        updateMessageCount();
    }

    function addTypingIndicator() {
        const indicatorDiv = document.createElement('div');
        indicatorDiv.className = 'message bot typing-indicator-container';
        indicatorDiv.id = 'typingIndicator';
        indicatorDiv.innerHTML = `
            <div class="typing-indicator active">
                <span></span><span></span><span></span>
            </div>
        `;
        chatMessages.appendChild(indicatorDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        return indicatorDiv;
    }

    function removeTypingIndicator() {
        const indicator = document.getElementById('typingIndicator');
        if (indicator) {
            indicator.remove();
        }
    }

    async function sendMessage() {
        const message = messageInput.value.trim();
        if (!message) return;

        // Disable input while sending
        messageInput.value = '';
        messageInput.disabled = true;
        sendButton.disabled = true;

        // Add user message
        addMessage(message, true);

        // Add typing indicator
        const typingIndicator = addTypingIndicator();

        try {
            // Prepare request data
            const requestData = {
                message: message,
                sessionId: sessionId,
                isDeepThink: deepThinkCheckbox ? deepThinkCheckbox.checked : false,
                keywords: keywordsInput ? keywordsInput.value.split(',').map(k => k.trim()).filter(k => k) : []
            };

            if (DEBUG_MODE) {
                console.log('📤 Sending request:', requestData);
            }

            // Send request to backend using SSE
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'text/event-stream'
                },
                body: JSON.stringify(requestData)
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            // Remove typing indicator before streaming starts
            removeTypingIndicator();

            // Create empty bot message for streaming to ensure correct order
            addMessage("");

            // Process SSE stream
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';
            let botResponse = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop(); // Keep incomplete line in buffer

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        // Preserve whitespace by not trimming the content part excessively
                        // data: content -> content
                        let data = line.substring(5); 
                        
                        // Only trim the very beginning to remove the space after 'data:'
                        if (data.startsWith(' ')) {
                            data = data.substring(1);
                        }
                        
                        if (data && data !== '__REQUEST_ID__' && !data.startsWith('__REQUEST_ID__:')) {
                            // Replace literal \n with actual newlines if they come as escaped strings
                            // But usually SSE sends actual newlines as separate data lines or as part of the string
                            // If the server sends "data: \n", we want to keep that newline.
                            
                            botResponse += data;
                            
                            // Update the LAST bot message (the empty one we just created)
                            const messages = chatMessages.querySelectorAll('.message.bot');
                            if (messages.length > 0) {
                                const lastMessage = messages[messages.length - 1];
                                const contentDiv = lastMessage.querySelector('.message-content');
                                if (contentDiv) {
                                    // Preserve timestamp
                                    const timestamp = contentDiv.querySelector('.message-timestamp');
                                    contentDiv.innerHTML = formatMarkdown(botResponse);
                                    if (timestamp) {
                                        contentDiv.appendChild(timestamp);
                                    }
                                }
                            }
                            
                            // Smooth scroll to bottom
                            requestAnimationFrame(() => {
                                chatMessages.scrollTo({
                                    top: chatMessages.scrollHeight,
                                    behavior: 'smooth'
                                });
                            });
                        }
                    }
                }
            }

            // Final update with complete response
            if (!botResponse) {
                addMessage("죄송합니다. 응답을 받지 못했습니다.");
            }

            if (DEBUG_MODE) {
                console.log('✅ Response received, length:', botResponse.length);
            }

        } catch (error) {
            console.error('❌ Error:', error);
            removeTypingIndicator();
            addMessage("죄송합니다. 서버 연결에 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            // Re-enable input
            messageInput.disabled = false;
            sendButton.disabled = false;
            messageInput.focus();
        }
    }

    // Event listeners
    if (sendButton) {
        sendButton.addEventListener('click', sendMessage);
    }

    if (messageInput) {
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                sendMessage();
            }
        });
    }
});
