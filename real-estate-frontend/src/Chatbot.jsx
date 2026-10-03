import { useEffect, useRef, useState } from 'react';
import { sendChatbotMessage } from './api';
import './Chatbot.css';

export default function Chatbot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'assistant',
      content: 'Hi! How can I help with your real-estate questions?',
    },
  ]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef(null);
  const messagesRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    const container = messagesRef.current;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  }, [messages, loading]);

  async function handleSubmit(event) {
    event.preventDefault();
    const message = draft.trim();

    if (!message || loading) return;

    setDraft('');
    setError('');
    setMessages((current) => [
      ...current,
      { id: `user-${Date.now()}`, role: 'user', content: message },
    ]);
    setLoading(true);

    try {
      const response = await sendChatbotMessage(message);
      setMessages((current) => [
        ...current,
        {
          id: `assistant-${Date.now()}`,
          role: 'assistant',
          content: response?.reply || 'I was unable to generate a reply.',
        },
      ]);
    } catch (requestError) {
      setError(requestError.message || 'Unable to reach the chatbot. Please try again.');
    } finally {
      setLoading(false);
      inputRef.current?.focus();
    }
  }

  return (
    <div className="chatbot-widget">
      {isOpen && (
        <section className="chatbot-panel" aria-label="Real-estate assistant">
          <header className="chatbot-header">
            <div>
              <h2>Property assistant</h2>
              <p>Real-estate questions, answered</p>
            </div>
            <button
              type="button"
              className="chatbot-close"
              onClick={() => setIsOpen(false)}
              aria-label="Close chatbot"
            >
              ×
            </button>
          </header>

          <div className="chatbot-messages" ref={messagesRef} aria-live="polite">
            {messages.map((item) => (
              <div key={item.id} className={`chatbot-message ${item.role}`}>
                {item.content}
              </div>
            ))}
            {loading && (
              <div className="chatbot-message assistant chatbot-loading">
                Thinking...
              </div>
            )}
          </div>

          {error && <p className="chatbot-error" role="alert">{error}</p>}

          <form className="chatbot-form" onSubmit={handleSubmit}>
            <input
              ref={inputRef}
              type="text"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="Ask a real-estate question..."
              aria-label="Message the property assistant"
              disabled={loading}
              maxLength={2000}
            />
            <button type="submit" disabled={loading || !draft.trim()}>
              Send
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        className="chatbot-toggle"
        onClick={() => setIsOpen((open) => !open)}
        aria-expanded={isOpen}
        aria-label={isOpen ? 'Close chatbot' : 'Open chatbot'}
      >
        <span className="chatbot-toggle-icon" aria-hidden="true">✦</span>
        <span>{isOpen ? 'Close' : 'Ask AI'}</span>
      </button>
    </div>
  );
}