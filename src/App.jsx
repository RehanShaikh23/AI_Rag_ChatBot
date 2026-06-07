import { useState, useRef, useCallback, useEffect } from 'react';
import './App.css';

import { AuthProvider, useAuth } from './contexts/AuthContext';
import AuthModal from './components/AuthModal';
import Sidebar from './components/Sidebar';
import MobileTopBar from './components/MobileTopBar';
import ChatInput from './components/ChatInput';
import StarterCards from './components/StarterCards';
import { UserMessage, BotMessage, TypingIndicator, StreamingBotMessage } from './components/ChatMessage';
import { sendMessage, streamMessage, getMessages } from './api/chat';
import { getConversations, deleteConversation } from './api/conversations';
import { uploadDocument } from './api/documents';

function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning.';
  if (hour < 17) return 'Good afternoon.';
  return 'Good evening.';
}

const starterCards = [
  {
    id: 'summarize',
    icon: 'description',
    label: 'Summarize',
    prompt: 'Summarize a long document or article into key points.',
    colorClass: 'card-primary',
  },
  {
    id: 'code',
    icon: 'code',
    label: 'Code',
    prompt: 'Write a Python script for data processing.',
    colorClass: 'card-info',
  },
  {
    id: 'analyze',
    icon: 'analytics',
    label: 'Analyze',
    prompt: 'Analyze data sets and extract meaningful insights.',
    colorClass: 'card-success',
  },
];

