import React, { useState, useRef } from 'react';
import { useAuthStore } from '../store/authStore';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { tweetService, mediaService } from '../services/api';
import { Image, Film, Smile, MapPin, X, Loader2 } from 'lucide-react';
import { useUser } from '../hooks/useUser';
import toast from 'react-hot-toast';
import EmojiPicker from './EmojiPicker';

interface TweetBoxProps {
  placeholder?: string;
  onSuccess?: () => void;
}

const TweetBox: React.FC<TweetBoxProps> = ({ placeholder = "What's happening?!", onSuccess }) => {
  const { user } = useAuthStore();
  const { data: profile } = useUser(user?.userId);
  const queryClient = useQueryClient();
  const [content, setContent] = useState('');
  const [isUploading, setIsUploading] = useState(false);
  const [attachedMedia, setAttachedMedia] = useState<{ url: string; type: 'IMAGE' | 'VIDEO' | 'GIF' }[]>([]);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  const gifInputRef = useRef<HTMLInputElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const createTweetMutation = useMutation({
    mutationFn: (data: any) => tweetService.createTweet(data),
    onSuccess: () => {
      setContent('');
      setAttachedMedia([]);
      queryClient.invalidateQueries({ queryKey: ['tweets'] });
      queryClient.invalidateQueries({ queryKey: ['feed-tweets', user?.userId] });
      queryClient.invalidateQueries({ queryKey: ['user-posts', user?.userId] });
      queryClient.invalidateQueries({ queryKey: ['trending-hashtags'] });
      toast.success('Your post was sent!');
      if (onSuccess) onSuccess();
    },
    onError: (err: any) => {
      console.error(err);
      toast.error('Failed to create post');
    }
  });

  const handleMediaUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0 || !user) return;
    
    setIsUploading(true);
    try {
      const uploadPromises = Array.from(e.target.files).map(async (file) => {
        const response = await mediaService.uploadMedia(file, user.userId);
        const isVideo = file.type.startsWith('video/');
        const isGif = file.type === 'image/gif';
        const mediaType: 'IMAGE' | 'VIDEO' | 'GIF' = isVideo ? 'VIDEO' : isGif ? 'GIF' : 'IMAGE';
        return { url: response.url, type: mediaType };
      });

      const uploadedFiles = await Promise.all(uploadPromises);
      setAttachedMedia(prev => [...prev, ...uploadedFiles]);
      toast.success('Media uploaded successfully');
    } catch (err) {
      console.error(err);
      toast.error('Failed to upload media');
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
      if (gifInputRef.current) gifInputRef.current.value = '';
    }
  };

  const insertEmoji = (emoji: string) => {
    const textarea = textareaRef.current;
    if (!textarea) {
      setContent(prev => prev + emoji);
      return;
    }

    const startPos = textarea.selectionStart;
    const endPos = textarea.selectionEnd;
    const text = textarea.value;

    const newContent = text.substring(0, startPos) + emoji + text.substring(endPos);
    setContent(newContent);

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(startPos + emoji.length, startPos + emoji.length);
    }, 0);
  };

  const removeMedia = (index: number) => {
    setAttachedMedia(prev => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = () => {
    if (!content.trim() && attachedMedia.length === 0) return;
    if (!user) {
      toast.error('You must be logged in to post');
      return;
    }

    // Auto-parse hashtags (e.g. #React -> ['React'])
    const hashtagRegex = /#(\w+)/g;
    const hashtags: string[] = [];
    let match;
    while ((match = hashtagRegex.exec(content)) !== null) {
      hashtags.push(match[1]);
    }

    const payload = {
      content: content.trim(),
      mediaUrls: attachedMedia.map(m => ({
        mediaUrl: m.url,
        mediaType: m.type
      })),
      hashtags
    };

    createTweetMutation.mutate(payload);
  };

  const isPostDisabled = (!content.trim() && attachedMedia.length === 0) || isUploading || createTweetMutation.isPending;

  return (
    <div className="flex gap-3 px-4 py-3 border-b border-twitter-dark-4 text-left">
      <img
        src={profile?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${user?.username}`}
        alt="Avatar"
        className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
      />
      <div className="flex-grow">
        {/* Input Text Area */}
        <textarea
          ref={textareaRef}
          placeholder={placeholder}
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={3}
          maxLength={280}
          className="w-full bg-transparent border-0 outline-none resize-none text-[19px] leading-relaxed text-white placeholder-twitter-gray-1 focus:ring-0 focus:outline-none focus:border-transparent mt-1"
        />

        {/* Media Attachments Preview */}
        {attachedMedia.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
            {attachedMedia.map((media, index) => (
              <div key={index} className="relative rounded-2xl overflow-hidden group max-h-[300px] border border-twitter-dark-4">
                {media.type === 'VIDEO' ? (
                  <video src={media.url} controls className="w-full h-full object-cover" />
                ) : (
                  <img src={media.url} alt="Attached Media" className="w-full h-full object-cover" />
                )}
                <button
                  onClick={() => removeMedia(index)}
                  className="absolute top-2 right-2 bg-black/75 hover:bg-neutral-800 text-white p-1.5 rounded-full transition-colors duration-200"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Uploading Spinner */}
        {isUploading && (
          <div className="flex items-center gap-2 text-twitter-blue py-2 mt-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span className="text-xs font-semibold">Uploading media...</span>
          </div>
        )}

        {/* Actions Bar */}
        <div className="flex items-center justify-between border-t border-twitter-dark-4/50 mt-3 pt-3">
          <div className="flex items-center gap-1 -ml-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading}
              className="p-2 text-twitter-blue hover:bg-twitter-blue/10 rounded-full transition-colors duration-200 disabled:opacity-50"
              title="Media"
            >
              <Image className="w-5 h-5" />
            </button>
            <input
              type="file"
              ref={fileInputRef}
              onChange={handleMediaUpload}
              accept="image/*,video/*"
              className="hidden"
              multiple
            />
            <button
              type="button"
              onClick={() => gifInputRef.current?.click()}
              disabled={isUploading}
              className="p-2 text-twitter-blue hover:bg-twitter-blue/10 rounded-full transition-colors duration-200"
              title="GIF"
            >
              <Film className="w-5 h-5" />
            </button>
            <input
              type="file"
              ref={gifInputRef}
              onChange={handleMediaUpload}
              accept="image/gif"
              className="hidden"
            />
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                className="p-2 text-twitter-blue hover:bg-twitter-blue/10 rounded-full transition-colors duration-200"
                title="Emoji"
              >
                <Smile className="w-5 h-5" />
              </button>
              {showEmojiPicker && (
                <EmojiPicker
                  onSelect={(emoji) => {
                    insertEmoji(emoji);
                  }}
                  onClose={() => setShowEmojiPicker(false)}
                />
              )}
            </div>
            <button
              type="button"
              className="p-2 text-twitter-blue hover:bg-twitter-blue/10 rounded-full transition-colors duration-200 opacity-50 cursor-not-allowed"
              title="Location"
              disabled
            >
              <MapPin className="w-5 h-5" />
            </button>
          </div>

          <div className="flex items-center gap-3">
            {content.length > 0 && (
              <span className={`text-[13px] font-semibold ${content.length > 250 ? 'text-red-500' : 'text-twitter-gray-1'}`}>
                {280 - content.length}
              </span>
            )}
            <button
              onClick={handleSubmit}
              disabled={isPostDisabled}
              className="px-5 py-2 bg-twitter-blue hover:bg-twitter-blue-hover disabled:bg-twitter-blue/50 text-white font-bold rounded-full text-[15px] transition-all duration-200 active:scale-95 disabled:active:scale-100 flex items-center gap-1.5"
            >
              {createTweetMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
              <span>Post</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TweetBox;
