import { useRef, useEffect } from 'react';

export default function ChatInput({
  value,
  onChange,
  onSend,
  placeholder,
  variant, // 'landing' | 'footer'
}) {
  const textareaRef = useRef(null);

  // Auto-resize for footer variant
  useEffect(() => {
    if (variant === 'footer' && textareaRef.current) {
      const el = textareaRef.current;
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 128) + 'px';
    }
  }, [value, variant]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSend();
    }
  };

  if (variant === 'landing') {
    return (
      <div className="chat-input-landing">
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          rows={2}
          aria-label="Type your message"
        />
        <div className="chat-input-actions">
          <button className="btn-icon" aria-label="Attach a file">
            <span className="material-symbols-outlined">attach_file</span>
          </button>
          <button className="btn-send" aria-label="Send message" onClick={onSend}>
            <span className="material-symbols-outlined">arrow_upward</span>
          </button>
        </div>
      </div>
    );
  }

  // Footer variant
  return (
    <div className="chat-input-footer">
      <div className="chat-input-footer-inner">
        <div className="chat-input-bar">
          <button className="btn-icon" aria-label="Attach a file">
            <span className="material-symbols-outlined">attach_file</span>
          </button>
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={placeholder}
            rows={1}
            aria-label="Type your reply"
          />
          <button className="btn-send" aria-label="Send message" onClick={onSend}>
            <span className="material-symbols-outlined">arrow_upward</span>
          </button>
        </div>
        <div className="chat-input-disclaimer">
          AI can make mistakes. Please verify important information.
        </div>
      </div>
    </div>
  );
}
