import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { tweetService, socialService } from '../services/api';
import TweetCard from '../components/TweetCard';
import { Search as SearchIcon, Loader2, Sparkles, Sliders, TrendingUp } from 'lucide-react';

const Search: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryParam = searchParams.get('q') || '';

  // Query: User search results
  const { data: userResults } = useQuery({
    queryKey: ['search-users', queryParam],
    queryFn: async () => {
      if (!queryParam.trim()) return [];
      try {
        const res = await socialService.searchProfiles(queryParam.trim());
        return res.data || [];
      } catch (err) {
        console.error('Failed to search user profiles:', err);
        return [];
      }
    },
    enabled: !!queryParam.trim(),
  });
  
  const [searchInput, setSearchInput] = useState(queryParam);
  const [suggestions, setSuggestions] = useState<any[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setSearchInput(queryParam);
  }, [queryParam]);

  // Debounce logic for suggestions
  useEffect(() => {
    if (!searchInput.trim()) {
      setSuggestions([]);
      setIsLoadingSuggestions(false);
      return;
    }

    setIsLoadingSuggestions(true);
    const delayDebounceFn = setTimeout(async () => {
      try {
        const response = await tweetService.getSuggestions(searchInput.trim());
        setSuggestions(response || []);
      } catch (error) {
        console.error('Error fetching suggestions:', error);
      } finally {
        setIsLoadingSuggestions(false);
      }
    }, 300); // 300ms debounce

    return () => clearTimeout(delayDebounceFn);
  }, [searchInput]);

  // Click outside detection to close suggestions dropdown
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(event.target as Node)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Query: Search results
  const { data: results, isLoading, isError, refetch } = useQuery({
    queryKey: ['search-results', queryParam],
    queryFn: async () => {
      if (!queryParam.trim()) return [];
      
      const cleanQuery = queryParam.trim();
      // If search query is a hashtag, call hashtag endpoint
      if (cleanQuery.startsWith('#')) {
        return tweetService.getTweetsByHashtag(cleanQuery.slice(1));
      } else {
        return tweetService.searchTweets(cleanQuery);
      }
    },
    enabled: !!queryParam.trim(),
  });

  // Query: Trending hashtags (for initial explore view)
  const { data: trendingHashtags } = useQuery({
    queryKey: ['trending-hashtags'],
    queryFn: () => tweetService.getTrendingHashtags(),
  });

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setSearchParams({ q: searchInput.trim() });
      setShowSuggestions(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-black">
      
      {/* Sticky Top Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 p-3 text-left">
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div ref={searchRef} className="relative flex-grow group">
            <input
              type="text"
              placeholder="Search posts or hashtags..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onFocus={() => setShowSuggestions(true)}
              className="w-full bg-twitter-dark-3 border border-transparent rounded-full py-2.5 pl-12 pr-4 text-white text-sm placeholder-twitter-gray-1 focus:outline-none focus:bg-black focus:border-twitter-blue focus:ring-1 focus:ring-twitter-blue transition-all duration-200"
            />
            <SearchIcon className="w-5 h-5 absolute left-4 top-3 text-twitter-gray-1 group-focus-within:text-twitter-blue transition-colors duration-200" />

            {/* Suggestions Dropdown */}
            {showSuggestions && searchInput.trim() && (
              <div className="absolute left-0 right-0 mt-2 bg-black border border-twitter-dark-4 rounded-2xl shadow-xl z-20 max-h-80 overflow-y-auto divide-y divide-twitter-dark-4">
                {isLoadingSuggestions ? (
                  <div className="flex items-center justify-center py-4 text-twitter-gray-1 text-sm gap-2">
                    <span className="animate-spin text-twitter-blue font-bold">◌</span>
                    Loading...
                  </div>
                ) : (
                  <>
                    <div 
                      onClick={() => {
                        setSearchParams({ q: searchInput.trim() });
                        setShowSuggestions(false);
                      }}
                      className="p-3.5 hover:bg-neutral-900 transition-colors duration-150 cursor-pointer text-left"
                    >
                      <span className="text-twitter-gray-1 text-xs">Search for</span>
                      <p className="font-bold text-white text-sm truncate">"{searchInput}"</p>
                    </div>
                    {suggestions.length > 0 ? (
                      suggestions.map((tweet) => (
                        <div 
                          key={tweet.tweetId}
                          onClick={() => {
                            setSearchParams({ q: tweet.content });
                            setShowSuggestions(false);
                          }}
                          className="p-3.5 hover:bg-neutral-900 transition-colors duration-150 cursor-pointer text-left"
                        >
                          <span className="text-twitter-gray-1 text-xs">Tweet Suggestion</span>
                          <p className="text-white text-sm line-clamp-2 mt-0.5">{tweet.content}</p>
                          {tweet.hashtags && tweet.hashtags.length > 0 && (
                            <div className="flex gap-1.5 flex-wrap mt-1">
                              {tweet.hashtags.map((tag: string, i: number) => (
                                <span key={i} className="text-twitter-blue text-xs font-semibold">
                                  #{tag.startsWith('#') ? tag.slice(1) : tag}
                                </span>
                              ))}
                            </div>
                          )}
                        </div>
                      ))
                    ) : (
                      <div className="p-3.5 text-twitter-gray-1 text-xs text-left">
                        No matching tweets found.
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
          </div>
          <button 
            type="submit"
            className="px-5 py-2.5 bg-twitter-blue hover:bg-twitter-blue-hover text-white rounded-full text-sm font-bold transition-all duration-200 active:scale-95 flex-shrink-0"
          >
            Search
          </button>
        </form>
      </div>

      {/* Content */}
      <div className="flex-grow">
        {isLoading ? (
          <div className="flex justify-center items-center h-48 text-twitter-blue">
            <Loader2 className="w-8 h-8 animate-spin" />
          </div>
        ) : isError ? (
          <div className="p-8 text-center text-twitter-gray-1">
            <p className="font-semibold text-white">Search failed</p>
            <button onClick={() => refetch()} className="mt-2 bg-twitter-blue text-white py-1 px-4 rounded-full text-xs">
              Retry
            </button>
          </div>
        ) : !queryParam.trim() ? (
          /* Dedicated Trending Page (When no search query is active) */
          <div className="flex flex-col text-left">
            <div className="px-4 py-3 border-b border-twitter-dark-4 bg-twitter-dark-2/10 flex items-center gap-2">
              <TrendingUp className="w-5 h-5 text-twitter-blue" />
              <h3 className="font-black text-white text-lg tracking-tight">Trends for you</h3>
            </div>
            <div className="divide-y divide-twitter-dark-4">
              {trendingHashtags && trendingHashtags.map((h, i) => {
                const hashtagName = h.hashtag.startsWith('#') ? h.hashtag : `#${h.hashtag}`;
                const cleanedTag = h.hashtag.startsWith('#') ? h.hashtag.slice(1) : h.hashtag;
                return (
                  <div
                    key={i}
                    className="px-4 py-4 hover:bg-white/5 transition-colors duration-150 cursor-pointer text-left"
                    onClick={() => {
                      setSearchParams({ q: hashtagName });
                    }}
                  >
                    <span className="text-twitter-gray-1 text-xs">Trending in Technology</span>
                    <p className="font-extrabold text-white text-base mt-0.5">{hashtagName}</p>
                    <span className="text-twitter-gray-1 text-xs block mt-0.5">{h.posts} Posts</span>
                  </div>
                );
              })}
              {(!trendingHashtags || trendingHashtags.length === 0) && (
                <div className="p-8 text-center text-twitter-gray-1">
                  No trends available.
                </div>
              )}
            </div>
          </div>
        ) : (results && results.length === 0) && (userResults && userResults.length === 0) ? (
          /* No Results Found */
          <div className="p-12 text-center text-twitter-gray-1">
            <p className="font-bold text-lg text-white">No results for "{queryParam}"</p>
            <p className="text-sm mt-1">Try checking your spelling or searching for another keyword.</p>
          </div>
        ) : (
          /* Search Results */
          <div className="divide-y divide-twitter-dark-4">
            {/* Matching Users/People Section */}
            {userResults && userResults.length > 0 && (
              <div className="border-b border-twitter-dark-4 pb-2">
                <div className="px-4 py-2 border-b border-twitter-dark-4 bg-twitter-dark-2/10 flex items-center justify-between">
                  <span className="text-xs text-twitter-gray-1 font-bold">People matching "{queryParam}"</span>
                </div>
                <div className="flex flex-col">
                  {userResults.map((profile) => (
                    <div 
                      key={profile.userId}
                      onClick={() => navigate(`/profile/${profile.userId}`)}
                      className="flex items-center justify-between px-4 py-3 hover:bg-neutral-900/30 cursor-pointer transition-colors duration-150 border-b border-twitter-dark-4/50 last:border-b-0"
                    >
                      <div className="flex gap-3">
                        <img
                          src={profile.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${profile.username}`}
                          alt={profile.username}
                          className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
                        />
                        <div className="text-left">
                          <div className="flex items-center gap-1.5">
                            <span className="font-bold text-white text-sm hover:underline">
                              {profile.displayName || profile.username}
                            </span>
                            {profile.isVerified && (
                              <span className="text-twitter-blue text-xs">✓</span>
                            )}
                          </div>
                          <span className="text-twitter-gray-1 text-xs">@{profile.username}</span>
                          {profile.bio && (
                            <p className="text-twitter-gray-1 text-xs mt-1 line-clamp-1">{profile.bio}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Tweets Results Section */}
            {results && results.length > 0 && (
              <div>
                <div className="px-4 py-2 border-b border-twitter-dark-4 bg-twitter-dark-2/10 flex items-center justify-between">
                  <span className="text-xs text-twitter-gray-1 font-bold">Posts matching "{queryParam}"</span>
                  <Sliders className="w-3.5 h-3.5 text-twitter-gray-1" />
                </div>
                {results.map((tweet) => (
                  <TweetCard key={tweet.tweetId} tweet={tweet} />
                ))}
              </div>
            )}
          </div>
        )}
      </div>

    </div>
  );
};

export default Search;
