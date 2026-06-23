import { create } from 'zustand';
import { User, LoginResponse } from '../types';
import { authService } from '../services/api';

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isComposerOpen: boolean;
  setComposerOpen: (open: boolean) => void;
  login: (data: any) => Promise<void>;
  register: (data: any) => Promise<void>;
  logout: () => Promise<void>;
  initAuth: () => Promise<void>;
  updateUser: (updater: Partial<User>) => void;
}

// Decode userId from JWT tokens if available, or fetch it
const parseJwt = (token: string) => {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch (e) {
    return null;
  }
};

export const useAuthStore = create<AuthState>((set, get) => {
  // Listen for global logout events (like on 401 refresh fail)
  if (typeof window !== 'undefined') {
    window.addEventListener('auth-logout', () => {
      set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
        isLoading: false,
      });
    });
  }

  return {
    user: null,
    accessToken: null,
    refreshToken: null,
    isAuthenticated: false,
    isLoading: true,
    isComposerOpen: false,
    setComposerOpen: (open) => set({ isComposerOpen: open }),

    initAuth: async () => {
      set({ isLoading: true });
      const accessToken = localStorage.getItem('accessToken');
      const refreshToken = localStorage.getItem('refreshToken');
      const userIdStr = localStorage.getItem('userId');

      if (accessToken && refreshToken && userIdStr) {
        try {
          const userId = parseInt(userIdStr, 10);
          // Retrieve user info from backend
          const userData = await authService.getCurrentUser(userId);
          
          set({
            user: userData,
            accessToken,
            refreshToken,
            isAuthenticated: true,
          });
        } catch (error) {
          console.error('Failed to restore auth session:', error);
          // If server fails (like user disabled or db reset), clear storage
          localStorage.clear();
          set({
            user: null,
            accessToken: null,
            refreshToken: null,
            isAuthenticated: false,
          });
        }
      }
      set({ isLoading: false });
    },

    login: async (credentials) => {
      set({ isLoading: true });
      try {
        const response = await authService.login(credentials);
        const { accessToken, refreshToken } = response.data;
        
        // Decode JWT to get userId and details
        const payload = parseJwt(accessToken);
        const userId = payload?.userId || payload?.id;
        const role = payload?.role || 'USER';

        if (!userId) {
          throw new Error('JWT token does not contain user identifier');
        }

        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('userId', String(userId));
        localStorage.setItem('userRole', role);

        // Retrieve real user profile details (username, email, role)
        const userData = await authService.getCurrentUser(Number(userId));

        set({
          user: userData,
          accessToken,
          refreshToken,
          isAuthenticated: true,
          isLoading: false,
        });
      } catch (error) {
        set({ isLoading: false });
        throw error;
      }
    },

    register: async (data) => {
      set({ isLoading: true });
      try {
        await authService.register(data);
        set({ isLoading: false });
      } catch (error) {
        set({ isLoading: false });
        throw error;
      }
    },

    logout: async () => {
      const refreshToken = get().refreshToken || localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          await authService.logout(refreshToken);
        } catch (error) {
          console.error('API logout failed, clearing locally anyway', error);
        }
      }
      localStorage.clear();
      set({
        user: null,
        accessToken: null,
        refreshToken: null,
        isAuthenticated: false,
      });
    },

    updateUser: (updater) => {
      const currentUser = get().user;
      if (currentUser) {
        set({ user: { ...currentUser, ...updater } });
      }
    }
  };
});
