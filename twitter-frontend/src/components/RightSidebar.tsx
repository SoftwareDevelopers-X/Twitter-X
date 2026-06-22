import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { Search, TrendingUp, UserPlus, Check, Sparkles } from 'lucide-react';
import { useUser } from '../hooks/useUser';
import toast from 'react-hot-toast';

// Helper component for recommended follow items
const FollowSuggestion: React.FC<{ targetUserId: number; currentUserId: number }> = ({ targetUserId, currentUserId }) => {
  const queryClient = useQueryClient();
  const { data: profile, isLoading } = useUser(targetUserId);

  const { data: followStatus } = useQuery({
    queryKey: ['follow-status', currentUserId, targetUserId],
    queryFn: async () => {
      const res = await socialService.isFollowing(currentUserId, targetUserId);
      return res.data;
    },
    enabled: !!currentUserId && !!targetUserId && currentUserId !== targetUserId,
  });

  const followMutation = useMutation({
    mutationFn: () => socialService.followUser(currentUserId, targetUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['follow-status', currentUserId, targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['user-profile', targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['user-profile', currentUserId] });
      toast.success(`Followed @${profile?.username || 'user'}`);
    },
    onError: (err: any) => {
      console.error(err);
      toast.error('Failed to follow user');
    }
  });

  const unfollowMutation = useMutation({
    mutationFn: () => socialService.unfollowUser(currentUserId, targetUserId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['follow-status', currentUserId, targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['user-profile', targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['user-profile', currentUserId] });
      toast.success(`Unfollowed @${profile?.username || 'user'}`);
    },
    onError: (err: any) => {
      console.error(err);
      toast.error('Failed to unfollow user');
    }
  });

  if (isLoading || !profile || targetUserId === currentUserId) return null;

  const isFollowing = !!followStatus;

  return (
    <div className="flex items-center justify-between py-2.5 transition-colors duration-200">
      <div className="flex items-center gap-2.5 cursor-pointer">
        <img
          src={profile.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${profile.username}`}
          alt={profile.username}
          className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4"
        />
        <div className="text-left">
          <div className="flex items-center gap-1">
            <span className="font-bold text-white text-sm hover:underline">{profile.displayName || profile.username}</span>
            {profile.isVerified && <span className="text-twitter-blue text-xs">✓</span>}
          </div>
          <span className="text-twitter-gray-1 text-xs">@{profile.username}</span>
        </div>
      </div>
      <button
        onClick={() => isFollowing ? unfollowMutation.mutate() : followMutation.mutate()}
        className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all duration-200 ${
          isFollowing 
            ? 'bg-transparent border border-twitter-dark-4 text-white hover:bg-red-500/10 hover:text-red-500 hover:border-red-500/50' 
            : 'bg-white hover:bg-neutral-200 text-black'
        }`}
      >
        {isFollowing ? 'Following' : 'Follow'}
      </button>
    </div>
  );
};

