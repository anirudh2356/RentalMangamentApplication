import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import {
  createConversation,
  fetchConversations,
  fetchMessages,
  markConversationAsRead,
  sendMessage as sendMessageApi,
  updateMessage,
  deleteMessage,
  fetchUsers,
} from './api';
import Reviews from './Reviews';
import Chatbot from './Chatbot';
import './App.css';



function formatTimestamp(iso) {
  if (!iso) return '';

  const date = new Date(iso);

  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  });
}

function formatConversationTime(iso) {
  if (!iso) return '';

  const date = new Date(iso);
  const now = new Date();

  const sameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();

  if (sameDay) {
    return date.toLocaleTimeString(undefined, {
      hour: 'numeric',
      minute: '2-digit',
    });
  }

  return date.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
  });
}

function getInitial(name) {
  if (!name) return '?';
  return name.trim().charAt(0).toUpperCase();
}

function getAvatarClass(name) {
  const colors = [
    'avatar-blue',
    'avatar-purple',
    'avatar-green',
    'avatar-orange',
    'avatar-pink',
  ];

  if (!name) return colors[0];

  let total = 0;

  for (let i = 0; i < name.length; i++) {
    total += name.charCodeAt(i);
  }

  return colors[total % colors.length];
}

export default function App() {
  const [view, setView] = useState('messages');
  const [users, setUsers] = useState([]);
  const [currentUserId, setCurrentUserId] = useState(1);

  const currentUser =
    users.find((user) => user.id === currentUserId) || users[0] || { id: currentUserId, name: 'User', role: 'Messaging account' };

  const [conversations, setConversations] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [messages, setMessages] = useState([]);

  const [draft, setDraft] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  const [loadingUsers, setLoadingUsers] = useState(true);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [sending, setSending] = useState(false);
  const [editingMessageId, setEditingMessageId] = useState(null);
  const [editingContent, setEditingContent] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);
  const [openMessageMenuId, setOpenMessageMenuId] = useState(null);
  const [socketConnected, setSocketConnected] = useState(false);

  const [error, setError] = useState('');

  const [newChatUserId, setNewChatUserId] = useState('');

  const stompClientRef = useRef(null);
  const subscriptionRef = useRef(null);
  const messagesEndRef = useRef(null);
  const sendingRef = useRef(false);


  useEffect(() => {
    let cancelled = false;

    async function loadUsers() {
      try {
        const data = await fetchUsers();

        if (cancelled) return;

        const normalizedUsers = data
          .map((user) => ({
            id: Number(user.id),
            name: user.name,
            role: user.role || 'Messaging account',
          }))
          .filter((user) => Number.isFinite(user.id));

        setUsers(normalizedUsers);

        if (normalizedUsers.length === 0) {
          setError('No users are available for messaging.');
          setLoadingUsers(false);
          return;
        }

        const effectiveCurrentUserId = normalizedUsers.some(
          (user) => user.id === currentUserId
        )
          ? currentUserId
          : normalizedUsers[0].id;

        setCurrentUserId(effectiveCurrentUserId);

        const firstOther = normalizedUsers.find(
          (user) => user.id !== effectiveCurrentUserId
        );

        setNewChatUserId(firstOther ? String(firstOther.id) : '');
      } catch (e) {
        if (!cancelled) {
          setError(e.message || 'Failed to load users');
        }
      } finally {
        if (!cancelled) {
          setLoadingUsers(false);
        }
      }
    }

    loadUsers();

    return () => {
      cancelled = true;
    };
  }, []);

  const loadConversations = useCallback(async (silent = false) => {
    try {
      const data = await fetchConversations(currentUserId);
      setConversations(data);
      if (!silent) {
        setError('');
      }
    } catch (e) {
      if (!silent) {
        setError(e.message || 'Failed to load conversations');
      }
    } finally {
      if (!silent) {
        setLoadingList(false);
      }
    }
  }, [currentUserId]);

  useEffect(() => {
    let cancelled = false;

    fetchConversations(currentUserId)
      .then((data) => {
        if (!cancelled) {
          setConversations(data);
          setError('');
        }
      })
      .catch((requestError) => {
        if (!cancelled) {
          setError(requestError.message || 'Failed to load conversations');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingList(false);
        }
      });

    const interval = setInterval(() => {
      loadConversations(true);
    }, 5000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [loadConversations]);

  /*
   * WebSocket connection
   */
  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8081/ws',
      reconnectDelay: 5000,

      onConnect: () => {
        setSocketConnected(true);
        loadConversations(true);
      },

      onDisconnect: () => {
        setSocketConnected(false);
      },

      onStompError: (frame) => {
        setSocketConnected(false);
        console.error(
          'WebSocket STOMP error:',
          frame.headers['message']
        );
      },

      onWebSocketError: (error) => {
        setSocketConnected(false);
        console.error(
          'WebSocket connection error:',
          error
        );
      },
    });

    client.activate();
    stompClientRef.current = client;

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }

      client.deactivate();
      stompClientRef.current = null;
      setSocketConnected(false);
    };
  }, [loadConversations]);

  /*
   * Load selected conversation messages
   */
  useEffect(() => {
    if (selectedId == null) {
      return;
    }

    let cancelled = false;

    markConversationAsRead(selectedId, currentUserId)
      .then(() => {
        if (cancelled) return;

        setConversations((previous) =>
          previous.map((conversation) =>
            conversation.id === selectedId
              ? { ...conversation, unreadCount: 0 }
              : conversation
          )
        );
        return loadConversations(true);
      })
      .catch((requestError) => {
        if (!cancelled) {
          setError(requestError.message || 'Failed to mark messages as read');
        }
      });

    (async () => {
      setLoadingMessages(true);
      setError('');

      try {
        const data = await fetchMessages(selectedId, currentUserId);

        if (!cancelled) {
          setMessages(data);
        }
      } catch (e) {
        if (!cancelled) {
          setError(
            e.message || 'Failed to load messages'
          );
        }
      } finally {
        if (!cancelled) {
          setLoadingMessages(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [selectedId, currentUserId, loadConversations]);

  useEffect(() => {
    if (!selectedId || !socketConnected) return;

    let cancelled = false;

    fetchMessages(selectedId, currentUserId)
      .then((data) => {
        if (cancelled) return;

        setMessages((current) => {
          const byId = new Map(data.map((message) => [message.id, message]));
          current.forEach((message) => byId.set(message.id, message));
          return [...byId.values()].sort(
            (a, b) => new Date(a.sentAt) - new Date(b.sentAt)
          );
        });
      })
      .catch((requestError) => {
        if (!cancelled) {
          setError(requestError.message || 'Failed to refresh messages');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [selectedId, currentUserId, socketConnected]);

  /*
   * Subscribe to selected conversation
   */
  useEffect(() => {
    if (!selectedId) return;

    const client = stompClientRef.current;

    if (!client || !client.connected || !socketConnected) {
      return;
    }

    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
      subscriptionRef.current = null;
    }

    subscriptionRef.current = client.subscribe(
      `/topic/conversations/${selectedId}`,
      (message) => {
        try {
          const receivedMessage = JSON.parse(
            message.body
          );

          setMessages((prev) => {
            const alreadyExists = prev.some(
              (m) => m.id === receivedMessage.id
            );

            if (alreadyExists) {
              return prev;
            }

            return [...prev, receivedMessage].sort(
              (a, b) => new Date(a.sentAt) - new Date(b.sentAt)
            );
          });

          if (receivedMessage.senderId !== currentUserId) {
            markConversationAsRead(selectedId, currentUserId)
              .then(() => loadConversations(true))
              .catch((requestError) => {
                setError(requestError.message || 'Failed to mark messages as read');
              });
          } else {
            loadConversations(true);
          }
        } catch (e) {
          console.error(
            'Failed to process WebSocket message:',
            e
          );
        }
      }
    );

    return () => {
      if (subscriptionRef.current) {
        subscriptionRef.current.unsubscribe();
        subscriptionRef.current = null;
      }
    };
  }, [selectedId, socketConnected, currentUserId, loadConversations]);

  /*
   * Scroll to latest message
   */
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({
      behavior: 'smooth',
    });
  }, [messages]);

  const selectedConversation =
    conversations.find(
      (c) => c.id === selectedId
    );

  const filteredConversations =
    conversations.filter((conversation) => {
      const query = searchQuery.trim().toLowerCase();

      if (!query) return true;

      return (
        conversation.otherUserName
          ?.toLowerCase()
          .includes(query) ||
        conversation.latestMessage
          ?.toLowerCase()
          .includes(query)
      );
    });

  async function handleSend(e) {
    e.preventDefault();

    const text = draft.trim();

    if (!selectedId || !text || sendingRef.current) {
      return;
    }

    sendingRef.current = true;
    setSending(true);
    setError('');

    const client = stompClientRef.current;

    try {
      if (client && client.connected) {
        client.publish({
          destination:
            `/app/conversations/${selectedId}`,
          body: JSON.stringify({
            content: text,
            senderId: currentUserId,
          }),
        });

        setDraft('');
      } else {
        const created = await sendMessageApi(
          selectedId,
          text,
          currentUserId
        );

        setMessages((prev) => {
          const alreadyExists = prev.some(
            (message) => message.id === created.id
          );

          if (alreadyExists) {
            return prev;
          }

          return [...prev, created].sort(
            (a, b) => new Date(a.sentAt) - new Date(b.sentAt)
          );
        });

        setDraft('');

        setConversations((prev) =>
          prev
            .map((conversation) =>
              conversation.id === selectedId
                ? {
                    ...conversation,
                    latestMessage: created.content,
                    latestMessageAt: created.sentAt,
                  }
                : conversation
            )
            .sort(
              (a, b) =>
                new Date(b.latestMessageAt) -
                new Date(a.latestMessageAt)
            )
        );
      }
    } catch (err) {
      setError(
        err.message || 'Failed to send message'
      );
    } finally {
      sendingRef.current = false;
      setSending(false);
    }
  }

  function handleUserChange(e) {
    const userId = Number(e.target.value);

    setCurrentUserId(userId);
    setSelectedId(null);
    setMessages([]);
    setDraft('');
    setSearchQuery('');
    setError('');
    setLoadingList(true);

    const otherUser = users.find((user) => user.id !== userId);
    setNewChatUserId(String(otherUser?.id || ''));
  }

  async function handleStartConversation(e) {
    e.preventDefault();

    const otherUserId = Number(newChatUserId);

    if (!otherUserId) {
      return;
    }

    if (otherUserId === currentUserId) {
      setError('You cannot start a conversation with yourself.');
      return;
    }

    setError('');

    try {
      const created = await createConversation(
        otherUserId,
        currentUserId
      );

      await loadConversations();

      setSelectedId(created.id);

      setNewChatUserId('');
    } catch (err) {
      setError(
        err.message || 'Failed to create conversation'
      );
    }
  }

  function selectConversation(id) {
    setSelectedId(id);
    setEditingMessageId(null);
    setEditingContent('');
    setOpenMessageMenuId(null);
    setError('');
  }

  function handleStartEdit(message) {
    setEditingMessageId(message.id);
    setEditingContent(message.content || '');
    setOpenMessageMenuId(null);
    setError('');
  }

  function handleCancelEdit() {
    setEditingMessageId(null);
    setEditingContent('');
  }

  async function handleSaveEdit(messageId) {
    const content = editingContent.trim();

    if (!selectedId || !content || savingEdit) {
      return;
    }

    setSavingEdit(true);
    setError('');

    try {
      const updated = await updateMessage(
        selectedId,
        messageId,
        content,
        currentUserId
      );

      setMessages((prev) =>
        prev.map((message) =>
          message.id === messageId ? updated : message
        )
      );

      setEditingMessageId(null);
      setEditingContent('');
      setOpenMessageMenuId(null);
      await loadConversations(true);
    } catch (err) {
      setError(err.message || 'Failed to edit message');
    } finally {
      setSavingEdit(false);
    }
  }

  async function handleDeleteMessage(messageId) {
    if (!selectedId) {
      return;
    }

    const confirmed = window.confirm(
      'Are you sure you want to delete this message?'
    );

    if (!confirmed) {
      return;
    }

    setError('');

    try {
      await deleteMessage(
        selectedId,
        messageId,
        currentUserId
      );

      setMessages((prev) =>
        prev.filter((message) => message.id !== messageId)
      );

      if (editingMessageId === messageId) {
        setEditingMessageId(null);
        setEditingContent('');
      }

      setOpenMessageMenuId(null);
      await loadConversations(true);
    } catch (err) {
      setError(err.message || 'Failed to delete message');
    }
  }

  return (
    <div className="app">

      {/* =====================================================
          APPLICATION HEADER
          ===================================================== */}

      <header className="app-header">

        <div className="app-brand">
          <div className="app-logo">
            RE
          </div>

          <div>
            <h1>Real Estate Connect</h1>

            <p className="subtitle">
              Talk to buyers, owners and agents in one place
            </p>
          </div>
        </div>

        <div className="user-profile">

          <div className="user-avatar avatar-blue">
            {currentUser.name.charAt(0)}
          </div>

          <div className="user-profile-info">
            <strong>{currentUser.name}</strong>
            <span>{currentUser.role}</span>
          </div>

          <select
            value={currentUserId}
            onChange={handleUserChange}
            className="demo-user-select"
            aria-label="Select current user"
            disabled={loadingUsers || users.length === 0}
          >
            {loadingUsers ? (
              <option>Loading users...</option>
            ) : (
              users.map((user) => (
                <option key={user.id} value={user.id}>
                  Chat as {user.name}
                </option>
              ))
            )}
          </select>

          <span className="online-dot" />

        </div>

      </header>


      {/* =====================================================
          NAVIGATION
          ===================================================== */}

      <div className="top-nav">

        <button
          className={
            view === 'messages'
              ? 'active'
              : ''
          }
          onClick={() => setView('messages')}
        >
          <span className="nav-icon">▣</span>
          Messages

          {conversations.length > 0 && (
            <span className="nav-count">
              {conversations.length}
            </span>
          )}
        </button>

        <button
          className={
            view === 'reviews'
              ? 'active'
              : ''
          }
          onClick={() => setView('reviews')}
        >
          <span className="nav-icon">★</span>
          Reviews
        </button>

      </div>


      {/* =====================================================
          GLOBAL ERROR
          ===================================================== */}

      {error && (
        <div className="error-banner">
          <span className="error-icon">!</span>
          <span>{error}</span>
        </div>
      )}


      {/* =====================================================
          REVIEWS
          ===================================================== */}

      {view === 'reviews' ? (

        <Reviews />

      ) : (

        /* ===================================================
           MESSAGING
           =================================================== */

        <div className="messaging-shell">

          {/* =================================================
              CONVERSATION SIDEBAR
              ================================================= */}

          <aside className="conversation-list-panel">

            <div className="inbox-header">

              <div>
                <span className="section-eyebrow">
                  YOUR INBOX
                </span>

                <h2>Messages</h2>
              </div>

              <span className="conversation-total">
                {conversations.length}
              </span>

            </div>


            {/* Search */}

            <div className="conversation-search">

              <span className="search-icon">
                ⌕
              </span>

              <input
                type="text"
                placeholder="Search conversations..."
                value={searchQuery}
                onChange={(e) =>
                  setSearchQuery(e.target.value)
                }
              />

              {searchQuery && (
                <button
                  type="button"
                  className="clear-search"
                  onClick={() =>
                    setSearchQuery('')
                  }
                >
                  ×
                </button>
              )}

            </div>


            {/* Start chat */}

            <div className="new-chat-form">

              <div className="new-chat-title">
                <span>Start a new conversation</span>
              </div>

              <form
                onSubmit={handleStartConversation}
              >

                <div className="new-chat-row">

                  <input
                    id="other-user"
                    type="number"
                    min="1"
                    max="3"
                    placeholder="User ID"
                    value={newChatUserId}
                    onChange={(e) =>
                      setNewChatUserId(
                        e.target.value
                      )
                    }
                  />

                  <button type="submit">
                    Start
                  </button>

                </div>

              </form>

              <p className="hint">
                Demo users: 1 = Alice · 2 = Bob · 3 = Carol
              </p>

            </div>


            {/* Conversation list */}

            <div className="conversation-list-wrapper">

              {loadingList ? (

                <div className="sidebar-loading">
                  <div className="loading-spinner" />
                  <span>
                    Loading conversations...
                  </span>
                </div>

              ) : filteredConversations.length === 0 ? (

                <div className="sidebar-empty">

                  <div className="sidebar-empty-icon">
                    ◌
                  </div>

                  <strong>
                    {searchQuery
                      ? 'No matches found'
                      : 'No conversations yet'}
                  </strong>

                  <span>
                    {searchQuery
                      ? 'Try another name or message'
                      : 'Start a chat to connect with a user'}
                  </span>

                </div>

              ) : (

                <ul className="conversation-list">

                  {filteredConversations.map(
                    (conversation) => {

                      const selected =
                        conversation.id === selectedId;

                      return (
                        <li
                          key={conversation.id}
                        >

                          <button
                            type="button"
                            className={
                              selected
                                ? 'conversation-item selected'
                                : 'conversation-item'
                            }
                            onClick={() =>
                              selectConversation(
                                conversation.id
                              )
                            }
                          >

                            <div
                              className={
                                `conversation-avatar ${
                                  getAvatarClass(
                                    conversation.otherUserName
                                  )
                                }`
                              }
                            >
                              {getInitial(
                                conversation.otherUserName
                              )}
                            </div>


                            <div className="conversation-content">

                              <div className="conversation-top">

                                <span className="name">
                                  {
                                    conversation.otherUserName
                                  }
                                </span>

                                <span className="time">
                                  {formatConversationTime(
                                    conversation.latestMessageAt
                                  )}
                                </span>

                              </div>

                              <div className="conversation-preview-row">
                                <p className="preview">
                                  {conversation.latestMessage ||
                                    'Start a conversation'}
                                </p>

                                {conversation.unreadCount > 0 && (
                                  <span
                                    className="unread-badge"
                                    aria-label={`${conversation.unreadCount} unread messages`}
                                  >
                                    {conversation.unreadCount > 99
                                      ? '99+'
                                      : conversation.unreadCount}
                                  </span>
                                )}
                              </div>

                            </div>

                          </button>

                        </li>
                      );
                    }
                  )}

                </ul>

              )}

            </div>

          </aside>


          {/* =================================================
              CHAT PANEL
              ================================================= */}

          <main className="message-panel">

            {!selectedId ? (

              <div className="chat-empty-state">

                <div className="chat-empty-icon">
                  <span>⌁</span>
                </div>

                <h2>
                  Your conversations
                </h2>

                <p>
                  Select a conversation from your inbox
                  to start chatting.
                </p>

                <div className="empty-tip">
                  <span>Tip</span>
                  Ask about property availability,
                  pricing or viewing details.
                </div>

              </div>

            ) : (

              <>

                {/* =========================================
                    CHAT HEADER
                    ========================================= */}

                <div className="message-header">

                  <div className="chat-person">

                    <div
                      className={
                        `chat-avatar ${
                          getAvatarClass(
                            selectedConversation?.otherUserName
                          )
                        }`
                      }
                    >
                      {getInitial(
                        selectedConversation?.otherUserName
                      )}
                    </div>

                    <div>

                      <h2>
                        {selectedConversation
                          ?.otherUserName ??
                          'Conversation'}
                      </h2>

                      <div className="chat-status">
                        <span
                          className={
                            socketConnected
                              ? 'status-dot connected'
                              : 'status-dot disconnected'
                          }
                        />
                        {socketConnected
                          ? 'Live'
                          : 'Reconnecting; API fallback'}
                      </div>

                    </div>

                  </div>


                  <div className="chat-actions">

                    <button
                      type="button"
                      className="chat-action-button"
                      title="Conversation details"
                    >
                      ⓘ
                    </button>

                  </div>

                </div>


                {/* =========================================
                    PROPERTY CONTEXT PLACEHOLDER
                    ========================================= */}

                <div className="property-context-bar">

                  <div className="property-context-icon">
                    ◆
                  </div>

                  <div>
                    <strong>
                      Property conversation
                    </strong>

                    <span>
                      Keep your property enquiry and
                      communication together.
                    </span>
                  </div>

                </div>


                {/* =========================================
                    MESSAGES
                    ========================================= */}

                <div className="messages">

                  {loadingMessages ? (

                    <div className="chat-loading">

                      <div className="loading-spinner" />

                      <span>
                        Loading messages...
                      </span>

                    </div>

                  ) : messages.length === 0 ? (

                    <div className="no-messages">

                      <div className="no-messages-icon">
                        ✦
                      </div>

                      <strong>
                        Start the conversation
                      </strong>

                      <span>
                        Send a message to{' '}
                        {selectedConversation
                          ?.otherUserName}
                      </span>

                    </div>

                  ) : (

                    <>

                      <div className="conversation-date">
                        Conversation
                      </div>

                      {messages.map((message) => {

                        const isOwn =
                          message.senderId ===
                          currentUserId;

                        return (
                          <div
                            key={message.id}
                            className={
                              isOwn
                                ? 'message-row own'
                                : 'message-row'
                            }
                            onMouseEnter={(e) => {
                              const button = e.currentTarget.querySelector('[data-message-options]');
                              if (button) {
                                button.style.opacity = '1';
                                button.style.pointerEvents = 'auto';
                              }
                            }}
                            onMouseLeave={(e) => {
                              const button = e.currentTarget.querySelector('[data-message-options]');
                              if (button) {
                                button.style.opacity = openMessageMenuId === message.id ? '1' : '0';
                                button.style.pointerEvents = openMessageMenuId === message.id ? 'auto' : 'none';
                              }
                            }}
                          >

                            {!isOwn && (
                              <div
                                className={
                                  `message-avatar ${
                                    getAvatarClass(
                                      message.senderName
                                    )
                                  }`
                                }
                              >
                                {getInitial(
                                  message.senderName
                                )}
                              </div>
                            )}

                            <article
                              className={
                                isOwn
                                  ? 'message own-message'
                                  : 'message'
                              }
                              style={{
                                position: 'relative',
                              }}
                            >

                              {!isOwn && (
                                <span className="message-sender">
                                  {message.senderName}
                                </span>
                              )}

                              {editingMessageId === message.id ? (
                                <div className="message-edit-box">
                                  <textarea
                                    value={editingContent}
                                    onChange={(e) =>
                                      setEditingContent(e.target.value)
                                    }
                                    maxLength={2000}
                                    rows={1}
                                    autoFocus
                                  />

                                  <div className="message-edit-actions">
                                    <button
                                      type="button"
                                      onClick={handleCancelEdit}
                                      disabled={savingEdit}
                                    >
                                      Cancel
                                    </button>

                                    <button
                                      type="button"
                                      onClick={() =>
                                        handleSaveEdit(message.id)
                                      }
                                      disabled={
                                        savingEdit ||
                                        !editingContent.trim()
                                      }
                                    >
                                      {savingEdit ? 'Saving...' : 'Save'}
                                    </button>
                                  </div>
                                </div>
                              ) : (
                                <p>{message.content}</p>
                              )}

                              <div className="message-meta">
                                <span>
                                  {formatTimestamp(message.sentAt)}
                                </span>

                                {isOwn && (
                                  <>
                                    <span className="message-check">
                                      ✓
                                    </span>

                                    {editingMessageId !== message.id && (
                                      <div
                                        style={{
                                          position: 'absolute',
                                          top: '-8px',
                                          right: '-8px',
                                          zIndex: 5,
                                        }}
                                      >
                                        <button
                                          type="button"
                                          aria-label="Message options"
                                          title="Message options"
                                          data-message-options
                                          onClick={(e) => {
                                            e.stopPropagation();
                                            setOpenMessageMenuId((current) =>
                                              current === message.id
                                                ? null
                                                : message.id
                                            );
                                          }}
                                          style={{
                                            width: 30,
                                            height: 30,
                                            borderRadius: '50%',
                                            border: '1px solid rgba(15, 23, 42, 0.10)',
                                            background: 'rgba(255, 255, 255, 0.96)',
                                            boxShadow: '0 6px 18px rgba(15, 23, 42, 0.10)',
                                            color: '#64748b',
                                            fontSize: 18,
                                            lineHeight: 1,
                                            cursor: 'pointer',
                                            opacity: openMessageMenuId === message.id ? 1 : 0,
                                            transition: 'opacity 0.15s ease, transform 0.15s ease',
                                            transform: openMessageMenuId === message.id ? 'scale(1)' : 'scale(0.92)',
                                            pointerEvents: openMessageMenuId === message.id ? 'auto' : 'none',
                                          }}
                                          onMouseEnter={(e) => {
                                            e.currentTarget.style.opacity = '1';
                                            e.currentTarget.style.pointerEvents = 'auto';
                                          }}
                                        >
                                          ⋮
                                        </button>

                                        <div
                                          style={{
                                            position: 'absolute',
                                            top: 34,
                                            right: 0,
                                            minWidth: 120,
                                            padding: 6,
                                            borderRadius: 12,
                                            background: '#ffffff',
                                            border: '1px solid rgba(15, 23, 42, 0.08)',
                                            boxShadow: '0 14px 34px rgba(15, 23, 42, 0.15)',
                                            display: openMessageMenuId === message.id ? 'block' : 'none',
                                          }}
                                          onClick={(e) => e.stopPropagation()}
                                        >
                                          <button
                                            type="button"
                                            onClick={() => handleStartEdit(message)}
                                            style={{
                                              display: 'block',
                                              width: '100%',
                                              border: 0,
                                              background: 'transparent',
                                              padding: '9px 10px',
                                              borderRadius: 8,
                                              textAlign: 'left',
                                              cursor: 'pointer',
                                              color: '#0f172a',
                                            }}
                                          >
                                            Edit
                                          </button>
                                          <button
                                            type="button"
                                            onClick={() => handleDeleteMessage(message.id)}
                                            style={{
                                              display: 'block',
                                              width: '100%',
                                              border: 0,
                                              background: 'transparent',
                                              padding: '9px 10px',
                                              borderRadius: 8,
                                              textAlign: 'left',
                                              cursor: 'pointer',
                                              color: '#dc2626',
                                            }}
                                          >
                                            Delete
                                          </button>
                                        </div>
                                      </div>
                                    )}
                                  </>
                                )}
                              </div>

                            </article>

                          </div>
                        );
                      })}

                      <div
                        ref={messagesEndRef}
                      />

                    </>

                  )}

                </div>


                {/* =========================================
                    MESSAGE COMPOSER
                    ========================================= */}

                <form
                  className="send-form"
                  onSubmit={handleSend}
                >

                  <div className="composer-wrapper">

                    <input
                      type="text"
                      placeholder="Write a message..."
                      value={draft}
                      onChange={(e) =>
                        setDraft(e.target.value)
                      }
                      maxLength={2000}
                      disabled={sending}
                    />

                    <span className="character-count">
                      {draft.length}/2000
                    </span>

                  </div>

                  <button
                    type="submit"
                    disabled={!draft.trim() || sending}
                    className="send-button"
                  >
                    <span>{sending ? 'Sending...' : 'Send'}</span>
                    <span className="send-arrow">
                      →
                    </span>
                  </button>

                </form>

              </>

            )}

          </main>

        </div>

      )}

      <Chatbot />

    </div>
  );
}