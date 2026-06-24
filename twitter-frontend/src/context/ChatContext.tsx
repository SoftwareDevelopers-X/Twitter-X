import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { chatService } from '../services/api';
import { createChatSocket, STOMP_DESTINATIONS, conversationTopic } from '../services/chatSocket';
import { useAuthStore } from '../store/authStore';

export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'VIDEO';
  status: string;
  createdAt: string;
}

export interface Conversation {
  id: number;
  type: 'ONE_TO_ONE' | 'GROUP';
  name?: string | null;
  participantIds: number[];
  lastMessage?: Message | null;
  unreadCount: number;
  updatedAt: string;
}

interface ChatContextType {
  connectionState: string;
  connectionError: string | null;
  conversations: Conversation[];
  conversationsLoading: boolean;
  activeConversationId: number | null;
  messages: Message[];
  messagesLoading: boolean;
  hasMoreMessages: boolean;
  typingUserIds: number[];
  openConversation: (conversationId: number) => Promise<void>;
  loadOlderMessages: (conversationId: number) => Promise<void>;
  sendMessage: (conversationId: number, content: string, messageType?: string) => boolean;
  sendTyping: (conversationId: number) => void;
  sendMarkRead: (conversationId: number, lastReadMessageId: number) => void;
  startOneToOne: (otherUserId: number) => Promise<Conversation>;
  startGroup: (participantIds: number[], groupName: string) => Promise<Conversation>;
  refreshConversations: () => Promise<void>;
}

const ChatContext = createContext<ChatContextType | null>(null);

const TYPING_TIMEOUT_MS = 3000;

