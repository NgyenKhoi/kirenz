import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse } from '../types/dto';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: number | null;
  email: string | null;
  isPremium: boolean;
  
  // Actions
  setAuthData: (authResponse: AuthResponse) => void;
  clearAuthData: () => void;
  updateTokens: (accessToken: string, refreshToken: string) => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      email: null,
      isPremium: false,

      setAuthData: (authResponse: AuthResponse) => {
        set({
          accessToken: authResponse.accessToken,
          refreshToken: authResponse.refreshToken,
          userId: authResponse.userId,
          email: authResponse.email,
          isPremium: authResponse.isPremium,
        });
      },

      clearAuthData: () => {
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          email: null,
          isPremium: false,
        });
      },

      updateTokens: (accessToken: string, refreshToken: string) => {
        set({ accessToken, refreshToken });
      },

      isAuthenticated: () => {
        return !!get().accessToken;
      },
    }),
    {
      name: 'auth-storage', // localStorage key
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        email: state.email,
        isPremium: state.isPremium,
      }),
    }
  )
);
