import React from 'react';
import Avatar from './Avatar';
import { useUserDisplay } from '../../hooks/userDisplay';
import './TypingIndicator.css';

interface TypingIndicatorProps {
  userIds: number[];
}

export default function TypingIndicator({ userIds }: TypingIndicatorProps) {
  if (!userIds || userIds.length === 0) return null;
  const user = useUserDisplay(userIds[0]);

  return (
    <div className="msg-row">
      <div className="msg-row__avatar-slot">
        <Avatar user={user} size={28} />
      </div>
      <div className="typing-bubble">
        <span className="typing-bubble__dot" />
        <span className="typing-bubble__dot" />
        <span className="typing-bubble__dot" />
      </div>
    </div>
  );
}
