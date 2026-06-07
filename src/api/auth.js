/* ============================================================
   Auth API — Register, Login, Profile
   ============================================================ */

import { api, setToken, setStoredUser, clearAuth } from './client';

/**
 * Register a new user.
 * @returns {{ token, tokenType, expiresIn, user }}
 */
export async function register(email, password, displayName) {
  const data = await api.post('/auth/register', { email, password, displayName });
  setToken(data.token);
  setStoredUser(data.user);
  return data;
}

/**
 * Login with email and password.
 * @returns {{ token, tokenType, expiresIn, user }}
 */
export async function login(email, password) {
  const data = await api.post('/auth/login', { email, password });
  setToken(data.token);
  setStoredUser(data.user);
  return data;
}

/**
 * Get the current user's profile.
 * @returns {{ id, email, displayName, role }}
 */
export async function getProfile() {
  return api.get('/auth/me');
}

/**
 * Log out — clear local auth state.
 */
export function logout() {
  clearAuth();
  window.dispatchEvent(new CustomEvent('auth:logout'));
}
