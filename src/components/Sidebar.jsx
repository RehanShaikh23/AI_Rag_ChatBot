import { defaultHistory } from '../data/responses';

export default function Sidebar({
  isOpen,
  onClose,
  activeNav,
  onNavClick,
  onNewChat,
  chatHistory,
}) {
  const allHistory = [...chatHistory, ...defaultHistory];

  return (
    <>
      {/* Overlay (mobile) */}
      <div
        className={`sidebar-overlay${isOpen ? ' visible' : ''}`}
        onClick={onClose}
      />

      {/* Sidebar */}
      <aside className={`sidebar${isOpen ? ' open' : ''}`}>
        {/* Brand */}
        <div className="sidebar-brand">
          <span className="sidebar-brand-name">AI ChatBot</span>
          <span className="sidebar-badge">RAG</span>
        </div>

        {/* Nav */}
        <nav className="sidebar-nav">
          <a
            className={`sidebar-nav-link${activeNav === 'new-chat' ? ' active' : ''}`}
            href="#"
            onClick={(e) => { e.preventDefault(); onNewChat(); }}
          >
            <span className="material-symbols-outlined">add</span>
            <span>New Chat</span>
          </a>
          <a
            className={`sidebar-nav-link${activeNav === 'history' ? ' active' : ''}`}
            href="#"
            onClick={(e) => { e.preventDefault(); onNavClick('history'); }}
          >
            <span className="material-symbols-outlined">history</span>
            <span>History</span>
          </a>
          <a
            className={`sidebar-nav-link${activeNav === 'projects' ? ' active' : ''}`}
            href="#"
            onClick={(e) => { e.preventDefault(); onNavClick('projects'); }}
          >
            <span className="material-symbols-outlined">folder</span>
            <span>Projects</span>
          </a>
          <a
            className={`sidebar-nav-link${activeNav === 'settings' ? ' active' : ''}`}
            href="#"
            onClick={(e) => { e.preventDefault(); onNavClick('settings'); }}
          >
            <span className="material-symbols-outlined">settings</span>
            <span>Settings</span>
          </a>

          {/* Chat History */}
          <div className="sidebar-history">
            <span className="sidebar-history-label">Recent</span>
            {allHistory.map((item, i) => (
              <a className="sidebar-history-item" href="#" key={i} onClick={(e) => e.preventDefault()}>
                <span className="material-symbols-outlined">chat_bubble_outline</span>
                <span>{item.length > 30 ? item.substring(0, 30) + '…' : item}</span>
              </a>
            ))}
          </div>
        </nav>

        {/* Footer */}
        <div className="sidebar-footer">
          <button className="sidebar-upgrade-btn">Upgrade to Pro</button>
          <nav className="sidebar-footer-nav">
            <a className="sidebar-footer-link" href="#" onClick={(e) => e.preventDefault()}>
              <span className="material-symbols-outlined">help</span>
              <span>Help &amp; Support</span>
            </a>
            <a className="sidebar-footer-link" href="#" onClick={(e) => e.preventDefault()}>
              <span className="material-symbols-outlined">logout</span>
              <span>Log out</span>
            </a>
          </nav>
        </div>
      </aside>
    </>
  );
}
