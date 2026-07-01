import React, { useState } from 'react';
import { X, Search, Plus, Camera } from 'lucide-react';
import Avatar from './Avatar';
import Spinner from './Spinner';
import { getUserDisplay } from '../../hooks/userDisplay';
import { useChat } from '../../context/ChatContext';
import { chatApi } from '../../services/chatApi';
import { mediaService, socialService } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import './NewMessageModal.css';


interface NewMessageModalProps {
  onClose: (newConversationId?: number) => void;
}

const NewMessageModal: React.FC<NewMessageModalProps> = ({ onClose }) => {
  const { startOneToOne, startGroup } = useChat();
  const { user: currentUser } = useAuthStore();

  const [query, setQuery] = useState('');
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [groupName, setGroupName] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isGroup = selectedIds.length > 1;

  const handleSearch = async (val: string) => {
    setQuery(val);
    if (!val.trim() || !currentUser) {
      setSearchResults([]);
      return;
    }

    setSearching(true);
    try {
      const res = await socialService.searchProfiles(val.trim(), currentUser.userId);
      // Filter out current user and already selected users
      const list = (res.data || []).filter(
        (p: any) => p.userId !== currentUser.userId && !selectedIds.includes(p.userId)
      );
      setSearchResults(list);
    } catch (err) {
      console.error('Search profiles failed:', err);
    } finally {
      setSearching(false);
    }
  };

  const handleAddUser = (userId: number) => {
    if (!selectedIds.includes(userId)) {
      setSelectedIds((prev) => [...prev, userId]);
    }
    setQuery('');
    setSearchResults([]);
  };

  const handleRemoveUser = (userId: number) => {
    setSelectedIds((prev) => prev.filter((x) => x !== userId));
  };

  const [groupImageUrl, setGroupImageUrl] = useState('');
  const [uploading, setUploading] = useState(false);

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !currentUser) return;
    setUploading(true);
    try {
      const media = await mediaService.uploadMedia(file, currentUser.userId);
      setGroupImageUrl(media.url);
    } catch (err) {
      alert('Failed to upload group image');
    } finally {
      setUploading(false);
    }
  };

  const handleNext = async () => {
    if (selectedIds.length === 0) return;
    setError(null);
    setSubmitting(true);
    try {
      let conversation;
      if (isGroup) {
        if (!groupName.trim()) {
          setError('Give your group a name first.');
          setSubmitting(false);
          return;
        }
        conversation = await startGroup(selectedIds, groupName.trim());
        if (groupImageUrl) {
          try {
            await chatApi.updateGroupSettings(conversation.id, undefined, groupImageUrl);
            conversation.groupImageUrl = groupImageUrl;
          } catch (e) {
            console.error('Failed to set group image:', e);
          }
        }
      } else {
        conversation = await startOneToOne(selectedIds[0]);
      }
      onClose(conversation.id);
    } catch (err: any) {
      setError(err.message || 'Could not start the conversation.');
    } finally {
      setSubmitting(false);
    }
  };


  return (
    <div className="modal-overlay" onClick={() => onClose()}>
      <div className="new-msg-modal" onClick={(e) => e.stopPropagation()}>
        <div className="new-msg-modal__header">
          <button className="icon-btn p-1 text-white hover:bg-neutral-800 rounded-full" onClick={() => onClose()} aria-label="Close">
            <X size={20} />
          </button>
          <h2>New message</h2>
          <button
            className="new-msg-modal__next"
            disabled={selectedIds.length === 0 || submitting}
            onClick={handleNext}
          >
            {submitting ? <Spinner size={16} /> : 'Next'}
          </button>
        </div>

        <div className="new-msg-modal__search">
          <Search size={16} className="new-msg-modal__search-icon" />
          <input
            type="text"
            placeholder="Search people..."
            value={query}
            onChange={(e) => handleSearch(e.target.value)}
          />
          {searching && <Spinner size={14} />}
        </div>

        {selectedIds.length > 0 && (
          <div className="new-msg-modal__chips">
            {selectedIds.map((id) => {
              const u = getUserDisplay(id);
              return (
                <span key={id} className="new-msg-modal__chip">
                  <Avatar user={u} size={20} />
                  <span className="max-w-[100px] overflow-hidden text-ellipsis whitespace-nowrap">{u.name}</span>
                  <button onClick={() => handleRemoveUser(id)} aria-label={`Remove ${u.name}`}>
                    <X size={10} />
                  </button>
                </span>
              );
            })}
          </div>
        )}

        {searchResults.length > 0 && (
          <div className="flex-1 overflow-y-auto max-h-[300px] divide-y divide-[#2f3336] bg-black">
            {searchResults.map((profile) => (
              <div
                key={profile.userId}
                onClick={() => handleAddUser(profile.userId)}
                className="flex items-center justify-between p-3 hover:bg-[#16181c] cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <Avatar user={{
                    avatarUrl: profile.avatarUrl,
                    name: profile.displayName || profile.username,
                    username: profile.username
                  }} size={36} />
                  <div className="flex flex-col text-left">
                    <span className="text-sm font-bold text-white leading-tight">{profile.displayName || profile.username}</span>
                    <span className="text-xs text-twitter-gray-1">@{profile.username}</span>
                  </div>
                </div>
                <button className="bg-twitter-blue hover:bg-twitter-blue-hover text-white p-1 rounded-full">
                  <Plus size={14} />
                </button>
              </div>
            ))}
          </div>
        )}

        {isGroup && (
          <div className="new-msg-modal__group-name flex items-center gap-4 text-left p-4 border-b border-[#2f3336]">
            <div className="relative cursor-pointer">
              <div className="w-14 h-14 rounded-full overflow-hidden border border-[#2f3336] bg-[#202327] flex items-center justify-center relative">
                {groupImageUrl ? (
                  <img src={groupImageUrl} alt="Group Preview" className="w-full h-full object-cover" />
                ) : (
                  <span className="text-white text-lg font-bold">G</span>
                )}
                {uploading && (
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                    <Spinner size={12} />
                  </div>
                )}
              </div>
              <label className="absolute -bottom-1 -right-1 bg-twitter-blue p-1.5 rounded-full text-white cursor-pointer border border-black shadow">
                <Camera size={12} />
                <input type="file" accept="image/*" className="hidden" onChange={handleImageChange} disabled={uploading} />
              </label>
            </div>

            <input
              type="text"
              placeholder="Group name"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              className="flex-grow bg-black border border-[#2f3336] rounded-xl px-4 py-2 text-white focus:outline-none focus:border-twitter-blue transition-colors"
            />
          </div>
        )}


        {error && <div className="new-msg-modal__error">{error}</div>}

        <p className="new-msg-modal__hint text-left">
          Search for users to start a direct message or create a group conversation.
        </p>
      </div>
    </div>
  );
};

export default NewMessageModal;
