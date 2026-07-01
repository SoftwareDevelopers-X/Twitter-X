import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { chatApi, ChatMessageResponse, ConversationResponse } from '../services/chatApi';
import { createChatSocket, STOMP_DESTINATIONS, conversationTopic } from '../services/chatSocket';
import { useAuthStore } from '../store/authStore';

interface UserStatus {
  userId: number;
  online: boolean;
  lastSeen: string;
}

interface ChatContextType {
  connectionState: string;
  connectionError: string | null;
  conversations: ConversationResponse[];
  conversationsLoading: boolean;
  activeConversationId: number | null;
  messages: ChatMessageResponse[];
  messagesLoading: boolean;
  hasMoreMessages: boolean;
  typingUserIds: number[];
  onlineStatuses: Record<number, UserStatus>;
  openConversation: (id: number) => Promise<void>;
  loadOlderMessages: (id: number) => Promise<void>;
  sendMessage: (id: number, content: string, messageType?: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM') => boolean;
  sendTyping: (id: number) => void;
  sendMarkRead: (id: number, lastReadMessageId: number) => void;
  startOneToOne: (otherUserId: number) => Promise<ConversationResponse>;
  startGroup: (participantIds: number[], groupName: string) => Promise<ConversationResponse>;
  refreshConversations: () => Promise<void>;
  editMessage: (messageId: number, content: string) => Promise<void>;
  deleteForEveryone: (messageId: number) => Promise<void>;
  deleteForMe: (messageId: number) => Promise<void>;
  addReaction: (messageId: number, reaction: string) => Promise<void>;
  removeReaction: (messageId: number, reaction: string) => Promise<void>;
  fetchUserStatus: (userId: number) => Promise<void>;
  updateGroupSettings: (id: number, name?: string, groupImageUrl?: string) => Promise<void>;
  addParticipant: (id: number, userId: number) => Promise<void>;
  removeParticipant: (id: number, userId: number) => Promise<void>;
  leaveGroup: (id: number) => Promise<void>;
}

const ChatContext = createContext<ChatContextType | null>(null);

const TYPING_TIMEOUT_MS = 3000;

export const ChatProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, user } = useAuthStore();

  const [connectionState, setConnectionState] = useState('disconnected');
  const [connectionError, setConnectionError] = useState<string | null>(null);

  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [conversationsLoading, setConversationsLoading] = useState(true);

  const [activeConversationId, setActiveConversationId] = useState<number | null>(null);
  const [messagesByConversation, setMessagesByConversation] = useState<Record<number, ChatMessageResponse[]>>({});
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [hasMoreByConversation, setHasMoreByConversation] = useState<Record<number, boolean>>({});
  const [pageByConversation, setPageByConversation] = useState<Record<number, number>>({});

  const [typingByConversation, setTypingByConversation] = useState<Record<number, Set<number>>>({});
  const [onlineStatuses, setOnlineStatuses] = useState<Record<number, UserStatus>>({});

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

  // Handle incoming STOMP frame payload
  const handleIncoming = useCallback((conversationId: number, payload: any) => {
    const eventType = payload.type;

    if (eventType === 'TYPING' || eventType === 'STOP_TYPING') {
      setTypingByConversation((prev) => {
        const next = { ...prev };
        const set = new Set<number>(next[conversationId] || []);
        const key = `${conversationId}:${payload.userId}`;

        if (eventType === 'TYPING') {
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

    if (eventType === 'READ_RECEIPT') {
      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === conversationId) {
            if (payload.userId === currentUserIdRef.current) {
              return { ...c, unreadCount: 0 };
            }
          }
          return c;
        })
      );

      setMessagesByConversation((prev) => {
        const list = prev[conversationId] || [];
        const updatedList = list.map((m) => {
          if (m.senderId !== payload.userId && m.id <= payload.lastReadMessageId) {
            return { ...m, status: 'READ' as const };
          }
          return m;
        });
        return { ...prev, [conversationId]: updatedList };
      });
      return;
    }

    if (eventType === 'USER_STATUS') {
      setOnlineStatuses((prev) => ({
        ...prev,
        [payload.userId]: {
          userId: payload.userId,
          online: payload.online,
          lastSeen: payload.lastSeen,
        },
      }));
      return;
    }

    if (eventType === 'MESSAGE_EDITED') {
      setMessagesByConversation((prev) => {
        const list = prev[conversationId] || [];
        const updatedList = list.map((m) => {
          if (m.id === payload.messageId) {
            return {
              ...m,
              content: payload.content,
              edited: true,
              editedAt: payload.timestamp,
            };
          }
          return m;
        });
        return { ...prev, [conversationId]: updatedList };
      });

      setConversations((prev) => {
        return prev.map((c) => {
          if (c.id === conversationId && c.lastMessage?.id === payload.messageId) {
            return {
              ...c,
              lastMessage: {
                ...c.lastMessage,
                content: payload.content,
                edited: true,
              } as ChatMessageResponse,

            };
          }
          return c;
        });
      });
      return;
    }

    if (eventType === 'MESSAGE_DELETED') {
      setMessagesByConversation((prev) => {
        const list = prev[conversationId] || [];
        const updatedList = list.map((m) => {
          if (m.id === payload.messageId) {
            return {
              ...m,
              content: 'This message was deleted',
              deleted: true,
            };
          }
          return m;
        });
        return { ...prev, [conversationId]: updatedList };
      });

      setConversations((prev) => {
        return prev.map((c) => {
          if (c.id === conversationId && c.lastMessage?.id === payload.messageId) {
            return {
              ...c,
              lastMessage: {
                ...c.lastMessage,
                content: 'This message was deleted',
                deleted: true,
              } as ChatMessageResponse,

            };
          }
          return c;
        });
      });
      return;
    }

    if (eventType === 'REACTION_ADD') {
      setMessagesByConversation((prev) => {
        const list = prev[conversationId] || [];
        const updatedList = list.map((m) => {
          if (m.id === payload.messageId) {
            const rx = m.reactions || [];
            if (!rx.some((r) => r.userId === payload.userId && r.reaction === payload.reaction)) {
              return {
                ...m,
                reactions: [...rx, { reaction: payload.reaction, userId: payload.userId }],
              };
            }
          }
          return m;
        });
        return { ...prev, [conversationId]: updatedList };
      });
      return;
    }

    if (eventType === 'REACTION_REMOVE') {
      setMessagesByConversation((prev) => {
        const list = prev[conversationId] || [];
        const updatedList = list.map((m) => {
          if (m.id === payload.messageId) {
            const rx = m.reactions || [];
            return {
              ...m,
              reactions: rx.filter((r) => !(r.userId === payload.userId && r.reaction === payload.reaction)),
            };
          }
          return m;
        });
        return { ...prev, [conversationId]: updatedList };
      });
      return;
    }

    if (eventType === 'GROUP_UPDATE') {
      setConversations((prev) => {
        const exists = prev.some((c) => c.id === payload.conversation.id);
        const updated = exists
          ? prev.map((c) => (c.id === payload.conversation.id ? payload.conversation : c))
          : [payload.conversation, ...prev];
        return updated.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
      });
      return;
    }

    // Otherwise it's a new persisted message
    const msg = payload as ChatMessageResponse;
    setMessagesByConversation((prev) => {
      const existing = prev[conversationId] || [];
      if (existing.some((m) => m.id === msg.id)) return prev;
      return { ...prev, [conversationId]: [...existing, msg] };
    });

    setConversations((prev) => {
      const updated = prev.map((c) =>
        c.id === conversationId
          ? {
              ...c,
              lastMessage: msg,
              updatedAt: msg.createdAt,
              unreadCount:
                msg.senderId === currentUserIdRef.current ||
                conversationId === activeConversationIdRef.current
                  ? c.unreadCount
                  : (c.unreadCount || 0) + 1,
            }
          : c
      );
      return [...updated].sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
    });
  }, []);

  // Subscribe to STOMP destination
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
      const data = await chatApi.listConversations();
      setConversations(data || []);
      
      // Fetch status for all direct chat partners
      data.forEach((c) => {
        if (c.type === 'ONE_TO_ONE' && currentUserIdRef.current) {
          const otherId = c.participantIds.find((id) => id !== currentUserIdRef.current);
          if (otherId) {
            chatApi.getUserStatus(otherId)
              .then((status) => {
                setOnlineStatuses((prev) => ({ ...prev, [otherId]: status }));
              })
              .catch(() => {});
          }
        }
      });
    } finally {
      setConversationsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthenticated) refreshConversations();
  }, [isAuthenticated, refreshConversations]);

  // STOMP connection lifecycle
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

  // Subscribe to all conversations
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
          const page = await chatApi.getMessages(conversationId, 0, 30);
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

      // Trigger read receipt
      const localMsgs = messagesByConversation[conversationId] || [];
      if (localMsgs.length > 0) {
        const lastMsg = localMsgs[localMsgs.length - 1];
        if (lastMsg.senderId !== currentUserIdRef.current) {
          const client = stompClientRef.current;
          if (client?.connected) {
            client.publish({
              destination: STOMP_DESTINATIONS.markRead,
              body: JSON.stringify({ conversationId, lastReadMessageId: lastMsg.id }),
            });
          }
          chatApi.markRead(conversationId, lastMsg.id).catch(() => {});
        }
      }
    },
    [messagesByConversation, subscribeToConversation]
  );

  const loadOlderMessages = useCallback(
    async (conversationId: number) => {
      const nextPage = (pageByConversation[conversationId] || 0) + 1;
      const page = await chatApi.getMessages(conversationId, nextPage, 30);
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

  const sendMessage = useCallback((conversationId: number, content: string, messageType: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM' = 'TEXT') => {
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
    chatApi.markRead(conversationId, lastReadMessageId).catch(() => {});
  }, []);

  const startOneToOne = useCallback(
    async (otherUserId: number) => {
      const conversation = await chatApi.createOneToOne(otherUserId);
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
      const conversation = await chatApi.createGroup(participantIds, groupName);
      setConversations((prev) => [conversation, ...prev]);
      subscribeToConversation(conversation.id);
      return conversation;
    },
    [subscribeToConversation]
  );

  const editMessage = useCallback(async (messageId: number, content: string) => {
    await chatApi.editMessage(messageId, content);
  }, []);

  const deleteForEveryone = useCallback(async (messageId: number) => {
    await chatApi.deleteForEveryone(messageId);
  }, []);

  const deleteForMe = useCallback(async (messageId: number) => {
    await chatApi.deleteForMe(messageId);
    if (activeConversationId) {
      setMessagesByConversation((prev) => {
        const list = prev[activeConversationId] || [];
        return {
          ...prev,
          [activeConversationId]: list.filter((m) => m.id !== messageId),
        };
      });
    }
  }, [activeConversationId]);

  const addReaction = useCallback(async (messageId: number, reaction: string) => {
    await chatApi.addReaction(messageId, reaction);
  }, []);

  const removeReaction = useCallback(async (messageId: number, reaction: string) => {
    await chatApi.removeReaction(messageId, reaction);
  }, []);

  const fetchUserStatus = useCallback(async (userId: number) => {
    try {
      const status = await chatApi.getUserStatus(userId);
      setOnlineStatuses((prev) => ({ ...prev, [userId]: status }));
    } catch {}
  }, []);

  const updateGroupSettings = useCallback(async (conversationId: number, name?: string, groupImageUrl?: string) => {
    await chatApi.updateGroupSettings(conversationId, name, groupImageUrl);
  }, []);

  const addParticipant = useCallback(async (conversationId: number, participantId: number) => {
    await chatApi.addParticipant(conversationId, participantId);
  }, []);

  const removeParticipant = useCallback(async (conversationId: number, participantId: number) => {
    await chatApi.removeParticipant(conversationId, participantId);
  }, []);

  const leaveGroup = useCallback(async (conversationId: number) => {
    await chatApi.leaveGroup(conversationId);
    setConversations((prev) => prev.filter((c) => c.id !== conversationId));
    if (activeConversationId === conversationId) {
      setActiveConversationId(null);
    }
  }, [activeConversationId]);

  const value = {
    connectionState,
    connectionError,
    conversations,
    conversationsLoading,
    activeConversationId,
    messages: activeConversationId ? messagesByConversation[activeConversationId] || [] : [],
    messagesLoading,
    hasMoreMessages: activeConversationId ? Boolean(hasMoreByConversation[activeConversationId]) : false,
    typingUserIds: activeConversationId ? Array.from(typingByConversation[activeConversationId] || []) : [],
    onlineStatuses,
    openConversation,
    loadOlderMessages,
    sendMessage,
    sendTyping,
    sendMarkRead,
    startOneToOne,
    startGroup,
    refreshConversations,
    editMessage,
    deleteForEveryone,
    deleteForMe,
    addReaction,
    removeReaction,
    fetchUserStatus,
    updateGroupSettings,
    addParticipant,
    removeParticipant,
    leaveGroup,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

export const useChat = () => {
  const ctx = useContext(ChatContext);
  if (!ctx) throw new Error('useChat must be used within ChatProvider');
  return ctx;
};
