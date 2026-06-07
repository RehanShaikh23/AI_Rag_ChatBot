import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getProfile } from '../api/auth';
import { getToken, clearAuth, getStoredUser } from '../api/client';

const AuthContext = createContext(null);

/**
 * Auth provider — manages authentication state across the app.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Check auth on mount
  useEffect(() => {
    const token = getToken();
    if (token) {
      // Try stored user first for instant UI
      const stored = getStoredUser();
      if (stored) {
        setUser(stored);
        setIsAuthenticated(true);
      }
      // Validate token with backend
      getProfile()
        .then((profile) => {
          setUser(profile);
          setIsAuthenticated(true);
        })
        .catch(() => {
          clearAuth();
          setUser(null);
          setIsAuthenticated(false);
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, []);

  // Listen for forced logout (401)
  useEffect(() => {
    const handleLogout = () => {
      setUser(null);
      setIsAuthenticated(false);
    };
    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  const handleLogin = useCallback((loginData) => {
    setUser(loginData.user);
    setIsAuthenticated(true);
  }, []);

  const handleLogout = useCallback(() => {
    clearAuth();
    setUser(null);
    setIsAuthenticated(false);
  }, []);

  return (
    <AuthContext.Provider value={{
      user,
      loading,
      isAuthenticated,
      login: handleLogin,
      logout: handleLogout,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
