import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function createChatSocket({
  onConnect,
  onDisconnect,
  onError,
}: {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (err: string) => void;
}) {
  const token = localStorage.getItem('accessToken');
  // Pass the token as a query parameter '?token=...' so API Gateway validates it during handshake
  const wsUrl = `/chat-service/ws${token ? `?token=${encodeURIComponent(token)}` : ''}`;

  const client = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {}, // Set to console.log for verbose STOMP frame logging

    onConnect: () => {
      onConnect?.();
    },
    onDisconnect: () => {
      onDisconnect?.();
    },
    onStompError: (frame) => {
      onError?.(frame?.headers?.message || 'WebSocket protocol error');
    },
    onWebSocketError: () => {
      onError?.('Could not establish a WebSocket connection.');
    },
  });

  return client;
}

export const STOMP_DESTINATIONS = {
  sendMessage: '/app/chat.sendMessage',
  typing: '/app/chat.typing',
  stopTyping: '/app/chat.stopTyping',
  markRead: '/app/chat.markRead',
};

export function conversationTopic(conversationId: number) {
  return `/topic/conversations.${conversationId}`;
}
