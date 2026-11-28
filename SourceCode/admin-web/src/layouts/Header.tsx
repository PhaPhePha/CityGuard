import React from 'react';
import { LogOut, Shield } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { Badge } from '@/components/common/Badge';

export const Header: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 bg-white border-b border-slate-200 px-6 flex items-center justify-between sticky top-0 z-10">
      <div className="flex items-center space-x-3">
        <h2 className="text-base font-semibold text-slate-800">
          Cổng Quản Trị Hệ Thống Giao Thông CityGuard
        </h2>
      </div>

      <div className="flex items-center space-x-4">
        {/* User Identity */}
        <div className="flex items-center space-x-3 text-right">
          <div>
            <div className="text-sm font-semibold text-slate-800 leading-tight">
              {user?.displayName || user?.username}
            </div>
            <div className="flex justify-end mt-0.5">
              <Badge variant={user?.role === 'ADMIN' ? 'danger' : 'info'}>
                <Shield className="w-3 h-3 mr-1 inline" />
                {user?.role}
              </Badge>
            </div>
          </div>
        </div>

        {/* Logout action */}
        <button
          onClick={logout}
          title="Đăng xuất"
          className="p-2 text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
        >
          <LogOut className="w-5 h-5" />
        </button>
      </div>
    </header>
  );
};
