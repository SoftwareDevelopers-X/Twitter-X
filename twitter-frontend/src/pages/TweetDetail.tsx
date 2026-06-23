import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import { updateTweetInCache } from '../utils/queryCache';
import { useAuthStore } from '../store/authStore';
import { useUser } from '../hooks/useUser';
import TweetCard from '../components/TweetCard';
import { ArrowLeft, Loader2, MessageSquare, Trash2, ShieldAlert } from 'lucide-react';
import { formatRelativeTime } from '../utils/date';
import toast from 'react-hot-toast';
import { Reply } from '../types';

// Helper component to render each individual reply with profile enrichment
const ReplyItem: React.FC<{ reply: Reply; currentUserId: number; isAdmin: boolean }> = ({ reply, currentUserId, isAdmin }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { data: author, isLoading } = useUser(reply.userId);

  const deleteReplyMutation = useMutation({
    mutationFn: () => socialService.deleteReply(reply.replyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['replies', reply.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', reply.tweetId] });
      toast.success('Reply deleted');
    },
    onError: () => {
      toast.error('Failed to delete reply');
    }
  });

  const isOwnReply = reply.userId === currentUserId;
  const canDelete = isOwnReply || isAdmin;

  if (isLoading) {
    return (
      <div className="p-4 flex gap-3 animate-pulse border-b border-twitter-dark-4">
        <div className="w-9 h-9 bg-twitter-dark-3 rounded-full flex-shrink-0" />
        <div className="flex-grow space-y-2">
          <div className="h-3.5 bg-twitter-dark-3 rounded w-1/4" />
          <div className="h-3.5 bg-twitter-dark-3 rounded w-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 hover:bg-neutral-900/10 border-b border-twitter-dark-4 flex gap-3 text-left">
      {/* Avatar */}
      <img
        src={author?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${author?.username || 'user'}`}
        alt="Avatar"
        onClick={() => navigate(`/profile/${reply.userId}`)}
        className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 cursor-pointer hover:opacity-90"
      />
      {/* Content */}
      <div className="flex-grow min-w-0">
        <div className="flex items-center justify-between">
          <div 
            onClick={() => navigate(`/profile/${reply.userId}`)}
            className="flex items-center gap-1.5 flex-wrap cursor-pointer"
          >
            <span className="font-bold text-white text-sm hover:underline">
              {author?.displayName || author?.username}
            </span>
            {author?.isVerified && <span className="text-twitter-blue text-xs">✓</span>}
            <span className="text-twitter-gray-1 text-xs">@{author?.username}</span>
            <span className="text-twitter-gray-1 text-xs">·</span>
            <span className="text-twitter-gray-1 text-xs">{formatRelativeTime(reply.repliedAt)}</span>
          </div>

          {/* Delete Action Button */}
          {canDelete && (
            <button
              onClick={() => {
                if (window.confirm('Delete this reply?')) {
                  deleteReplyMutation.mutate();
                }
              }}
              disabled={deleteReplyMutation.isPending}
              className="text-twitter-gray-1 hover:text-red-500 p-1 rounded-full hover:bg-red-500/10 transition-colors duration-200"
              title="Delete reply"
            >
              <Trash2 className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
        <p className="text-white text-[15px] mt-1.5 whitespace-pre-wrap leading-relaxed">
          {reply.content}
        </p>
      </div>
    </div>
  );
};

const TweetDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const currentUserId = user?.userId || 0;
  const isAdmin = user?.role === 'ADMIN';

  const tweetId = Number(id);
  
  const [replyContent, setReplyContent] = useState('');

  // 1. Fetch Tweet Info
  const { data: tweet, isLoading: isTweetLoading, isError: isTweetError, refetch: refetchTweet } = useQuery({
    queryKey: ['tweet-detail', tweetId],
    queryFn: () => tweetService.getTweet(tweetId),
    enabled: !!tweetId,
  });

  // 2. Fetch Replies List
  const { data: replies, isLoading: isRepliesLoading } = useQuery({
    queryKey: ['replies', tweetId],
    queryFn: () => socialService.getRepliesByTweet(tweetId),
    enabled: !!tweetId,
  });

  // Mutation: Submit Inline Reply
  const replyMutation = useMutation({
    mutationFn: () => {
      if (!user) throw new Error('Must be logged in');
      return socialService.addReply(tweetId, user.userId, replyContent.trim());
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['tweets'] });
      await queryClient.cancelQueries({ queryKey: ['tweet-detail', tweetId] });

      const snapshots: Array<{ queryKey: any; data: any }> = [];
      queryClient.getQueryCache().findAll().forEach((query) => {
        const data = query.state.data;
        if (!data) return;
        const hasTweet = 
          (data && typeof data === 'object' && 'tweetId' in data && (data as any).tweetId === tweetId) ||
          (Array.isArray(data) && data.some((t: any) => t && t.tweetId === tweetId)) ||
          (data && typeof data === 'object' && 'content' in data && Array.isArray((data as any).content) && (data as any).content.some((t: any) => t && t.tweetId === tweetId));

        if (hasTweet) {
          snapshots.push({ queryKey: query.queryKey, data });
        }
      });

      updateTweetInCache(queryClient, tweetId, (t) => ({
        ...t,
        replyCount: (t.replyCount || 0) + 1,
      }));

      return { snapshots };
    },
    onError: (err, _variables, context) => {
      console.error(err);
      if (context) {
        context.snapshots.forEach((snapshot) => {
          queryClient.setQueryData(snapshot.queryKey, snapshot.data);
        });
      }
      toast.error('Failed to submit reply');
    },
    onSuccess: () => {
      setReplyContent('');
      queryClient.invalidateQueries({ queryKey: ['replies', tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweets'] });
      toast.success('Reply posted!');
    },
  });

  const handlePostReply = () => {
    if (!replyContent.trim()) return;
    replyMutation.mutate();
  };

  if (isTweetLoading) {
    return (
      <div className="flex justify-center items-center h-96 text-twitter-blue">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  if (isTweetError || !tweet) {
    return (
      <div className="p-8 text-center text-twitter-gray-1">
        <p className="font-bold text-lg text-white">Post not found</p>
        <button onClick={() => refetchTweet()} className="mt-4 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-2 px-6 rounded-full">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-black">
      
      {/* Sticky Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 flex items-center gap-6 px-4 py-3 text-left">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-white/10 rounded-full text-white transition-colors duration-200"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="font-extrabold text-xl text-white">Post</h2>
      </div>

      {/* Main Tweet Card */}
      <TweetCard tweet={tweet} />

      {/* Inline Reply Box (Only if logged in) */}
      {user && (
        <div className="border-b border-twitter-dark-4 p-4 flex gap-3 text-left bg-twitter-dark-2/20">
          <img
            src={`https://api.dicebear.com/7.x/adventurer/svg?seed=${user.username}`}
            alt="Avatar"
            className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
          />
          <div className="flex-grow">
            <textarea
              placeholder="Post your reply"
              value={replyContent}
              onChange={(e) => setReplyContent(e.target.value)}
              rows={2}
              maxLength={280}
              className="w-full bg-transparent border-0 outline-none resize-none text-[17px] text-white placeholder-twitter-gray-1 focus:ring-0 mt-1"
            />
            <div className="flex justify-between items-center border-t border-twitter-dark-4/50 pt-3 mt-2">
              <span className="text-xs text-twitter-gray-1">
                {replyContent.length > 0 && `${280 - replyContent.length} left`}
              </span>
              <button
                onClick={handlePostReply}
                disabled={!replyContent.trim() || replyMutation.isPending}
                className="px-4 py-1.5 bg-twitter-blue hover:bg-twitter-blue-hover disabled:bg-twitter-blue/50 text-white font-bold rounded-full text-[13px] transition-all duration-200"
              >
                {replyMutation.isPending ? 'Replying...' : 'Reply'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Replies List */}
      <div className="flex-grow">
        {isRepliesLoading ? (
          <div className="flex justify-center py-6 text-twitter-blue">
            <Loader2 className="w-6 h-6 animate-spin" />
          </div>
        ) : !replies || replies.length === 0 ? (
          <div className="p-8 text-center text-twitter-gray-1 flex flex-col items-center justify-center gap-2">
            <MessageSquare className="w-8 h-8 opacity-45" />
            <p className="text-sm font-semibold">Be the first to reply!</p>
          </div>
        ) : (
          <div className="divide-y divide-twitter-dark-4">
            {replies.map((reply) => (
              <ReplyItem 
                key={reply.replyId} 
                reply={reply} 
                currentUserId={currentUserId}
                isAdmin={isAdmin}
              />
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default TweetDetail;
