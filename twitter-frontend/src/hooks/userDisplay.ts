import { useEffect } from 'react';
import { useUser } from './useUser';

export const userCache = new Map<number, any>();

const PALETTE = ['#1d9bf0', '#00ba7c', '#f4212e', '#ffd400', '#7856ff', '#f91880', '#ff7a00'];

function colorForId(id: number) {
  const n = Number(id) || 0;
  return PALETTE[n % PALETTE.length];
}

export function primeUserCache(users: any[]) {
  users.forEach((u) => {
    if (u && (u.id != null || u.userId != null)) {
      const id = u.userId ?? u.id;
      userCache.set(id, u);
    }
  });
}

export function getUserDisplay(id: number) {
  const cached = userCache.get(id);
  if (cached) {
    return {
      id,
      name: cached.displayName || cached.name || cached.username || `User ${id}`,
      username: cached.username || `user${id}`,
      avatarUrl: cached.avatarUrl || null,
      color: colorForId(id),
      initial: (cached.displayName || cached.name || cached.username || `${id}`).charAt(0).toUpperCase(),
    };
  }

  return {
    id,
    name: `User ${id}`,
    username: `user${id}`,
    avatarUrl: null,
    color: colorForId(id),
    initial: String(id).charAt(0).toUpperCase(),
  };
}

export function useUserDisplay(id: number | undefined) {
  const { data: profile } = useUser(id || undefined);

  useEffect(() => {
    if (profile && id) {
      userCache.set(id, profile);
    }
  }, [id, profile]);

  if (!id) {
    return {
      id: 0,
      name: 'System',
      username: 'system',
      avatarUrl: null,
      color: '#71767b',
      initial: 'S',
    };
  }

  if (profile) {
    return {
      id,
      name: profile.displayName || profile.username || `User ${id}`,
      username: profile.username || `user${id}`,
      avatarUrl: profile.avatarUrl || null,
      color: colorForId(id),
      initial: (profile.displayName || profile.username || `${id}`).charAt(0).toUpperCase(),
    };
  }


  // Fallback to cache or default display
  return getUserDisplay(id);
}

