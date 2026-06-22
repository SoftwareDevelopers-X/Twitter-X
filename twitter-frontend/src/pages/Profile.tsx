import React, { useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { socialService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useUser } from '../hooks/useUser';
import TweetCard from '../components/TweetCard';
import { 
  ArrowLeft, 
  MapPin, 
  Link2, 
  Calendar, 
  Camera, 
  CheckCircle, 
  Loader2, 
  X,
  Lock,
  MessageSquare
} from 'lucide-react';
import { formatJoinedDate, formatDateOfBirth } from '../utils/date';
import toast from 'react-hot-toast';

const Profile: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();

  const userId = Number(id);
  const isOwnProfile = user?.userId === userId;

  // Tabs: 'posts' | 'replies' | 'media' | 'likes'
  const [activeTab, setActiveTab] = useState<'posts' | 'replies' | 'media' | 'likes'>('posts');
  
  // Edit Profile modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editBio, setEditBio] = useState('');
  const [editLocation, setEditLocation] = useState('');
  const [editWebsite, setEditWebsite] = useState('');
  const [editDob, setEditDob] = useState('');
  
  const avatarInputRef = useRef<HTMLInputElement>(null);
  const bannerInputRef = useRef<HTMLInputElement>(null);

  // Fetch full enriched profile
  const { data: profile, isLoading, isError, refetch } = useQuery({
    queryKey: ['user-profile', userId],
    queryFn: async () => {
      const res = await socialService.getProfile(userId, user?.userId);
      return res.data;
    },
    enabled: !!userId,
  });

  // Query: Tab Content
  const { data: postsData, isLoading: isPostsLoading } = useQuery({
    queryKey: ['user-posts', userId, activeTab],
    queryFn: async () => {
      if (activeTab === 'posts') {
        const res = await socialService.getPosts(userId, 0, 50);
        return res.data.content;
      } else if (activeTab === 'media') {
        const res = await socialService.getMedia(userId, 0, 50);
        return res.data.content;
      } else if (activeTab === 'likes') {
        const res = await socialService.getLikedTweets(userId, 0, 50);
        return res.data.content;
      }
      return [];
    },
    enabled: !!userId && activeTab !== 'replies',
  });

  // Query: User Replies (Reply list is slightly different, returns ReplyDto)
  const { data: repliesData, isLoading: isRepliesLoading } = useQuery({
    queryKey: ['user-replies', userId],
    queryFn: async () => {
      const res = await socialService.getReplies(userId, 0, 50);
      return res.data.content;
    },
    enabled: !!userId && activeTab === 'replies',
  });

  // Mutation: Follow/Unfollow user
  const followMutation = useMutation({
    mutationFn: () => {
      if (profile?.isFollowedByCurrentUser) {
        return socialService.unfollowUser(user?.userId || 0, userId);
      } else {
        return socialService.followUser(user?.userId || 0, userId);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-profile', userId] });
      queryClient.invalidateQueries({ queryKey: ['user-profile', user?.userId] });
      toast.success(profile?.isFollowedByCurrentUser ? `Unfollowed @${profile.username}` : `Followed @${profile?.username}`);
    },
    onError: () => {
      toast.error('Failed to change follow status');
    }
  });

  // Mutation: Update Profile Details
  const updateProfileMutation = useMutation({
    mutationFn: (data: any) => socialService.updateProfile(userId, data),
    onSuccess: () => {
      setIsEditModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['user-profile', userId] });
      toast.success('Profile updated successfully');
    },
    onError: () => {
      toast.error('Failed to update profile info');
    }
  });

  // Upload Avatar
  const uploadAvatarMutation = useMutation({
    mutationFn: (file: File) => socialService.uploadAvatar(userId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-profile', userId] });
      toast.success('Avatar updated successfully');
    },
    onError: () => {
      toast.error('Failed to upload avatar');
    }
  });

  // Upload Banner
  const uploadBannerMutation = useMutation({
    mutationFn: (file: File) => socialService.uploadBanner(userId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-profile', userId] });
      toast.success('Banner updated successfully');
    },
    onError: () => {
      toast.error('Failed to upload banner');
    }
  });

  const handleEditClick = () => {
    if (profile) {
      setEditBio(profile.bio || '');
      setEditLocation(profile.location || '');
      setEditWebsite(profile.website || '');
      setEditDob(profile.dateOfBirth || '');
      setIsEditModalOpen(true);
    }
  };

  const handleProfileSave = () => {
    updateProfileMutation.mutate({
      bio: editBio,
      location: editLocation,
      website: editWebsite,
      dateOfBirth: editDob || null,
      isPrivate: profile?.isPrivate || false
    });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>, type: 'avatar' | 'banner') => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      if (type === 'avatar') {
        uploadAvatarMutation.mutate(file);
      } else {
        uploadBannerMutation.mutate(file);
      }
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-96 text-twitter-blue">
        <Loader2 className="w-10 h-10 animate-spin" />
      </div>
    );
  }

  if (isError || !profile) {
    return (
      <div className="p-8 text-center text-twitter-gray-1">
        <p className="font-bold text-lg text-white">Profile not found</p>
        <button onClick={() => refetch()} className="mt-4 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-2 px-6 rounded-full">
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-black text-left">
      
      {/* Sticky Top Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 flex items-center gap-6 px-4 py-2">
        <button
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-white/10 rounded-full text-white transition-colors duration-200"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <div className="flex items-center gap-1.5">
            <h2 className="font-black text-lg text-white leading-tight">{profile.displayName || profile.username}</h2>
            {profile.isVerified && <CheckCircle className="w-4 h-4 text-twitter-blue fill-current" />}
          </div>
          <span className="text-twitter-gray-1 text-xs">{profile.postsCount} Posts</span>
        </div>
      </div>

      {/* Banner */}
      <div className="h-[200px] w-full bg-twitter-dark-3 relative group overflow-hidden border-b border-twitter-dark-4">
        {profile.bannerUrl && (
          <img src={profile.bannerUrl} alt="Banner" className="w-full h-full object-cover" />
        )}
        {isOwnProfile && (
          <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-200">
            <button 
              onClick={() => bannerInputRef.current?.click()}
              className="p-3 bg-black/60 rounded-full hover:bg-neutral-800 transition-colors duration-200 text-white"
              title="Upload Banner"
            >
              <Camera className="w-6 h-6" />
            </button>
            <input 
              type="file" 
              ref={bannerInputRef} 
              className="hidden" 
              accept="image/*"
              onChange={(e) => handleFileChange(e, 'banner')}
            />
          </div>
        )}
      </div>

      {/* Profile Details section */}
      <div className="px-4 pb-4 relative flex flex-col">
        
        {/* Avatar Container */}
        <div className="relative -mt-[70px] mb-3 w-[130px] h-[130px] rounded-full overflow-hidden border-4 border-black bg-twitter-dark-3 group flex-shrink-0">
          <img
            src={profile.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${profile.username}`}
            alt="Avatar"
            className="w-full h-full object-cover"
          />
          {isOwnProfile && (
            <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity duration-200">
              <button 
                onClick={() => avatarInputRef.current?.click()}
                className="p-2.5 bg-black/60 rounded-full hover:bg-neutral-800 transition-colors duration-200 text-white"
                title="Upload Avatar"
              >
                <Camera className="w-5 h-5" />
              </button>
              <input 
                type="file" 
                ref={avatarInputRef} 
                className="hidden" 
                accept="image/*"
                onChange={(e) => handleFileChange(e, 'avatar')}
              />
            </div>
          )}
        </div>

        {/* Action Button (Edit Profile or Follow/Unfollow) */}
        <div className="absolute top-3 right-4">
          {isOwnProfile ? (
            <button
              onClick={handleEditClick}
              className="px-4 py-1.5 border border-twitter-dark-4 hover:bg-white/10 text-white font-bold rounded-full text-sm transition-all duration-200"
            >
              Edit profile
            </button>
          ) : (
            user && (
              <button
                onClick={() => followMutation.mutate()}
                className={`px-5 py-2 font-bold rounded-full text-sm transition-all duration-200 ${
                  profile.isFollowedByCurrentUser 
                    ? 'bg-transparent border border-twitter-dark-4 text-white hover:bg-red-500/10 hover:text-red-500 hover:border-red-500/40' 
                    : 'bg-white hover:bg-neutral-200 text-black'
                }`}
              >
                {profile.isFollowedByCurrentUser ? 'Following' : 'Follow'}
              </button>
            )
          )}
        </div>

        {/* Name & Handle */}
        <div className="mb-3">
          <div className="flex items-center gap-1.5">
            <h1 className="text-xl font-black text-white">{profile.displayName || profile.username}</h1>
            {profile.isVerified && <CheckCircle className="w-5 h-5 text-twitter-blue fill-current" />}
          </div>
          <span className="text-twitter-gray-1 text-sm">@{profile.username}</span>
        </div>

        {/* Bio */}
        {profile.bio && (
          <p className="text-white text-[15px] mb-3 leading-relaxed whitespace-pre-wrap">{profile.bio}</p>
        )}

        {/* User metadata tags */}
        <div className="flex flex-wrap gap-x-4 gap-y-1.5 text-twitter-gray-1 text-sm mb-3">
          {profile.location && (
            <div className="flex items-center gap-1">
              <MapPin className="w-4 h-4 flex-shrink-0" />
              <span>{profile.location}</span>
            </div>
          )}
          {profile.website && (
            <div className="flex items-center gap-1">
              <Link2 className="w-4 h-4 flex-shrink-0" />
              <a 
                href={profile.website.startsWith('http') ? profile.website : `https://${profile.website}`} 
                target="_blank" 
                rel="noopener noreferrer" 
                className="text-twitter-blue hover:underline"
              >
                {profile.website}
              </a>
            </div>
          )}
          {profile.dateOfBirth && (
            <div className="flex items-center gap-1">
              <Calendar className="w-4 h-4 flex-shrink-0" />
              <span>Born {formatDateOfBirth(profile.dateOfBirth)}</span>
            </div>
          )}
          <div className="flex items-center gap-1">
            <Calendar className="w-4 h-4 flex-shrink-0" />
            <span>Joined {formatJoinedDate(profile.joinedAt)}</span>
          </div>
        </div>

        {/* Followers / Following counts */}
        <div className="flex gap-4 text-sm">
          <div className="hover:underline cursor-pointer">
            <span className="font-bold text-white">{profile.followingCount}</span>
            <span className="text-twitter-gray-1 ml-1">Following</span>
          </div>
          <div className="hover:underline cursor-pointer">
            <span className="font-bold text-white">{profile.followersCount}</span>
            <span className="text-twitter-gray-1 ml-1">Followers</span>
          </div>
        </div>

      </div>

      {/* Profile Navigation Tabs */}
      <div className="flex border-b border-twitter-dark-4 w-full">
        {(['posts', 'replies', 'media', 'likes'] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className="flex-grow py-3.5 text-center hover:bg-white/5 transition-colors duration-200 relative font-bold text-[14px] capitalize"
          >
            <span className={activeTab === tab ? 'text-white' : 'text-twitter-gray-1'}>
              {tab}
            </span>
            {activeTab === tab && (
              <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-1 bg-twitter-blue rounded-full" />
            )}
          </button>
        ))}
      </div>

      {/* Tab Feed Content */}
      <div className="divide-y divide-twitter-dark-4 flex-grow">
        {activeTab === 'replies' ? (
          isRepliesLoading ? (
            <div className="flex justify-center py-8 text-twitter-blue">
              <Loader2 className="w-6 h-6 animate-spin" />
            </div>
          ) : repliesData?.length === 0 ? (
            <div className="p-8 text-center text-twitter-gray-1">No replies found.</div>
          ) : (
            repliesData?.map((reply) => (
              <div key={reply.replyId} className="p-4 hover:bg-neutral-900/10 border-b border-twitter-dark-4 flex gap-3 text-left">
                <img
                  src={profile.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${profile.username}`}
                  alt="Avatar"
                  className="w-10 h-10 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4 flex-shrink-0"
                />
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-bold text-white text-sm">{profile.displayName || profile.username}</span>
                    <span className="text-twitter-gray-1 text-xs">@{profile.username}</span>
                    <span className="text-twitter-gray-1 text-xs">·</span>
                    <span className="text-twitter-gray-1 text-xs">{formatJoinedDate(reply.repliedAt)}</span>
                  </div>
                  <div 
                    onClick={() => navigate(`/tweet/${reply.tweetId}`)}
                    className="text-twitter-blue text-xs flex items-center gap-1 cursor-pointer hover:underline mt-0.5"
                  >
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>Replied to Post #{reply.tweetId}</span>
                  </div>
                  <p className="text-white text-[15px] mt-2 whitespace-pre-wrap">{reply.content}</p>
                </div>
              </div>
            ))
          )
        ) : (
          isPostsLoading ? (
            <div className="flex justify-center py-8 text-twitter-blue">
              <Loader2 className="w-6 h-6 animate-spin" />
            </div>
          ) : postsData?.length === 0 ? (
            <div className="p-8 text-center text-twitter-gray-1">No posts found.</div>
          ) : (
            postsData?.map((tweet) => (
              <TweetCard key={tweet.tweetId} tweet={tweet} />
            ))
          )
        )}
      </div>

      {/* Edit Profile Modal */}
      {isEditModalOpen && (
        <div className="fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-50 flex items-start justify-center pt-[8%] px-4">
          <div className="bg-black border border-twitter-dark-4 rounded-2xl w-full max-w-[540px] overflow-hidden shadow-2xl animate-fade-in text-left">
            {/* Modal Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-twitter-dark-4">
              <div className="flex items-center gap-3">
                <button 
                  onClick={() => setIsEditModalOpen(false)}
                  className="p-1.5 hover:bg-white/10 rounded-full transition-colors duration-200 text-white"
                >
                  <X className="w-5 h-5" />
                </button>
                <span className="font-extrabold text-white text-lg">Edit profile</span>
              </div>
              <button
                onClick={handleProfileSave}
                disabled={updateProfileMutation.isPending}
                className="px-5 py-1.5 bg-white hover:bg-neutral-200 text-black font-bold rounded-full text-sm transition-all duration-200 flex items-center gap-1"
              >
                {updateProfileMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Save</span>
              </button>
            </div>

            {/* Modal fields */}
            <div className="p-4 space-y-4 max-h-[460px] overflow-y-auto">
              
              {/* Display Name / Info message */}
              <div className="bg-twitter-dark-2 rounded-xl p-3 border border-twitter-dark-4 text-xs text-twitter-gray-1">
                Username and Display Name are synced from authentication and can be enriched automatically.
              </div>

              {/* Bio Field */}
              <div>
                <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Bio</label>
                <textarea
                  value={editBio}
                  onChange={(e) => setEditBio(e.target.value)}
                  maxLength={160}
                  rows={3}
                  className="w-full bg-transparent border border-twitter-dark-4 focus:border-twitter-blue rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none transition-all duration-200"
                  placeholder="Tell us about yourself"
                />
                <span className="text-[11px] text-twitter-gray-1 float-right mt-1">{160 - editBio.length} characters left</span>
              </div>

              {/* Location Field */}
              <div>
                <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Location</label>
                <input
                  type="text"
                  value={editLocation}
                  onChange={(e) => setEditLocation(e.target.value)}
                  maxLength={100}
                  className="w-full bg-transparent border border-twitter-dark-4 focus:border-twitter-blue rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none transition-all duration-200"
                  placeholder="Where are you located?"
                />
              </div>

              {/* Website Field */}
              <div>
                <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Website</label>
                <input
                  type="text"
                  value={editWebsite}
                  onChange={(e) => setEditWebsite(e.target.value)}
                  maxLength={100}
                  className="w-full bg-transparent border border-twitter-dark-4 focus:border-twitter-blue rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none transition-all duration-200"
                  placeholder="Your personal or professional link"
                />
              </div>

              {/* Birth Date Field */}
              <div>
                <label className="block text-twitter-gray-1 text-xs font-bold uppercase mb-1.5 pl-1">Date of Birth</label>
                <input
                  type="date"
                  value={editDob}
                  onChange={(e) => setEditDob(e.target.value)}
                  className="w-full bg-transparent border border-twitter-dark-4 focus:border-twitter-blue rounded-xl px-4 py-3 text-white placeholder-twitter-gray-1 focus:outline-none transition-all duration-200"
                />
              </div>

            </div>
          </div>
        </div>
      )}

    </div>
  );
};

export default Profile;
