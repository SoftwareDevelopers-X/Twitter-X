import React from 'react';
import Avatar from './Avatar';
import { formatClockTime } from '../../hooks/formatTime';
import { useUserDisplay } from '../../hooks/userDisplay';
import { Check, CheckCheck } from 'lucide-react';
import './MessageBubble.css';

interface Message {
  id: number;
  senderId: number;
  content: string;
  createdAt: string;
  status: string;
  messageType?: 'TEXT' | 'IMAGE' | 'VIDEO';
}

interface MessageBubbleProps {
  message: Message;
  isMine: boolean;
  showAvatar: boolean;
  groupPosition: 'single' | 'first' | 'middle' | 'last';
  showStatus: boolean;
}

export default function MessageBubble({
  message,
  isMine,
  showAvatar,
  groupPosition,
  showStatus,
}: MessageBubbleProps) {
  const sender = useUserDisplay(message.senderId);

  return (
    <div className={`msg-row${isMine ? ' msg-row--mine' : ''}`}>
      {!isMine && (
        <div className="msg-row__avatar-slot">
          {showAvatar && <Avatar user={sender} size={28} />}
        </div>
      )}

      <div
        className={[
          'msg-bubble',
          isMine ? 'msg-bubble--mine' : 'msg-bubble--theirs',
          `msg-bubble--${groupPosition}`,
        ].join(' ')}
      >
        {message.messageType === 'IMAGE' ? (
          <img src={message.content} alt="Attachment" className="msg-bubble__image" />
        ) : (
          <span className="msg-bubble__text">{message.content}</span>
        )}
        <span className="msg-bubble__time">
          {formatClockTime(message.createdAt)}
          {isMine && showStatus && (
            <span className="msg-bubble__status">
              {message.status === 'READ' ? (
                <CheckCheck size={14} />
              ) : message.status === 'DELIVERED' ? (
                <CheckCheck size={14} />
              ) : (
                <Check size={14} />
              )}
            </span>
          )}
        </span>
      </div>

      {isMine && (
        <div className="msg-row__avatar-slot">
          {showAvatar && <Avatar user={sender} size={28} />}
        </div>
      )}
    </div>
  );
}
