import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import TweetBox from '../components/TweetBox';
import TweetCard from '../components/TweetCard';
import { Loader2, AlertCircle } from 'lucide-react';
import { Tweet } from '../types';

const Home: React.FC = () => {
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState<'for-you' | 'following'>('for-you');

  // Query 1: For You (Global Tweets)
  const { 
    data: globalTweetsPage, 
    isLoading: isGlobalLoading, 
    isError: isGlobalError,
    refetch: refetchGlobal 
  } = useQuery({
    queryKey: ['tweets', 'global'],
    queryFn: () => tweetService.getAllTweets(0, 40),
  });

  // Query 2: Following Feed
  const { 
    data: feedTweetsList, 
    isLoading: isFeedLoading, 
    isError: isFeedError,
    refetch: refetchFeed 
  } = useQuery({
    queryKey: ['feed-tweets', user?.userId],
    queryFn: () => socialService.getFeed(user?.userId || 0, 0, 40),
    enabled: !!user?.userId && activeTab === 'following',
  });

  const handleTabChange = (tab: 'for-you' | 'following') => {
    setActiveTab(tab);
  };

  const getTweetsToRender = (): Tweet[] => {
    let list: Tweet[] = [];
    if (activeTab === 'for-you') {
      list = globalTweetsPage?.content ? [...globalTweetsPage.content] : [];
    } else {
      // Convert FeedTweetDto[] to Tweet[] so TweetCard can render it.
      // FeedTweetDto has identical ID, content, and counts but misses media/hashtags.
      list = (feedTweetsList || []).map(ft => ({
        tweetId: ft.tweetId,
        userId: ft.userId,
        content: ft.content,
        createdAt: ft.createdAt,
        likeCount: ft.likeCount,
        retweetCount: ft.retweetCount,
        replyCount: ft.replyCount,
        viewCount: 0,
        mediaUrls: [], // Fallback
        hashtags: []   // Fallback
      }));
    }
    return list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  };

  const isLoading = activeTab === 'for-you' ? isGlobalLoading : isFeedLoading;
  const isError = activeTab === 'for-you' ? isGlobalError : isFeedError;
  const tweets = getTweetsToRender();

  return (
    <div className="flex flex-col h-full bg-black">
      
      {/* Sticky Header */}
      <div className="sticky top-0 bg-black/80 backdrop-blur-md border-b border-twitter-dark-4 z-10">
        <div className="px-4 py-3 flex items-center justify-between">
          <h2 className="font-extrabold text-xl text-white">Home</h2>
        </div>

        {/* Feed Selection Tabs */}
        {user && (
          <div className="flex w-full border-t border-twitter-dark-4/40">
            <button
              onClick={() => handleTabChange('for-you')}
              className="flex-grow py-3 text-center hover:bg-white/5 transition-colors duration-200 relative font-bold text-[15px]"
            >
              <span className={activeTab === 'for-you' ? 'text-white' : 'text-twitter-gray-1'}>
                For you
              </span>
              {activeTab === 'for-you' && (
                <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-16 h-1 bg-twitter-blue rounded-full" />
              )}
            </button>
            <button
              onClick={() => handleTabChange('following')}
              className="flex-grow py-3 text-center hover:bg-white/5 transition-colors duration-200 relative font-bold text-[15px]"
            >
              <span className={activeTab === 'following' ? 'text-white' : 'text-twitter-gray-1'}>
                Following
              </span>
              {activeTab === 'following' && (
                <div className="absolute bottom-0 left-1/2 -translate-x-1/2.5 w-16 h-1 bg-twitter-blue rounded-full" />
              )}
            </button>
          </div>
        )}
      </div>

      {/* Tweets List or Loading state */}
      <div className="flex-grow divide-y divide-twitter-dark-4">
        {isLoading ? (
          <div className="flex flex-col">
            {[1, 2, 3].map((n) => (
              <div key={n} className="p-4 border-b border-twitter-dark-4 animate-pulse flex gap-3 text-left">
                <div className="w-10 h-10 bg-twitter-dark-3 rounded-full flex-shrink-0" />
                <div className="flex-grow space-y-3">
                  <div className="h-4 bg-twitter-dark-3 rounded w-1/3" />
                  <div className="h-4 bg-twitter-dark-3 rounded w-full" />
                  <div className="h-4 bg-twitter-dark-3 rounded w-5/6" />
                  <div className="h-10 bg-twitter-dark-3 rounded-xl w-full" />
                </div>
              </div>
            ))}
            <div className="flex justify-center items-center py-6 text-twitter-blue">
              <Loader2 className="w-6 h-6 animate-spin" />
            </div>
          </div>
        ) : isError ? (
          <div className="p-8 text-center text-twitter-gray-1 flex flex-col items-center gap-3">
            <AlertCircle className="w-12 h-12 text-red-500/80" />
            <p className="font-semibold text-white">Oops! Failed to load posts</p>
            <p className="text-xs">Make sure all backend microservices, Eureka, and Redis are running.</p>
            <button 
              onClick={activeTab === 'for-you' ? () => refetchGlobal() : () => refetchFeed()}
              className="mt-2 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-1.5 px-4 rounded-full text-xs"
            >
              Retry
            </button>
          </div>
        ) : tweets.length === 0 ? (
          <div className="p-12 text-center text-twitter-gray-1">
            <p className="font-bold text-lg text-white">Welcome to X-Clone!</p>
            <p className="text-sm mt-1">No posts found. Start following people or compose your first post to begin!</p>
          </div>
        ) : (
          tweets.map((tweet) => (
            <TweetCard key={tweet.tweetId} tweet={tweet} />
          ))
        )}
      </div>

    </div>
  );
};

export default Home;
