import React from 'react';
import { Outlet } from 'react-router-dom';

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen bg-slate-900 flex flex-col justify-center items-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex w-12 h-12 rounded-xl bg-brand-500 items-center justify-center text-slate-950 font-black text-xl mb-3 shadow-lg shadow-brand-500/20">
            CG
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight">CityGuard Portal</h1>
          <p className="text-slate-400 text-sm mt-1">Đăng nhập Quản trị viên & Kiểm duyệt viên</p>
        </div>
        <div className="bg-white rounded-2xl shadow-xl p-8 border border-slate-100">
          <Outlet />
        </div>
      </div>
    </div>
  );
};
