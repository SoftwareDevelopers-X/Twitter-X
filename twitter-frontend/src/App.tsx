import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { QueryClient, QueryClientProvider, useQueryClient } from '@tanstack/react-query';
import toast, { Toaster } from 'react-hot-toast';
import { useAuthStore } from './store/authStore';
import { updateTweetInCache } from './utils/queryCache';

// Components & Layout
import Layout from './components/Layout';

// Pages
import Login from './pages/Login';
import Register from './pages/Register';
import Home from './pages/Home';
import Profile from './pages/Profile';
import TweetDetail from './pages/TweetDetail';
import Notifications from './pages/Notifications';
import Bookmarks from './pages/Bookmarks';
import Search from './pages/Search';
import Settings from './pages/Settings';
import Messages from './pages/Messages';
import { ChatProvider } from './context/ChatContext';

import { Loader2, Mail } from 'lucide-react';
import './App.css';

// Create a React Query client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

// Guard component for protected routes
const PrivateRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center text-twitter-blue">
        <Loader2 className="w-10 h-10 animate-spin" />
        <span className="text-xs text-twitter-gray-1 mt-4 font-semibold">Connecting to X-Clone...</span>
      </div>
    );
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

// Guard component to redirect authenticated users away from auth pages
const AuthRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="min-h-screen bg-black flex flex-col items-center justify-center text-twitter-blue">
        <Loader2 className="w-10 h-10 animate-spin" />
      </div>
    );
  }

  return !isAuthenticated ? <Outlet /> : <Navigate to="/" replace />;
};

const WebSocketListener: React.FC = () => {
  const { user, isAuthenticated } = useAuthStore();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated || !user?.userId) return;

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws/notifications?userId=${user.userId}`;
    
    let socket: WebSocket;
    let reconnectTimeout: any;

    const connect = () => {
      console.log('Connecting to WebSocket...');
      socket = new WebSocket(wsUrl);

      socket.onopen = () => {
        console.log('WebSocket connected successfully');
      };

      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          const { type, data } = message;
          console.log('WebSocket event received:', type, data);

          if (type === 'NOTIFICATION') {
            queryClient.invalidateQueries({ queryKey: ['notifications', user.userId] });
            toast(data.message, {
              icon: '🔔',
            });
          } else {
            // If the event was triggered by our own user, ignore it since optimistic updates already applied it
            if (data.userId && Number(data.userId) === Number(user?.userId)) {
              return;
            }

            const tweetId = Number(data.tweetId);

            if (type === 'TWEET_LIKED' || type === 'TWEET_UNLIKED') {
              queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweetId] });
              const isLiked = type === 'TWEET_LIKED';
              updateTweetInCache(queryClient, tweetId, (t) => ({
                ...t,
                likeCount: Math.max(0, t.likeCount + (isLiked ? 1 : -1))
              }));
            } else if (type === 'TWEET_RETWEETED' || type === 'TWEET_RETWEET_REMOVED') {
              queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweetId] });
              const isRetweeted = type === 'TWEET_RETWEETED';
              updateTweetInCache(queryClient, tweetId, (t) => ({
                ...t,
                retweetCount: Math.max(0, t.retweetCount + (isRetweeted ? 1 : -1))
              }));
            } else if (type === 'TWEET_REPLIED' || type === 'TWEET_REPLY_DELETED') {
              queryClient.invalidateQueries({ queryKey: ['tweet-detail', tweetId] });
              queryClient.invalidateQueries({ queryKey: ['replies', tweetId] });
              queryClient.invalidateQueries({ queryKey: ['user-replies'] });
              const isReplied = type === 'TWEET_REPLIED';
              updateTweetInCache(queryClient, tweetId, (t) => ({
                ...t,
                replyCount: Math.max(0, (t.replyCount || 0) + (isReplied ? 1 : -1))
              }));
            }
          }
        } catch (err) {
          console.error('Error handling WebSocket message:', err);
        }
      };

      socket.onclose = () => {
        console.log('WebSocket disconnected, reconnecting in 3 seconds...');
        reconnectTimeout = setTimeout(connect, 3000);
      };

      socket.onerror = (err) => {
        console.error('WebSocket error:', err);
        socket.close();
      };
    };

    connect();

    return () => {
      if (socket) {
        socket.onclose = null;
        socket.close();
      }
      clearTimeout(reconnectTimeout);
    };
  }, [isAuthenticated, user?.userId, queryClient]);

  return null;
};

class ChatErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: any, errorInfo: any) {
    console.error("ChatProvider error caught by boundary:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return <Outlet />;
    }
    return this.props.children;
  }
}

class LocalErrorBoundary extends React.Component<
  { children: React.ReactNode; fallback: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback;
    }
    return this.props.children;
  }
}

const ChatPageErrorBoundary: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  return (
    <LocalErrorBoundary fallback={
      <div className="flex flex-col items-center justify-center p-8 text-center min-h-[300px] h-full text-white w-full">
        <Mail className="w-12 h-12 text-twitter-blue mb-4" />
        <h2 className="text-xl font-bold mb-2">Direct Messages Unavailable</h2>
        <p className="text-twitter-gray-1 text-sm max-w-sm">
          There was an issue loading your messages. The rest of X-Clone remains functional.
        </p>
      </div>
    }>
      {children}
    </LocalErrorBoundary>
  );
};

const ChatLayout: React.FC = () => {
  return (
    <ChatErrorBoundary>
      <ChatProvider>
        <Outlet />
      </ChatProvider>
    </ChatErrorBoundary>
  );
};

function App() {
  const { initAuth } = useAuthStore();

  // Restore session from localStorage on mount
  useEffect(() => {
    initAuth();
  }, [initAuth]);

  return (
    <QueryClientProvider client={queryClient}>
      <WebSocketListener />
      <BrowserRouter>
        <Routes>
          {/* Public Auth Routes */}
          <Route element={<AuthRoute />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Route>

          {/* Protected Routes inside App Layout */}
          <Route element={<PrivateRoute />}>
            <Route element={<ChatLayout />}>
              <Route element={<Layout />}>
                <Route path="/" element={<Home />} />
                <Route path="/profile/:id" element={<Profile />} />
                <Route path="/tweet/:id" element={<TweetDetail />} />
                <Route path="/notifications" element={<Notifications />} />
                <Route path="/bookmarks" element={<Bookmarks />} />
                <Route path="/messages" element={<ChatPageErrorBoundary><Messages /></ChatPageErrorBoundary>} />
                <Route path="/messages/:conversationId" element={<ChatPageErrorBoundary><Messages /></ChatPageErrorBoundary>} />
                <Route path="/search" element={<Search />} />
                <Route path="/settings" element={<Settings />} />
              </Route>
            </Route>
          </Route>

          {/* Fallback route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      
      {/* Toast Notifications */}
      <Toaster 
        position="bottom-center"
        toastOptions={{
          style: {
            background: '#16181c',
            color: '#fff',
            border: '1px solid #2f3336',
            borderRadius: '9999px',
            fontSize: '14px',
            fontWeight: 'bold',
          },
        }}
      />
      
    </QueryClientProvider>
  );
}

export default App;
