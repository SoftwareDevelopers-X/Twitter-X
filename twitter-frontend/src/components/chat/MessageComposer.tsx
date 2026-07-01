import React, { useRef, useState } from 'react';
import { Image, Smile, Send } from 'lucide-react';
import EmojiPicker from '../EmojiPicker';
import { mediaService } from '../../services/api';
import { useAuthStore } from '../../store/authStore';
import Spinner from './Spinner';
import './MessageComposer.css';

interface MessageComposerProps {
  onSend: (content: string, type?: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM') => void;
  onTyping?: () => void;
  disabled?: boolean;
}

const MessageComposer: React.FC<MessageComposerProps> = ({ onSend, onTyping, disabled }) => {
  const { user } = useAuthStore();
  const [value, setValue] = useState('');
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [mediaUploading, setMediaUploading] = useState(false);
  
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const autoGrow = (el: HTMLTextAreaElement) => {
    el.style.height = 'auto';
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
  };

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setValue(e.target.value);
    autoGrow(e.target);
    if (e.target.value.trim() && onTyping) {
      onTyping();
    }
  };

  const handleSend = () => {
    const trimmed = value.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed, 'TEXT');
    setValue('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.focus();
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleEmojiSelect = (emoji: string) => {
    if (textareaRef.current) {
      const start = textareaRef.current.selectionStart;
      const end = textareaRef.current.selectionEnd;
      const text = value;
      const newValue = text.substring(0, start) + emoji + text.substring(end);
      setValue(newValue);
      
      // Auto-grow after text update
      setTimeout(() => {
        if (textareaRef.current) {
          autoGrow(textareaRef.current);
          textareaRef.current.focus();
          textareaRef.current.selectionStart = textareaRef.current.selectionEnd = start + emoji.length;
        }
      }, 0);
    } else {
      setValue((prev) => prev + emoji);
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;

    setMediaUploading(true);
    try {
      const media = await mediaService.uploadMedia(file, user.userId);
      const isVideo = file.type.startsWith('video/');
      onSend(media.url, isVideo ? 'VIDEO' : 'IMAGE');
    } catch (err) {
      console.error('Failed to upload media:', err);
      alert('Failed to upload media message');
    } finally {
      setMediaUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="composer">
      {/* File input for images/videos */}
      <input 
        type="file" 
        accept="image/*,video/*" 
        ref={fileInputRef} 
        onChange={handleFileChange} 
        className="hidden" 
        disabled={disabled || mediaUploading}
      />
      
      <button 
        className="icon-btn composer__media-btn" 
        aria-label="Add image/video" 
        onClick={() => fileInputRef.current?.click()}
        disabled={disabled || mediaUploading}
      >
        {mediaUploading ? <Spinner size={16} /> : <Image size={20} />}
      </button>

      <div className="composer__input-wrap">
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Start a new message"
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          disabled={disabled || mediaUploading}
        />
        
        <div className="relative">
          <button 
            className="composer__emoji-btn" 
            aria-label="Add emoji" 
            onClick={() => setShowEmojiPicker((prev) => !prev)}
            disabled={disabled || mediaUploading}
          >
            <Smile size={20} />
          </button>
          
          {showEmojiPicker && (
            <div className="absolute bottom-12 right-0 z-50">
              <EmojiPicker onSelect={handleEmojiSelect} onClose={() => setShowEmojiPicker(false)} />
            </div>
          )}
        </div>
      </div>

      <button
        className={`composer__send-btn${value.trim() ? ' composer__send-btn--active' : ''}`}
        onClick={handleSend}
        disabled={!value.trim() || disabled || mediaUploading}
        aria-label="Send message"
      >
        <Send size={18} />
      </button>
    </div>
  );
};

export default MessageComposer;
