/* ============================================================
   Documents API — Upload and manage RAG documents
   ============================================================ */

import { api, getToken } from './client';

/**
 * Upload a document for RAG processing.
 * @param {File} file
 * @returns {Promise<DocumentDto>}
 */
export async function uploadDocument(file) {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/documents/upload', formData);
}

/**
 * List user's documents (paginated).
 */
export async function getDocuments(page = 0, size = 20) {
  return api.get(`/documents?page=${page}&size=${size}`);
}

/**
 * Delete a document and its vector data.
 */
export async function deleteDocument(id) {
  return api.delete(`/documents/${id}`);
}
