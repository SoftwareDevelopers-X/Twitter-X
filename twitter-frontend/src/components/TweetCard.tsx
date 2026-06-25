import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import { useUser } from '../hooks/useUser';
import { updateTweetInCache, snapshotTweetsCache } from '../utils/queryCache';
import { 
  MessageCircle, 
  Repeat2, 
  Heart, 
  Bookmark, 
  Trash2, 
  Edit3,
  MoreHorizontal,
  CheckCircle,
  Eye
} from 'lucide-react';
import { formatRelativeTime } from '../utils/date';
import ReplyDialog from './ReplyDialog';
import ImageViewer from './ImageViewer';
import toast from 'react-hot-toast';
import { Tweet } from '../types';

interface TweetCardProps {
  tweet: Tweet;
}

const TweetCard: React.FC<TweetCardProps> = ({ tweet }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const currentUserId = user?.userId || 0;
  
  // Fetch author details
  const { data: author } = useUser(tweet.userId);

  // States
  const [isReplyOpen, setIsReplyOpen] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(tweet.content);
  const [showMenu, setShowMenu] = useState(false);
  const [viewerIndex, setViewerIndex] = useState<number | null>(null);

  // --- Likes status & mutation ---
  const { data: isLiked } = useQuery({
    queryKey: ['like-status', currentUserId, tweet.tweetId],
    queryFn: () => socialService.isTweetLiked(currentUserId, tweet.tweetId),
    enabled: !!currentUserId,
  });

  const likeMutation = useMutation({
    mutationFn: async () => {
      if (isLiked) {
        const res = await socialService.unlikeTweet(tweet.tweetId, currentUserId);
        return { data: res };
      } else {
        return socialService.likeTweet(tweet.tweetId, currentUserId);
      }
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['like-status', currentUserId, tweet.tweetId] });
      await queryClient.cancelQueries({ queryKey: ['tweets'] });
      await queryClient.cancelQueries({ queryKey: ['feed-tweets'] });
      await queryClient.cancelQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
      await queryClient.cancelQueries({ queryKey: ['user-posts'] });
      await queryClient.cancelQueries({ queryKey: ['liked-posts'] });

      const previousLikeStatus = queryClient.getQueryData(['like-status', currentUserId, tweet.tweetId]);
      const previousTweetsSnapshots = snapshotTweetsCache(queryClient, tweet.tweetId);

      const nextLiked = !isLiked;
      queryClient.setQueryData(['like-status', currentUserId, tweet.tweetId], nextLiked);

      updateTweetInCache(queryClient, tweet.tweetId, (t) => ({
        ...t,
        likeCount: Math.max(0, t.likeCount + (isLiked ? -1 : 1))
      }));

      return { previousLikeStatus, previousTweetsSnapshots };
    },
    onError: (_err, _variables, context) => {
      if (context) {
        queryClient.setQueryData(['like-status', currentUserId, tweet.tweetId], context.previousLikeStatus);
        context.previousTweetsSnapshots.forEach((snapshot) => {
          queryClient.setQueryData(snapshot.queryKey, snapshot.data);
        });
      }
      toast.error('Failed to update like status');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['like-status', currentUserId, tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
    }
  });

  // --- Retweets status & mutation ---
  const { data: isRetweeted } = useQuery({
    queryKey: ['retweet-status', currentUserId, tweet.tweetId],
    queryFn: () => socialService.isRetweeted(currentUserId, tweet.tweetId),
    enabled: !!currentUserId,
  });

  const retweetMutation = useMutation({
    mutationFn: async () => {
      if (isRetweeted) {
        const res = await socialService.undoRetweet(tweet.tweetId, currentUserId);
        return { data: res };
      } else {
        return socialService.retweet(tweet.tweetId, currentUserId);
      }
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['retweet-status', currentUserId, tweet.tweetId] });
      await queryClient.cancelQueries({ queryKey: ['tweets'] });
      await queryClient.cancelQueries({ queryKey: ['feed-tweets'] });
      await queryClient.cancelQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
      await queryClient.cancelQueries({ queryKey: ['user-posts'] });

      const previousRetweetStatus = queryClient.getQueryData(['retweet-status', currentUserId, tweet.tweetId]);
      const previousTweetsSnapshots = snapshotTweetsCache(queryClient, tweet.tweetId);

      const nextRetweeted = !isRetweeted;
      queryClient.setQueryData(['retweet-status', currentUserId, tweet.tweetId], nextRetweeted);

      updateTweetInCache(queryClient, tweet.tweetId, (t) => ({
        ...t,
        retweetCount: Math.max(0, t.retweetCount + (isRetweeted ? -1 : 1))
      }));

      return { previousRetweetStatus, previousTweetsSnapshots };
    },
    onError: (_err, _variables, context) => {
      if (context) {
        queryClient.setQueryData(['retweet-status', currentUserId, tweet.tweetId], context.previousRetweetStatus);
        context.previousTweetsSnapshots.forEach((snapshot) => {
          queryClient.setQueryData(snapshot.queryKey, snapshot.data);
        });
      }
      toast.error('Failed to update retweet status');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['retweet-status', currentUserId, tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
    },
    onSuccess: () => {
      toast.success(isRetweeted ? 'Retweet removed' : 'Retweeted successfully');
    }
  });

  // --- Bookmarks status & mutation ---
  const { data: isBookmarked } = useQuery({
    queryKey: ['bookmark-status', currentUserId, tweet.tweetId],
    queryFn: () => socialService.isBookmarked(currentUserId, tweet.tweetId),
    enabled: !!currentUserId,
  });

  const bookmarkMutation = useMutation({
    mutationFn: async () => {
      if (isBookmarked) {
        const res = await socialService.removeBookmark(tweet.tweetId, currentUserId);
        return { data: res };
      } else {
        return socialService.bookmarkTweet(tweet.tweetId, currentUserId);
      }
    },
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['bookmark-status', currentUserId, tweet.tweetId] });
      await queryClient.cancelQueries({ queryKey: ['bookmarks', currentUserId] });

      const previousBookmarkStatus = queryClient.getQueryData(['bookmark-status', currentUserId, tweet.tweetId]);

      queryClient.setQueryData(['bookmark-status', currentUserId, tweet.tweetId], !isBookmarked);

      return { previousBookmarkStatus };
    },
    onError: (_err, _variables, context) => {
      if (context) {
        queryClient.setQueryData(['bookmark-status', currentUserId, tweet.tweetId], context.previousBookmarkStatus);
      }
      toast.error('Failed to update bookmark status');
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['bookmark-status', currentUserId, tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['bookmarks', currentUserId] });
    },
    onSuccess: () => {
      toast.success(isBookmarked ? 'Removed from Bookmarks' : 'Saved to Bookmarks');
    }
  });

  // --- Delete Tweet Mutation ---
  const deleteMutation = useMutation({
    mutationFn: () => tweetService.deleteTweet(tweet.tweetId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tweets'] });
      queryClient.invalidateQueries({ queryKey: ['feed-tweets'] });
      queryClient.invalidateQueries({ queryKey: ['user-posts'] });
      toast.success('Post deleted successfully');
    },
    onError: () => {
      toast.error('Failed to delete post');
    }
  });

  // --- Update Tweet Mutation ---
  const updateMutation = useMutation({
    mutationFn: () => tweetService.updateTweet(tweet.tweetId, { content: editContent.trim() }),
    onSuccess: () => {
      setIsEditing(false);
      queryClient.invalidateQueries({ queryKey: ['tweets'] });
      queryClient.invalidateQueries({ queryKey: ['feed-tweets'] });
      queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweet.tweetId] });
      queryClient.invalidateQueries({ queryKey: ['user-posts'] });
      toast.success('Post updated');
    },
    onError: () => {
      toast.error('Failed to update post');
    }
  });

  const isOwnTweet = tweet.userId === currentUserId;
  const isAdmin = user?.role === 'ADMIN';
  const canDelete = isOwnTweet || isAdmin;

  const handleHashtagClick = (tag: string, e: React.MouseEvent) => {
    e.stopPropagation();
    const cleanTag = tag.startsWith('#') ? tag.slice(1) : tag;
    navigate(`/search?q=${encodeURIComponent(cleanTag)}`);
  };

  // Function to render tweet content and make hashtags clickable
  const renderFormattedContent = (content: string) => {
    const words = content.split(/(\s+)/);
    return words.map((word, i) => {
      if (word.startsWith('#')) {
        return (
          <span 
            key={i} 
            onClick={(e) => handleHashtagClick(word, e)}
            className="text-twitter-blue hover:underline cursor-pointer"
          >
            {word}
          </span>
        );
      }
      return word;
    });
  };

  return (
    <div 
      onClick={() => navigate(`/tweet/${tweet.tweetId}`)}
      className="border-b border-twitter-dark-4 p-4 hover:bg-neutral-900/20 transition-colors duration-150 cursor-pointer flex gap-3 text-left relative"
    >
      {/* Profile Avatar */}
      <div onClick={(e) => { e.stopPropagation(); navigate(`/profile/${tweet.userId}`); }}>
        <img
          src={author?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${author?.username || 'user'}`}
          alt="Avatar"
          className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 hover:opacity-90 transition-opacity duration-150"
        />
      </div>

      {/* Tweet Body */}
      <div className="flex-grow min-w-0">
        
        {/* Author Header */}
        <div className="flex items-center justify-between">
          <div 
            onClick={(e) => { e.stopPropagation(); navigate(`/profile/${tweet.userId}`); }}
            className="flex items-center gap-1.5 flex-wrap min-w-0"
          >
            <span className="font-bold text-white text-sm hover:underline truncate">
              {author?.displayName || author?.username || 'Loading...'}
            </span>
            {author?.isVerified && (
              <CheckCircle className="w-4 h-4 text-twitter-blue fill-current flex-shrink-0" />
            )}
            <span className="text-twitter-gray-1 text-xs truncate">@{author?.username || 'user'}</span>
            <span className="text-twitter-gray-1 text-xs">·</span>
            <span className="text-twitter-gray-1 text-xs hover:underline" title={tweet.createdAt}>
              {formatRelativeTime(tweet.createdAt)}
            </span>
          </div>

          {/* Action Menu (Delete, Edit etc.) */}
          {user && (
            <div className="relative" onClick={(e) => e.stopPropagation()}>
              <button
                onClick={() => setShowMenu(!showMenu)}
                className="p-1.5 hover:bg-white/10 text-twitter-gray-1 hover:text-white rounded-full transition-colors duration-200"
              >
                <MoreHorizontal className="w-4 h-4" />
              </button>
              {showMenu && (
                <div className="absolute right-0 mt-1 bg-black border border-twitter-dark-4 rounded-xl py-1.5 w-36 shadow-2xl z-20">
                  {isOwnTweet && (
                    <button
                      onClick={() => { setIsEditing(true); setShowMenu(false); }}
                      className="w-full text-left px-4 py-2 text-xs font-semibold text-white hover:bg-white/5 flex items-center gap-2"
                    >
                      <Edit3 className="w-3.5 h-3.5" />
                      Edit Post
                    </button>
                  )}
                  {canDelete && (
                    <button
                      onClick={() => { 
                        if (window.confirm('Delete this post?')) {
                          deleteMutation.mutate();
                        }
                        setShowMenu(false);
                      }}
                      className="w-full text-left px-4 py-2 text-xs font-semibold text-red-500 hover:bg-red-500/10 flex items-center gap-2"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Delete Post
                    </button>
                  )}
                  <button
                    onClick={() => setShowMenu(false)}
                    className="w-full text-left px-4 py-2 text-xs text-twitter-gray-1 hover:bg-white/5"
                  >
                    Cancel
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Content Area */}
        {isEditing ? (
          <div className="mt-2" onClick={(e) => e.stopPropagation()}>
            <textarea
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
              rows={3}
              maxLength={280}
              className="w-full bg-twitter-dark-3 border border-twitter-dark-4 rounded-lg p-2.5 text-white outline-none focus:border-twitter-blue"
            />
            <div className="flex justify-end gap-2 mt-2">
              <button
                onClick={() => setIsEditing(false)}
                className="px-3 py-1 bg-transparent hover:bg-white/10 rounded-full border border-twitter-dark-4 text-xs font-bold text-white"
              >
                Cancel
              </button>
              <button
                onClick={() => updateMutation.mutate()}
                disabled={!editContent.trim() || updateMutation.isPending}
                className="px-3 py-1 bg-twitter-blue hover:bg-twitter-blue-hover rounded-full text-xs font-bold text-white disabled:opacity-50"
              >
                Save
              </button>
            </div>
          </div>
        ) : (
          <p className="text-white text-[15px] mt-1.5 whitespace-pre-wrap break-words leading-relaxed">
            {renderFormattedContent(tweet.content)}
          </p>
        )}

        {/* Media Attachments */}
        {tweet.mediaUrls && tweet.mediaUrls.length > 0 && !isEditing && (
          <div className="mt-3 rounded-2xl overflow-hidden border border-twitter-dark-4 bg-twitter-dark-2">
            {tweet.mediaUrls.map((url, index) => {
              const isVideo = /\.(mp4|mov|webm)($|\?)/i.test(url) || url.includes('/video/') || url.includes('mediaType=VIDEO');
              const imageAndGifUrls = tweet.mediaUrls.filter(u => !(/\.(mp4|mov|webm)($|\?)/i.test(u) || u.includes('/video/') || u.includes('mediaType=VIDEO')));
              const clickIndex = imageAndGifUrls.indexOf(url);
              
              return (
                <div key={index} className="max-h-[500px] overflow-hidden flex justify-center items-center">
                  {isVideo ? (
                    <video src={url} controls className="w-full object-cover max-h-[500px]" onClick={(e) => e.stopPropagation()} />
                  ) : (
                    <img 
                      src={url} 
                      alt="Attached Media" 
                      className="w-full object-cover max-h-[500px] hover:opacity-95 transition-opacity duration-150" 
                      onClick={(e) => {
                        e.stopPropagation();
                        if (clickIndex !== -1) {
                          setViewerIndex(clickIndex);
                        }
                      }}
                    />
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* Interactive Stats Buttons */}
        <div className="flex items-center justify-between w-full max-w-[425px] mt-4 text-twitter-gray-1" onClick={(e) => e.stopPropagation()}>
          
          {/* Reply Button */}
          <div className="flex items-center group text-twitter-gray-1 hover:text-twitter-blue">
            <button 
              onClick={() => setIsReplyOpen(true)}
              className="p-2 rounded-full group-hover:bg-twitter-blue/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
            >
              <MessageCircle className="w-[18px] h-[18px] transition-transform duration-150 active:scale-75" />
            </button>
            <span className="text-[13px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">{tweet.replyCount || 0}</span>
          </div>

          {/* Retweet Button */}
          <div className={`flex items-center group ${isRetweeted ? 'text-green-500' : 'text-twitter-gray-1 hover:text-green-500'}`}>
            <button 
              onClick={() => retweetMutation.mutate()}
              className="p-2 rounded-full group-hover:bg-green-500/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
            >
              <Repeat2 className="w-[18px] h-[18px] transition-transform duration-150 active:scale-75" />
            </button>
            <span className="text-[13px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">{tweet.retweetCount || 0}</span>
          </div>

          {/* Like Button */}
          <div className={`flex items-center group ${isLiked ? 'text-pink-600' : 'text-twitter-gray-1 hover:text-pink-600'}`}>
            <button 
              onClick={() => likeMutation.mutate()}
              className="p-2 rounded-full group-hover:bg-pink-600/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
            >
              <Heart className={`w-[18px] h-[18px] transition-transform duration-150 active:scale-75 ${isLiked ? 'fill-current' : ''}`} />
            </button>
            <span className="text-[13px] ml-1 select-none w-8 text-left transition-colors duration-200 leading-none tabular-nums flex items-center">{tweet.likeCount || 0}</span>
          </div>

          {/* Bookmark Button */}
          <div className={`flex items-center group ${isBookmarked ? 'text-twitter-blue' : 'text-twitter-gray-1 hover:text-twitter-blue'}`}>
            <button 
              onClick={() => bookmarkMutation.mutate()}
              className="p-2 rounded-full group-hover:bg-twitter-blue/10 transition-all duration-200 flex items-center justify-center flex-shrink-0"
            >
              <Bookmark className={`w-[18px] h-[18px] transition-transform duration-150 active:scale-75 ${isBookmarked ? 'fill-current' : ''}`} />
            </button>
          </div>

          {/* Views Counter (Display only) */}
          <div className="flex items-center text-twitter-gray-1">
            <div className="p-2 flex items-center justify-center flex-shrink-0">
              <Eye className="w-[18px] h-[18px]" />
            </div>
            <span className="text-[13px] ml-1 select-none w-8 text-left leading-none tabular-nums flex items-center">{tweet.viewCount || 0}</span>
          </div>

        </div>

      </div>

      {/* Reply Modal Dialog */}
      <ReplyDialog
        tweet={tweet}
        isOpen={isReplyOpen}
        onClose={() => setIsReplyOpen(false)}
      />

      {/* Image Viewer Modal */}
      {viewerIndex !== null && (
        <ImageViewer
          urls={tweet.mediaUrls.filter(u => !(/\.(mp4|mov|webm)($|\?)/i.test(u) || u.includes('/video/') || u.includes('mediaType=VIDEO')))}
          initialIndex={viewerIndex}
          onClose={() => setViewerIndex(null)}
        />
      )}
    </div>
  );
};

export default TweetCard;
