import React, { useState, useEffect } from 'react';
import { ArrowLeft, MoreVertical } from 'lucide-react';

import { useChat } from '../../context/ChatContext';
import { useAuthStore } from '../../store/authStore';
import { getUserDisplay, useUserDisplay } from '../../hooks/userDisplay';
import { formatClockTime, formatDayDivider } from '../../hooks/formatTime';
import Avatar from './Avatar';
import GroupDetailsModal from './GroupDetailsModal';
import './ThreadHeader.css';

interface ThreadHeaderProps {
  conversation: any;
  onBack: () => void;
}

const ThreadHeader: React.FC<ThreadHeaderProps> = ({ conversation, onBack }) => {
  const { user: currentUser } = useAuthStore();
  const { onlineStatuses, fetchUserStatus } = useChat();

  const [showDetails, setShowDetails] = useState(false);

  const isGroup = conversation.type === 'GROUP';
  const otherParticipantIds = conversation.participantIds.filter((x: number) => x !== currentUser?.userId);

  const groupTitle = conversation.name || 'Group Chat';

  let title = groupTitle;
  let subtitle = `${conversation.participantIds.length} members`;
  let avatarUser = { name: groupTitle, initial: 'G', color: '#7856ff', username: 'group' };

  const otherId = otherParticipantIds[0];
  const otherDisplay = useUserDisplay(otherId);

  if (!isGroup && otherId) {
    title = otherDisplay.name;
    avatarUser = otherDisplay;
    
    // Check online status in context
    const status = onlineStatuses[otherId];
    if (status) {
      if (status.online) {
        subtitle = 'Online';
      } else {
        subtitle = `Last seen ${formatClockTime(status.lastSeen)}`;
      }
    } else {
      subtitle = `@${otherDisplay.username}`;
    }
  }

  // Trigger status refresh on mount for DM chat partner
  useEffect(() => {
    if (!isGroup && otherId) {
      fetchUserStatus(otherId);
    }
  }, [isGroup, otherId, fetchUserStatus]);

  return (
    <>
      <header className="thread-hdr">
        <button className="thread-hdr__back font-bold" onClick={onBack} aria-label="Back">
          <ArrowLeft size={20} />
        </button>

        <div className="thread-hdr__avatar">
          {isGroup && !conversation.groupImageUrl ? (
            <div className="group-avatar-stack">
              {otherParticipantIds.slice(0, 2).map((id: number, idx: number) => {
                const u = getUserDisplay(id);
                return (
                  <div key={id} className="group-avatar-stack__item" style={{ zIndex: 2 - idx }}>
                    <Avatar user={u} size={24} />
                  </div>
                );
              })}
            </div>
          ) : (
            <Avatar 
              user={isGroup ? { ...avatarUser, avatarUrl: conversation.groupImageUrl } : avatarUser} 
              size={36} 
              online={!isGroup && otherId && onlineStatuses[otherId]?.online} 
            />
          )}
        </div>

        <div className="thread-hdr__info select-none cursor-pointer" onClick={() => isGroup && setShowDetails(true)}>
          <span className="thread-hdr__title">{title}</span>
          <span className={`thread-hdr__subtitle ${subtitle === 'Online' ? 'text-[#00ba7c] font-semibold' : ''}`}>{subtitle}</span>
        </div>

        <div className="thread-hdr__actions">
          <button className="icon-btn" aria-label="Conversation details" onClick={() => setShowDetails(true)}>
            <MoreVertical size={18} />
          </button>
        </div>

      </header>

      {showDetails && (
        <GroupDetailsModal conversationId={conversation.id} onClose={() => setShowDetails(false)} />
      )}
    </>
  );
};

export default ThreadHeader;
