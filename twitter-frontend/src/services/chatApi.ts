import api from './api'; // Import the default axios instance that has interceptors for Authorization, token-refresh, X-User-Id, etc.
import { SpringPage } from './api';

// Re-export type definitions to match backend entities
export interface ChatMessageResponse {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM';
  status: 'SENT' | 'DELIVERED' | 'READ';
  createdAt: string;
  edited: boolean;
  editedAt?: string;
  deleted: boolean;
  reactions?: Array<{
    reaction: string;
    userId: number;
  }>;
}

export interface ConversationResponse {
  id: number;
  type: 'ONE_TO_ONE' | 'GROUP';
  name?: string;
  groupImageUrl?: string;
  participantIds: number[];
  lastMessage?: ChatMessageResponse;
  unreadCount: number;
  updatedAt: string;
}

export interface UserStatusResponse {
  userId: number;
  online: boolean;
  lastSeen: string;
}

// Default export of chat api methods
export const chatApi = {
  listConversations: async (): Promise<ConversationResponse[]> => {
    const res = await api.get<ConversationResponse[]>('/chat-service/api/v1/conversations');
    return res.data;
  },

  createOneToOne: async (otherUserId: number): Promise<ConversationResponse> => {
    const res = await api.post<ConversationResponse>('/chat-service/api/v1/conversations', {
      type: 'ONE_TO_ONE',
      participantIds: [otherUserId],
    });
    return res.data;
  },

  createGroup: async (participantIds: number[], groupName: string): Promise<ConversationResponse> => {
    const res = await api.post<ConversationResponse>('/chat-service/api/v1/conversations', {
      type: 'GROUP',
      participantIds,
      groupName,
    });
    return res.data;
  },

  getMessages: async (conversationId: number, page = 0, size = 30): Promise<SpringPage<ChatMessageResponse>> => {
    const res = await api.get<SpringPage<ChatMessageResponse>>(
      `/chat-service/api/v1/conversations/${conversationId}/messages?page=${page}&size=${size}`
    );
    return res.data;
  },

  markRead: async (conversationId: number, lastReadMessageId: number): Promise<void> => {
    await api.post(`/chat-service/api/v1/conversations/${conversationId}/read?lastReadMessageId=${lastReadMessageId}`);
  },

  editMessage: async (messageId: number, content: string): Promise<ChatMessageResponse> => {
    const res = await api.put<ChatMessageResponse>(
      `/chat-service/api/v1/messages/${messageId}?content=${encodeURIComponent(content)}`
    );
    return res.data;
  },

  deleteForEveryone: async (messageId: number): Promise<void> => {
    await api.delete(`/chat-service/api/v1/messages/${messageId}/everyone`);
  },

  deleteForMe: async (messageId: number): Promise<void> => {
    await api.delete(`/chat-service/api/v1/messages/${messageId}/me`);
  },

  addReaction: async (messageId: number, reaction: string): Promise<void> => {
    await api.post(`/chat-service/api/v1/messages/${messageId}/reactions?reaction=${encodeURIComponent(reaction)}`);
  },

  removeReaction: async (messageId: number, reaction: string): Promise<void> => {
    await api.delete(`/chat-service/api/v1/messages/${messageId}/reactions?reaction=${encodeURIComponent(reaction)}`);
  },

  getUserStatus: async (userId: number): Promise<UserStatusResponse> => {
    const res = await api.get<UserStatusResponse>(`/chat-service/api/v1/users/${userId}/status`);
    return res.data;
  },

  updateGroupSettings: async (
    conversationId: number,
    name?: string,
    groupImageUrl?: string
  ): Promise<ConversationResponse> => {
    const res = await api.put<ConversationResponse>(`/chat-service/api/v1/conversations/${conversationId}`, {
      name,
      groupImageUrl,
    });
    return res.data;
  },


  addParticipant: async (conversationId: number, participantId: number): Promise<void> => {
    await api.post(`/chat-service/api/v1/conversations/${conversationId}/participants?participantId=${participantId}`);
  },

  removeParticipant: async (conversationId: number, participantId: number): Promise<void> => {
    await api.delete(`/chat-service/api/v1/conversations/${conversationId}/participants/${participantId}`);
  },

  leaveGroup: async (conversationId: number): Promise<void> => {
    await api.post(`/chat-service/api/v1/conversations/${conversationId}/leave`);
  },
};
