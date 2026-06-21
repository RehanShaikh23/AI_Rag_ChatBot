/* ============================================================
   Chat API — Send messages, stream responses, get history
   ============================================================ */

import { api, getToken, BASE_URL } from './client';

/**
 * Send a message and get a complete AI response.
 * @param {number|null} conversationId - null to auto-create
 * @param {string} message
 * @param {boolean} useRag
 * @param {number|null} activeDocumentId - scopes RAG to this document
 * @returns {Promise<ChatResponse>}
 */
export async function sendMessage(conversationId, message, useRag = true, activeDocumentId = null) {
  return api.post('/chat', { conversationId, message, useRag, activeDocumentId });
}

/**
 * Stream a response via Server-Sent Events.
 * Returns an object with { reader, conversationId } for consuming the stream.
 *
 * @param {number|null} conversationId
 * @param {string} message
 * @param {boolean} useRag
 * @param {number|null} activeDocumentId - scopes RAG to this document
 * @param {function} onToken  - called with each text token
 * @param {function} onDone   - called when stream completes, receives { conversationId }
 * @param {function} onError  - called on error
 * @returns {function} abort - call to cancel the stream
 */
export function streamMessage(conversationId, message, useRag = true, activeDocumentId = null, onToken, onDone, onError) {
  const controller = new AbortController();
  let doneEmitted = false;
  let accumulatedText = '';

  // Safety timeout: if no data arrives in 120 seconds, treat as error
  let lastActivityTime = Date.now();
  const TIMEOUT_MS = 120_000;
  const timeoutCheck = setInterval(() => {
    if (Date.now() - lastActivityTime > TIMEOUT_MS && !doneEmitted) {
      clearInterval(timeoutCheck);
      controller.abort();
      onError?.(new Error('Stream timeout — no data received'));
    }
  }, 5000);

  (async () => {
    try {
      const response = await fetch(`${BASE_URL}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${getToken()}`,
        },
        body: JSON.stringify({ conversationId, message, useRag, activeDocumentId }),
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

        lastActivityTime = Date.now();
        buffer += decoder.decode(value, { stream: true });

        // Process SSE lines
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // Keep incomplete line in buffer

        for (const line of lines) {
          // Support both 'data: ' and 'data:' formats (SseEmitter may omit space)
          let data = null;
          if (line.startsWith('data: ')) {
            data = line.slice(6);
          } else if (line.startsWith('data:')) {
            data = line.slice(5);
          } else {
            continue;
          }

          if (!data || data.trim() === '') continue;

          if (data === '[DONE]') {
            continue; // Next event has the conversationId
          }

          if (data === '[ERROR]') {
            throw new Error('Server encountered an error while generating the response');
          }

          // Check if it's metadata JSON
          if (data.startsWith('{') && data.includes('conversationId')) {
            try {
              const meta = JSON.parse(data);
              meta.accumulatedText = accumulatedText;
              if (!doneEmitted) {
                doneEmitted = true;
                onDone?.(meta);
              }
            } catch {
              // Not valid JSON, treat as token
              const tokenText = data.replace(/\\n/g, '\n');
              accumulatedText += tokenText;
              onToken?.(tokenText);
            }
          } else {
            const tokenText = data.replace(/\\n/g, '\n');
            accumulatedText += tokenText;
            onToken?.(tokenText);
          }
        }
      }

      // If onDone wasn't called via metadata, call it now
      if (!doneEmitted) {
        doneEmitted = true;
        onDone?.({ accumulatedText });
      }
    } catch (err) {
      if (err.name !== 'AbortError') {
        onError?.(err);
      }
    } finally {
      clearInterval(timeoutCheck);
    }
  })();

  // Return abort function
  return () => {
    clearInterval(timeoutCheck);
    controller.abort();
  };
}

/**
 * Get all messages for a conversation.
 * @param {number} conversationId
 * @returns {Promise<ChatResponse[]>}
 */
export async function getMessages(conversationId) {
  return api.get(`/chat/${conversationId}/messages`);
}
