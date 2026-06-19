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

/**
 * Poll for a specific document's processing status.
 * Fetches the documents list and finds the matching document by ID.
 * @param {number} docId
 * @returns {Promise<DocumentDto|null>}
 */
export async function getDocumentStatus(docId) {
  const page = await getDocuments(0, 50);
  const docs = page?.content || page || [];
  return docs.find((d) => d.id === docId) || null;
}
