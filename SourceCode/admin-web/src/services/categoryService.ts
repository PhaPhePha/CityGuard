import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { AdminCategory } from '@/types/category.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const categoryService = {
  async getCategories(): Promise<AdminCategory[]> {
    if (isMock) {
      return mockService.getCategories();
    }
    const res = await apiClient.get<{ success: boolean; data: AdminCategory[] }>(
      '/api/v1/admin/categories'
    );
    return res.data.data;
  }
};