export function ChatProvider({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuthStore();

  const [connectionState, setConnectionState] = useState('disconnected'); // disconnected | connecting | connected | error
  const [connectionError, setConnectionError] = useState<string | null>(null);

  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [conversationsLoading, setConversationsLoading] = useState(true);

  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [messagesByConversation, setMessagesByConversation] = useState<Record<number, Message[]>>({});
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [hasMoreByConversation, setHasMoreByConversation] = useState<Record<number, boolean>>({});
  const [pageByConversation, setPageByConversation] = useState<Record<number, number>>({});

  const [typingByConversation, setTypingByConversation] = useState<Record<number, Set<number>>>({});

  const stompClientRef = useRef<any>(null);
  const subscriptionsRef = useRef<Map<number, any>>(new Map());
  const typingTimersRef = useRef<Map<number, any>>(new Map());
  const remoteTypingTimersRef = useRef<Map<string, any>>(new Map());

  const currentUserIdRef = useRef<number | null>(null);
  const activeConversationIdRef = useRef<number | null>(null);

  useEffect(() => {
    currentUserIdRef.current = user?.userId ?? null;
  }, [user]);

  useEffect(() => {
    activeConversationIdRef.current = activeConversationId;
  }, [activeConversationId]);

  const handleIncoming = useCallback((conversationId: number, payload: any) => {
    const isTypingEvent = payload.type === 'TYPING' || payload.type === 'STOP_TYPING';
    const isReadReceipt = payload.type === 'READ_RECEIPT';

    if (isTypingEvent) {
      setTypingByConversation((prev) => {
        const next = { ...prev };
        const set = new Set<number>(next[conversationId] || []);
        const key = `${conversationId}:${payload.userId}`;

        if (payload.type === 'TYPING') {
          set.add(payload.userId);
          clearTimeout(remoteTypingTimersRef.current.get(key));
          const timer = setTimeout(() => {
            setTypingByConversation((p2) => {
              const n2 = { ...p2 };
              const s2 = new Set<number>(n2[conversationId] || []);
              s2.delete(payload.userId);
              n2[conversationId] = s2;
              return n2;
            });
          }, TYPING_TIMEOUT_MS);
          remoteTypingTimersRef.current.set(key, timer);
        } else {
          set.delete(payload.userId);
          clearTimeout(remoteTypingTimersRef.current.get(key));
        }

        next[conversationId] = set;
        return next;
      });
      return;
    }

    if (isReadReceipt) {
      return;
    }

    // Otherwise it's a persisted message
    setMessagesByConversation((prev) => {
      const existing = prev[conversationId] || [];
      if (existing.some((m) => m.id === payload.id)) return prev; // de-dupe
      return { ...prev, [conversationId]: [...existing, payload] };
    });

    setConversations((prev) => {
      const updated = prev.map((c) =>
        c.id === conversationId
          ? {
              ...c,
              lastMessage: payload,
              updatedAt: payload.createdAt,
              unreadCount:
                payload.senderId === currentUserIdRef.current ||
                conversationId === activeConversationIdRef.current
                  ? c.unreadCount
                  : (c.unreadCount || 0) + 1,
            }
          : c
      );
      return [...updated].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    });
  }, []);

  const subscribeToConversation = useCallback(
    (conversationId: number) => {
      const client = stompClientRef.current;
      if (!client || !client.connected) return;
      if (subscriptionsRef.current.has(conversationId)) return;

      const sub = client.subscribe(conversationTopic(conversationId), (frame: any) => {
        const payload = JSON.parse(frame.body);
        handleIncoming(conversationId, payload);
      });
      subscriptionsRef.current.set(conversationId, sub);
    },
    [handleIncoming]
  );

  const refreshConversations = useCallback(async () => {
    setConversationsLoading(true);
    try {
      const data = await chatService.listConversations();
      setConversations(data || []);
    } finally {
      setConversationsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) refreshConversations();
  }, [isAuthenticated, refreshConversations]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;

    let cancelled = false;
    setConnectionState('connecting');

    const client = createChatSocket({
      onConnect: () => {
        if (cancelled) return;
        setConnectionState('connected');
        setConnectionError(null);
      },
      onDisconnect: () => {
        if (!cancelled) setConnectionState('disconnected');
      },
      onError: (msg) => {
        if (cancelled) return;
        setConnectionState('error');
        setConnectionError(msg);
      },
    });

    stompClientRef.current = client;
    client.activate();

    const subscriptionsAtMount = subscriptionsRef.current;
    return () => {
      cancelled = true;
      subscriptionsAtMount.forEach((sub) => sub.unsubscribe());
      subscriptionsAtMount.clear();
      client.deactivate();
      stompClientRef.current = null;
    };
  }, [isAuthenticated]);

  useEffect(() => {
    if (connectionState !== 'connected') return;
    conversations.forEach((c) => subscribeToConversation(c.id));
  }, [conversations, connectionState, subscribeToConversation]);

  const openConversation = useCallback(
    async (conversationId: number) => {
      setActiveConversationId(conversationId);
      subscribeToConversation(conversationId);

      const alreadyLoaded = Boolean(messagesByConversation[conversationId]);
      if (!alreadyLoaded) {
        setMessagesLoading(true);
        try {
          const page = await chatService.getMessages(conversationId, 0, 30);
          const ordered = [...(page.content || [])].reverse();
          setMessagesByConversation((prev) => ({ ...prev, [conversationId]: ordered }));
          setHasMoreByConversation((prev) => ({ ...prev, [conversationId]: !page.last }));
          setPageByConversation((prev) => ({ ...prev, [conversationId]: 0 }));
        } finally {
          setMessagesLoading(false);
        }
      }

      setConversations((prev) =>
        prev.map((c) => (c.id === conversationId ? { ...c, unreadCount: 0 } : c))
      );
    },
    [messagesByConversation, subscribeToConversation]
  );

  const loadOlderMessages = useCallback(
    async (conversationId: number) => {
      const nextPage = (pageByConversation[conversationId] || 0) + 1;
      const page = await chatService.getMessages(conversationId, nextPage, 30);
      const ordered = [...(page.content || [])].reverse();
      setMessagesByConversation((prev) => ({
        ...prev,
        [conversationId]: [...ordered, ...(prev[conversationId] || [])],
      }));
      setHasMoreByConversation((prev) => ({ ...prev, [conversationId]: !page.last }));
      setPageByConversation((prev) => ({ ...prev, [conversationId]: nextPage }));
    },
    [pageByConversation]
  );

  const sendMessage = useCallback((conversationId: number, content: string, messageType = 'TEXT') => {
    const client = stompClientRef.current;
    if (!client || !client.connected) return false;
    client.publish({
      destination: STOMP_DESTINATIONS.sendMessage,
      body: JSON.stringify({ conversationId, content, messageType }),
    });
    return true;
  }, []);

  const sendTyping = useCallback((conversationId: number) => {
    const client = stompClientRef.current;
    if (!client || !client.connected) return;
    client.publish({
      destination: STOMP_DESTINATIONS.typing,
      body: JSON.stringify({ conversationId }),
    });

    clearTimeout(typingTimersRef.current.get(conversationId));
    const timer = setTimeout(() => {
      client.publish({
        destination: STOMP_DESTINATIONS.stopTyping,
        body: JSON.stringify({ conversationId }),
      });
    }, TYPING_TIMEOUT_MS);
    typingTimersRef.current.set(conversationId, timer);
  }, []);

  const sendMarkRead = useCallback((conversationId: number, lastReadMessageId: number) => {
    const client = stompClientRef.current;
    if (client?.connected) {
      client.publish({
        destination: STOMP_DESTINATIONS.markRead,
        body: JSON.stringify({ conversationId, lastReadMessageId }),
      });
    }
    chatService.markRead(conversationId, lastReadMessageId).catch(() => {});
  }, []);

  const startOneToOne = useCallback(
    async (otherUserId: number) => {
      const conversation = await chatService.createOneToOne(otherUserId);
      setConversations((prev) => {
        const exists = prev.some((c) => c.id === conversation.id);
        return exists ? prev : [conversation, ...prev];
      });
      subscribeToConversation(conversation.id);
      return conversation;
    },
    [subscribeToConversation]
  );

  const startGroup = useCallback(
    async (participantIds: number[], groupName: string) => {
      const conversation = await chatService.createGroup(participantIds, groupName);
      setConversations((prev) => [conversation, ...prev]);
      subscribeToConversation(conversation.id);
      return conversation;
    },
    [subscribeToConversation]
  );

  const value = {
    connectionState,
    connectionError,
    conversations,
    conversationsLoading,
    activeConversationId,
    messages: activeConversationId ? (messagesByConversation[activeConversationId] || []) : [],
    messagesLoading,
    hasMoreMessages: activeConversationId ? Boolean(hasMoreByConversation[activeConversationId]) : false,
    typingUserIds: activeConversationId ? Array.from(typingByConversation[activeConversationId] || []) : [],
    openConversation,
    loadOlderMessages,
    sendMessage,
    sendTyping,
    sendMarkRead,
    startOneToOne,
    startGroup,
    refreshConversations,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export function useChat() {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChat must be used within ChatProvider');
  return ctx;
}
