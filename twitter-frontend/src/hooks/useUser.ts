import { useQuery } from '@tanstack/react-query';
import { socialService } from '../services/api';

export const useUser = (userId: number | undefined) => {
  return useQuery({
    queryKey: ['user-profile', userId],
    queryFn: async () => {
      if (!userId) return null;
      try {
        const response = await socialService.getProfile(userId);
        return response.data;
      } catch (err) {
        console.error(`Failed to fetch profile for user ${userId}:`, err);
        return null;
      }
    },
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // Cache profiles for 5 minutes
    gcTime: 30 * 60 * 1000,   // Keep cache in memory for 30 minutes
  });
};
