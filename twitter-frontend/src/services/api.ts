import axios, { AxiosRequestConfig } from 'axios';
import {
  User,
  LoginResponse,
  RegisterResponse,
  Tweet,
  TweetRequest,
  UpdateTweetRequest,
  Profile,
  UpdateProfileRequest,
  Reply,
  FeedTweet,
  Notification,
  ApiResponse,
  HashtagResponse
} from '../types';

// Spring Page interface helper
export interface SpringPage<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// Social service PagedResponse interface helper
export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

const api = axios.create({
  baseURL: '', // Empty base URL so it uses the Vite dev proxy (/api or /media)
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach access token to outgoing requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
      
      // Extract userId from token if possible and set X-User-Id header for endpoints 
      // that directly look for it (like TweetService & SocialService)
      const userId = localStorage.getItem('userId');
      if (userId) {
        config.headers['X-User-Id'] = userId;
      }
      const role = localStorage.getItem('userRole');
      if (role) {
        config.headers['X-Role'] = role;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle token refresh on 401 Unauthorized
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Avoid infinite loop on refresh endpoint
    if (originalRequest.url?.includes('/api/auth/refresh')) {
      localStorage.clear();
      window.dispatchEvent(new Event('auth-logout'));
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.clear();
        window.dispatchEvent(new Event('auth-logout'));
        isRefreshing = false;
        return Promise.reject(error);
      }

      try {
        const response = await axios.post<ApiResponse<LoginResponse>>('/api/auth/refresh', {
          refreshToken,
        });

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data.data;
        
        localStorage.setItem('accessToken', newAccessToken);
        if (newRefreshToken) {
          localStorage.setItem('refreshToken', newRefreshToken);
        }

        api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        
        processQueue(null, newAccessToken);
        isRefreshing = false;
        
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        isRefreshing = false;
        localStorage.clear();
        window.dispatchEvent(new Event('auth-logout'));
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

// --- AUTH SERVICE ---
export const authService = {
  register: async (data: any) => {
    const res = await api.post<ApiResponse<RegisterResponse>>('/api/auth/register', data);
    return res.data;
  },

  login: async (data: any) => {
    const res = await api.post<ApiResponse<LoginResponse>>('/api/auth/login', data);
    return res.data;
  },

  logout: async (refreshToken: string) => {
    const res = await api.post<ApiResponse<string>>('/api/auth/logout', { refreshToken });
    return res.data;
  },

  changePassword: async (data: any) => {
    const res = await api.post<ApiResponse<string>>('/api/auth/change-password', data);
    return res.data;
  },

  forgotPassword: async (email: string) => {
    const res = await api.post<ApiResponse<string>>('/api/auth/forgot-password', { email });
    return res.data;
  },

  resetPassword: async (data: any) => {
    const res = await api.post<ApiResponse<string>>('/api/auth/reset-password', data);
    return res.data;
  },

  getCurrentUser: async (userId: number) => {
    // We can call internal user fetch endpoint
    const res = await api.get<User>(`/api/internal/users/${userId}`);
    return res.data;
  },
};

// --- TWEET SERVICE ---
export const tweetService = {
  createTweet: async (data: TweetRequest) => {
    const res = await api.post<Tweet>('/api/tweets', data);
    return res.data;
  },

  getTweet: async (tweetId: number) => {
    const res = await api.get<Tweet>(`/api/tweets/${tweetId}`);
    return res.data;
  },

  updateTweet: async (tweetId: number, data: UpdateTweetRequest) => {
    const res = await api.put<Tweet>(`/api/tweets/${tweetId}`, data);
    return res.data;
  },

  deleteTweet: async (tweetId: number) => {
    const res = await api.delete<string>(`/api/tweets/${tweetId}`);
    return res.data;
  },

  getUserTweets: async (userId: number) => {
    const res = await api.get<Tweet[]>(`/api/tweets/user/${userId}`);
    return res.data;
  },

  getTweetsByHashtag: async (hashtag: string) => {
    const res = await api.get<Tweet[]>(`/api/tweets/hashtag/${hashtag}`);
    return res.data;
  },

  searchTweets: async (keyword: string) => {
    const res = await api.get<Tweet[]>(`/api/tweets/search?keyword=${keyword}`);
    return res.data;
  },

  getSuggestions: async (keyword: string) => {
    const res = await api.get<Tweet[]>(`/api/tweets/suggestions?keyword=${keyword}`);
    return res.data;
  },

  getAllTweets: async (page = 0, size = 10) => {
    const res = await api.get<SpringPage<Tweet>>(`/api/tweets?page=${page}&size=${size}`);
    return res.data;
  },

  getTrendingTweets: async (window = '24h') => {
    const res = await api.get<Tweet[]>(`/api/tweets/trending?window=${window}`);
    return res.data;
  },

  getTrendingHashtags: async () => {
    const res = await api.get<HashtagResponse[]>('/api/tweets/hashtags/trending');
    return res.data;
  },
};

// --- SOCIAL SERVICE ---
export const socialService = {
  getProfile: async (userId: number, currentUserId?: number) => {
    const headers: AxiosRequestConfig['headers'] = {};
    if (currentUserId) {
      headers['X-User-Id'] = String(currentUserId);
    }
    const res = await api.get<ApiResponse<Profile>>(`/api/profile/${userId}`, { headers });
    return res.data;
  },

  updateProfile: async (userId: number, data: UpdateProfileRequest) => {
    const res = await api.put<ApiResponse<Profile>>(`/api/profile/${userId}`, data);
    return res.data;
  },

  uploadAvatar: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post<ApiResponse<Profile>>(`/api/profile/${userId}/avatar`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  updateAvatar: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.put<ApiResponse<Profile>>(`/api/profile/${userId}/avatar`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  deleteAvatar: async (userId: number) => {
    const res = await api.delete<ApiResponse<void>>(`/api/profile/${userId}/avatar`);
    return res.data;
  },

  uploadBanner: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.post<ApiResponse<Profile>>(`/api/profile/${userId}/banner`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  updateBanner: async (userId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.put<ApiResponse<Profile>>(`/api/profile/${userId}/banner`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  deleteBanner: async (userId: number) => {
    const res = await api.delete<ApiResponse<void>>(`/api/profile/${userId}/banner`);
    return res.data;
  },

  getPosts: async (userId: number, page = 0, size = 20) => {
    const res = await api.get<ApiResponse<PagedResponse<Tweet>>>(`/api/profile/${userId}/posts?page=${page}&size=${size}`);
    return res.data;
  },

  searchProfiles: async (query: string, currentUserId?: number) => {
    const headers: AxiosRequestConfig['headers'] = {};
    if (currentUserId) {
      headers['X-User-Id'] = String(currentUserId);
    }
    const res = await api.get<ApiResponse<Profile[]>>(`/api/profile/search?query=${encodeURIComponent(query)}`, { headers });
    return res.data;
  },

  getReplies: async (userId: number, page = 0, size = 20) => {
    const res = await api.get<ApiResponse<PagedResponse<Reply>>>(`/api/profile/${userId}/replies?page=${page}&size=${size}`);
    return res.data;
  },

  getMedia: async (userId: number, page = 0, size = 20) => {
    const res = await api.get<ApiResponse<PagedResponse<Tweet>>>(`/api/profile/${userId}/media?page=${page}&size=${size}`);
    return res.data;
  },

  getLikedTweets: async (userId: number, page = 0, size = 20) => {
    const res = await api.get<ApiResponse<PagedResponse<Tweet>>>(`/api/profile/${userId}/likes?page=${page}&size=${size}`);
    return res.data;
  },

  bookmarkTweet: async (tweetId: number, userId: number) => {
    const res = await api.post<ApiResponse<string>>('/api/bookmarks', { tweetId, userId });
    return res.data;
  },

  removeBookmark: async (tweetId: number, userId: number) => {
    const res = await api.delete<string>('/api/bookmarks', { data: { tweetId, userId } });
    return res.data;
  },

  getBookmarkedTweets: async (userId: number) => {
    const res = await api.get<number[]>(`/api/bookmarks/user/${userId}`);
    return res.data;
  },

  isBookmarked: async (userId: number, tweetId: number) => {
    const res = await api.get<boolean>(`/api/bookmarks/status?userId=${userId}&tweetId=${tweetId}`);
    return res.data;
  },

  followUser: async (followerId: number, followingId: number) => {
    const res = await api.post<ApiResponse<string>>('/api/follows', { followerId, followingId });
    return res.data;
  },

  unfollowUser: async (followerId: number, followingId: number) => {
    const res = await api.delete<ApiResponse<string>>('/api/follows', { data: { followerId, followingId } });
    return res.data;
  },

  getFollowers: async (userId: number) => {
    const res = await api.get<ApiResponse<number[]>>(`/api/follows/followers/${userId}`);
    return res.data;
  },

  getFollowing: async (userId: number) => {
    const res = await api.get<ApiResponse<number[]>>(`/api/follows/following/${userId}`);
    return res.data;
  },

  isFollowing: async (followerId: number, followingId: number) => {
    const res = await api.get<ApiResponse<boolean>>(`/api/follows/status?followerId=${followerId}&followingId=${followingId}`);
    return res.data;
  },

  getFollowSuggestions: async () => {
    const res = await api.get<ApiResponse<number[]>>('/api/follows/suggestions');
    return res.data;
  },

  likeTweet: async (tweetId: number, userId: number) => {
    const res = await api.post<ApiResponse<string>>('/api/likes', { tweetId, userId });
    return res.data;
  },

  unlikeTweet: async (tweetId: number, userId: number) => {
    const res = await api.delete<string>('/api/likes', { data: { tweetId, userId } });
    return res.data;
  },

  getLikeCount: async (tweetId: number) => {
    const res = await api.get<number>(`/api/likes/count/${tweetId}`);
    return res.data;
  },

  isTweetLiked: async (userId: number, tweetId: number) => {
    const res = await api.get<boolean>(`/api/likes/status?userId=${userId}&tweetId=${tweetId}`);
    return res.data;
  },

  addReply: async (tweetId: number, userId: number, content: string) => {
    const res = await api.post<ApiResponse<string>>('/api/replies', { tweetId, userId, content });
    return res.data;
  },

  deleteReply: async (replyId: number) => {
    const res = await api.delete<string>(`/api/replies/${replyId}`);
    return res.data;
  },

  getRepliesByTweet: async (tweetId: number) => {
    const res = await api.get<Reply[]>(`/api/replies/tweet/${tweetId}`);
    return res.data;
  },

  getRepliesByUser: async (userId: number) => {
    const res = await api.get<Reply[]>(`/api/replies/user/${userId}`);
    return res.data;
  },

  retweet: async (tweetId: number, userId: number) => {
    const res = await api.post<ApiResponse<string>>('/api/retweets', { tweetId, userId });
    return res.data;
  },

  undoRetweet: async (tweetId: number, userId: number) => {
    const res = await api.delete<string>('/api/retweets', { data: { tweetId, userId } });
    return res.data;
  },

  isRetweeted: async (userId: number, tweetId: number) => {
    const res = await api.get<boolean>(`/api/retweets/status?userId=${userId}&tweetId=${tweetId}`);
    return res.data;
  },

  getRetweetCount: async (tweetId: number) => {
    const res = await api.get<number>(`/api/retweets/count/${tweetId}`);
    return res.data;
  },

  getFeed: async (userId: number, page = 0, size = 20) => {
    const res = await api.get<FeedTweet[]>(`/api/feed/${userId}?page=${page}&size=${size}`);
    return res.data;
  },
};

// --- NOTIFICATION SERVICE ---
export const notificationService = {
  getNotification: async (notificationId: number) => {
    const res = await api.get<Notification>(`/api/notification/${notificationId}`);
    return res.data;
  },

  markAsRead: async (notificationId: number) => {
    const res = await api.put<Notification>(`/api/notification/${notificationId}/read`);
    return res.data;
  },

  getUserNotifications: async (userId: number) => {
    const res = await api.get<Notification[]>(`/api/notification/user/${userId}`);
    return res.data;
  },
};

// --- MEDIA SERVICE ---
export const mediaService = {
  uploadMedia: async (file: File, userId: number) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('userId', String(userId));
    const res = await api.post<{ mediaId: number; url: string }>('/media/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  updateMedia: async (mediaId: number, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    const res = await api.put<{ mediaId: number; url: string }>(`/media/update/${mediaId}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data;
  },

  deleteMedia: async (mediaId: number) => {
    const res = await api.delete<void>(`/media/delete/${mediaId}`);
    return res.data;
  },
};

export default api;
