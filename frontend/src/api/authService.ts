import axiosClient from './axiosClient';
import type { LoginRequest, RegisterRequest, RefreshTokenRequest, AuthResponse } from '../types/dto';
import { useAuthStore } from '../stores/authStore';

class AuthApi {
  /**
   * Login user with email and password
   */
  async login(email: string, password: string): Promise<AuthResponse> {
    const request: LoginRequest = { email, password };
    const response = await axiosClient.post<{ result: AuthResponse }>('/auth/login', request);
    
    // Store tokens and user info in Zustand store
    useAuthStore.getState().setAuthData(response.data.result);
    
    return response.data.result;
  }

  /**
   * Register new user with email and password
   */
  async register(email: string, password: string): Promise<AuthResponse> {
    const request: RegisterRequest = { email, password };
    const response = await axiosClient.post<{ result: AuthResponse }>('/auth/register', request);
    
    // Store tokens and user info in Zustand store
    useAuthStore.getState().setAuthData(response.data.result);
    
    return response.data.result;
  }

  /**
   * Refresh access token using refresh token
   */
  async refreshToken(): Promise<string> {
    const refreshToken = this.getRefreshToken();
    
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const request: RefreshTokenRequest = { refreshToken };
    const response = await axiosClient.post<{ result: AuthResponse }>('/auth/refresh', request);
    
    // Update stored tokens in Zustand store
    useAuthStore.getState().setAuthData(response.data.result);
    
    return response.data.result.accessToken;
  }

  /**
   * Logout user by clearing all stored tokens and user info
   */
  logout(): void {
    useAuthStore.getState().clearAuthData();
  }

  /**
   * Get access token from Zustand store
   */
  getAccessToken(): string | null {
    return useAuthStore.getState().accessToken;
  }

  /**
   * Get refresh token from Zustand store
   */
  getRefreshToken(): string | null {
    return useAuthStore.getState().refreshToken;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return useAuthStore.getState().isAuthenticated();
  }

  /**
   * Check if user has premium status
   */
  isPremium(): boolean {
    return useAuthStore.getState().isPremium;
  }

  /**
   * Get current user ID
   */
  getUserId(): number | null {
    return useAuthStore.getState().userId;
  }

  /**
   * Get current user email
   */
  getUserEmail(): string | null {
    return useAuthStore.getState().email;
  }
}

// Export singleton instance
export const authApi = new AuthApi();
