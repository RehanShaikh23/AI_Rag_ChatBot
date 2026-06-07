import { useState, useCallback } from 'react';
import { login as apiLogin, register as apiRegister } from '../api/auth';
import { useAuth } from '../contexts/AuthContext';

/**
 * Claude-inspired Login/Register page.
 * Clean, minimal design with warm cream background, serif headline,
 * coral CTA, and Google OAuth option — based on Stitch UI screens.
 */
export default function AuthModal() {
  const { login } = useAuth();
  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = useCallback(async (e) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      let data;
      if (mode === 'login') {
        data = await apiLogin(email, password);
      } else {
        data = await apiRegister(email, password, displayName);
      }
      login(data);
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setIsLoading(false);
    }
  }, [mode, email, password, displayName, login]);

  const toggleMode = () => {
    setMode(m => m === 'login' ? 'register' : 'login');
    setError('');
  };

  return (
    <div className="auth-page">
      <main className="auth-page-inner">
        {/* Logo */}
        <div className="auth-logo-area">
          <svg
            className="auth-logo-icon"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
            aria-label="AI ChatBot Logo"
          >
            <path d="M12 2L2 22h4.5l2.5-5h6l2.5 5H22L12 2zm-1.5 12.5L12 11l1.5 3.5h-3z" />
          </svg>
        </div>

        {/* Card */}
        <div className="auth-card">
          <h1 className="auth-card-title">
            {mode === 'login' ? 'Welcome back' : 'Create your account'}
          </h1>

          {error && (
            <div className="auth-error">
              <span className="material-symbols-outlined" style={{ fontSize: 16 }}>error</span>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="auth-card-form">
            {mode === 'register' && (
              <div className="auth-input-group">
                <label htmlFor="displayName" className="sr-only">Display Name</label>
                <input
                  id="displayName"
                  type="text"
                  value={displayName}
                  onChange={e => setDisplayName(e.target.value)}
                  placeholder="Full name"
                  required
                  minLength={2}
                  className="auth-input"
                />
              </div>
            )}

            <div className="auth-input-group">
              <label htmlFor="email" className="sr-only">Email address</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="Email address"
                required
                autoComplete="email"
                className="auth-input"
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="password" className="sr-only">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="Password"
                required
                minLength={6}
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                className="auth-input"
              />
            </div>

            <button
              type="submit"
              className="auth-cta-btn"
              disabled={isLoading}
            >
              {isLoading && <span className="auth-cta-spinner" />}
              {isLoading
                ? 'Please wait...'
                : mode === 'login' ? 'Continue with email' : 'Continue'}
            </button>
          </form>

          {/* Divider */}
          <div className="auth-divider">
            <div className="auth-divider-line" />
            <span className="auth-divider-text">or</span>
            <div className="auth-divider-line" />
          </div>

          {/* Google OAuth */}
          <button type="button" className="auth-google-btn">
            <svg className="auth-google-icon" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4" />
              <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853" />
              <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05" />
              <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335" />
            </svg>
            Continue with Google
          </button>

          {/* Terms — only on signup */}
          {mode === 'register' && (
            <p className="auth-terms">
              By continuing, you agree to our{' '}
              <a href="#" className="auth-terms-link">Terms of Service</a> and{' '}
              <a href="#" className="auth-terms-link">Privacy Policy</a>.
            </p>
          )}
        </div>

        {/* Toggle link */}
        <div className="auth-toggle-area">
          <button type="button" onClick={toggleMode} className="auth-toggle-link">
            {mode === 'login' ? 'Sign up for an account' : 'Log in to an existing account'}
            <span className="material-symbols-outlined auth-toggle-arrow">arrow_forward</span>
          </button>
        </div>
      </main>
    </div>
  );
}
