import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { useQuery } from '@tanstack/react-query';
import { notificationService } from '../services/api';
import { 
  Home, 
  Search, 
  Bell, 
  Bookmark, 
  User, 
  Settings, 
  LogOut, 
  PlusSquare,
  Sparkles
} from 'lucide-react';
import { useUser } from '../hooks/useUser';

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const { data: profile } = useUser(user?.userId);

  // Fetch notifications to display unread badge
  const { data: notifications } = useQuery({
    queryKey: ['notifications', user?.userId],
    queryFn: () => notificationService.getUserNotifications(user?.userId || 0),
    enabled: !!user?.userId,
    refetchInterval: 15000, // Poll notifications every 15 seconds
  });

  const unreadCount = notifications?.filter(n => !n.isRead).length || 0;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItems = [
    { name: 'Home', path: '/', icon: Home },
    { name: 'Explore', path: '/search', icon: Search },
    { name: 'Notifications', path: '/notifications', icon: Bell, badge: unreadCount },
    { name: 'Bookmarks', path: '/bookmarks', icon: Bookmark },
    { name: 'Profile', path: user ? `/profile/${user.userId}` : '#', icon: User },
    { name: 'Settings', path: '/settings', icon: Settings },
  ];

  return (
    <div className="h-full flex flex-col justify-between py-4 pr-0 xl:pr-4">
      <div className="flex flex-col items-center xl:items-start">
        {/* Logo */}
        <NavLink 
          to="/" 
          className="p-3 hover:bg-white/10 rounded-full transition-all duration-200 w-fit mb-2 flex items-center gap-3 text-twitter-blue"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true" className="w-8 h-8 fill-current text-white xl:text-twitter-blue hover:scale-105 transition-transform duration-200">
            <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"></path>
          </svg>
          <span className="hidden xl:inline font-black text-2xl tracking-tighter text-white">X-Clone</span>
        </NavLink>

        {/* Navigation Links */}
        <nav className="space-y-1 w-full">
          {navItems.map((item) => (
            <NavLink
              key={item.name}
              to={item.path}
              className={({ isActive }) =>
                `flex items-center gap-4 p-3 rounded-full hover:bg-white/10 transition-all duration-200 w-fit xl:w-full group ${
                  isActive ? 'font-bold text-white' : 'text-twitter-gray-2 hover:text-white'
                }`
              }
            >
              <div className="relative">
                <item.icon className="w-7 h-7 group-hover:scale-105 transition-transform duration-200" />
                {item.badge !== undefined && item.badge > 0 && (
                  <span className="absolute -top-1 -right-1 bg-twitter-blue text-white text-[11px] font-bold rounded-full w-4 h-4 flex items-center justify-center border border-black animate-pulse">
                    {item.badge}
                  </span>
                )}
              </div>
              <span className="hidden xl:inline text-xl">{item.name}</span>
            </NavLink>
          ))}
        </nav>

        {/* Post Button */}
        <button
          onClick={() => navigate('/')}
          className="mt-4 p-3 xl:py-3 xl:px-8 bg-twitter-blue hover:bg-twitter-blue-hover text-white rounded-full font-bold transition-all duration-200 w-fit xl:w-full flex items-center justify-center gap-2 shadow-lg shadow-twitter-blue/20 active:scale-95"
        >
          <PlusSquare className="w-6 h-6 xl:hidden" />
          <span className="hidden xl:inline text-lg">Post</span>
        </button>
      </div>

      {/* User Actions Account Banner */}
      {user && (
        <div className="flex flex-col items-center xl:items-stretch gap-2">
          {/* Admin Tag */}
          {user.role === 'ADMIN' && (
            <div className="hidden xl:flex items-center gap-1.5 px-3 py-1 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg text-xs font-semibold w-fit mx-2">
              <Sparkles className="w-3.5 h-3.5" />
              <span>Administrator</span>
            </div>
          )}

          <div className="flex items-center justify-between p-3 rounded-full hover:bg-white/10 transition-all duration-200 w-fit xl:w-full select-none cursor-pointer group">
            <div className="flex items-center gap-3" onClick={() => navigate(`/profile/${user.userId}`)}>
              <img
                src={profile?.avatarUrl || `https://api.dicebear.com/7.x/adventurer/svg?seed=${user.username}`}
                alt="Avatar"
                className="w-10 h-10 rounded-full object-cover border border-twitter-dark-4 bg-twitter-dark-3"
              />
              <div className="hidden xl:block text-left">
                <p className="font-bold text-white text-sm leading-tight group-hover:underline">
                  {profile?.displayName || user.username}
                </p>
                <p className="text-twitter-gray-1 text-xs leading-none">@{user.username}</p>
              </div>
            </div>
            
            {/* Logout Trigger */}
            <button
              onClick={handleLogout}
              title="Logout"
              className="hidden xl:block p-2 text-twitter-gray-1 hover:text-red-500 hover:bg-red-500/10 rounded-full transition-all duration-200"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
          
          {/* Mobile-only logout button icon */}
          <button
            onClick={handleLogout}
            title="Logout"
            className="xl:hidden p-3 text-twitter-gray-1 hover:text-red-500 hover:bg-red-500/10 rounded-full transition-all duration-200 mt-2"
          >
            <LogOut className="w-6 h-6" />
          </button>
        </div>
      )}
    </div>
  );
};

export default Sidebar;
