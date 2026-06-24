import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useUser } from '../hooks/useUser';
import TweetCard from '../components/TweetCard';
import { 
  ArrowLeft, 
  Loader2, 
  MessageSquare, 
  Trash2, 
  ShieldAlert, 
  Heart, 
  Repeat2, 
  Bookmark, 
  Eye 
} from 'lucide-react';
import { formatRelativeTime } from '../utils/date';
import toast from 'react-hot-toast';
import { Reply } from '../types';

interface ReplyNode {
  reply: Reply;
  children: ReplyNode[];
}

const buildReplyTree = (flatReplies: Reply[]): ReplyNode[] => {
  const map: { [key: number]: ReplyNode } = {};
  const roots: ReplyNode[] = [];

  // Initialize nodes
  flatReplies.forEach((r) => {
    map[r.replyId] = { reply: r, children: [] };
  });

  // Build tree
  flatReplies.forEach((r) => {
    const node = map[r.replyId];
    if (r.parentReplyId && map[r.parentReplyId]) {
      map[r.parentReplyId].children.push(node);
    } else {
      roots.push(node);
    }
  });

  return roots;
};

// Module-level set to ensure a view API is called at most once per reply session
const viewedReplies = new Set<number>();

const updateReplyInCache = (
  queryClient: any,
  tweetId: number,
  replyId: number,
  updateFn: (reply: Reply) => Reply
) => {
  queryClient.setQueryData(['replies', tweetId], (oldReplies: Reply[] | undefined) => {
    if (!oldReplies) return oldReplies;
    return oldReplies.map((r) => r.replyId === replyId ? updateFn(r) : r);
  });
};

