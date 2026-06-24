import React, { useEffect, useMemo, useState } from 'react';
import { Settings, SquarePen, Search } from 'lucide-react';
import { useChat } from '../../context/ChatContext';
import { useAuthStore } from '../../store/authStore';
import ConversationListItem from './ConversationListItem';
import { useUserDisplay } from '../../hooks/userDisplay';
import Spinner from './Spinner';
import Avatar from './Avatar';
import './ConversationList.css';

interface ConversationListProps {
  activeId: number | null;
  onSelect: (id: number) => void;
  onNewMessage: () => void;
}

export default function ConversationList({ activeId, onSelect, onNewMessage }: ConversationListProps) {
  const { conversations, conversationsLoading, connectionState } = useChat();
  const { user } = useAuthStore();
  const [query, setQuery] = useState('');

  const me = useUserDisplay(user?.userId ?? 0);

  const filtered = useMemo(() => {
    if (!query.trim()) return conversations;
    const q = query.toLowerCase();
    return conversations.filter((c) => {
      if (c.type === 'GROUP') return c.name?.toLowerCase().includes(q);
      const otherId = (c.participantIds || []).find((id) => id !== user?.userId);
      if (!otherId) return false;
      
      // Look up profile details synchronously since they are updated in background
      // Note: we can't call hooks inside a filter/callback, but we can do string check
      // or rely on userDisplay cache. For simplicity, just check names or default username.
      const usernameLower = `user${otherId}`.toLowerCase();
      const displayNameLower = `User ${otherId}`.toLowerCase();
      return usernameLower.includes(q) || displayNameLower.includes(q);
    });
  }, [conversations, query, user]);

  return (
    <div className="conv-list">
      <div className="conv-list__header">
        <div className="conv-list__header-row">
          {me && <Avatar user={me} size={32} />}
          <h1 className="conv-list__title">Messages</h1>
          <div className="conv-list__header-actions">
            <button className="icon-btn" aria-label="Settings">
              <Settings size={20} />
            </button>
            <button className="icon-btn" aria-label="New message" onClick={onNewMessage}>
              <SquarePen size={20} />
            </button>
          </div>
        </div>

        <ConnectionBanner state={connectionState} />

        <div className="conv-list__search">
          <Search size={16} className="conv-list__search-icon" />
          <input
            type="text"
            placeholder="Search Direct Messages"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      <div className="conv-list__scroll">
        {conversationsLoading ? (
          <div className="conv-list__loading">
            <Spinner />
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState hasQuery={Boolean(query.trim())} onNewMessage={onNewMessage} />
        ) : (
          filtered.map((c) => (
            <ConversationListItem
              key={c.id}
              conversation={c}
              active={c.id === activeId}
              onClick={() => onSelect(c.id)}
            />
          ))
        )}
      </div>
    </div>
  );
}

function ConnectionBanner({ state }: { state: string }) {
  const [displayState, setDisplayState] = useState(state);

  useEffect(() => {
    if (state === 'connected') {
      setDisplayState('connected');
      const timer = setTimeout(() => {
        setDisplayState('hidden');
      }, 3000);
      return () => clearTimeout(timer);
    } else {
      setDisplayState(state);
    }
  }, [state]);

  if (displayState === 'hidden') return null;
  const label =
    displayState === 'connecting'
      ? 'Connecting…'
      : displayState === 'connected'
        ? 'Connected'
        : displayState === 'error'
          ? 'Connection lost — retrying…'
          : 'Offline';
  return <div className={`conv-list__conn-banner conv-list__conn-banner--${displayState}`}>{label}</div>;
}

function EmptyState({ hasQuery, onNewMessage }: { hasQuery: boolean; onNewMessage: () => void }) {
  if (hasQuery) {
    return (
      <div className="conv-list__empty">
        <p>No results found.</p>
      </div>
    );
  }
  return (
    <div className="conv-list__empty">
      <h2>Welcome to your inbox!</h2>
      <p>Drop a line, share posts and more with private conversations between you and others on X.</p>
      <button className="conv-list__empty-cta" onClick={onNewMessage}>
        Write a message
      </button>
    </div>
  );
}
