import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationService } from '../services/api';
import { useAuthStore } from '../store/authStore';
import { useUser } from '../hooks/useUser';
import { 
  Bell, 
  Heart, 
  UserPlus, 
  MessageCircle, 
  Repeat2, 
  CheckCheck, 
  Loader2, 
  AlertCircle 
} from 'lucide-react';
import toast from 'react-hot-toast';
import { Notification } from '../types';

const NotificationItem: React.FC<{ notification: Notification }> = ({ notification }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // Fetch sender profile details
  const { data: sender } = useUser(notification.senderUserId);

  const markReadMutation = useMutation({
    mutationFn: () => notificationService.markAsRead(notification.notificationId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
    }
  });

  const handleClick = () => {
    if (!notification.isRead) {
      markReadMutation.mutate();
    }

    if (notification.type === 'FOLLOW') {
      navigate(`/profile/${notification.senderUserId}`);
    } else if (notification.tweetId) {
      navigate(`/tweet/${notification.tweetId}`);
    }
  };

  const getIcon = () => {
    switch (notification.type) {
      case 'LIKE':
        return <Heart className="w-6 h-6 text-pink-600 fill-current" />;
      case 'FOLLOW':
        return <UserPlus className="w-6 h-6 text-twitter-blue" />;
      case 'REPLY':
        return <MessageCircle className="w-6 h-6 text-twitter-blue" />;
      case 'RETWEET':
        return <Repeat2 className="w-6 h-6 text-green-500" />;
      default:
        return <Bell className="w-6 h-6 text-twitter-gray-1" />;
    }
  };

  return (
    <div 
      onClick={handleClick}
      className={`p-4 hover:bg-neutral-900/10 border-b border-twitter-dark-4 flex gap-4 text-left transition-colors duration-150 cursor-pointer ${
        !notification.isRead ? 'bg-twitter-blue/5 border-l-2 border-l-twitter-blue' : ''
      }`}
    >
      <div className="flex-shrink-0 mt-0.5">
        {getIcon()}
      </div>

      <div className="flex-grow min-w-0">
        <div className="flex items-center gap-3">
          <img
            src={sender?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${sender?.username || 'user'}`}
            alt="Sender Avatar"
            className="w-8 h-8 rounded-full object-cover bg-twitter-dark-3 border border-twitter-dark-4"
          />
          <div className="text-sm">
            <span className="font-bold text-white hover:underline">
              {sender?.displayName || sender?.username || 'Loading...'}
            </span>
            <span className="text-twitter-gray-1 ml-1">@{sender?.username}</span>
          </div>
        </div>
        <p className="text-white text-[15px] mt-2 leading-relaxed">
          {notification.message}
        </p>
      </div>
    </div>
  );
};

const Notifications: React.FC = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();

  const { data: notifications, isLoading, isError, refetch } = useQuery({
    queryKey: ['notifications', user?.userId],
    queryFn: () => notificationService.getUserNotifications(user?.userId || 0),
    enabled: !!user?.userId,
  });

  const markAllReadMutation = useMutation({
    mutationFn: async () => {
      const unread = notifications?.filter(n => !n.isRead) || [];
      const promises = unread.map(n => notificationService.markAsRead(n.notificationId));
      await Promise.all(promises);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('All notifications marked as read');
    },
    onError: () => {
      toast.error('Failed to mark notifications as read');
    }
  });

  const hasUnread = notifications?.some(n => !n.isRead) || false;

  return (
    <div className="flex flex-col min-h-screen bg-black">
      
      {/* Sticky Header */}
      <div className="sticky top-0 bg-black/85 backdrop-blur-md border-b border-twitter-dark-4 z-20 flex items-center justify-between px-4 py-3 text-left">
        <h2 className="font-extrabold text-xl text-white">Notifications</h2>
        {user && hasUnread && (
          <button
            onClick={() => markAllReadMutation.mutate()}
            disabled={markAllReadMutation.isPending}
            className="flex items-center gap-1.5 px-3.5 py-1.5 bg-twitter-dark-3 hover:bg-twitter-dark-4 disabled:opacity-50 text-white text-xs font-bold rounded-full border border-twitter-dark-4 transition-all duration-200"
          >
            {markAllReadMutation.isPending ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <CheckCheck className="w-3.5 h-3.5" />
            )}
            <span>Mark all read</span>
          </button>
        )}
      </div>

      {/* Notifications List */}
      <div className="flex-grow">
        {isLoading ? (
          <div className="flex justify-center items-center h-48 text-twitter-blue">
            <Loader2 className="w-8 h-8 animate-spin" />
          </div>
        ) : isError ? (
          <div className="p-8 text-center text-twitter-gray-1 flex flex-col items-center gap-2">
            <AlertCircle className="w-10 h-10 text-red-500" />
            <p className="font-semibold text-white">Failed to load notifications</p>
            <button onClick={() => refetch()} className="mt-2 bg-twitter-blue hover:bg-twitter-blue-hover text-white font-bold py-1.5 px-4 rounded-full text-xs">
              Retry
            </button>
          </div>
        ) : !notifications || notifications.length === 0 ? (
          <div className="p-12 text-center text-twitter-gray-1 flex flex-col items-center gap-3">
            <Bell className="w-12 h-12 opacity-35" />
            <p className="font-bold text-lg text-white">Nothing to see here — yet</p>
            <p className="text-sm">When you receive likes, follows, or replies, they'll show up here.</p>
          </div>
        ) : (
          <div className="divide-y divide-twitter-dark-4">
            {notifications.map((notification) => (
              <NotificationItem key={notification.notificationId} notification={notification} />
            ))}
          </div>
        )}
      </div>

    </div>
  );
};

export default Notifications;
