import React, { useState, useEffect } from 'react';
import { X, Camera, Plus, Trash2, LogOut, Check } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useChat } from '../../context/ChatContext';
import { useAuthStore } from '../../store/authStore';
import { getUserDisplay, useUserDisplay } from '../../hooks/userDisplay';
import { mediaService, socialService } from '../../services/api';
import Avatar from './Avatar';
import Spinner from './Spinner';

interface GroupDetailsModalProps {
  conversationId: number;
  onClose: () => void;
}

const GroupDetailsModal: React.FC<GroupDetailsModalProps> = ({ conversationId, onClose }) => {
  const navigate = useNavigate();
  const { 
    conversations, 
    updateGroupSettings, 
    addParticipant, 
    removeParticipant, 
    leaveGroup 
  } = useChat();
  const { user: currentUser } = useAuthStore();

  const conversation = conversations.find((c) => c.id === conversationId);

  const [groupName, setGroupName] = useState('');
  const [groupImageUrl, setGroupImageUrl] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [saving, setSaving] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (conversation) {
      setGroupName(conversation.name || '');
      setGroupImageUrl(conversation.groupImageUrl || '');
    }
  }, [conversation]);

  if (!conversation || !currentUser) return null;

  const isGroup = conversation.type === 'GROUP';
  const otherParticipantIds = conversation.participantIds.filter((id) => id !== currentUser.userId);
  const otherId = otherParticipantIds[0];
  const otherUser = useUserDisplay(isGroup ? undefined : otherId);

  if (!isGroup) {
    return (
      <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4" onClick={onClose}>
        <div className="bg-[#15181c] border border-[#2f3336] rounded-2xl w-full max-w-md flex flex-col max-h-[85vh] shadow-2xl animate-in fade-in zoom-in-95 duration-150" onClick={(e) => e.stopPropagation()}>
          
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-[#2f3336]">
            <h2 className="text-xl font-bold text-white">Conversation info</h2>
            <button onClick={onClose} className="p-1.5 hover:bg-white/10 rounded-full text-white transition-colors">
              <X size={20} />
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 flex flex-col items-center">
            {otherUser ? (
              <>
                <Avatar user={otherUser} size={80} />
                <div className="text-center space-y-1">
                  <h3 className="text-lg font-bold text-white">{otherUser.name}</h3>
                  <p className="text-sm text-twitter-gray-1">@{otherUser.username}</p>
                </div>
                <button
                  onClick={() => {
                    navigate(`/profile/${otherId}`);
                    onClose();
                  }}
                  className="bg-white hover:bg-neutral-200 text-black font-bold px-6 py-2 rounded-full transition-all text-sm w-full mt-4"
                >
                  View Profile
                </button>
              </>
            ) : (
              <Spinner size={24} />
            )}
          </div>
        </div>
      </div>
    );
  }

  const isAdmin = conversation.participantIds.includes(currentUser.userId) && 

    (conversation.name !== undefined); // Simplified check or based on creator / admin flag. 
    // In our ConversationResponse, we can assume group creators/admins are enabled.
    // Let's assume current user is admin if they created it or if they are in participants.
    // To be safe, we allow any member of group to update name/image, but check admin permissions for add/remove if requested.
    // Wait, the backend assertAdmin checks if p.admin is true. In ConversationService.createConversation,
    // the creator is admin. So we can check if requester is creator or if they have admin status.
    // To match backend, let's allow editing if the backend doesn't reject, and show friendly error alerts if it does.

  const handleSaveSettings = async () => {
    setSaving(true);
    try {
      await updateGroupSettings(conversationId, groupName, groupImageUrl);
      onClose();
    } catch (err: any) {
      alert(err.message || 'Failed to update group settings');
    } finally {
      setSaving(false);
    }
  };

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploading(true);
    try {
      const media = await mediaService.uploadMedia(file, currentUser.userId);
      setGroupImageUrl(media.url);
      await updateGroupSettings(conversationId, undefined, media.url);
    } catch (err: any) {
      console.error('Failed to upload group image:', err);
      alert(err.message || 'Failed to upload group image');
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveGroupPhoto = async () => {
    if (!window.confirm('Are you sure you want to remove the group photo?')) return;
    setUploading(true);
    try {
      setGroupImageUrl('');
      await updateGroupSettings(conversationId, undefined, 'REMOVE');
    } catch (err: any) {
      console.error('Failed to remove group image:', err);
      alert(err.message || 'Failed to remove group image');
    } finally {
      setUploading(false);
    }
  };


  const handleSearchUsers = async (query: string) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }
    setSearching(true);
    try {
      const res = await socialService.searchProfiles(query, currentUser.userId);
      // Filter out users who are already participants
      const filtered = (res.data || []).filter(
        (profile: any) => !conversation.participantIds.includes(profile.userId)
      );
      setSearchResults(filtered);
    } catch (err) {
      console.error('Search error:', err);
    } finally {
      setSearching(false);
    }
  };

  const handleAddMember = async (userId: number) => {
    try {
      await addParticipant(conversationId, userId);
      setSearchQuery('');
      setSearchResults([]);
    } catch (err: any) {
      alert(err.message || 'Failed to add participant');
    }
  };

  const handleRemoveMember = async (userId: number) => {
    if (!window.confirm('Are you sure you want to remove this member?')) return;
    try {
      await removeParticipant(conversationId, userId);
    } catch (err: any) {
      alert(err.message || 'Only group admins can remove members');
    }
  };

  const handleLeaveGroup = async () => {
    if (!window.confirm('Are you sure you want to leave this group chat?')) return;
    try {
      await leaveGroup(conversationId);
      onClose();
    } catch (err: any) {
      alert(err.message || 'Failed to leave group');
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-[#15181c] border border-[#2f3336] rounded-2xl w-full max-w-md flex flex-col max-h-[85vh] shadow-2xl animate-in fade-in zoom-in-95 duration-150">
        
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-[#2f3336]">
          <div className="flex items-center gap-2">
            <h2 className="text-xl font-bold text-white">Group info</h2>
          </div>
          <button onClick={onClose} className="p-1.5 hover:bg-white/10 rounded-full text-white transition-colors">
            <X size={20} />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-6">
          
          {/* Group Avatar and Name */}
          <div className="flex flex-col items-center gap-4">
            <div className="flex flex-col items-center gap-3">
              <div className="relative group">
                <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-twitter-blue relative bg-[#2f3336] flex items-center justify-center">
                  {groupImageUrl ? (
                    <img src={groupImageUrl} alt="Group Avatar" className="w-full h-full object-cover" />
                  ) : (
                    <span className="text-white text-3xl font-bold">G</span>
                  )}
                  {uploading && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                      <Spinner size={16} />
                    </div>
                  )}
                </div>
              </div>
              <div className="flex gap-2">
                <label className="bg-twitter-blue hover:bg-twitter-blue-hover text-white text-xs px-3 py-1.5 rounded-full font-bold cursor-pointer transition-colors shadow">
                  Change Photo
                  <input type="file" accept="image/*" className="hidden" onChange={handleImageChange} disabled={uploading} />
                </label>
                {groupImageUrl && (
                  <button
                    type="button"
                    onClick={handleRemoveGroupPhoto}
                    disabled={uploading}
                    className="bg-red-500/10 hover:bg-red-500/20 text-twitter-red text-xs px-3 py-1.5 rounded-full font-bold transition-colors border border-twitter-red/30"
                  >
                    Remove Photo
                  </button>
                )}
              </div>
            </div>


            <div className="w-full space-y-2">
              <label className="text-xs text-twitter-gray-1 font-semibold block uppercase tracking-wider">Group Name</label>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  placeholder="e.g. Flight Club"
                  className="flex-1 bg-black border border-[#2f3336] rounded-xl px-4 py-2 text-white focus:outline-none focus:border-twitter-blue transition-colors"
                />
                <button
                  onClick={handleSaveSettings}
                  disabled={saving || !groupName.trim()}
                  className="bg-white hover:bg-neutral-200 text-black px-4 py-2 rounded-xl font-bold transition-all disabled:opacity-50 flex items-center gap-1.5"
                >
                  {saving ? <Spinner size={14} /> : <Check size={16} />}
                  Save
                </button>
              </div>
            </div>
          </div>

          {/* Add Members Section */}
          <div className="space-y-3">
            <label className="text-xs text-twitter-gray-1 font-semibold block uppercase tracking-wider">Add Member</label>
            <div className="relative">
              <input
                type="text"
                placeholder="Search people to add..."
                value={searchQuery}
                onChange={(e) => handleSearchUsers(e.target.value)}
                className="w-full bg-black border border-[#2f3336] rounded-xl pl-4 pr-10 py-2 text-white focus:outline-none focus:border-twitter-blue transition-colors"
              />
              {searching && (
                <div className="absolute right-3 top-2.5">
                  <Spinner size={14} />
                </div>
              )}
            </div>

            {searchResults.length > 0 && (
              <div className="bg-black border border-[#2f3336] rounded-xl overflow-hidden max-h-48 overflow-y-auto divide-y divide-[#2f3336]">
                {searchResults.map((profile) => (
                  <div key={profile.userId} className="flex items-center justify-between p-2.5 hover:bg-[#16181c]">
                    <div className="flex items-center gap-3">
                      <Avatar user={{ 
                        avatarUrl: profile.avatarUrl,
                        name: profile.displayName || profile.username,
                        username: profile.username
                      }} size={32} />
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-white leading-tight">{profile.displayName || profile.username}</span>
                        <span className="text-xs text-twitter-gray-1">@{profile.username}</span>
                      </div>
                    </div>
                    <button
                      onClick={() => handleAddMember(profile.userId)}
                      className="bg-twitter-blue hover:bg-twitter-blue-hover text-white p-1.5 rounded-full transition-colors"
                    >
                      <Plus size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Members List */}
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <label className="text-xs text-twitter-gray-1 font-semibold uppercase tracking-wider">
                Group Members ({conversation.participantIds.length})
              </label>
            </div>
            <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
              {conversation.participantIds.map((memberId) => {
                // Fetch display details
                const memberUser = getUserDisplay(memberId);
                const isSelf = memberId === currentUser.userId;

                return (
                  <div key={memberId} className="flex items-center justify-between py-2 border-b border-[#2f3336]/30 last:border-b-0">
                    <div className="flex items-center gap-3">
                      <Avatar user={memberUser} size={36} />
                      <div className="flex flex-col">
                        <span className="text-sm font-bold text-white leading-tight">
                          {memberUser.name} {isSelf && <span className="text-twitter-gray-1 font-normal">(You)</span>}
                        </span>
                        <span className="text-xs text-twitter-gray-1">@{memberUser.username}</span>
                      </div>
                    </div>

                    {!isSelf && (
                      <button
                        onClick={() => handleRemoveMember(memberId)}
                        className="text-twitter-red hover:bg-red-500/10 p-1.5 rounded-full transition-colors"
                        title="Remove member"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Actions / Danger Zone */}
          <div className="pt-2 border-t border-[#2f3336] flex justify-end">
            <button
              onClick={handleLeaveGroup}
              className="text-twitter-red hover:bg-red-500/10 hover:text-red-500 border border-twitter-red px-4 py-2 rounded-xl font-bold transition-all flex items-center gap-2 text-sm active:scale-95"
            >
              <LogOut size={16} />
              Leave Conversation
            </button>
          </div>

        </div>
      </div>
    </div>
  );
};

export default GroupDetailsModal;
