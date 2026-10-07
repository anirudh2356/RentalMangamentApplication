const API_BASE = '/api';

async function request(path, options = {}, responseType = 'json') {
  const { headers, ...restOptions } = options;
  const isMultipart = typeof FormData !== 'undefined' && restOptions.body instanceof FormData;

  const response = await fetch(`${API_BASE}${path}`, {
    ...restOptions,
    credentials: 'include',
    headers: {
      ...(!isMultipart && { 'Content-Type': 'application/json' }),
      ...headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    let message = text || `Request failed (${response.status})`;
    try {
      const body = JSON.parse(text);
      message = body.error || body.detail || message;
    } catch {
      // Keep the original response text when the server did not return JSON.
    }
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  if (response.status === 204) {
    return null;
  }

  if (responseType === 'blob') return response.blob();
  if (responseType === 'text') return response.text();
  return response.json();
}

// Authentication uses an HttpOnly server session and a CSRF token kept in memory.
export function fetchCsrfToken() {
  return request('/auth/csrf');
}

export function login(email, password, csrfToken) {
  return request('/auth/login', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
    body: JSON.stringify({ email, password }),
  });
}

export function fetchCurrentUser() {
  return request('/auth/me');
}

export function fetchProfile() {
  return request('/profile');
}

export function updateProfile(profile, csrfToken) {
  return request('/profile', {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(profile),
  });
}

export function changeProfilePassword(passwordChange, csrfToken) {
  return request('/profile/password', {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(passwordChange),
  });
}

export function fetchBrokerSettings() {
  return request('/settings');
}

export function searchWorkspace(query) {
  const params = new URLSearchParams({ q: query });
  return request(`/search?${params.toString()}`);
}

export function updateBrokerSettings(settings, csrfToken) {
  return request('/settings', {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(settings),
  });
}

function reportQuery(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const query = params.toString();
  return query ? `?${query}` : '';
}

export function fetchReport(reportType, filters = {}) {
  return request(`/reports/${reportType}${reportQuery(filters)}`);
}

export function downloadReportCsv(reportType, filters = {}) {
  return request(`/reports/${reportType}/export${reportQuery(filters)}`, {}, 'blob');
}

export function logout(csrfToken) {
  return request('/auth/logout', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
  });
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

// Broker property management
export function fetchProperties() {
  return request('/properties');
}

export function fetchProperty(propertyId) {
  return request(`/properties/${propertyId}`);
}

export function createProperty(property, csrfToken) {
  return request('/properties', {
    method: 'POST',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
    body: JSON.stringify(property),
  });
}

export function updateProperty(propertyId, property, csrfToken) {
  return request(`/properties/${propertyId}`, {
    method: 'PUT',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
    body: JSON.stringify(property),
  });
}

export function deactivateProperty(propertyId, csrfToken) {
  return request(`/properties/${propertyId}`, {
    method: 'DELETE',
    headers: {
      'X-XSRF-TOKEN': csrfToken,
    },
  });
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

// Broker lead management
export function fetchLeads(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return request(`/leads${query ? `?${query}` : ''}`);
}

export function fetchPropertyMatches(leadId) {
  return request(`/property-matching/lead/${leadId}`);
}

export function createLead(lead, csrfToken) {
  return request('/leads', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(lead),
  });
}

export function updateLead(leadId, lead, csrfToken) {
  return request(`/leads/${leadId}`, {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(lead),
  });
}

export function deleteLead(leadId, csrfToken) {
  return request(`/leads/${leadId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

// Broker site visit management
export function fetchSiteVisits(status = '') {
  const query = status ? `?status=${encodeURIComponent(status)}` : '';
  return request(`/site-visits${query}`);
}

export function fetchSiteVisit(visitId) {
  return request(`/site-visits/${visitId}`);
}

export function createSiteVisit(siteVisit, csrfToken) {
  return request('/site-visits', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(siteVisit),
  });
}

export function updateSiteVisit(visitId, siteVisit, csrfToken) {
  return request(`/site-visits/${visitId}`, {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(siteVisit),
  });
}

export function deleteSiteVisit(visitId, csrfToken) {
  return request(`/site-visits/${visitId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

// Broker deal management
export function fetchDeals(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const query = params.toString();
  return request(`/deals${query ? `?${query}` : ''}`);
}

export function fetchDeal(dealId) {
  return request(`/deals/${dealId}`);
}

export function createDeal(deal, csrfToken) {
  return request('/deals', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(deal),
  });
}

export function updateDeal(dealId, deal, csrfToken) {
  return request(`/deals/${dealId}`, {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(deal),
  });
}

export function deleteDeal(dealId, csrfToken) {
  return request(`/deals/${dealId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

// Broker commissions are derived from won deals.
export function fetchCommissions() {
  return request('/commissions');
}

export function fetchAnalyticsOverview() {
  return request('/analytics/overview');
}

// Broker follow-up management
export function fetchFollowUps(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const query = params.toString();
  return request(`/followups${query ? `?${query}` : ''}`);
}

export function fetchFollowUp(followUpId) {
  return request(`/followups/${followUpId}`);
}

export function createFollowUp(followUp, csrfToken) {
  return request('/followups', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(followUp),
  });
}

export function updateFollowUp(followUpId, followUp, csrfToken) {
  return request(`/followups/${followUpId}`, {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(followUp),
  });
}

export function deleteFollowUp(followUpId, csrfToken) {
  return request(`/followups/${followUpId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

export function fetchCalendarEvents(from, to) {
  const params = new URLSearchParams();
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString();
  return request(`/calendar/events${query ? `?${query}` : ''}`);
}

export function fetchTeamMembers(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const query = params.toString();
  return request(`/team/members${query ? `?${query}` : ''}`);
}

export function fetchDocuments(filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) params.set(key, value);
  });
  const query = params.toString();
  return request(`/documents${query ? `?${query}` : ''}`);
}

export function uploadDocument(formData, csrfToken) {
  return request('/documents', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: formData,
  });
}

export function updateDocument(documentId, document, csrfToken) {
  return request(`/documents/${documentId}`, {
    method: 'PUT',
    headers: { 'X-XSRF-TOKEN': csrfToken },
    body: JSON.stringify(document),
  });
}

export function deleteDocument(documentId, csrfToken) {
  return request(`/documents/${documentId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

export function downloadDocument(documentId) {
  return request(`/documents/${documentId}/download`, {}, 'blob');
}

export function fetchNotifications(unreadOnly = false) {
  const params = new URLSearchParams();
  if (unreadOnly) params.set('unreadOnly', 'true');
  const query = params.toString();
  return request(`/notifications${query ? `?${query}` : ''}`);
}

export function fetchUnreadNotificationCount() {
  return request('/notifications/unread-count');
}

export function fetchNotification(notificationId) {
  return request(`/notifications/${notificationId}`);
}

export function markNotificationRead(notificationId, csrfToken) {
  return request(`/notifications/${notificationId}/read`, {
    method: 'PATCH',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

export function markAllNotificationsRead(csrfToken) {
  return request('/notifications/read-all', {
    method: 'PATCH',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}

export function deleteNotification(notificationId, csrfToken) {
  return request(`/notifications/${notificationId}`, {
    method: 'DELETE',
    headers: { 'X-XSRF-TOKEN': csrfToken },
  });
}