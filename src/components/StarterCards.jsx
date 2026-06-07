export default function StarterCards({ onCardClick, cards }) {
  return (
    <div className="starter-cards">
      {(cards || []).map((card) => (
        <div
          key={card.id}
          className={`starter-card ${card.colorClass}`}
          onClick={() => onCardClick(card.prompt)}
        >
          <div className="starter-card-header">
            <span className="material-symbols-outlined">{card.icon}</span>
            <span>{card.label}</span>
          </div>
          <p className="starter-card-body">{card.prompt}</p>
        </div>
      ))}
    </div>
  );
}
