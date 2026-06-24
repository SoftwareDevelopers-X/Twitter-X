import React from 'react';
import Avatar from './Avatar';
import { useUserDisplay } from '../../hooks/userDisplay';
import { formatRelativeTime } from '../../hooks/formatTime';
import { useAuthStore } from '../../store/authStore';
import './ConversationListItem.css';

interface Message {
  id: number;
  senderId: number;
  content: string;
  createdAt: string;
}

interface Conversation {
  id: number;
  type: 'ONE_TO_ONE' | 'GROUP';
  name?: string | null;
  participantIds: number[];
  lastMessage?: Message | null;
  unreadCount: number;
  updatedAt: string;
}

interface ConversationListItemProps {
  conversation: Conversation;
  active: boolean;
  onClick: () => void;
}

export default function ConversationListItem({
  conversation,
  active,
  onClick,
}: ConversationListItemProps) {
  const { user } = useAuthStore();

  const isGroup = conversation.type === 'GROUP';
  const otherParticipantIds = (conversation.participantIds || []).filter(
    (id) => id !== user?.userId
  );

  const primaryUser = isGroup ? null : useUserDisplay(otherParticipantIds[0]);
  const title = isGroup ? conversation.name : primaryUser?.name;

  const last = conversation.lastMessage;
  const lastSenderIsMe = last && last.senderId === user?.userId;
  const preview = last
    ? `${lastSenderIsMe ? 'You: ' : ''}${last.content}`
    : 'Start the conversation';

  const unread = conversation.unreadCount > 0;

  return (
    <button
      className={`conv-item${active ? ' conv-item--active' : ''}`}
      onClick={onClick}
    >
      <div className="conv-item__avatar">
        {isGroup ? (
          <GroupAvatarStack participantIds={otherParticipantIds} />
        ) : (
          primaryUser && <Avatar user={primaryUser} size={48} />
        )}
      </div>

      <div className="conv-item__body">
        <div className="conv-item__top">
          <span className={`conv-item__title${unread ? ' conv-item__title--unread' : ''}`}>
            {title}
          </span>
          {!isGroup && primaryUser && (
            <span className="conv-item__username">@{primaryUser.username}</span>
          )}
          <span className="conv-item__dot">·</span>
          <span className="conv-item__time">{formatRelativeTime(conversation.updatedAt)}</span>
        </div>
        <div className={`conv-item__preview${unread ? ' conv-item__preview--unread' : ''}`}>
          {preview}
        </div>
      </div>

      {unread && <span className="conv-item__unread-dot" />}
    </button>
  );
}

interface GroupAvatarStackProps {
  participantIds: number[];
}

function GroupAvatarStack({ participantIds }: GroupAvatarStackProps) {
  const user1 = useUserDisplay(participantIds[0]);
  const user2 = useUserDisplay(participantIds[1]);
  const users = [user1, user2].filter(Boolean);

  return (
    <div className="group-avatar-stack">
      {users.map((u, i) => (
        <span key={u.id} className="group-avatar-stack__item" style={{ zIndex: 2 - i }}>
          <Avatar user={u} size={i === 0 ? 34 : 26} />
        </span>
      ))}
    </div>
  );
}
