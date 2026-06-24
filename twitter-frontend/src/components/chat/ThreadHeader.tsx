import React from 'react';
import { ArrowLeft, Info } from 'lucide-react';
import Avatar from './Avatar';
import { useUserDisplay } from '../../hooks/userDisplay';
import { useAuthStore } from '../../store/authStore';
import './ThreadHeader.css';

interface Conversation {
  id: number;
  type: 'ONE_TO_ONE' | 'GROUP';
  name?: string | null;
  participantIds: number[];
}

interface ThreadHeaderProps {
  conversation: Conversation;
  onBack?: () => void;
  onShowInfo?: () => void;
}

export default function ThreadHeader({ conversation, onBack, onShowInfo }: ThreadHeaderProps) {
  const { user } = useAuthStore();
  if (!conversation) return null;

  const isGroup = conversation.type === 'GROUP';
  const otherIds = (conversation.participantIds || []).filter((id) => id !== user?.userId);
  const otherUserId = otherIds[0] || 0;
  const primaryUser = useUserDisplay(otherUserId);

  const title = isGroup ? conversation.name : primaryUser?.name;
  const subtitle = isGroup
    ? `${conversation.participantIds?.length || 0} people`
    : `@${primaryUser?.username}`;

  return (
    <div className="thread-header">
      <button className="icon-btn thread-header__back" onClick={onBack} aria-label="Back">
        <ArrowLeft size={20} />
      </button>

      <div className="thread-header__identity">
        {isGroup ? (
          <div className="thread-header__group-icon">{conversation.name?.charAt(0) || 'G'}</div>
        ) : (
          primaryUser && <Avatar user={primaryUser} size={36} />
        )}
        <div className="thread-header__text">
          <span className="thread-header__title">{title}</span>
          <span className="thread-header__subtitle">{subtitle}</span>
        </div>
      </div>

      <button className="icon-btn" aria-label="Conversation info" onClick={onShowInfo}>
        <Info size={20} />
      </button>
    </div>
  );
}
