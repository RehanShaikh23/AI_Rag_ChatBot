/* ============================================================
   Chat API — Send messages, stream responses, get history
   ============================================================ */

import { api, getToken } from './client';

/**
 * Send a message and get a complete AI response.
 * @param {number|null} conversationId - null to auto-create
 * @param {string} message
 * @param {boolean} useRag
 * @returns {Promise<ChatResponse>}
 */
export async function sendMessage(conversationId, message, useRag = true) {
  return api.post('/chat', { conversationId, message, useRag });
}

/**
 * Stream a response via Server-Sent Events.
 * Returns an object with { reader, conversationId } for consuming the stream.
 *
 * @param {number|null} conversationId
 * @param {string} message
 * @param {boolean} useRag
 * @param {function} onToken  - called with each text token
 * @param {function} onDone   - called when stream completes, receives { conversationId }
 * @param {function} onError  - called on error
 * @returns {function} abort - call to cancel the stream
 */
export function streamMessage(conversationId, message, useRag = true, onToken, onDone, onError) {
  const controller = new AbortController();

  (async () => {
    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify({ conversationId, message, useRag }),
        signal: controller.signal,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Stream failed: ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // Process SSE lines
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // Keep incomplete line in buffer

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6);

            if (data === '[DONE]') {
              continue; // Next event has the conversationId
            }

            // Check if it's metadata JSON
            if (data.startsWith('{') && data.includes('conversationId')) {
              try {
                const meta = JSON.parse(data);
                onDone?.(meta);
              } catch {
                // Not valid JSON, treat as token
                onToken?.(data.replace(/\\n/g, '\n'));
              }
            } else {
              onToken?.(data.replace(/\\n/g, '\n'));
            }
          }
        }
      }

      // If onDone wasn't called via metadata, call it now
      onDone?.({});
    } catch (err) {
      if (err.name !== 'AbortError') {
        onError?.(err);
      }
    }
  })();

  // Return abort function
  return () => controller.abort();
}

/**
 * Get all messages for a conversation.
 * @param {number} conversationId
 * @returns {Promise<ChatResponse[]>}
 */
export async function getMessages(conversationId) {
  return api.get(`/chat/${conversationId}/messages`);
}
