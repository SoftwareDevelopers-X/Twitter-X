import React, { useEffect, useLayoutEffect, useRef, useState } from 'react';
import MessageBubble from './MessageBubble';
import TypingIndicator from './TypingIndicator';
import { formatDayDivider } from '../../hooks/formatTime';
import { useAuthStore } from '../../store/authStore';
import Spinner from './Spinner';
import './MessageThread.css';

interface MessageThreadProps {
  messages: any[];
  typingUserIds: number[];
  hasMore: boolean;
  onLoadMore: () => void;
  loading: boolean;
  isGroup: boolean;
}


const GROUP_WINDOW_MS = 5 * 60 * 1000;

const MessageThread: React.FC<MessageThreadProps> = ({
  messages,
  typingUserIds,
  hasMore,
  onLoadMore,
  loading,
  isGroup,
}) => {
  const { user: currentUser } = useAuthStore();
  const scrollRef = useRef<HTMLDivElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const prevScrollHeightRef = useRef(0);
  const isNearBottomRef = useRef(true);

  // Auto-scroll on new messages / typing status changes
  useEffect(() => {
    if (isNearBottomRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages.length, typingUserIds.length]);

  // Maintain scroll position when older history is loaded
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el || !loadingMore) return;
    el.scrollTop = el.scrollHeight - prevScrollHeightRef.current;
    setLoadingMore(false);
  }, [messages, loadingMore]);

  const handleScroll = () => {
    const el = scrollRef.current;
    if (!el) return;

    isNearBottomRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 120;

    if (el.scrollTop < 80 && hasMore && !loadingMore) {
      prevScrollHeightRef.current = el.scrollHeight;
      setLoadingMore(true);
      onLoadMore();
    }
  };

  if (loading) {
    return (
      <div className="msg-thread msg-thread--loading flex flex-col justify-center items-center h-full">
        <Spinner size={32} />
      </div>
    );
  }

  if (messages.length === 0) {
    return (
      <div className="msg-thread msg-thread--empty flex flex-col justify-center items-center h-full text-center p-4">
        <p className="text-twitter-gray-1 text-sm font-semibold">This is the beginning of your conversation history.</p>
      </div>
    );
  }

  const items = buildRenderItems(messages, currentUser?.userId);

  return (
    <div className="msg-thread flex-1 overflow-y-auto" ref={scrollRef} onScroll={handleScroll}>
      {loadingMore && (
        <div className="msg-thread__load-more py-2">
          <Spinner size={20} />
        </div>
      )}

      {items.map((item: any) =>
        item.kind === 'divider' ? (
          <div key={item.key} className="msg-thread__divider flex justify-center py-4 select-none">
            <span className="bg-[#16181c] text-twitter-gray-1 border border-[#2f3336] rounded-full px-3 py-1 text-xs font-bold shadow-sm">
              {item.label}
            </span>
          </div>
        ) : (
          <MessageBubble
            key={item.message.id}
            message={item.message}
            isMine={item.isMine}
            showAvatar={item.showAvatar}
            showStatus={item.showStatus}
            groupPosition={item.groupPosition}
            isGroup={isGroup}
          />
        )
      )}

      <TypingIndicator userIds={typingUserIds} />

      <div ref={bottomRef} />
    </div>
  );
};

function buildRenderItems(messages: any[], myUserId: number | undefined) {
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

export default MessageThread;
