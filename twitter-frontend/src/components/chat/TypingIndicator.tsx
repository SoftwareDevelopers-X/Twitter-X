import React from 'react';
import Avatar from './Avatar';
import { getUserDisplay } from '../../hooks/userDisplay';
import './TypingIndicator.css';

interface TypingIndicatorProps {
  userIds?: number[];
}

const TypingIndicator: React.FC<TypingIndicatorProps> = ({ userIds }) => {
  if (!userIds || userIds.length === 0) return null;
  const user = getUserDisplay(userIds[0]);

  return (
    <div className="msg-row py-1 flex items-center gap-2 select-none">
      <div className="msg-row__avatar-slot">
        <Avatar user={user} size={28} />
      </div>
      <div className="typing-bubble">
        <span className="typing-bubble__dot" />
        <span className="typing-bubble__dot" />
        <span className="typing-bubble__dot" />
      </div>
      {userIds.length > 1 ? (
        <span className="text-xs text-twitter-gray-1">Several people are typing...</span>
      ) : (
        <span className="text-xs text-twitter-gray-1">{user.name} is typing...</span>
      )}
    </div>
  );
};

export default TypingIndicator;
