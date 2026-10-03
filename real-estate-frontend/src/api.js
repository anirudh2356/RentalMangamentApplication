const API_BASE = '/api';

async function request(path, options = {}) {
  const { headers, ...restOptions } = options;

  const response = await fetch(`${API_BASE}${path}`, {
    ...restOptions,
    headers: {
      'Content-Type': 'application/json',
      ...headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed (${response.status})`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

// Get all users from the messaging users table
export function fetchUsers() {
  return request('/users');
}

// Get conversations for the current user
export function fetchConversations(currentUserId) {
  return request('/conversations', {
    headers: {
      'X-User-Id': String(currentUserId),
    },
  });
}

// Create/find a conversation between current user and another user
export function createConversation(otherUserId, currentUserId) {
  return request('/conversations', {
    method: 'POST',
    headers: {
      'X-User-Id': String(currentUserId),
    },
    body: JSON.stringify({
      otherUserId,
    }),
  });
}

// Get messages for a conversation
export function fetchMessages(conversationId, currentUserId) {
  return request(`/conversations/${conversationId}/messages`, {
    headers: {
      'X-User-Id': String(currentUserId),
    },
  });
}

// Mark conversation as read
export function markConversationAsRead(
  conversationId,
  currentUserId
) {
  return request(`/conversations/${conversationId}/read`, {
    method: 'POST',
    headers: {
      'X-User-Id': String(currentUserId),
    },
  });
}

// Send message
export function sendMessage(
  conversationId,
  content,
  currentUserId
) {
  return request(`/conversations/${conversationId}/messages`, {
    method: 'POST',
    headers: {
      'X-User-Id': String(currentUserId),
    },
    body: JSON.stringify({
      content,
    }),
  });
}

// Edit own message
export function updateMessage(
  conversationId,
  messageId,
  content,
  currentUserId
) {
  return request(
    `/conversations/${conversationId}/messages/${messageId}`,
    {
      method: 'PUT',
      headers: {
        'X-User-Id': String(currentUserId),
      },
      body: JSON.stringify({
        content,
      }),
    }
  );
}

// Delete own message
export function deleteMessage(
  conversationId,
  messageId,
  currentUserId
) {
  return request(
    `/conversations/${conversationId}/messages/${messageId}`,
    {
      method: 'DELETE',
      headers: {
        'X-User-Id': String(currentUserId),
      },
    }
  );
}

// Reviews
export function fetchPropertyReviews(propertyId) {
  return request(`/properties/${propertyId}/reviews`);
}

export function postPropertyReview(
  propertyId,
  rating,
  comment
) {
  return request(`/properties/${propertyId}/reviews`, {
    method: 'POST',
    body: JSON.stringify({
      rating,
      comment,
    }),
  });
}

export function updatePropertyReview(
  propertyId,
  reviewId,
  rating,
  comment
) {
  return request(
    `/properties/${propertyId}/reviews/${reviewId}`,
    {
      method: 'PUT',
      body: JSON.stringify({
        rating,
        comment,
      }),
    }
  );
}

export function deletePropertyReview(
  propertyId,
  reviewId
) {
  return request(
    `/properties/${propertyId}/reviews/${reviewId}`,
    {
      method: 'DELETE',
    }
  );
}

// Chatbot
export function sendChatbotMessage(message) {
  return request('/chatbot', {
    method: 'POST',
    body: JSON.stringify({
      message,
    }),
  });
}