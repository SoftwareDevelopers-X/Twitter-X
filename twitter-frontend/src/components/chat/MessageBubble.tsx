import React, { useState } from 'react';
import { Check, CheckCheck, Smile, MoreHorizontal, Edit3, Trash2, ShieldAlert, X } from 'lucide-react';
import Avatar from './Avatar';
import { formatClockTime } from '../../hooks/formatTime';
import { getUserDisplay } from '../../hooks/userDisplay';
import { useChat } from '../../context/ChatContext';
import ImageViewer from '../ImageViewer';
import './MessageBubble.css';

interface MessageBubbleProps {
  message: any;
  isMine: boolean;
  showAvatar: boolean;
  groupPosition: 'single' | 'first' | 'middle' | 'last';
  showStatus: boolean;
  isGroup: boolean;
}

const POPULAR_EMOJIS = ['👍', '❤️', '😂', '😮', '😢', '🙏'];

const MessageBubble: React.FC<MessageBubbleProps> = ({
  message,
  isMine,
  showAvatar,
  groupPosition,
  showStatus,
  isGroup,
}) => {

  const { 
    editMessage, 
    deleteForEveryone, 
    deleteForMe, 
    addReaction, 
    removeReaction 
  } = useChat();

  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState(message.content);
  const [showMenu, setShowMenu] = useState(false);
  const [showEmojiBar, setShowEmojiBar] = useState(false);
  const [viewerOpen, setViewerOpen] = useState(false);

  const sender = getUserDisplay(message.senderId);

  const handleEdit = async () => {
    if (!editText.trim() || editText.trim() === message.content) {
      setIsEditing(false);
      return;
    }
    try {
      await editMessage(message.id, editText.trim());
      setIsEditing(false);
    } catch (err) {
      alert('Failed to edit message');
    }
  };

  const handleToggleReaction = async (emoji: string) => {
    const hasReacted = message.reactions?.some(
      (r: any) => r.userId === sender.id && r.reaction === emoji
    );
    try {
      if (hasReacted) {
        await removeReaction(message.id, emoji);
      } else {
        await addReaction(message.id, emoji);
      }
      setShowEmojiBar(false);
    } catch (err) {
      console.error('Failed to update reaction:', err);
    }
  };

  const handleDeleteEveryone = async () => {
    if (!window.confirm('Delete this message for everyone?')) return;
    try {
      await deleteForEveryone(message.id);
      setShowMenu(false);
    } catch (err) {
      alert('Failed to delete message for everyone');
    }
  };

  const handleDeleteMe = async () => {
    if (!window.confirm('Delete this message for yourself? It will be removed from your view.')) return;
    try {
      await deleteForMe(message.id);
      setShowMenu(false);
    } catch (err) {
      alert('Failed to delete message locally');
    }
  };

  // Group reactions by emoji character
  const groupedReactions = message.reactions?.reduce((acc: Record<string, number[]>, curr: any) => {
    if (!acc[curr.reaction]) acc[curr.reaction] = [];
    acc[curr.reaction].push(curr.userId);
    return acc;
  }, {}) || {};

  return (
    <div className={`msg-row${isMine ? ' msg-row--mine' : ''} group`}>
      {/* Sender Avatar */}
      {!isMine && (
        <div className="msg-row__avatar-slot">
          {showAvatar && <Avatar user={sender} size={28} />}
        </div>
      )}

      {/* Action panel (appears on hover) */}
      {isMine && !message.deleted && (
        <div className="msg-bubble__actions-container flex gap-1">
          {/* Reaction Trigger */}
          <div className="relative">
            <button 
              onClick={() => setShowEmojiBar((prev) => !prev)} 
              className="msg-bubble__action-btn"
              title="Add reaction"
            >
              <Smile size={16} />
            </button>
            {showEmojiBar && (
              <div className="absolute bottom-8 right-0 bg-neutral-900 border border-neutral-800 rounded-full py-1 px-2 flex gap-1 shadow-xl z-30">
                {POPULAR_EMOJIS.map((emoji) => (
                  <button 
                    key={emoji} 
                    onClick={() => handleToggleReaction(emoji)}
                    className="hover:scale-125 transition-transform duration-75 text-sm p-1"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Menu Options Trigger */}
          <div className="relative">
            <button 
              onClick={() => setShowMenu((prev) => !prev)} 
              className="msg-bubble__action-btn"
              title="More actions"
            >
              <MoreHorizontal size={16} />
            </button>
            {showMenu && (
              <div className="absolute bottom-8 right-0 bg-neutral-900 border border-[#2f3336] rounded-xl overflow-hidden shadow-2xl z-30 w-36 text-left">
                <button 
                  onClick={() => { setIsEditing(true); setShowMenu(false); }} 
                  className="w-full text-left px-3 py-2 text-xs text-white hover:bg-neutral-800 flex items-center gap-2"
                >
                  <Edit3 size={12} /> Edit message
                </button>
                <button 
                  onClick={handleDeleteEveryone} 
                  className="w-full text-left px-3 py-2 text-xs text-twitter-red hover:bg-neutral-800 flex items-center gap-2"
                >
                  <ShieldAlert size={12} /> Delete for everyone
                </button>
                <button 
                  onClick={handleDeleteMe} 
                  className="w-full text-left px-3 py-2 text-xs text-twitter-red hover:bg-neutral-800 flex items-center gap-2 border-t border-[#2f3336]"
                >
                  <Trash2 size={12} /> Delete for me
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Theirs message options (only Delete for Me and Reactions) */}
      {!isMine && !message.deleted && (
        <div className="msg-bubble__actions-container flex gap-1">
          {/* Reaction Trigger */}
          <div className="relative">
            <button 
              onClick={() => setShowEmojiBar((prev) => !prev)} 
              className="msg-bubble__action-btn"
              title="Add reaction"
            >
              <Smile size={16} />
            </button>
            {showEmojiBar && (
              <div className="absolute bottom-8 left-0 bg-neutral-900 border border-neutral-800 rounded-full py-1 px-2 flex gap-1 shadow-xl z-30">
                {POPULAR_EMOJIS.map((emoji) => (
                  <button 
                    key={emoji} 
                    onClick={() => handleToggleReaction(emoji)}
                    className="hover:scale-125 transition-transform duration-75 text-sm p-1"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="relative">
            <button 
              onClick={() => setShowMenu((prev) => !prev)} 
              className="msg-bubble__action-btn"
              title="More actions"
            >
              <MoreHorizontal size={16} />
            </button>
            {showMenu && (
              <div className="absolute bottom-8 left-0 bg-neutral-900 border border-[#2f3336] rounded-xl overflow-hidden shadow-2xl z-30 w-36 text-left">
                <button 
                  onClick={handleDeleteMe} 
                  className="w-full text-left px-3 py-2 text-xs text-twitter-red hover:bg-neutral-800 flex items-center gap-2"
                >
                  <Trash2 size={12} /> Delete for me
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Message Bubble Body */}
      <div
        className={[
          'msg-bubble',
          isMine ? 'msg-bubble--mine' : 'msg-bubble--theirs',
          `msg-bubble--${groupPosition}`,
        ].join(' ')}
      >
        {!isMine && isGroup && (groupPosition === 'first' || groupPosition === 'single') && (
          <span className="text-[12px] font-bold text-twitter-blue px-3.5 pt-2 select-none block leading-tight">
            {sender.name}
          </span>
        )}
        {isEditing ? (
          <div className="flex flex-col gap-1 w-full min-w-[180px] p-3">
            <textarea
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              className="bg-black/50 border border-white/20 rounded-lg p-1.5 text-sm text-white resize-none outline-none focus:border-white"
              rows={2}
            />
            <div className="flex justify-end gap-1.5 mt-1">
              <button 
                onClick={() => setIsEditing(false)} 
                className="p-1 hover:bg-white/10 rounded-full text-white/70 hover:text-white"
              >
                <X size={14} />
              </button>
              <button 
                onClick={handleEdit} 
                className="p-1 hover:bg-white/10 rounded-full text-white/70 hover:text-white"
              >
                <Check size={14} />
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Render media content if applicable */}
            {message.messageType === 'IMAGE' && (
              <div className="flex flex-col w-full">
                <img 
                  src={message.content} 
                  alt="Chat attachment" 
                  className="msg-bubble__media w-full"
                  onClick={() => setViewerOpen(true)}
                />
                <span className="msg-bubble__time px-3 py-1.5 self-end flex items-center gap-1 select-none">
                  {formatClockTime(message.createdAt)}
                  {isMine && showStatus && !message.deleted && (
                    <span className="msg-bubble__status">
                      {message.status === 'READ' ? (
                        <CheckCheck size={14} className="text-[#00ba7c]" />
                      ) : message.status === 'DELIVERED' ? (
                        <CheckCheck size={14} />
                      ) : (
                        <Check size={14} />
                      )}
                    </span>
                  )}
                </span>
                {viewerOpen && (
                  <ImageViewer 
                    urls={[message.content]} 
                    initialIndex={0} 
                    onClose={() => setViewerOpen(false)} 
                  />
                )}
              </div>
            )}

            {message.messageType === 'VIDEO' && (
              <div className="flex flex-col w-full relative">
                <video 
                  src={message.content} 
                  className="msg-bubble__media w-full cursor-pointer"
                  onClick={() => setViewerOpen(true)}
                />
                <div 
                  onClick={() => setViewerOpen(true)}
                  className="absolute inset-0 flex items-center justify-center bg-black/25 hover:bg-black/40 transition-colors cursor-pointer"
                  style={{ height: 'calc(100% - 30px)' }}
                >
                  <svg className="w-12 h-12 text-white fill-current drop-shadow-md hover:scale-110 transition-transform duration-100" viewBox="0 0 24 24">
                    <path d="M8 5v14l11-7z" />
                  </svg>
                </div>
                <span className="msg-bubble__time px-3 py-1.5 self-end flex items-center gap-1 select-none">
                  {formatClockTime(message.createdAt)}
                  {isMine && showStatus && !message.deleted && (
                    <span className="msg-bubble__status">
                      {message.status === 'READ' ? (
                        <CheckCheck size={14} className="text-[#00ba7c]" />
                      ) : message.status === 'DELIVERED' ? (
                        <CheckCheck size={14} />
                      ) : (
                        <Check size={14} />
                      )}
                    </span>
                  )}
                </span>
                {viewerOpen && (
                  <ImageViewer 
                    urls={[message.content]} 
                    initialIndex={0} 
                    onClose={() => setViewerOpen(false)} 
                  />
                )}
              </div>
            )}


            {/* Render text content */}
            {(message.messageType === 'TEXT' || message.messageType === 'SYSTEM') && (
              <div className="flex flex-col w-full px-3.5 py-2">
                <span className="msg-bubble__text text-[15px] text-white">
                  {message.deleted ? <span className="italic text-twitter-gray-1">This message was deleted</span> : message.content}
                </span>
                <span className="msg-bubble__time mt-1 self-end flex items-center gap-1 select-none">
                  {formatClockTime(message.createdAt)}
                  {message.edited && !message.deleted && <span className="text-[9px] opacity-75 font-normal">(edited)</span>}
                  {isMine && showStatus && !message.deleted && (
                    <span className="msg-bubble__status">
                      {message.status === 'READ' ? (
                        <CheckCheck size={14} className="text-[#00ba7c]" />
                      ) : message.status === 'DELIVERED' ? (
                        <CheckCheck size={14} />
                      ) : (
                        <Check size={14} />
                      )}
                    </span>
                  )}
                </span>
              </div>
            )}
          </>
        )}

        {/* Reaction Badges List */}
        {Object.keys(groupedReactions).length > 0 && !message.deleted && (
          <div className="msg-bubble__reactions px-3.5 pb-2">

            {Object.entries(groupedReactions).map(([emoji, userIds]) => {
              const uids = userIds as number[];
              return (
                <span 
                  key={emoji} 
                  onClick={() => handleToggleReaction(emoji)}
                  className="msg-bubble__reaction-badge"
                  title={`${uids.length} reaction(s)`}
                >
                  {emoji} {uids.length > 1 && uids.length}
                </span>
              );
            })}

          </div>
        )}
      </div>
    </div>
  );
};

export default MessageBubble;
