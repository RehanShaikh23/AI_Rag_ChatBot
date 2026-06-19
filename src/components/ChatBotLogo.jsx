/**
 * ChatBotLogo — inline SVG chatbot icon with no background.
 * Renders a friendly robot head with gradient fill.
 * Props: size (px, default 32), className, animated (boolean)
 */
export default function ChatBotLogo({ size = 32, className = '', animated = false }) {
  const cls = `chatbot-logo${animated ? ' chatbot-logo--animated' : ''}${className ? ` ${className}` : ''}`;

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={cls}
      aria-label="AI ChatBot"
    >
      <defs>
        <linearGradient id="botGrad" x1="0" y1="0" x2="64" y2="64" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#4F46E5" />
          <stop offset="100%" stopColor="#06B6D4" />
        </linearGradient>
        <linearGradient id="botGradActive" x1="0" y1="0" x2="64" y2="64" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#6366F1" />
          <stop offset="100%" stopColor="#22D3EE" />
        </linearGradient>
      </defs>

      {/* Antenna ball */}
      <circle className="bot-antenna-ball" cx="32" cy="6" r="3.5" fill="url(#botGrad)" />
      {/* Antenna stem */}
      <rect className="bot-antenna-stem" x="30" y="8" width="4" height="8" rx="2" fill="url(#botGrad)" />

      {/* Head */}
      <rect className="bot-head" x="8" y="16" width="48" height="40" rx="12" fill="url(#botGrad)" />

      {/* Eyes — white sclera */}
      <ellipse className="bot-eye bot-eye-left" cx="23" cy="34" rx="5.5" ry="5.5" fill="#ffffff" />
      <ellipse className="bot-eye bot-eye-right" cx="41" cy="34" rx="5.5" ry="5.5" fill="#ffffff" />

      {/* Pupils */}
      <circle className="bot-pupil bot-pupil-left" cx="24.5" cy="33.5" r="2.5" fill="#1E1B4B" />
      <circle className="bot-pupil bot-pupil-right" cx="42.5" cy="33.5" r="2.5" fill="#1E1B4B" />

      {/* Smile */}
      <path
        className="bot-smile"
        d="M24 44 C28 48, 36 48, 40 44"
        stroke="#ffffff"
        strokeWidth="2.5"
        strokeLinecap="round"
        fill="none"
      />

      {/* Ear accents */}
      <rect className="bot-ear bot-ear-left" x="2" y="28" width="6" height="14" rx="3" fill="url(#botGrad)" />
      <rect className="bot-ear bot-ear-right" x="56" y="28" width="6" height="14" rx="3" fill="url(#botGrad)" />
    </svg>
  );
}
