import CodeBlock from './CodeBlock';

function formatText(text) {
  if (!text) return null;

  // Remove code blocks from the text display (they're rendered separately)
  const cleanedText = text.replace(/```\w*\n[\s\S]*?```/g, '').trim();
  if (!cleanedText) return null;

  return cleanedText.split('\n\n').map((paragraph, i) => {
    // Bold
    let html = paragraph.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>');
    // Bullet points
    html = html.replace(/^[•\-] (.+)$/gm, '<span style="display:block;padding-left:16px;">• $1</span>');
    // Numbered lists
    html = html.replace(/^(\d+)\. (.+)$/gm, '<span style="display:block;padding-left:16px;">$1. $2</span>');
    return <p key={i} dangerouslySetInnerHTML={{ __html: html }} />;
  });
}

/**
 * Extract the first code block from text (markdown-style).
 */
function extractCodeBlock(text) {
  if (!text) return null;
  const match = text.match(/```(\w+)?\n([\s\S]*?)```/);
  if (match) {
    return { lang: match[1] || 'text', content: match[2].trim() };
  }
  return null;
}

export function UserMessage({ text }) {
  return (
    <div className="message">
      <div className="message-avatar user-avatar">
        <span className="material-symbols-outlined">person</span>
      </div>
      <div className="message-body">
        <span className="message-sender">You</span>
        <div className="message-text">
          <p>{text}</p>
        </div>
      </div>
    </div>
  );
}

export function BotMessage({ text, code }) {
  // If code is passed directly, use it; otherwise try to extract from text
  const codeBlock = code || extractCodeBlock(text);

  return (
    <div className="message">
      <div className="message-avatar bot-avatar">
        <span className="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div className="message-body">
        <span className="message-sender">AI ChatBot</span>
        <div className="message-text">
          {formatText(text)}
          {codeBlock && <CodeBlock lang={codeBlock.lang} content={codeBlock.content} />}
        </div>
      </div>
    </div>
  );
}

/**
 * Streaming bot message — shows text as it arrives token-by-token.
 */
export function StreamingBotMessage({ text }) {
  const codeBlock = extractCodeBlock(text);

  return (
    <div className="message streaming">
      <div className="message-avatar bot-avatar">
        <span className="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div className="message-body">
        <span className="message-sender">AI ChatBot</span>
        <div className="message-text">
          {formatText(text)}
          {codeBlock && <CodeBlock lang={codeBlock.lang} content={codeBlock.content} />}
          <span className="streaming-cursor" />
        </div>
      </div>
    </div>
  );
}

export function TypingIndicator() {
  return (
    <div className="typing-indicator">
      <div className="message-avatar bot-avatar">
        <span className="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div className="message-body">
        <span className="message-sender">AI ChatBot</span>
        <div className="typing-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>
  );
}
