/**
 * Sidebar — real conversation history from backend, user profile, and logout.
 */
export default function Sidebar({
  isOpen,
  onClose,
  activeNav,
  onNavClick,
  onNewChat,
  conversations = [],
  onSelectConversation,
  onDeleteConversation,
  user,
  onLogout,
}) {
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

          {/* Chat History — from backend */}
          <div className="sidebar-history">
            <span className="sidebar-history-label">Recent</span>
            {conversations.length === 0 && (
              <span className="sidebar-history-empty">No conversations yet</span>
            )}
            {conversations.map((conv) => (
              <div className="sidebar-history-row" key={conv.id}>
                <a
                  className="sidebar-history-item"
                  href="#"
                  onClick={(e) => { e.preventDefault(); onSelectConversation(conv.id); }}
                  title={conv.title}
                >
                  <span className="material-symbols-outlined">chat_bubble_outline</span>
                  <span>{conv.title?.length > 28 ? conv.title.substring(0, 28) + '…' : conv.title}</span>
                </a>
                <button
                  className="sidebar-history-delete"
                  onClick={(e) => { e.stopPropagation(); onDeleteConversation(conv.id); }}
                  aria-label="Delete conversation"
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 16 }}>delete</span>
                </button>
              </div>
            ))}
          </div>
        </nav>

        {/* Footer */}
        <div className="sidebar-footer">
          {user && (
            <div className="sidebar-user">
              <div className="sidebar-user-avatar">
                {user.displayName?.[0]?.toUpperCase() || 'U'}
              </div>
              <div className="sidebar-user-info">
                <span className="sidebar-user-name">{user.displayName}</span>
                <span className="sidebar-user-email">{user.email}</span>
              </div>
            </div>
          )}
          <nav className="sidebar-footer-nav">
            <a className="sidebar-footer-link" href="#" onClick={(e) => e.preventDefault()}>
              <span className="material-symbols-outlined">help</span>
              <span>Help &amp; Support</span>
            </a>
            <a
              className="sidebar-footer-link"
              href="#"
              onClick={(e) => { e.preventDefault(); onLogout?.(); }}
            >
              <span className="material-symbols-outlined">logout</span>
              <span>Log out</span>
            </a>
          </nav>
        </div>
      </aside>
    </>
  );
}
