// --- AUTH SERVICE TYPES ---
export type UserRole = 'USER' | 'ADMIN';

export interface User {
  userId: number;
  username: string;
  email: string;
  role?: UserRole;
  enabled?: boolean;
  accountLocked?: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
}

export interface RegisterResponse {
  userId: number;
  username: string;
  email: string;
}

// --- TWEET SERVICE TYPES ---
export type MediaType = 'IMAGE' | 'VIDEO' | 'GIF';

export interface MediaRequest {
  mediaUrl: string;
  mediaType: MediaType;
}

export interface TweetRequest {
  content: string;
  mediaUrls: MediaRequest[];
  hashtags: string[];
}

export interface UpdateTweetRequest {
  content: string;
}

export interface Tweet {
  tweetId: number;
  userId: number;
  content: string;
  mediaUrls: string[]; // TweetResponse has private List<String> mediaUrls
  hashtags: string[];
  likeCount: number;
  replyCount: number;
  retweetCount: number;
  viewCount: number;
  createdAt: string; // ISO string
}

// --- SOCIAL SERVICE TYPES ---
export interface Profile {
  userId: number;
  username: string;
  displayName: string;
  bio?: string;
  location?: string;
  website?: string;
  avatarUrl?: string;
  bannerUrl?: string;
  dateOfBirth?: string; // LocalDate (YYYY-MM-DD)
  isVerified?: boolean;
  isPrivate?: boolean;
  joinedAt?: string; // LocalDateTime
  followersCount: number;
  followingCount: number;
  postsCount: number;
  isFollowedByCurrentUser?: boolean;
  isOwnProfile?: boolean;
}

export interface UpdateProfileRequest {
  bio?: string;
  location?: string;
  website?: string;
  dateOfBirth?: string; // YYYY-MM-DD
  isPrivate?: boolean;
}

export interface Reply {
  replyId: number;
  userId: number;
  tweetId: number;
  content: string;
  repliedAt: string;
}

export interface ReplyDto {
  replyId: number;
  userId: number;
  tweetId: number;
  content: string;
  repliedAt: string;
}

export interface FeedTweet {
  tweetId: number;
  userId: number;
  content: string;
  createdAt: string;
  likeCount: number;
  retweetCount: number;
  replyCount: number;
  score: number;
}

// --- NOTIFICATION SERVICE TYPES ---
export type NotificationType = 'LIKE' | 'FOLLOW' | 'REPLY' | 'RETWEET';

export interface Notification {
  notificationId: number;
  senderUserId: number;
  receiverUserId: number;
  tweetId?: number;
  message: string;
  isRead: boolean;
  type: NotificationType;
  createdAt: string;
}

// --- COMMON EVENT OR GENERAL RESPONSES ---
export interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  status?: string; // social-service uses ApiResponse with status instead of success
  data: T;
  timestamp?: number;
}
