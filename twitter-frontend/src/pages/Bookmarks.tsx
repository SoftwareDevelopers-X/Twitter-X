import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { socialService, tweetService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import TweetCard from '../components/TweetCard';
import { Bookmark, Loader2, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import { Tweet } from '../types';

const Bookmarks: React.FC = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const userId = user?.userId || 0;

  // Query: Fetch bookmarked tweet IDs, then fetch full details for each
  const { data: bookmarkedTweets, isLoading, isError, refetch } = useQuery({
    queryKey: ['bookmarks', userId],
    queryFn: async () => {
      if (!userId) return [];
      // 1. Get bookmarked IDs
      const ids = await socialService.getBookmarkedTweets(userId);
      if (!ids || ids.length === 0) return [];
      
      // 2. Fetch full tweet details in parallel
      const tweetPromises = ids.map(async (tweetId) => {
        try {
          return await tweetService.getTweet(tweetId);
        } catch (err) {
          console.error(`Failed to fetch bookmarked tweet ${tweetId}:`, err);
          return null; // Ignore deleted/not found tweets
        }
      });
      
      const results = await Promise.all(tweetPromises);
      return results.filter((t): t is Tweet => t !== null);
    },
    enabled: !!userId,
  });

  return (
    <div className="flex flex-col min-h-screen bg-black">
      
      {/* Sticky Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 flex items-center justify-between px-4 py-3 text-left">
        <div>
          <h2 className="font-extrabold text-xl text-white leading-tight">Bookmarks</h2>
          {user && (
            <span className="text-twitter-gray-1 text-xs">@{user.username}</span>
          )}
        </div>
      </div>

      {/* Bookmarked Tweets List */}
      <div className="flex-grow">
        {isLoading ? (
          <div className="flex justify-center items-center h-48 text-twitter-blue">
            <Loader2 className="w-8 h-8 animate-spin" />
          </div>
        ) : isError ? (
          <div className="p-8 text-center text-twitter-gray-1 flex flex-col items-center gap-2">
            <AlertCircle className="w-10 h-10 text-red-500" />
            <p className="font-semibold text-white">Failed to load bookmarks</p>
            <button onClick={() => refetch()} className="mt-2 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-1.5 px-4 rounded-full text-xs">
              Retry
            </button>
          </div>
        ) : !bookmarkedTweets || bookmarkedTweets.length === 0 ? (
          <div className="p-12 text-center text-twitter-gray-1 flex flex-col items-center gap-3">
            <div className="w-16 h-16 bg-twitter-blue/10 text-twitter-blue rounded-full flex items-center justify-center border border-twitter-blue/20">
              <Bookmark className="w-8 h-8" />
            </div>
            <h3 className="font-black text-xl text-white tracking-tight">Save posts for later</h3>
            <p className="text-sm max-w-[325px]">
              Don’t let the good ones fly away! Bookmark posts to easily find them again in the future.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-twitter-dark-4">
            {bookmarkedTweets.map((tweet) => (
              <TweetCard key={tweet.tweetId} tweet={tweet} />
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default Bookmarks;