function ChatApp() {
  const { user, isAuthenticated, logout } = useAuth();

  // ---- State ----
  const [view, setView] = useState('new-chat'); // 'new-chat' | 'active-chat'
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeNav, setActiveNav] = useState('new-chat');
  const [inputValue, setInputValue] = useState('');
  const [messages, setMessages] = useState([]); // { id, type, text, code?, streaming? }
  const [isTyping, setIsTyping] = useState(false);
  const [conversations, setConversations] = useState([]);
  const [activeConversationId, setActiveConversationId] = useState(null);
  const [streamingText, setStreamingText] = useState('');
  const [greeting] = useState(getGreeting);

  const threadRef = useRef(null);
  const abortStreamRef = useRef(null);

  // ---- Load conversations on auth ----
  useEffect(() => {
    if (isAuthenticated) {
      loadConversations();
    }
  }, [isAuthenticated]);

  const loadConversations = useCallback(async () => {
    try {
      const data = await getConversations();
      setConversations(data.content || []);
    } catch (err) {
      console.error('Failed to load conversations:', err);
    }
  }, []);

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

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping, streamingText, scrollToBottom]);

  // ---- Keyboard: Escape closes sidebar ----
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') setSidebarOpen(false);
    };
    document.addEventListener('keydown', handleEsc);
    return () => document.removeEventListener('keydown', handleEsc);
  }, []);

  // ---- Send message (streaming) ----
  const handleSendMessage = useCallback(async (text) => {
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
    setIsTyping(true);
    setStreamingText('');

    // Start streaming response
    const abort = streamMessage(
      activeConversationId,
      trimmed,
      true,
      // onToken
      (token) => {
        setStreamingText((prev) => prev + token);
      },
      // onDone
      (meta) => {
        setStreamingText((prev) => {
          const finalText = prev;
          // Move streaming text to a permanent message
          const codeBlock = extractCodeBlock(finalText);
          const botMsg = {
            id: Date.now() + 1,
            type: 'bot',
            text: finalText,
            code: codeBlock,
          };
          setMessages((msgs) => [...msgs, botMsg]);
          return '';
        });

        setIsTyping(false);

        // Update conversation ID
        if (meta?.conversationId) {
          setActiveConversationId(meta.conversationId);
        }

        // Refresh sidebar conversations
        loadConversations();
      },
      // onError
      (err) => {
        console.error('Stream error:', err);
        setIsTyping(false);
        setStreamingText('');

        // Fallback: try synchronous request
        handleSendSync(trimmed);
      }
    );

    abortStreamRef.current = abort;
  }, [isTyping, view, activeConversationId, loadConversations]);

  // ---- Fallback synchronous send ----
  const handleSendSync = useCallback(async (text) => {
    try {
      setIsTyping(true);
      const response = await sendMessage(activeConversationId, text);

      const botMsg = {
        id: response.messageId || Date.now(),
        type: 'bot',
        text: response.content,
        code: response.codeBlock,
      };
      setMessages((prev) => [...prev, botMsg]);

      if (response.conversationId) {
        setActiveConversationId(response.conversationId);
      }

      loadConversations();
    } catch (err) {
      console.error('Chat error:', err);
      const errorMsg = {
        id: Date.now(),
        type: 'bot',
        text: `Sorry, I encountered an error: ${err.message}. Please try again.`,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  }, [activeConversationId, loadConversations]);

  // ---- Load a conversation from sidebar ----
  const loadConversation = useCallback(async (convId) => {
    try {
      const chatMessages = await getMessages(convId);
      const mapped = chatMessages.map((msg) => ({
        id: msg.messageId,
        type: msg.role === 'USER' ? 'user' : 'bot',
        text: msg.content,
        code: msg.codeBlock,
      }));
      setMessages(mapped);
      setActiveConversationId(convId);
      setView('active-chat');
      setActiveNav('');
      setSidebarOpen(false);
    } catch (err) {
      console.error('Failed to load conversation:', err);
    }
  }, []);

  // ---- Handlers ----
  const handleSend = useCallback(() => {
    handleSendMessage(inputValue);
  }, [inputValue, handleSendMessage]);

  const handleNewChat = useCallback(() => {
    if (abortStreamRef.current) {
      abortStreamRef.current();
      abortStreamRef.current = null;
    }
    setMessages([]);
    setInputValue('');
    setIsTyping(false);
    setStreamingText('');
    setActiveConversationId(null);
    setView('new-chat');
    setActiveNav('new-chat');
    setSidebarOpen(false);
  }, []);

  const handleNavClick = useCallback((nav) => {
    setActiveNav(nav);
    setSidebarOpen(false);
  }, []);

  const handleCardClick = useCallback((prompt) => {
    handleSendMessage(prompt);
  }, [handleSendMessage]);

  const handleDeleteConversation = useCallback(async (convId) => {
    try {
      await deleteConversation(convId);
      if (activeConversationId === convId) {
        handleNewChat();
      }
      loadConversations();
    } catch (err) {
      console.error('Failed to delete conversation:', err);
    }
  }, [activeConversationId, handleNewChat, loadConversations]);

  const handleFileUpload = useCallback(async (file) => {
    try {
      await uploadDocument(file);
      // TODO: show success notification
    } catch (err) {
      console.error('Upload failed:', err);
    }
  }, []);

  // ---- Render ----
  return (
    <div className="app-shell">
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        activeNav={activeNav}
        onNavClick={handleNavClick}
        onNewChat={handleNewChat}
        conversations={conversations}
        onSelectConversation={loadConversation}
        onDeleteConversation={handleDeleteConversation}
        user={user}
        onLogout={logout}
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
                onFileUpload={handleFileUpload}
                placeholder="How can I help you today?"
              />

              <StarterCards onCardClick={handleCardClick} cards={starterCards} />
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
                {isTyping && streamingText ? (
                  <StreamingBotMessage text={streamingText} />
                ) : isTyping ? (
                  <TypingIndicator />
                ) : null}
              </div>
            </div>

            <ChatInput
              variant="footer"
              value={inputValue}
              onChange={setInputValue}
              onSend={handleSend}
              onFileUpload={handleFileUpload}
              placeholder="Reply to AI ChatBot..."
            />
          </section>
        )}
      </main>
    </div>
  );
}

// ---- Extract code block from markdown ----
function extractCodeBlock(text) {
  if (!text) return null;
  const match = text.match(/```(\w+)?\n([\s\S]*?)```/);
  if (match) {
    return { lang: match[1] || 'text', content: match[2].trim() };
  }
  return null;
}

// ---- Root component with auth wrapper ----
export default function App() {
  return (
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  );
}

function AppRouter() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return (
      <div className="app-loading">
        <div className="app-loading-spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <AuthModal />;
  }

  return <ChatApp />;
}
