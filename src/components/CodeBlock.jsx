import { useState, useCallback } from 'react';

export default function CodeBlock({ lang, content }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(content).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }).catch(() => {
      // Fallback: select the code text
      const el = document.querySelector(`[data-code-id="${lang}-${content.length}"] code`);
      if (el) {
        const range = document.createRange();
        range.selectNodeContents(el);
        const sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
      }
    });
  }, [content, lang]);

  return (
    <div className="code-block" data-code-id={`${lang}-${content.length}`}>
      <div className="code-block-header">
        <span className="code-block-lang">
          <span className="material-symbols-outlined">code</span>
          {lang}
        </span>
        <button className={`btn-copy${copied ? ' copied' : ''}`} onClick={handleCopy}>
          <span className="material-symbols-outlined">
            {copied ? 'check' : 'content_copy'}
          </span>
          <span>{copied ? 'Copied!' : 'Copy'}</span>
        </button>
      </div>
      <div className="code-block-body">
        <pre><code>{content}</code></pre>
      </div>
    </div>
  );
}
