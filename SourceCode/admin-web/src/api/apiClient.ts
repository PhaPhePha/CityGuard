import axios, {type AxiosError } from 'axios';
import { tokenStorage } from '@/services/tokenStorage';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const loginEndpoint = '/api/v1/admin/auth/login';

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getToken();
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  }
);

apiClient.interceptors.response.use(
    (response) => response,
    (error: AxiosError) => {
        const token = tokenStorage.getToken();
        const authorization = error.config?.headers.get('Authorization');

        if (
            error.response?.status === 401 &&
            error.config?.url !== loginEndpoint &&
            token &&
            authorization === `Bearer ${token}`
        ) {
            tokenStorage.clear();

            if (window.location.pathname !== '/login') {
                window.location.replace('/login');
            }
        }

        return Promise.reject(error);
    }
);