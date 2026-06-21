/* ============================================================
   API Client — Centralized HTTP client with JWT auth
   ============================================================ */

const BASE_URL = import.meta.env.VITE_API_URL || '/api';

/**
 * Get the stored JWT token.
 */
export function getToken() {
  return localStorage.getItem('auth_token');
}

/**
 * Store the JWT token.
 */
export function setToken(token) {
  localStorage.setItem('auth_token', token);
}

/**
 * Clear stored auth data.
 */
export function clearAuth() {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('auth_user');
}

/**
 * Get stored user profile.
 */
export function getStoredUser() {
  const raw = localStorage.getItem('auth_user');
  return raw ? JSON.parse(raw) : null;
}

/**
 * Store user profile.
 */
export function setStoredUser(user) {
  localStorage.setItem('auth_user', JSON.stringify(user));
}

/**
 * Build headers with JWT auth.
 */
function authHeaders(extra = {}) {
  const headers = { 'Content-Type': 'application/json', ...extra };
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

/**
 * Unified fetch wrapper with error handling.
 */
async function request(method, path, body = null, options = {}) {
  const config = {
    method,
    headers: authHeaders(options.headers),
    ...options,
  };

  if (body && !(body instanceof FormData)) {
    config.body = JSON.stringify(body);
  } else if (body instanceof FormData) {
    // Remove Content-Type for FormData (browser sets boundary)
    delete config.headers['Content-Type'];
    config.body = body;
  }

  const response = await fetch(`${BASE_URL}${path}`, config);

  // Handle 401 — auto logout
  if (response.status === 401) {
    clearAuth();
    window.dispatchEvent(new CustomEvent('auth:logout'));
    throw new ApiError('Session expired. Please log in again.', 401);
  }

  // Handle non-JSON responses (e.g., SSE)
  const contentType = response.headers.get('content-type');
  if (contentType && !contentType.includes('application/json')) {
    if (!response.ok) {
      throw new ApiError(`Request failed: ${response.statusText}`, response.status);
    }
    return response;
  }

  const data = await response.json();

  if (!response.ok || !data.success) {
    throw new ApiError(
      data.message || data.error || 'Request failed',
      response.status,
      data.error
    );
  }

  return data.data;
}

/**
 * Custom error class for API errors.
 */
export class ApiError extends Error {
  constructor(message, status, code) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

// ---- Convenience methods ----
export const api = {
  get: (path, opts) => request('GET', path, null, opts),
  post: (path, body, opts) => request('POST', path, body, opts),
  put: (path, body, opts) => request('PUT', path, body, opts),
  patch: (path, body, opts) => request('PATCH', path, body, opts),
  delete: (path, opts) => request('DELETE', path, null, opts),
};
