import React, { createContext, useContext, useState, useEffect } from 'react';
import { AdminLoginRequest, AdminUser } from '@/types/auth.types';
import { Role } from '@/types/common.types';
import { authService } from '@/services/authService';

interface AuthContextType {
  user: AdminUser | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (req: AdminLoginRequest) => Promise<void>;
  logout: () => void;
  hasRole: (requiredRoles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    try {
      const storedUser = authService.getCurrentUser();
      if (storedUser && authService.isAuthenticated()) {
        setUser(storedUser);
      }
    } catch (e) {
      console.warn('Failed to parse current user session', e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = async (req: AdminLoginRequest) => {
    const res = await authService.login(req);
    setUser(res.user);
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  const hasRole = (requiredRoles: Role[]) => {
    if (!user) return false;
    return requiredRoles.includes(user.role);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        hasRole
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
