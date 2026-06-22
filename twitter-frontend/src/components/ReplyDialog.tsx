import React, { useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { socialService } from '../services/api';
import { useUser } from '../hooks/useUser';
import { X, Loader2 } from 'lucide-react';
import { Tweet } from '../types';
import { formatRelativeTime } from '../utils/date';
import toast from 'react-hot-toast';

interface ReplyDialogProps {
  tweet: Tweet;
  isOpen: boolean;
  onClose: () => void;
}

const ReplyDialog: React.FC<ReplyDialogProps> = ({ tweet, isOpen, onClose }) => {
  const { user } = useAuthStore();
  const { data: author } = useUser(tweet.userId);
  const { data: currentUserProfile } = useUser(user?.userId);
  const queryClient = useQueryClient();
  const [content, setContent] = useState('');

  const replyMutation = useMutation({
    mutationFn: () => {
      if (!user) throw new Error('Must be logged in');
      return socialService.addReply(tweet.tweetId, user.userId, content.trim());
    },
    onSuccess: () => {
      setContent('');
      queryClient.invalidateQueries({ queryKey: ['replies', tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweets'] });
      queryClient.invalidateQueries({ queryKey: ['user-replies', user?.userId] });
      toast.success('Your reply was sent!');
      onClose();
    },
    onError: (err: any) => {
      console.error(err);
      toast.error('Failed to post reply');
    }
  });

  if (!isOpen) return null;

  const handlePostReply = () => {
    if (!content.trim()) return;
    replyMutation.mutate();
  };

  return (
    <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-start justify-center pt-[10%] px-4">
      <div className="bg-black border border-twitter-dark-4 rounded-2xl w-full max-w-[600px] overflow-hidden shadow-2xl animate-fade-in text-left">
        
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-twitter-dark-4">
          <button 
            onClick={onClose}
            className="p-1.5 hover:bg-white/10 rounded-full transition-colors duration-200 text-white"
          >
            <X className="w-5 h-5" />
          </button>
          <span className="font-bold text-white">Draft Reply</span>
          <div className="w-8" /> {/* spacing */}
        </div>

        {/* Parent Tweet Context */}
        <div className="px-4 pt-4 flex gap-3 relative">
          <div className="flex flex-col items-center">
            <img
              src={author?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${author?.username || 'user'}`}
              alt="Author Avatar"
              className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4"
            />
            {/* Thread line */}
            <div className="w-0.5 flex-grow bg-twitter-dark-4 my-1 min-h-[40px]" />
          </div>
          <div className="flex-grow pb-4">
            <div className="flex items-center gap-1.5">
              <span className="font-bold text-white text-sm">{author?.displayName || author?.username}</span>
              <span className="text-twitter-gray-1 text-sm">@{author?.username}</span>
              <span className="text-twitter-gray-1 text-sm">·</span>
              <span className="text-twitter-gray-1 text-sm hover:underline">{formatRelativeTime(tweet.createdAt)}</span>
            </div>
            <p className="text-white text-[15px] mt-1 whitespace-pre-wrap leading-relaxed">
              {tweet.content}
            </p>
            <p className="text-twitter-gray-1 text-[13px] mt-2.5">
              Replying to <span className="text-twitter-blue">@{author?.username}</span>
            </p>
          </div>
        </div>

        {/* Reply Editor */}
        <div className="px-4 pb-4 flex gap-3">
          <img
            src={currentUserProfile?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${user?.username || 'user'}`}
            alt="Current User Avatar"
            className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
          />
          <div className="flex-grow">
            <textarea
              placeholder="Post your reply"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={4}
              maxLength={280}
              className="w-full bg-transparent border-0 outline-none resize-none text-[18px] leading-relaxed text-white placeholder-twitter-gray-1 focus:ring-0 focus:border-transparent mt-1"
            />
            
            {/* Actions */}
            <div className="flex items-center justify-between border-t border-twitter-dark-4 pt-3 mt-3">
              <span className="text-xs text-twitter-gray-1">
                {content.length > 0 && `${280 - content.length} characters left`}
              </span>
              
              <button
                onClick={handlePostReply}
                disabled={!content.trim() || replyMutation.isPending}
                className="px-5 py-2 bg-twitter-blue hover:bg-twitter-blue-hover disabled:bg-twitter-blue/50 text-white font-bold rounded-full text-sm transition-all duration-200 active:scale-95 disabled:active:scale-100 flex items-center gap-1.5"
              >
                {replyMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                <span>Reply</span>
              </button>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default ReplyDialog;
