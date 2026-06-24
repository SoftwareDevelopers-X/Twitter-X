import React, { useState, useEffect } from 'react';
import { X, Search } from 'lucide-react';
import Avatar from './Avatar';
import Spinner from './Spinner';
import { getUserDisplay, primeUserCache } from '../../hooks/userDisplay';
import { useChat } from '../../context/ChatContext';
import { socialService } from '../../services/api';
import './NewMessageModal.css';

interface NewMessageModalProps {
  onClose: (newConversationId?: number) => void;
}

export default function NewMessageModal({ onClose }: NewMessageModalProps) {
  const { startOneToOne, startGroup } = useChat();

  const [query, setQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState<number[]>([]);
  const [groupName, setGroupName] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isGroup = selected.length > 1;

  // Search users as they type
  useEffect(() => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    const delayDebounce = setTimeout(async () => {
      setSearching(true);
      try {
        const res = await socialService.searchProfiles(query.trim());
        const users = res.data || [];
        setSearchResults(users);
        primeUserCache(users);
      } catch (err) {
        console.error('Failed to search users:', err);
      } finally {
        setSearching(false);
      }
    }, 300);

    return () => clearTimeout(delayDebounce);
  }, [query]);

  function handleSelectUser(userId: number) {
    if (!selected.includes(userId)) {
      setSelected((prev) => [...prev, userId]);
    }
    setQuery('');
  }

  function handleManualAdd() {
    const id = Number(query.trim());
    if (!id || Number.isNaN(id)) return;
    if (!selected.includes(id)) {
      setSelected((prev) => [...prev, id]);
    }
    setQuery('');
  }

  function handleRemove(id: number) {
    setSelected((prev) => prev.filter((x) => x !== id));
  }

  async function handleNext() {
    if (selected.length === 0) return;
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
        conversation = await startGroup(selected, groupName.trim());
      } else {
        conversation = await startOneToOne(selected[0]);
      }
      onClose(conversation.id);
    } catch (err: any) {
      setError(err.message || 'Could not start the conversation.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={() => onClose()}>
      <div className="new-msg-modal" onClick={(e) => e.stopPropagation()}>
        <div className="new-msg-modal__header">
          <button className="icon-btn" onClick={() => onClose()} aria-label="Close">
            <X size={20} />
          </button>
          <h2>New message</h2>
          <button
            className="new-msg-modal__next"
            disabled={selected.length === 0 || submitting}
            onClick={handleNext}
          >
            {submitting ? <Spinner size={16} /> : 'Next'}
          </button>
        </div>

        <div className="new-msg-modal__search">
          <Search size={16} className="new-msg-modal__search-icon" />
          <input
            type="text"
            placeholder="Search people by username"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleManualAdd()}
          />
        </div>

        {/* Selected chips */}
        {selected.length > 0 && (
          <div className="new-msg-modal__chips">
            {selected.map((id) => {
              const u = getUserDisplay(id);
              return (
                <span key={id} className="new-msg-modal__chip">
                  <Avatar user={u} size={20} />
                  <span className="new-msg-modal__chip-name">{u.name}</span>
                  <button onClick={() => handleRemove(id)} aria-label={`Remove ${u.name}`}>
                    <X size={13} />
                  </button>
                </span>
              );
            })}
          </div>
        )}

        {/* Group Name input */}
        {isGroup && (
          <div className="new-msg-modal__group-name">
            <input
              type="text"
              placeholder="Group name"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
            />
          </div>
        )}

        {error && <div className="new-msg-modal__error">{error}</div>}

        {/* Search Results list */}
        <div className="new-msg-modal__results">
          {searching ? (
            <div className="new-msg-modal__searching">
              <Spinner size={20} />
            </div>
          ) : searchResults.length > 0 ? (
            searchResults.map((profile) => {
              const u = getUserDisplay(profile.userId);
              return (
                <div
                  key={profile.userId}
                  className="new-msg-modal__result-item"
                  onClick={() => handleSelectUser(profile.userId)}
                >
                  <Avatar user={u} size={36} />
                  <div className="new-msg-modal__result-text">
                    <span className="new-msg-modal__result-name">{u.name}</span>
                    <span className="new-msg-modal__result-username">@{u.username}</span>
                  </div>
                </div>
              );
            })
          ) : (
            query.trim() && <div className="new-msg-modal__no-results">No users found. Press Enter to add ID manually.</div>
          )}
        </div>
      </div>
    </div>
  );
}
