import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { tokenStorage } from './tokenStorage';
import { AdminAuthResponse, AdminLoginRequest, AdminUser } from '@/types/auth.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const authService = {
  async login(request: AdminLoginRequest): Promise<AdminAuthResponse> {
    if (isMock) {
      const response = await mockService.login(request);
      tokenStorage.setToken(response.token);
      tokenStorage.setUser(JSON.stringify(response.user));
      return response;
    }

    const res = await apiClient.post<{ success: boolean; data: AdminAuthResponse }>(
      '/api/v1/admin/auth/login',
      request
    );
    const data = res.data.data;
    tokenStorage.setToken(data.token);
    tokenStorage.setUser(JSON.stringify(data.user));
    return data;
  },

  getCurrentUser(): AdminUser | null {
    const raw = tokenStorage.getUser();
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  logout(): void {
    tokenStorage.clear();
  },

  isAuthenticated(): boolean {
    return !!tokenStorage.getToken();
  }
};
