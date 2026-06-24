import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import MessageBubble from './MessageBubble';
import TypingIndicator from './TypingIndicator';
import { formatDayDivider } from '../../hooks/formatTime';
import { useAuthStore } from '../../store/authStore';
import Spinner from './Spinner';
import './MessageThread.css';

interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  messageType: 'TEXT' | 'IMAGE' | 'VIDEO';
  status: string;
  createdAt: string;
}

interface MessageThreadProps {
  messages: Message[];
  typingUserIds: number[];
  hasMore: boolean;
  onLoadMore: () => void;
  loading: boolean;
}

const GROUP_WINDOW_MS = 5 * 60 * 1000; // 5 min grouping

export default function MessageThread({
  messages,
  typingUserIds,
  hasMore,
  onLoadMore,
  loading,
}: MessageThreadProps) {
  const { user } = useAuthStore();
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const prevScrollHeightRef = useRef(0);
  const isNearBottomRef = useRef(true);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    if (isNearBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, typingUserIds.length]);

  // Preserve scroll position when older messages prepended
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el || !loadingMore) return;
    el.scrollTop = el.scrollHeight - prevScrollHeightRef.current;
    setLoadingMore(false);
  }, [messages, loadingMore]);

  function handleScroll() {
    const el = scrollRef.current;
    if (!el) return;

    isNearBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 120;

    if (el.scrollTop < 80 && hasMore && !loadingMore) {
      prevScrollHeightRef.current = el.scrollHeight;
      setLoadingMore(true);
      onLoadMore();
    }
  }

  if (loading) {
    return (
      <div className="msg-thread msg-thread--loading">
        <Spinner />
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="msg-thread msg-thread--empty">
        <p>This is the beginning of your conversation.</p>
      </div>
    );
  }

  const items = buildRenderItems(messages, user?.userId ?? 0);

  return (
    <div className="msg-thread" ref={scrollRef} onScroll={handleScroll}>
      {loadingMore && (
        <div className="msg-thread__load-more">
          <Spinner size={20} />
        </div>
      )}

      {items.map((item: any) =>
        item.kind === 'divider' ? (
          <div key={item.key} className="msg-thread__divider">
            <span>{item.label}</span>
          </div>
        ) : (
          <MessageBubble
            key={item.message.id}
            message={item.message}
            isMine={item.isMine}
            showAvatar={item.showAvatar}
            showStatus={item.showStatus}
            groupPosition={item.groupPosition}
          />
        )
      )}

      <TypingIndicator userIds={typingUserIds} />

      <div ref={bottomRef} />
    </div>
  );
}

function buildRenderItems(messages: Message[], myUserId: number) {
  const items: any[] = [];
  let lastDateKey: string | null = null;

  messages.forEach((msg, i) => {
    const dateKey = new Date(msg.createdAt).toDateString();
    if (dateKey !== lastDateKey) {
      items.push({ kind: 'divider', key: `divider-${dateKey}`, label: formatDayDivider(msg.createdAt) });
      lastDateKey = dateKey;
    }

    const prev = messages[i - 1];
    const next = messages[i + 1];

    const sameSenderAsPrev =
      prev &&
      prev.senderId === msg.senderId &&
      new Date(msg.createdAt).getTime() - new Date(prev.createdAt).getTime() < GROUP_WINDOW_MS &&
      new Date(prev.createdAt).toDateString() === dateKey;

    const sameSenderAsNext =
      next &&
      next.senderId === msg.senderId &&
      new Date(next.createdAt).getTime() - new Date(msg.createdAt).getTime() < GROUP_WINDOW_MS &&
      new Date(next.createdAt).toDateString() === dateKey;

    let groupPosition: 'single' | 'first' | 'middle' | 'last' = 'single';
    if (sameSenderAsPrev && sameSenderAsNext) groupPosition = 'middle';
    else if (sameSenderAsPrev) groupPosition = 'last';
    else if (sameSenderAsNext) groupPosition = 'first';

    items.push({
      kind: 'message',
      message: msg,
      isMine: msg.senderId === myUserId,
      showAvatar: !sameSenderAsNext,
      showStatus: !sameSenderAsNext,
      groupPosition,
    });
  });

  return items;
}