const RightSidebar: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [searchQuery, setSearchQuery] = useState('');

  // Fetch trending tweets to generate trending hashtags
  const { data: trendingTweets } = useQuery({
    queryKey: ['trending-tweets'],
    queryFn: () => tweetService.getTrendingTweets('24h'),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  // Extract top trending hashtags from trending tweets, or fallback to mock trends
  const trends = React.useMemo(() => {
    const hashCounts: Record<string, number> = {};
    trendingTweets?.forEach(tweet => {
      tweet.hashtags?.forEach(tag => {
        const cleaned = tag.startsWith('#') ? tag.slice(1) : tag;
        hashCounts[cleaned] = (hashCounts[cleaned] || 0) + 1;
      });
    });

    const parsedTrends = Object.entries(hashCounts)
      .map(([hashtag, count]) => ({ hashtag: `#${hashtag}`, posts: count }))
      .sort((a, b) => b.posts - a.posts)
      .slice(0, 4);

    if (parsedTrends.length > 0) return parsedTrends;

    // Premium fallbacks
    return [
      { hashtag: '#Microservices', posts: 1420 },
      { hashtag: '#SpringBoot', posts: 840 },
      { hashtag: '#Kafka', posts: 652 },
      { hashtag: '#React19', posts: 1205 },
    ];
  }, [trendingTweets]);

  // Fallback follow suggestions if no DB profiles exist yet
  // We try IDs 1, 2, 3, 4 (often registered in local/dev DBs)
  const suggestedUserIds = [1, 2, 3, 4];

  return (
    <div className="py-3 flex flex-col gap-4 h-full">
      {/* Search Bar */}
      <form onSubmit={handleSearchSubmit} className="sticky top-0 bg-black pt-1 pb-2 z-10">
        <div className="relative group">
          <input
            type="text"
            placeholder="Search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-twitter-dark-3 border border-transparent rounded-full py-2.5 pl-12 pr-4 text-white text-sm placeholder-twitter-gray-1 focus:outline-none focus:bg-black focus:border-twitter-blue focus:ring-1 focus:ring-twitter-blue transition-all duration-200"
          />
          <Search className="w-5 h-5 absolute left-4 top-3 text-twitter-gray-1 group-focus-within:text-twitter-blue transition-colors duration-200" />
        </div>
      </form>

      {/* Subscribe to Premium Banner */}
      <div className="bg-twitter-dark-2 border border-twitter-dark-4 rounded-2xl p-4 flex flex-col gap-2">
        <div className="flex items-center gap-1.5 text-twitter-blue">
          <Sparkles className="w-5 h-5" />
          <h3 className="font-extrabold text-white text-lg">Premium</h3>
        </div>
        <p className="text-twitter-gray-1 text-sm leading-tight">
          Subscribe to unlock new features and if eligible, receive a share of ads revenue.
        </p>
        <button className="bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-2 px-4 rounded-full text-sm transition-all duration-200 mt-1 self-start active:scale-95">
          Subscribe
        </button>
      </div>

      {/* Trends Section */}
      <div className="bg-twitter-dark-2 border border-twitter-dark-4 rounded-2xl p-4 flex flex-col gap-3">
        <div className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5 text-twitter-blue" />
          <h3 className="font-black text-white text-lg tracking-tight">What's happening</h3>
        </div>
        <div className="divide-y divide-twitter-dark-4">
          {trends.map((trend, i) => (
            <div
              key={i}
              className="py-3 hover:bg-white/5 transition-colors duration-150 cursor-pointer -mx-4 px-4 text-left first:pt-0 last:pb-0"
              onClick={() => {
                const cleanedTag = trend.hashtag.startsWith('#') ? trend.hashtag.slice(1) : trend.hashtag;
                navigate(`/search?q=${encodeURIComponent(cleanedTag)}`);
              }}
            >
              <span className="text-twitter-gray-1 text-xs">Trending in technology</span>
              <p className="font-bold text-white text-sm">{trend.hashtag}</p>
              <span className="text-twitter-gray-1 text-xs">{trend.posts} Posts</span>
            </div>
          ))}
        </div>
      </div>

      {/* Who to follow Section */}
      {user && (
        <div className="bg-twitter-dark-2 border border-twitter-dark-4 rounded-2xl p-4 flex flex-col gap-3">
          <h3 className="font-black text-white text-lg tracking-tight text-left">Who to follow</h3>
          <div className="flex flex-col gap-1">
            {suggestedUserIds.map((id) => (
              <FollowSuggestion key={id} targetUserId={id} currentUserId={user.userId} />
            ))}
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="px-4 text-left text-xs text-twitter-gray-1 space-y-1">
        <div className="flex flex-wrap gap-x-2 gap-y-1">
          <a href="#" className="hover:underline">Terms of Service</a>
          <a href="#" className="hover:underline">Privacy Policy</a>
          <a href="#" className="hover:underline">Cookie Policy</a>
          <a href="#" className="hover:underline">Accessibility</a>
        </div>
        <p>© 2026 X Corp. Built with Antigravity</p>
      </div>
    </div>
  );
};

export default RightSidebar;
