import { QueryClient } from '@tanstack/react-query';
import { Tweet } from '../types';

/**
 * Helper to recursively traverse and immutably update a tweet within query data
 */
const updateTweetDeep = (data: any, tweetId: number, updateFn: (tweet: Tweet) => Tweet): any => {
  if (!data) return data;

  if (typeof data === 'object') {
    // If this object is the tweet itself
    if ('tweetId' in data && data.tweetId === tweetId) {
      return updateFn(data);
    }

    // If it's an array, map over elements recursively
    if (Array.isArray(data)) {
      let changed = false;
      const mapped = data.map((item) => {
        const updatedItem = updateTweetDeep(item, tweetId, updateFn);
        if (updatedItem !== item) {
          changed = true;
        }
        return updatedItem;
      });
      return changed ? mapped : data;
    }

    // If it's a plain object, recursively update its keys
    const updated: any = {};
    let changed = false;
    for (const key of Object.keys(data)) {
      const val = data[key];
      const updatedVal = updateTweetDeep(val, tweetId, updateFn);
      if (updatedVal !== val) {
        changed = true;
      }
      updated[key] = updatedVal;
    }
    return changed ? updated : data;
  }

  return data;
};

/**
 * Updates a tweet across all query caches (single tweet, array of tweets, or paginated responses)
 */
export const updateTweetInCache = (
  queryClient: QueryClient,
  tweetId: number,
  updateFn: (tweet: Tweet) => Tweet
) => {
  const queries = queryClient.getQueryCache().findAll();
  
  queries.forEach((query) => {
    const queryKey = query.queryKey;
    const data = query.state.data;
    
    if (!data) return;

    let hasTweet = false;
    const checkDeep = (obj: any): void => {
      if (!obj || hasTweet) return;
      if (typeof obj === 'object') {
        if ('tweetId' in obj && obj.tweetId === tweetId) {
          hasTweet = true;
          return;
        }
        if (Array.isArray(obj)) {
          obj.forEach(checkDeep);
        } else {
          Object.values(obj).forEach(checkDeep);
        }
      }
    };
    checkDeep(data);

    if (hasTweet) {
      queryClient.setQueryData(queryKey, (old: any) => {
        return updateTweetDeep(old, tweetId, updateFn);
      });
    }
  });
};

/**
 * Snapshots all queries containing a specific tweet, returning their query keys and data
 */
export const snapshotTweetsCache = (queryClient: QueryClient, tweetId: number) => {
  const snapshots: Array<{ queryKey: any; data: any }> = [];
  
  queryClient.getQueryCache().findAll().forEach((query) => {
    const data = query.state.data;
    if (!data) return;

    let hasTweet = false;
    const checkDeep = (obj: any): void => {
      if (!obj || hasTweet) return;
      if (typeof obj === 'object') {
        if ('tweetId' in obj && obj.tweetId === tweetId) {
          hasTweet = true;
          return;
        }
        if (Array.isArray(obj)) {
          obj.forEach(checkDeep);
        } else {
          Object.values(obj).forEach(checkDeep);
        }
      }
    };
    checkDeep(data);

    if (hasTweet) {
      snapshots.push({
        queryKey: query.queryKey,
        data: data,
      });
    }
  });

  return snapshots;
};
