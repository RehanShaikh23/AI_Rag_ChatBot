import { useState, useRef, useCallback, useEffect } from 'react';
import './App.css';

import Sidebar from './components/Sidebar';
import MobileTopBar from './components/MobileTopBar';
import ChatInput from './components/ChatInput';
import StarterCards from './components/StarterCards';
import { UserMessage, BotMessage, TypingIndicator } from './components/ChatMessage';
import { getResponse, getGreeting } from './data/responses';

export default function App() {
  // ---- State ----
  const [view, setView] = useState('new-chat'); // 'new-chat' | 'active-chat'
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeNav, setActiveNav] = useState('new-chat');
  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState([]); // { id, type: 'user'|'bot', text, code? }
  const [isTyping, setIsTyping] = useState(false);
  const [chatHistory, setChatHistory] = useState([]);
  const [greeting] = useState(getGreeting);

  const threadRef = useRef(null);
  const typingTimeoutRef = useRef(null);
  const responseTimeoutRef = useRef(null);

  // ---- Scroll to bottom ----
  const scrollToBottom = useCallback(() => {
    requestAnimationFrame(() => {
      if (threadRef.current) {
        threadRef.current.scrollTo({
          top: threadRef.current.scrollHeight,
          behavior: 'smooth',
        });
      }
    });
  }, []);

  // Scroll when messages change or typing state changes
  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping, scrollToBottom]);

  // ---- Cleanup timeouts on unmount ----
  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
      if (responseTimeoutRef.current) clearTimeout(responseTimeoutRef.current);
    };
  }, []);

  // ---- Keyboard: Escape closes sidebar ----
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') setSidebarOpen(false);
    };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, []);

  // ---- Send message ----
  const sendMessage = useCallback((text) => {
    const trimmed = text?.trim();
    if (!trimmed || isTyping) return;

    // Switch to chat view
    if (view !== 'active-chat') {
      setView('active-chat');
      setActiveNav('');
    }

    // Add user message
    const userMsg = { id: Date.now(), type: 'user', text: trimmed };
    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');

    // Get AI response
    const response = getResponse(trimmed);

    // Show typing indicator
    setIsTyping(true);

    typingTimeoutRef.current = setTimeout(() => {
      const responseDelay = 600 + Math.random() * 1200;

      responseTimeoutRef.current = setTimeout(() => {
        const botMsg = {
          id: Date.now() + 1,
          type: 'bot',
          text: response.text,
          code: response.code,
        };
        setMessages((prev) => [...prev, botMsg]);
        setIsTyping(false);

        // Add to sidebar history
        setChatHistory((prev) => [trimmed, ...prev]);
      }, responseDelay);
    }, 500);
  }, [isTyping, view]);

  // ---- Handlers ----
  const handleSend = useCallback(() => {
    sendMessage(inputValue);
  }, [inputValue, sendMessage]);

  const handleNewChat = useCallback(() => {
    // Clear timeouts
    if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
    if (responseTimeoutRef.current) clearTimeout(responseTimeoutRef.current);

    setMessages([]);
    setInputValue('');
    setIsTyping(false);
    setView('new-chat');
    setActiveNav('new-chat');
    setSidebarOpen(false);
  }, []);

  const handleNavClick = useCallback((nav) => {
    setActiveNav(nav);
    setSidebarOpen(false);
  }, []);

  const handleCardClick = useCallback((prompt) => {
    sendMessage(prompt);
  }, [sendMessage]);

  // ---- Render ----
  return (
    <div className="app-shell">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        activeNav={activeNav}
        onNavClick={handleNavClick}
        onNewChat={handleNewChat}
        chatHistory={chatHistory}
      />

      <main className="main-content">
        <MobileTopBar
          onMenuToggle={() => setSidebarOpen((o) => !o)}
          onNewChat={handleNewChat}
        />

        {/* ===== New Chat View ===== */}
        {view === 'new-chat' && (
          <section className="view-new-chat">
            <div className="new-chat-container">
              <h1 className="greeting-text">{greeting}</h1>

              <ChatInput
                variant="landing"
                value={inputValue}
                onChange={setInputValue}
                onSend={handleSend}
                placeholder="How can I help you today?"
              />

              <StarterCards onCardClick={handleCardClick} />
            </div>
          </section>
        )}

        {/* ===== Active Chat View ===== */}
        {view === 'active-chat' && (
          <section className="view-active-chat">
            <div className="message-thread" ref={threadRef}>
              <div className="message-thread-inner">
                {messages.map((msg) =>
                  msg.type === 'user' ? (
                    <UserMessage key={msg.id} text={msg.text} />
                  ) : (
                    <BotMessage key={msg.id} text={msg.text} code={msg.code} />
                  )
                )}
                {isTyping && <TypingIndicator />}
              </div>
            </div>

            <ChatInput
              variant="footer"
              value={inputValue}
              onChange={setInputValue}
              onSend={handleSend}
              placeholder="Reply to AI ChatBot..."
            />
          </section>
        )}
      </main>
    </div>
  );
}
