import { useRef, useEffect, useCallback } from 'react';

export default function ChatInput({
  value,
  onChange,
  onSend,
  onFileUpload,
  placeholder,
  variant, // 'landing' | 'footer'
  disabled = false,
}) {
  const textareaRef = useRef(null);
  const fileInputRef = useRef(null);

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
      if (!disabled) onSend();
    }
  };

  const handleFileClick = useCallback(() => {
    fileInputRef.current?.click();
  }, []);

  const handleFileChange = useCallback((e) => {
    const file = e.target.files?.[0];
    if (file && onFileUpload) {
      onFileUpload(file);
    }
    // Reset input for same-file re-upload
    e.target.value = '';
  }, [onFileUpload]);

  // Hidden file input for RAG document upload
  const fileInput = (
    <input
      ref={fileInputRef}
      type="file"
      accept=".pdf,.txt,.md,.docx,.doc,.csv"
      style={{ display: 'none' }}
      onChange={handleFileChange}
    />
  );

  if (variant === 'landing') {
    return (
      <div className={`chat-input-landing${disabled ? ' chat-input-disabled' : ''}`}>
        {fileInput}
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          rows={2}
          disabled={disabled}
          aria-label="Type your message"
        />
        <div className="chat-input-actions">
          <button className="btn-icon" aria-label="Upload a document for RAG" onClick={handleFileClick} disabled={disabled}>
            <span className="material-symbols-outlined">attach_file</span>
          </button>
          <button className="btn-send" aria-label="Send message" onClick={onSend} disabled={disabled}>
            <span className="material-symbols-outlined">arrow_upward</span>
          </button>
        </div>
      </div>
    );
  }

  // Footer variant
  return (
    <div className="chat-input-footer">
      {fileInput}
      <div className="chat-input-footer-inner">
        <div className={`chat-input-bar${disabled ? ' chat-input-disabled' : ''}`}>
          <button className="btn-icon" aria-label="Upload a document for RAG" onClick={handleFileClick} disabled={disabled}>
            <span className="material-symbols-outlined">attach_file</span>
          </button>
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={disabled ? 'AI is responding...' : placeholder}
            rows={1}
            disabled={disabled}
            aria-label="Type your reply"
          />
          <button className="btn-send" aria-label="Send message" onClick={onSend} disabled={disabled}>
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
