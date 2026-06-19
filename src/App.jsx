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
import { uploadDocument, getDocumentStatus } from './api/documents';

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
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [uploadingFile, setUploadingFile] = useState(null);
  const [activeDocumentId, setActiveDocumentId] = useState(null);

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

  // ---- Synchronous send (also used as fallback when streaming fails) ----
  const handleSendSync = useCallback(async (text) => {
    try {
      setIsTyping(true);
      const response = await sendMessage(activeConversationId, text, true, activeDocumentId);

      const botMsg = {
        id: response.messageId || (Date.now() + Math.random()),
        type: 'bot',
        text: response.content,
        code: response.codeBlock,
      };
      setMessages((prev) => [...prev, botMsg]);

      if (response.conversationId) {
        setActiveConversationId(response.conversationId);
      }

      loadConversations();
      setTimeout(() => loadConversations(), 1500);
    } catch (err) {
      console.error('Chat error:', err);
      const errorMsg = {
        id: Date.now() + Math.random(),
        type: 'bot',
        text: `Sorry, I encountered an error: ${err.message}. Please try again.`,
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  }, [activeConversationId, activeDocumentId, loadConversations]);

  // ---- Send message (streaming, with sync fallback) ----
  const handleSendMessage = useCallback(async (text) => {
    const trimmed = text?.trim();
    if (!trimmed || isTyping) return;

    // Switch to chat view
    if (view !== 'active-chat') {
      setView('active-chat');
      setActiveNav('');
    }

    // Add user message optimistically
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
      activeDocumentId,
      // onToken
      (token) => {
        setStreamingText((prev) => prev + token);
      },
      // onDone — append final AI message to state immediately
      (meta) => {
        const finalText = meta?.accumulatedText || '';
        const codeBlock = extractCodeBlock(finalText);
        const botMsg = {
          id: Date.now() + 1,
          type: 'bot',
          text: finalText,
          code: codeBlock,
        };
        setMessages((msgs) => [...msgs, botMsg]);
        setStreamingText('');
        setIsTyping(false);

        // Update conversation ID
        if (meta?.conversationId) {
          setActiveConversationId(meta.conversationId);
        }

        // Refresh sidebar conversations.
        loadConversations();
        setTimeout(() => loadConversations(), 1500);
      },
      // onError — fall back to synchronous request
      (err) => {
        console.error('Stream error, falling back to sync:', err);
        setIsTyping(false);
        setStreamingText('');

        // Call sync fallback safely
        try {
          handleSendSync(trimmed);
        } catch (syncErr) {
          console.error('Sync fallback also failed:', syncErr);
          setIsTyping(false);
          setMessages((prev) => [...prev, {
            id: Date.now() + Math.random(),
            type: 'bot',
            text: 'Sorry, something went wrong. Please try again.',
          }]);
        }
      }
    );

    abortStreamRef.current = abort;
  }, [isTyping, view, activeConversationId, activeDocumentId, loadConversations, handleSendSync]);

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
    setActiveDocumentId(null);
    setUploadedFiles([]);
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
      setUploadingFile(file.name);
      const result = await uploadDocument(file);
      // Track the newly uploaded document as the active one for RAG queries
      const docId = result?.id || result?.data?.id || null;
      const newFile = {
        name: file.name,
        size: file.size,
        time: Date.now(),
        id: docId,
        status: 'PROCESSING', // Start as processing (async backend task)
        errorMessage: null,
      };
      if (docId) {
        setActiveDocumentId(docId);
        console.log('Active document set to:', docId);
      }
      setUploadedFiles((prev) => [...prev, newFile]);
      setUploadingFile(null);

      // Poll for processing status until COMPLETED or FAILED
      if (docId) {
        const pollInterval = setInterval(async () => {
          try {
            const docStatus = await getDocumentStatus(docId);
            if (!docStatus) return;

            if (docStatus.status === 'COMPLETED' || docStatus.status === 'FAILED') {
              clearInterval(pollInterval);
              setUploadedFiles((prev) =>
                prev.map((f) =>
                  f.id === docId
                    ? { ...f, status: docStatus.status, errorMessage: docStatus.errorMessage }
                    : f
                )
              );
              if (docStatus.status === 'FAILED') {
                console.error('Document processing failed:', docStatus.errorMessage);
              } else {
                console.log('Document processing completed:', docId);
              }
            }
          } catch (err) {
            console.error('Status poll failed:', err);
          }
        }, 2000);

        // Safety: stop polling after 2 minutes
        setTimeout(() => clearInterval(pollInterval), 120000);
      }
    } catch (err) {
      console.error('Upload failed:', err);
      setUploadingFile(null);
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
                disabled={isTyping}
                placeholder="How can I help you today?"
              />

              {/* Uploaded files indicator */}
              {(uploadingFile || uploadedFiles.length > 0) && (
                <div className="uploaded-files-bar">
                  {uploadingFile && (
                    <div className="uploaded-file-chip uploading">
                      <span className="material-symbols-outlined spinning">progress_activity</span>
                      <span>Uploading {uploadingFile}…</span>
                    </div>
                  )}
                  {uploadedFiles.map((f) => (
                    <div
                      className={`uploaded-file-chip ${f.status === 'FAILED' ? 'failed' : ''}`}
                      key={f.time}
                      title={f.status === 'FAILED' ? `Error: ${f.errorMessage || 'Processing failed'}` : ''}
                    >
                      <span className="material-symbols-outlined">
                        {f.status === 'FAILED' ? 'error' : 'description'}
                      </span>
                      <span>{f.name}</span>
                      {f.status === 'COMPLETED' && (
                        <span className="uploaded-file-check material-symbols-outlined">check_circle</span>
                      )}
                      {f.status === 'PROCESSING' && (
                        <span className="material-symbols-outlined spinning">progress_activity</span>
                      )}
                      {f.status === 'FAILED' && (
                        <span className="uploaded-file-error">Failed</span>
                      )}
                    </div>
                  ))}
                </div>
              )}

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

            {/* Uploaded files indicator in chat view */}
            {(uploadingFile || uploadedFiles.length > 0) && (
              <div className="uploaded-files-bar chat-view">
                {uploadingFile && (
                  <div className="uploaded-file-chip uploading">
                    <span className="material-symbols-outlined spinning">progress_activity</span>
                    <span>Uploading {uploadingFile}…</span>
                  </div>
                )}
                {uploadedFiles.map((f) => (
                  <div
                    className={`uploaded-file-chip ${f.status === 'FAILED' ? 'failed' : ''}`}
                    key={f.time}
                    title={f.status === 'FAILED' ? `Error: ${f.errorMessage || 'Processing failed'}` : ''}
                  >
                    <span className="material-symbols-outlined">
                      {f.status === 'FAILED' ? 'error' : 'description'}
                    </span>
                    <span>{f.name}</span>
                    {f.status === 'COMPLETED' && (
                      <span className="uploaded-file-check material-symbols-outlined">check_circle</span>
                    )}
                    {f.status === 'PROCESSING' && (
                      <span className="material-symbols-outlined spinning">progress_activity</span>
                    )}
                    {f.status === 'FAILED' && (
                      <span className="uploaded-file-error">Failed</span>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* Warning banner for failed documents */}
            {uploadedFiles.some((f) => f.id === activeDocumentId && f.status === 'FAILED') && (
              <div className="document-warning-banner">
                <span className="material-symbols-outlined">warning</span>
                <span>The active document failed to process. The AI cannot access its content. Please re-upload the file.</span>
              </div>
            )}

            <ChatInput
              variant="footer"
              value={inputValue}
              onChange={setInputValue}
              onSend={handleSend}
              onFileUpload={handleFileUpload}
              disabled={isTyping}
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
