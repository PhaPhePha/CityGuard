import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  AlertTriangle,
  MessageSquare,
  Users,
  ShieldAlert,
  Server
} from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

export const Sidebar: React.FC = () => {
  const { user, hasRole } = useAuth();

  const navItems = [
    {
      to: '/dashboard',
      label: 'Tổng quan (Dashboard)',
      icon: LayoutDashboard,
      roles: ['ADMIN', 'MODERATOR']
    },
    {
      to: '/reports',
      label: 'Kiểm duyệt Báo cáo',
      icon: AlertTriangle,
      roles: ['ADMIN', 'MODERATOR']
    },
    {
      to: '/comments',
      label: 'Quản lý Bình luận',
      icon: MessageSquare,
      roles: ['ADMIN', 'MODERATOR']
    },
    {
      to: '/users',
      label: 'Quản lý Người dùng',
      icon: Users,
      roles: ['ADMIN']
    },
    {
      to: '/logs',
      label: 'Nhật ký Quản trị',
      icon: ShieldAlert,
      roles: ['ADMIN']
    }
  ];

  return (
    <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col flex-shrink-0 min-h-screen border-r border-slate-800">
      {/* Brand Header */}
      <div className="h-16 flex items-center px-6 border-b border-slate-800 space-x-3">
        <div className="w-8 h-8 rounded-lg bg-brand-500 flex items-center justify-center text-slate-950 font-bold">
          CG
        </div>
        <div>
          <h1 className="font-bold text-white text-base tracking-wide">CityGuard</h1>
          <p className="text-xs text-brand-400 font-medium">Admin & Moderation</p>
        </div>
      </div>

      {/* Mode status indicator */}
      <div className="px-4 py-3 mx-4 my-3 rounded-lg bg-slate-800/60 border border-slate-700/50 flex items-center space-x-2 text-xs">
        <Server className="w-3.5 h-3.5 text-emerald-400" />
        <span className="font-medium">
          CityGuard
        </span>
      </div>

      {/* Navigation menu */}
      <nav className="flex-1 px-3 py-2 space-y-1">
        {navItems.map((item) => {
          if (!hasRole(item.roles as any)) return null;
          const Icon = item.icon;
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-sm font-medium transition ${
                  isActive
                    ? 'bg-brand-500/10 text-brand-400 border border-brand-500/20'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                }`
              }
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      {/* Current User Quick Footer */}
      <div className="p-4 border-t border-slate-800 text-xs text-slate-400">
        <div className="font-medium text-white truncate">{user?.displayName}</div>
        <div className="text-slate-500 truncate">{user?.email}</div>
      </div>
    </aside>
  );
};
