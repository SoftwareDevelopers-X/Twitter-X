import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function createChatSocket({
  onConnect,
  onDisconnect,
  onError,
}: {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (msg: string) => void;
}) {
  const token = localStorage.getItem('accessToken');
  const wsUrl = `/chat-service/ws${token ? `?token=${encodeURIComponent(token)}` : ''}`;

  const client = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 4000, // auto-reconnect on drop
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: (str) => console.log('[STOMP]', str), // set to console.log for verbose frames

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
