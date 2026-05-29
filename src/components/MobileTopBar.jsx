export default function MobileTopBar({ onMenuToggle, onNewChat }) {
  return (
    <header className="topbar-mobile">
      <button
        className="topbar-btn"
        aria-label="Toggle sidebar menu"
        onClick={onMenuToggle}
      >
        <span className="material-symbols-outlined">menu</span>
      </button>
      <span className="topbar-mobile-brand">AI ChatBot</span>
      <button
        className="topbar-btn"
        aria-label="Start a new chat"
        onClick={onNewChat}
      >
        <span className="material-symbols-outlined">add</span>
      </button>
    </header>
  );
}