// Helper component to render each individual reply node and its children recursively
const ReplyNodeComponent: React.FC<{
  node: ReplyNode;
  currentUserId: number;
  isAdmin: boolean;
  tweetId: number;
  depth?: number;
}> = ({ node, currentUserId, isAdmin, tweetId, depth = 0 }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { reply, children } = node;
  const { data: author, isLoading } = useUser(reply.userId);
  
  const [isExpanded, setIsExpanded] = useState(false);
  const [isReplying, setIsReplying] = useState(false);
  const [inlineContent, setInlineContent] = useState('');

  // Increment view count on mount
  React.useEffect(() => {
    if (!viewedReplies.has(reply.replyId)) {
      viewedReplies.add(reply.replyId);
      socialService.viewReply(reply.replyId)
        .then((updatedReply) => {
          updateReplyInCache(queryClient, tweetId, reply.replyId, (r) => ({
            ...r,
            viewCount: updatedReply.viewCount
          }));
        })
        .catch(err => {
          console.error('Failed to register view for reply:', err);
        });
    }
  }, [reply.replyId, tweetId]);

  // Mutations
  const deleteReplyMutation = useMutation({
    mutationFn: () => socialService.deleteReply(reply.replyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['replies', tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweetId] });
      toast.success('Reply deleted');
    },
    onError: () => {
      toast.error('Failed to delete reply');
    }
  });

  const likeMutation = useMutation({
    mutationFn: () => reply.isLiked 
      ? socialService.unlikeReply(reply.replyId)
      : socialService.likeReply(reply.replyId),
    onSuccess: (updatedReply) => {
      updateReplyInCache(queryClient, tweetId, reply.replyId, (r) => ({
        ...r,
        isLiked: updatedReply.isLiked,
        likeCount: updatedReply.likeCount
      }));
    },
    onError: () => {
      toast.error('Failed to update like status');
    }
  });

  const retweetMutation = useMutation({
    mutationFn: () => reply.isRetweeted 
      ? socialService.unretweetReply(reply.replyId)
      : socialService.retweetReply(reply.replyId),
    onSuccess: (updatedReply) => {
      updateReplyInCache(queryClient, tweetId, reply.replyId, (r) => ({
        ...r,
        isRetweeted: updatedReply.isRetweeted,
        retweetCount: updatedReply.retweetCount
      }));
    },
    onError: () => {
      toast.error('Failed to update repost status');
    }
  });

  const bookmarkMutation = useMutation({
    mutationFn: () => reply.isBookmarked 
      ? socialService.unbookmarkReply(reply.replyId)
      : socialService.bookmarkReply(reply.replyId),
    onSuccess: (updatedReply) => {
      updateReplyInCache(queryClient, tweetId, reply.replyId, (r) => ({
        ...r,
        isBookmarked: updatedReply.isBookmarked,
        bookmarkCount: updatedReply.bookmarkCount
      }));
      toast.success(reply.isBookmarked ? 'Removed from bookmarks' : 'Bookmarked!');
    },
    onError: () => {
      toast.error('Failed to update bookmark status');
    }
  });

  const submitReplyMutation = useMutation({
    mutationFn: (content: string) => {
      return socialService.addReply(tweetId, currentUserId, content, reply.replyId);
    },
    onSuccess: (response) => {
      const newReply = response.data;
      if (newReply) {
        // 1. Append the new reply to cache
        queryClient.setQueryData(['replies', tweetId], (old: Reply[] | undefined) => {
          if (!old) return [newReply];
          return [...old, newReply];
        });
        // 2. Increment parent replyCount
        updateReplyInCache(queryClient, tweetId, reply.replyId, (r) => ({
          ...r,
          replyCount: (r.replyCount || 0) + 1
        }));
      }
      setInlineContent('');
      setIsReplying(false);
      setIsExpanded(true); // Automatically expand parent to show the new sub-reply
      toast.success('Reply posted!');
    },
    onError: () => {
      toast.error('Failed to post reply');
    }
  });

  const handlePostInlineReply = () => {
    if (!inlineContent.trim()) return;
    submitReplyMutation.mutate(inlineContent.trim());
  };

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

  // Nested Replies visibility configuration
  const previewCount = 2;
  const hasMoreThanPreview = children.length > previewCount;
  
  // depth === 0: Render preview of 2 replies, allow expanding the rest
  // depth >= 1: Collapsed by default (no children shown), expand only when isExpanded is true
  const renderedChildren = depth === 0
    ? (isExpanded ? children : children.slice(0, previewCount))
    : (isExpanded ? children : []);

  return (
    <div className="flex flex-col text-left w-full border-b border-twitter-dark-4 last:border-b-0">
      {/* Individual Comment Box */}
      <div className="p-4 hover:bg-neutral-900/10 flex gap-3">
        {/* Avatar */}
        <img
          src={author?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${author?.username || 'user'}`}
          alt="Avatar"
          onClick={() => navigate(`/profile/${reply.userId}`)}
          className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 cursor-pointer hover:opacity-90 flex-shrink-0"
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

          {/* Interactive Stats Buttons */}
          <div className="flex items-center justify-between w-full max-w-[425px] mt-3.5 text-twitter-gray-1">
            {/* Reply Icon */}
            <div className="flex items-center group text-twitter-gray-1 hover:text-twitter-blue">
              <button 
                onClick={() => {
                  if (!user) {
                    toast.error('You must be logged in to reply');
                    return;
                  }
                  setIsReplying(!isReplying);
                }}
                className="p-2 rounded-full group-hover:bg-twitter-blue/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
              >
                <MessageSquare className="w-4 h-4 transition-transform duration-150 active:scale-75" />
              </button>
              <span className="text-[12px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">
                {reply.replyCount || 0}
              </span>
            </div>

            {/* Retweet Icon */}
            <div className={`flex items-center group ${reply.isRetweeted ? 'text-green-500' : 'text-twitter-gray-1 hover:text-green-500'}`}>
              <button 
                onClick={() => {
                  if (!user) {
                    toast.error('You must be logged in to repost');
                    return;
                  }
                  retweetMutation.mutate();
                }}
                disabled={retweetMutation.isPending}
                className="p-2 rounded-full group-hover:bg-green-500/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
              >
                <Repeat2 className="w-4 h-4 transition-transform duration-150 active:scale-75" />
              </button>
              <span className="text-[12px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">
                {reply.retweetCount || 0}
              </span>
            </div>

            {/* Like Icon */}
            <div className={`flex items-center group ${reply.isLiked ? 'text-pink-600' : 'text-twitter-gray-1 hover:text-pink-600'}`}>
              <button 
                onClick={() => {
                  if (!user) {
                    toast.error('You must be logged in to like');
                    return;
                  }
                  likeMutation.mutate();
                }}
                disabled={likeMutation.isPending}
                className="p-2 rounded-full group-hover:bg-pink-600/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
              >
                <Heart className={`w-4 h-4 transition-transform duration-150 active:scale-75 ${reply.isLiked ? 'fill-current' : ''}`} />
              </button>
              <span className="text-[12px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">
                {reply.likeCount || 0}
              </span>
            </div>

            {/* Bookmark Icon */}
            <div className={`flex items-center group ${reply.isBookmarked ? 'text-twitter-blue' : 'text-twitter-gray-1 hover:text-twitter-blue'}`}>
              <button 
                onClick={() => {
                  if (!user) {
                    toast.error('You must be logged in to bookmark');
                    return;
                  }
                  bookmarkMutation.mutate();
                }}
                disabled={bookmarkMutation.isPending}
                className="p-2 rounded-full group-hover:bg-twitter-blue/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
              >
                <Bookmark className={`w-4 h-4 transition-transform duration-150 active:scale-75 ${reply.isBookmarked ? 'fill-current' : ''}`} />
              </button>
            </div>

            {/* Views Icon */}
            <div className="flex items-center text-twitter-gray-1">
              <div className="p-2 flex items-center justify-center flex-shrink-0">
                <Eye className="w-4 h-4" />
              </div>
              <span className="text-[12px] ml-1 select-none w-8 text-left leading-none tabular-nums flex items-center">
                {reply.viewCount || 0}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Inline Reply Input Field */}
      {isReplying && user && (
        <div className="pl-14 pr-4 py-3 bg-twitter-dark-2/15 border-b border-twitter-dark-4 flex gap-3 text-left">
          <img
            src={`https://api.dicebear.com/7.x/adventurer/svg?seed=${user.username}`}
            alt="Avatar"
            className="w-8 h-8 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
          />
          <div className="flex-grow">
            <textarea
              placeholder="Post your reply"
              value={inlineContent}
              onChange={(e) => setInlineContent(e.target.value)}
              rows={2}
              maxLength={280}
              className="w-full bg-transparent border-0 outline-none resize-none text-[15px] text-white placeholder-twitter-gray-1 focus:ring-0 mt-0.5"
            />
            <div className="flex justify-between items-center border-t border-twitter-dark-4/40 pt-2.5 mt-1.5">
              <span className="text-[11px] text-twitter-gray-1">
                {inlineContent.length > 0 && `${280 - inlineContent.length} left`}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    setIsReplying(false);
                    setInlineContent('');
                  }}
                  className="px-3.5 py-1 text-white border border-twitter-dark-4 hover:bg-white/5 font-bold rounded-full text-[12px] transition-all duration-200"
                >
                  Cancel
                </button>
                <button
                  onClick={handlePostInlineReply}
                  disabled={!inlineContent.trim() || submitReplyMutation.isPending}
                  className="px-3.5 py-1 bg-twitter-blue hover:bg-twitter-blue-hover disabled:bg-twitter-blue/50 text-white font-bold rounded-full text-[12px] transition-all duration-200"
                >
                  {submitReplyMutation.isPending ? 'Replying...' : 'Reply'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Expand button for top-level comment (depth === 0) */}
      {depth === 0 && !isExpanded && hasMoreThanPreview && (
        <button
          onClick={() => setIsExpanded(true)}
          className="text-twitter-blue hover:underline text-xs font-bold pl-14 mt-2.5 mb-3 text-left block"
        >
          Show remaining {children.length - previewCount} replies
        </button>
      )}

      {/* Expand button for deeper nested replies (depth >= 1) */}
      {depth >= 1 && !isExpanded && children.length > 0 && (
        <button
          onClick={() => setIsExpanded(true)}
          className="text-twitter-blue hover:underline text-xs font-bold pl-14 mt-2.5 mb-3 text-left block"
        >
          Show replies ({children.length})
        </button>
      )}

      {/* Render Sub-Replies (Tree children) */}
      {renderedChildren.length > 0 && (
        <div className="pl-4 sm:pl-8 border-l border-twitter-dark-4 mt-2 ml-8 flex flex-col gap-2">
          {renderedChildren.map((childNode) => (
            <ReplyNodeComponent
              key={childNode.reply.replyId}
              node={childNode}
              currentUserId={currentUserId}
              isAdmin={isAdmin}
              tweetId={tweetId}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
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
    onSuccess: (response) => {
      const newReply = response.data;
      if (newReply) {
        queryClient.setQueryData(['replies', tweetId], (old: Reply[] | undefined) => {
          if (!old) return [newReply];
          return [...old, newReply];
        });
      }
      setReplyContent('');
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
          <div className="divide-y divide-twitter-dark-4 pb-20">
            {buildReplyTree(replies).map((rootNode) => (
              <ReplyNodeComponent 
                key={rootNode.reply.replyId} 
                node={rootNode} 
                currentUserId={currentUserId}
                isAdmin={isAdmin}
                tweetId={tweetId}
              />
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default TweetDetail;
