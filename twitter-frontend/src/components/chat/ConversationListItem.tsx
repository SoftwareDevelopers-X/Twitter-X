import React from 'react';
import Avatar from './Avatar';
import { useUserDisplay, getUserDisplay } from '../../hooks/userDisplay';
import { formatRelativeTime } from '../../hooks/formatTime';
import { useAuthStore } from '../../store/authStore';
import './ConversationListItem.css';

interface ConversationListItemProps {
  conversation: any;
  active: boolean;
  onClick: () => void;
}

const ConversationListItem: React.FC<ConversationListItemProps> = ({
  conversation,
  active,
  onClick,
}) => {
  const { user: currentUser } = useAuthStore();

  const isGroup = conversation.type === 'GROUP';
  const otherParticipantIds = (conversation.participantIds || []).filter(
    (id: number) => id !== currentUser?.userId
  );

  const otherId = otherParticipantIds[0];
  const otherUserDisplay = useUserDisplay(isGroup ? undefined : otherId);

  const title = isGroup ? conversation.name : otherUserDisplay?.name;

  const last = conversation.lastMessage;
  const lastSenderIsMe = last && last.senderId === currentUser?.userId;
  
  let preview = 'Start the conversation';
  if (last) {
    if (last.deleted) {
      preview = lastSenderIsMe ? 'You: (Message deleted)' : '(Message deleted)';
    } else if (last.messageType === 'IMAGE') {
      preview = lastSenderIsMe ? 'You sent an image' : 'Sent an image';
    } else if (last.messageType === 'VIDEO') {
      preview = lastSenderIsMe ? 'You sent a video' : 'Sent a video';
    } else {
      preview = `${lastSenderIsMe ? 'You: ' : ''}${last.content}`;
    }
  }

  const unread = conversation.unreadCount > 0;

  return (
    <button
      className={`conv-item${active ? ' conv-item--active' : ''}`}
      onClick={onClick}
    >
      <div className="conv-item__avatar">
        {isGroup ? (
          conversation.groupImageUrl ? (
            <Avatar user={{ name: conversation.name, avatarUrl: conversation.groupImageUrl, initial: 'G' }} size={48} />
          ) : (
            <GroupAvatarStack participantIds={otherParticipantIds} />
          )
        ) : (
          <Avatar user={otherUserDisplay} size={48} />
        )}
      </div>

      <div className="conv-item__body">
        <div className="conv-item__top">
          <span className={`conv-item__title font-bold text-white ${unread ? 'conv-item__title--unread' : ''}`}>
            {title}
          </span>
          {!isGroup && otherUserDisplay && (
            <span className="conv-item__username text-twitter-gray-1">@{otherUserDisplay.username}</span>
          )}
          <span className="conv-item__dot text-twitter-gray-1">·</span>
          <span className="conv-item__time text-twitter-gray-1">{formatRelativeTime(conversation.updatedAt)}</span>
        </div>
        <div className={`conv-item__preview ${unread ? 'conv-item__preview--unread text-white font-semibold' : 'text-twitter-gray-1'}`}>
          {preview}
        </div>
      </div>

      {unread && <span className="conv-item__unread-dot" />}
    </button>
  );
};

interface GroupAvatarStackProps {
  participantIds: number[];
}

const GroupAvatarStack: React.FC<GroupAvatarStackProps> = ({ participantIds }) => {
  const users = participantIds.slice(0, 2).map((id) => getUserDisplay(id));
  return (
    <div className="group-avatar-stack">
      {users.map((u, i) => (
        <span key={u.id || i} className="group-avatar-stack__item" style={{ zIndex: 2 - i }}>
          <Avatar user={u} size={i === 0 ? 32 : 24} />
        </span>
      ))}
    </div>
  );
};

export default ConversationListItem;
