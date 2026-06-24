import React, { useRef, useState } from 'react';
import { Image, Smile, Send, Loader2 } from 'lucide-react';
import { useAuthStore } from '../../store/authStore';
import { mediaService } from '../../services/api';
import './MessageComposer.css';

interface MessageComposerProps {
  onSend: (content: string, messageType?: string) => void;
  onTyping?: () => void;
  disabled?: boolean;
}

export default function MessageComposer({ onSend, onTyping, disabled }: MessageComposerProps) {
  const [value, setValue] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { user } = useAuthStore();

  function autoGrow(el: HTMLTextAreaElement) {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  }

  function handleChange(e: React.ChangeEvent<HTMLTextAreaElement>) {
    setValue(e.target.value);
    autoGrow(e.target);
    if (e.target.value.trim()) onTyping?.();
  }

  function handleSend() {
    const trimmed = value.trim();
    if (!trimmed || disabled || isUploading) return;
    onSend(trimmed, 'TEXT');
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.focus();
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  const handleMediaClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset so same file can be selected again if needed
    e.target.value = '';

    if (!user?.userId) {
      console.error("No authenticated user ID found for upload");
      return;
    }

    setIsUploading(true);
    try {
      const response = await mediaService.uploadMedia(file, user.userId);
      console.log("Uploaded media response in MessageComposer:", response);
      if (response && response.url) {
        onSend(response.url, 'IMAGE');
      }
    } catch (err) {
      console.error("Failed to upload image:", err);
      alert("Failed to upload image. Please try again.");
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="composer">
      <button 
        className="icon-btn composer__media-btn" 
        aria-label="Add image" 
        onClick={handleMediaClick}
        disabled={disabled || isUploading}
      >
        {isUploading ? <Loader2 className="animate-spin" size={20} /> : <Image size={20} />}
      </button>
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        style={{ display: 'none' }}
      />

      <div className="composer__input-wrap">
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Start a new message"
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          disabled={disabled || isUploading}
        />
        <button className="composer__emoji-btn" aria-label="Add emoji" disabled>
          <Smile size={20} />
        </button>
      </div>

      <button
        className={`composer__send-btn${(value.trim() && !isUploading) ? ' composer__send-btn--active' : ''}`}
        onClick={handleSend}
        disabled={!value.trim() || disabled || isUploading}
        aria-label="Send message"
      >
        <Send size={18} />
      </button>
    </div>
  );
}
