import React from 'react';
import ConversationListItem from './ConversationListItem';
import Spinner from './Spinner';
import { useChat } from '../../context/ChatContext';
import { MailPlus, ShieldAlert } from 'lucide-react';
import './ConversationList.css';

interface ConversationListProps {
  activeId: number | null;
  onSelect: (id: number) => void;
  onNewMessage: () => void;
}

const ConversationList: React.FC<ConversationListProps> = ({
  activeId,
  onSelect,
  onNewMessage,
}) => {
  const { conversations, conversationsLoading, connectionState, connectionError } = useChat();

  return (
    <div className="conv-list">
      <div className="conv-list__header">
        <h1 className="font-extrabold text-white">Messages</h1>
        <button
          className="conv-list__new-btn"
          onClick={onNewMessage}
          aria-label="New message"
        >
          <MailPlus size={20} />
        </button>
      </div>

      {connectionState === 'error' && (
        <div className="conv-list__warning">
          <ShieldAlert size={16} />
          <span>Offline: {connectionError || 'Connection lost'}</span>
        </div>
      )}

      <div className="conv-list__scroll">
        {conversationsLoading ? (
          <Spinner size={32} />
        ) : conversations.length === 0 ? (
          <div className="conv-list__empty">
            <h2 className="font-black text-white">Welcome to your inbox!</h2>
            <p className="text-twitter-gray-1">Drop a line, share Tweets and more with private conversations on X.</p>
            <button className="conv-list__empty-btn font-bold mt-4" onClick={onNewMessage}>
              Write a message
            </button>
          </div>
        ) : (
          conversations.map((c) => (
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
};

export default ConversationList;
