/* ============================================================
   Conversations API — CRUD for conversation threads
   ============================================================ */

import { api } from './client';

/**
 * List conversations (paginated, most recent first).
 */
export async function getConversations(page = 0, size = 30) {
  return api.get(`/conversations?page=${page}&size=${size}`);
}

/**
 * Create a new empty conversation.
 */
export async function createConversation(title = null) {
  return api.post('/conversations', title ? { title } : {});
}

/**
 * Update a conversation's title.
 */
export async function updateConversationTitle(id, title) {
  return api.patch(`/conversations/${id}`, { title });
}

/**
 * Delete a conversation.
 */
export async function deleteConversation(id) {
  return api.delete(`/conversations/${id}`);
}
