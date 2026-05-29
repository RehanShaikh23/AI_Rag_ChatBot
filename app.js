/* ============================================================
   AI RAG ChatBot — Application Logic
   Handles view routing, chat engine, sidebar, and interactivity
   ============================================================ */

(function () {
  'use strict';

  // ---- DOM References ----
  const sidebar = document.getElementById('sidebar');
  const sidebarOverlay = document.getElementById('sidebarOverlay');
  const btnMenuToggle = document.getElementById('btnMenuToggle');
  const btnNewChatMobile = document.getElementById('btnNewChatMobile');
  const navNewChat = document.getElementById('navNewChat');

  const viewNewChat = document.getElementById('viewNewChat');
  const viewActiveChat = document.getElementById('viewActiveChat');

  const landingTextarea = document.getElementById('landingTextarea');
  const btnSendLanding = document.getElementById('btnSendLanding');
  const starterCards = document.getElementById('starterCards');

  const chatTextarea = document.getElementById('chatTextarea');
  const btnSendChat = document.getElementById('btnSendChat');
  const messageThreadInner = document.getElementById('messageThreadInner');
  const messageThread = document.getElementById('messageThread');

  const greetingText = document.getElementById('greetingText');

  // ---- State ----
  let currentView = 'new-chat'; // 'new-chat' | 'active-chat'
  let isTyping = false;

  // ---- Simulated AI Responses ----
  const aiResponses = {
    default: {
      text: `That's a great question! Let me think about this...\n\nAs an AI assistant powered by Retrieval-Augmented Generation (RAG), I can search through relevant knowledge bases and documents to provide you with accurate, contextual answers.\n\nHere are some key capabilities I can help with:\n\n• **Document Analysis** — Upload and summarize lengthy documents\n• **Code Generation** — Write, debug, and explain code across multiple languages\n• **Data Insights** — Analyze datasets and extract meaningful patterns\n• **Research** — Find and synthesize information from multiple sources\n\nFeel free to ask me anything specific, and I'll do my best to help!`,
      code: null
    },
    summarize: {
      text: `I'd be happy to help you summarize a document! Here's how you can use me effectively for summarization:\n\nJust paste your text or describe the document, and I'll extract the key points for you. I use RAG (Retrieval-Augmented Generation) to understand context and provide accurate summaries.\n\nHere's an example of how I structure summaries:`,
      code: {
        lang: 'markdown',
        content: `# Document Summary\n\n## Key Points\n- Main argument or thesis statement\n- Supporting evidence and data points\n- Notable conclusions or recommendations\n\n## Critical Insights\n- Patterns identified across sections\n- Gaps or areas needing further research\n\n## Action Items\n1. Review highlighted sections\n2. Cross-reference cited sources\n3. Validate statistical claims`
      }
    },
    code: {
      text: `Certainly! Here's a Python script that demonstrates a clean data processing pipeline. It reads CSV data, performs transformations, and outputs results with proper error handling.`,
      code: {
        lang: 'python',
        content: `<span class="token-keyword">import</span> pandas <span class="token-keyword">as</span> pd
<span class="token-keyword">from</span> pathlib <span class="token-keyword">import</span> Path

<span class="token-keyword">def</span> <span class="token-function">process_data</span>(input_path, output_path=<span class="token-string">"output.csv"</span>):
    <span class="token-string">"""
    Reads, cleans, and transforms CSV data.
    Returns processed DataFrame.
    """</span>
    <span class="token-keyword">try</span>:
        <span class="token-comment"># Read the source data</span>
        df = pd.read_csv(input_path)
        <span class="token-builtin">print</span>(<span class="token-string">f"Loaded {<span class="token-builtin">len</span>(df)} rows from {input_path}"</span>)

        <span class="token-comment"># Clean: drop nulls, normalize columns</span>
        df = df.dropna(subset=[<span class="token-string">"id"</span>, <span class="token-string">"value"</span>])
        df.columns = [c.lower().strip() <span class="token-keyword">for</span> c <span class="token-keyword">in</span> df.columns]

        <span class="token-comment"># Transform: add computed fields</span>
        df[<span class="token-string">"value_normalized"</span>] = (
            df[<span class="token-string">"value"</span>] / df[<span class="token-string">"value"</span>].max()
        )
        df[<span class="token-string">"category"</span>] = df[<span class="token-string">"value_normalized"</span>].apply(
            <span class="token-keyword">lambda</span> x: <span class="token-string">"high"</span> <span class="token-keyword">if</span> x > <span class="token-number">0.7</span> <span class="token-keyword">else</span> <span class="token-string">"low"</span>
        )

        <span class="token-comment"># Export results</span>
        df.to_csv(output_path, index=<span class="token-keyword">False</span>)
        <span class="token-builtin">print</span>(<span class="token-string">f"Saved {<span class="token-builtin">len</span>(df)} rows → {output_path}"</span>)
        <span class="token-keyword">return</span> df

    <span class="token-keyword">except</span> FileNotFoundError:
        <span class="token-builtin">print</span>(<span class="token-string">f"Error: {input_path} not found."</span>)
    <span class="token-keyword">except</span> pd.errors.EmptyDataError:
        <span class="token-builtin">print</span>(<span class="token-string">"Error: File is empty."</span>)

<span class="token-comment"># Usage</span>
result = process_data(<span class="token-string">"sales_data.csv"</span>)`
      }
    },
    analyze: {
      text: `Absolutely! Data analysis is one of my core strengths. Here's a practical example using Python's pandas and matplotlib to analyze a dataset and extract insights.\n\nThis script calculates summary statistics, detects outliers, and generates a visualization:`,
      code: {
        lang: 'python',
        content: `<span class="token-keyword">import</span> pandas <span class="token-keyword">as</span> pd
<span class="token-keyword">import</span> numpy <span class="token-keyword">as</span> np

<span class="token-keyword">def</span> <span class="token-function">analyze_dataset</span>(df, target_col):
    <span class="token-string">"""
    Performs comprehensive analysis on a DataFrame.
    Returns a summary dict with key insights.
    """</span>
    insights = {}

    <span class="token-comment"># Basic statistics</span>
    insights[<span class="token-string">"count"</span>] = <span class="token-builtin">len</span>(df)
    insights[<span class="token-string">"mean"</span>] = df[target_col].mean()
    insights[<span class="token-string">"median"</span>] = df[target_col].median()
    insights[<span class="token-string">"std_dev"</span>] = df[target_col].std()

    <span class="token-comment"># Outlier detection (IQR method)</span>
    Q1 = df[target_col].quantile(<span class="token-number">0.25</span>)
    Q3 = df[target_col].quantile(<span class="token-number">0.75</span>)
    IQR = Q3 - Q1
    outliers = df[
        (df[target_col] < Q1 - <span class="token-number">1.5</span> * IQR) |
        (df[target_col] > Q3 + <span class="token-number">1.5</span> * IQR)
    ]
    insights[<span class="token-string">"outlier_count"</span>] = <span class="token-builtin">len</span>(outliers)
    insights[<span class="token-string">"outlier_pct"</span>] = <span class="token-builtin">round</span>(
        <span class="token-builtin">len</span>(outliers) / <span class="token-builtin">len</span>(df) * <span class="token-number">100</span>, <span class="token-number">2</span>
    )

    <span class="token-comment"># Top correlations</span>
    numeric = df.select_dtypes(include=[np.number])
    corr = numeric.corr()[target_col].drop(target_col)
    insights[<span class="token-string">"top_correlation"</span>] = corr.abs().idxmax()

    <span class="token-keyword">return</span> insights`
      }
    }
  };

  // ---- Generic responses for free-form questions ----
  const genericResponses = [
    {
      text: `That's an interesting question! Let me break it down for you.\n\nBased on my analysis, here are the key points to consider:\n\n1. **Context matters** — The answer depends on several factors specific to your use case\n2. **Best practices** — Following established patterns will save you time and reduce errors\n3. **Iteration** — Start with a minimal approach and refine based on feedback\n\nWould you like me to dive deeper into any specific aspect of this?`,
      code: null
    },
    {
      text: `Great question! Here's what I can tell you about this topic.\n\nThe RAG (Retrieval-Augmented Generation) approach allows me to provide more accurate and contextual responses by combining large language model capabilities with real-time information retrieval.\n\nFor your specific query, I'd recommend:\n\n• Start by clearly defining your requirements\n• Break the problem into smaller, manageable parts\n• Test and validate each component independently\n• Iterate based on results\n\nLet me know if you'd like a more detailed explanation or a code example!`,
      code: null
    },
    {
      text: `I'd be happy to help with that! Let me put together a comprehensive response.\n\nHere's my analysis:\n\n**Overview**\nThis is a well-structured problem that can be approached systematically. The key insight is to leverage modern tools and methodologies to achieve optimal results.\n\n**Recommendations**\n• Use a modular architecture for better maintainability\n• Implement proper error handling and logging\n• Write tests to ensure reliability\n• Document your approach for future reference\n\nShall I provide a more detailed walkthrough or code implementation?`,
      code: null
    }
  ];

  // ============================================================
  // Greeting — Time-based
  // ============================================================
  function updateGreeting() {
    const hour = new Date().getHours();
    let greeting;
    if (hour < 12) greeting = 'Good morning.';
    else if (hour < 17) greeting = 'Good afternoon.';
    else greeting = 'Good evening.';
    greetingText.textContent = greeting;
  }

  // ============================================================
  // View Routing
  // ============================================================
  function showView(view) {
    currentView = view;

    if (view === 'new-chat') {
      viewNewChat.classList.remove('hidden');
      viewActiveChat.classList.remove('visible');

      // Reset animations
      viewNewChat.style.display = '';
      requestAnimationFrame(() => {
        greetingText.style.animation = 'none';
        greetingText.offsetHeight; // trigger reflow
        greetingText.style.animation = '';

        const landingInput = document.getElementById('landingInput');
        landingInput.style.animation = 'none';
        landingInput.offsetHeight;
        landingInput.style.animation = '';

        starterCards.style.animation = 'none';
        starterCards.offsetHeight;
        starterCards.style.animation = '';
      });

    } else if (view === 'active-chat') {
      viewNewChat.classList.add('hidden');
      viewActiveChat.classList.add('visible');
    }
  }

  function switchToActiveChat() {
    showView('active-chat');
  }

  function startNewChat() {
    // Clear messages
    messageThreadInner.innerHTML = '';
    // Reset textareas
    landingTextarea.value = '';
    chatTextarea.value = '';
    // Switch view
    showView('new-chat');
    // Update active nav
    setActiveNav(navNewChat);
    // Close mobile sidebar
    closeSidebar();
    // Focus the landing textarea
    setTimeout(() => landingTextarea.focus(), 400);
  }

  // ============================================================
  // Sidebar
  // ============================================================
  function openSidebar() {
    sidebar.classList.add('open');
    sidebarOverlay.classList.add('visible');
    document.body.style.overflow = 'hidden';
  }

  function closeSidebar() {
    sidebar.classList.remove('open');
    sidebarOverlay.classList.remove('visible');
    document.body.style.overflow = '';
  }

  function setActiveNav(activeEl) {
    document.querySelectorAll('.sidebar-nav-link').forEach(link => {
      link.classList.remove('active');
    });
    if (activeEl) activeEl.classList.add('active');
  }

  // ============================================================
  // Message Rendering
  // ============================================================
  function createUserMessage(text) {
    const msg = document.createElement('div');
    msg.className = 'message';
    msg.innerHTML = `
      <div class="message-avatar user-avatar">
        <span class="material-symbols-outlined">person</span>
      </div>
      <div class="message-body">
        <span class="message-sender">You</span>
        <div class="message-text">
          <p>${escapeHTML(text)}</p>
        </div>
      </div>
    `;
    return msg;
  }

  function createBotMessage(text, codeBlock) {
    const msg = document.createElement('div');
    msg.className = 'message';

    // Build text paragraphs
    const paragraphs = text.split('\n\n').map(p => {
      // Convert markdown-like bold to <strong>
      let processed = p.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
      // Convert markdown-like bullet points
      processed = processed.replace(/^• (.+)$/gm, '<span style="display:block;padding-left:16px;">• $1</span>');
      // Convert numbered lists
      processed = processed.replace(/^(\d+)\. (.+)$/gm, '<span style="display:block;padding-left:16px;">$1. $2</span>');
      return `<p>${processed}</p>`;
    }).join('');

    // Build code block HTML if present
    let codeHTML = '';
    if (codeBlock) {
      const copyId = 'copy-' + Date.now();
      codeHTML = `
        <div class="code-block">
          <div class="code-block-header">
            <span class="code-block-lang">
              <span class="material-symbols-outlined">code</span>
              ${escapeHTML(codeBlock.lang)}
            </span>
            <button class="btn-copy" id="${copyId}" data-code="${encodeURIComponent(stripHTML(codeBlock.content))}">
              <span class="material-symbols-outlined">content_copy</span>
              <span>Copy</span>
            </button>
          </div>
          <div class="code-block-body">
            <pre><code>${codeBlock.content}</code></pre>
          </div>
        </div>
      `;
    }

    msg.innerHTML = `
      <div class="message-avatar bot-avatar">
        <span class="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div class="message-body">
        <span class="message-sender">AI ChatBot</span>
        <div class="message-text">
          ${paragraphs}
          ${codeHTML}
        </div>
      </div>
    `;

    // Attach copy handler after rendering
    setTimeout(() => {
      if (codeBlock) {
        const copyBtn = msg.querySelector('.btn-copy');
        if (copyBtn) {
          copyBtn.addEventListener('click', handleCopy);
        }
      }
    }, 0);

    return msg;
  }

  function createTypingIndicator() {
    const indicator = document.createElement('div');
    indicator.className = 'typing-indicator';
    indicator.id = 'typingIndicator';
    indicator.innerHTML = `
      <div class="message-avatar bot-avatar">
        <span class="material-symbols-outlined icon-fill">smart_toy</span>
      </div>
      <div class="message-body">
        <span class="message-sender">AI ChatBot</span>
        <div class="typing-dots">
          <span></span><span></span><span></span>
        </div>
      </div>
    `;
    return indicator;
  }

  // ============================================================
  // Chat Engine
  // ============================================================
  function sendMessage(text) {
    if (!text.trim() || isTyping) return;

    // Switch to active chat if needed
    if (currentView !== 'active-chat') {
      switchToActiveChat();
    }

    // Add user message
    const userMsg = createUserMessage(text.trim());
    messageThreadInner.appendChild(userMsg);
    scrollToBottom();

    // Determine response
    const response = getResponse(text.trim());

    // Show typing indicator
    isTyping = true;
    const typingDelay = 500;
    setTimeout(() => {
      const typing = createTypingIndicator();
      messageThreadInner.appendChild(typing);
      scrollToBottom();

      // Simulate response delay (600-1800ms)
      const responseDelay = 600 + Math.random() * 1200;
      setTimeout(() => {
        // Remove typing indicator
        const typingEl = document.getElementById('typingIndicator');
        if (typingEl) typingEl.remove();

        // Add bot response
        const botMsg = createBotMessage(response.text, response.code);
        messageThreadInner.appendChild(botMsg);
        scrollToBottom();
        isTyping = false;

        // Add to chat history in sidebar
        addToChatHistory(text.trim());
      }, responseDelay);
    }, typingDelay);

    // Clear input
    landingTextarea.value = '';
    chatTextarea.value = '';
    autoResizeTextarea(chatTextarea);
  }

  function getResponse(userText) {
    const lower = userText.toLowerCase();

    if (lower.includes('summarize') || lower.includes('summary') || lower.includes('key points')) {
      return aiResponses.summarize;
    }
    if (lower.includes('code') || lower.includes('script') || lower.includes('python') || lower.includes('function') || lower.includes('program')) {
      return aiResponses.code;
    }
    if (lower.includes('analyze') || lower.includes('analysis') || lower.includes('data') || lower.includes('insight')) {
      return aiResponses.analyze;
    }

    // Return a random generic response
    const idx = Math.floor(Math.random() * genericResponses.length);
    return genericResponses[idx];
  }

  function scrollToBottom() {
    requestAnimationFrame(() => {
      messageThread.scrollTo({
        top: messageThread.scrollHeight,
        behavior: 'smooth'
      });
    });
  }

  function addToChatHistory(text) {
    const historySection = document.getElementById('chatHistory');
    const truncated = text.length > 30 ? text.substring(0, 30) + '…' : text;

    const item = document.createElement('a');
    item.className = 'sidebar-history-item';
    item.href = '#';
    item.innerHTML = `
      <span class="material-symbols-outlined">chat_bubble_outline</span>
      <span>${escapeHTML(truncated)}</span>
    `;

    // Insert after the label
    const label = historySection.querySelector('.sidebar-history-label');
    if (label && label.nextSibling) {
      historySection.insertBefore(item, label.nextSibling);
    } else {
      historySection.appendChild(item);
    }
  }

  // ============================================================
  // Copy to Clipboard
  // ============================================================
  function handleCopy(e) {
    const btn = e.currentTarget;
    const rawCode = decodeURIComponent(btn.dataset.code);

    navigator.clipboard.writeText(rawCode).then(() => {
      const labelSpan = btn.querySelector('span:last-child');
      const iconSpan = btn.querySelector('.material-symbols-outlined');
      const originalLabel = labelSpan.textContent;
      const originalIcon = iconSpan.textContent;

      labelSpan.textContent = 'Copied!';
      iconSpan.textContent = 'check';
      btn.style.color = 'var(--success)';

      setTimeout(() => {
        labelSpan.textContent = originalLabel;
        iconSpan.textContent = originalIcon;
        btn.style.color = '';
      }, 2000);
    }).catch(() => {
      // Fallback: select text
      const codeEl = btn.closest('.code-block').querySelector('code');
      const range = document.createRange();
      range.selectNodeContents(codeEl);
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
    });
  }

  // ============================================================
  // Textarea Auto-Resize
  // ============================================================
  function autoResizeTextarea(textarea) {
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 128) + 'px';
  }

  // ============================================================
  // Utility Functions
  // ============================================================
  function escapeHTML(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  function stripHTML(html) {
    const tmp = document.createElement('div');
    tmp.innerHTML = html;
    return tmp.textContent || tmp.innerText || '';
  }

  // ============================================================
  // Event Listeners
  // ============================================================

  // -- Sidebar toggle (mobile) --
  btnMenuToggle.addEventListener('click', () => {
    if (sidebar.classList.contains('open')) {
      closeSidebar();
    } else {
      openSidebar();
    }
  });

  sidebarOverlay.addEventListener('click', closeSidebar);

  // -- New Chat --
  navNewChat.addEventListener('click', (e) => {
    e.preventDefault();
    startNewChat();
  });

  btnNewChatMobile.addEventListener('click', (e) => {
    e.preventDefault();
    startNewChat();
  });

  // -- Sidebar nav links --
  document.querySelectorAll('.sidebar-nav-link').forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      setActiveNav(link);
      closeSidebar();
    });
  });

  // -- Send from landing --
  btnSendLanding.addEventListener('click', () => {
    sendMessage(landingTextarea.value);
  });

  landingTextarea.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(landingTextarea.value);
    }
  });

  // -- Send from chat --
  btnSendChat.addEventListener('click', () => {
    sendMessage(chatTextarea.value);
  });

  chatTextarea.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage(chatTextarea.value);
    }
  });

  chatTextarea.addEventListener('input', () => {
    autoResizeTextarea(chatTextarea);
  });

  // -- Starter cards --
  starterCards.addEventListener('click', (e) => {
    const card = e.target.closest('.starter-card');
    if (card) {
      const prompt = card.dataset.prompt;
      sendMessage(prompt);
    }
  });

  // -- Keyboard shortcut: Escape to close sidebar --
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && sidebar.classList.contains('open')) {
      closeSidebar();
    }
  });

  // ============================================================
  // Initialize
  // ============================================================
  updateGreeting();
  showView('new-chat');

  // Focus landing textarea on desktop
  if (window.innerWidth > 768) {
    setTimeout(() => landingTextarea.focus(), 500);
  }

})();
