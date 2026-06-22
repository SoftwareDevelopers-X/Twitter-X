import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { tweetService } from '../services/api';
import TweetCard from '../components/TweetCard';
import { Search as SearchIcon, Loader2, Sparkles, Sliders } from 'lucide-react';

const Search: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryParam = searchParams.get('q') || '';
  
  const [searchInput, setSearchInput] = useState(queryParam);

  useEffect(() => {
    setSearchInput(queryParam);
  }, [queryParam]);

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

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      setSearchParams({ q: searchInput.trim() });
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-black">
      
      {/* Sticky Top Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 p-3 text-left">
        <form onSubmit={handleSearchSubmit} className="flex gap-2">
          <div className="relative flex-grow group">
            <input
              type="text"
              placeholder="Search posts or hashtags..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              className="w-full bg-twitter-dark-3 border border-transparent rounded-full py-2.5 pl-12 pr-4 text-white text-sm placeholder-twitter-gray-1 focus:outline-none focus:bg-black focus:border-twitter-blue focus:ring-1 focus:ring-twitter-blue transition-all duration-200"
            />
            <SearchIcon className="w-5 h-5 absolute left-4 top-3 text-twitter-gray-1 group-focus-within:text-twitter-blue transition-colors duration-200" />
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
          /* Initial Search View */
          <div className="p-12 text-center text-twitter-gray-1 flex flex-col items-center gap-4">
            <div className="w-16 h-16 bg-twitter-blue/10 text-twitter-blue rounded-full flex items-center justify-center border border-twitter-blue/20">
              <SearchIcon className="w-7 h-7" />
            </div>
            <h3 className="font-black text-xl text-white tracking-tight">Explore X-Clone</h3>
            <p className="text-sm max-w-[340px]">
              Search for posts, topics, or hashtags to find what you are looking for. Try searching for <span className="text-twitter-blue hover:underline cursor-pointer" onClick={() => setSearchParams({ q: '#SpringBoot' })}>#SpringBoot</span>.
            </p>
          </div>
        ) : results && results.length === 0 ? (
          /* No Results Found */
          <div className="p-12 text-center text-twitter-gray-1">
            <p className="font-bold text-lg text-white">No results for "{queryParam}"</p>
            <p className="text-sm mt-1">Try checking your spelling or searching for another keyword.</p>
          </div>
        ) : (
          /* Search Results */
          <div className="divide-y divide-twitter-dark-4">
            <div className="px-4 py-2 border-b border-twitter-dark-4 bg-twitter-dark-2/10 flex items-center justify-between">
              <span className="text-xs text-twitter-gray-1 font-bold">Search results for "{queryParam}"</span>
              <Sliders className="w-3.5 h-3.5 text-twitter-gray-1" />
            </div>
            {results?.map((tweet) => (
              <TweetCard key={tweet.tweetId} tweet={tweet} />
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default Search;
