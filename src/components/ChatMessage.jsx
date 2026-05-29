import CodeBlock from './CodeBlock';

function formatText(text) {
  return text.split('\n\n').map((paragraph, i) => {
    // Bold
    let html = paragraph.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    // Bullet points
    html = html.replace(/^• (.+)$/gm, '<span style="display:block;padding-left:16px;">• $1</span>');
    // Numbered lists
    html = html.replace(/^(\d+)\. (.+)$/gm, '<span style="display:block;padding-left:16px;">$1. $2</span>');
    return <p key={i} dangerouslySetInnerHTML={{ __html: html }} />;
  });
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
  return (
    <div className="message">
      <div className="message-avatar bot-avatar">
        <span className="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div className="message-body">
        <span className="message-sender">AI ChatBot</span>
        <div className="message-text">
          {formatText(text)}
          {code && <CodeBlock lang={code.lang} content={code.content} />}
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
