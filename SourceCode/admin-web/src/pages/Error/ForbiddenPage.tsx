import React from 'react';
import { ShieldX } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/common/Button';

export const ForbiddenPage: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-white rounded-2xl border border-rose-100 shadow-sm">
      <div className="w-16 h-16 rounded-2xl bg-rose-50 text-rose-600 flex items-center justify-center mb-4">
        <ShieldX className="w-8 h-8" />
      </div>
      <h2 className="text-2xl font-bold text-slate-800">403 – Không Đủ Quyền Truy Cập</h2>
      <p className="text-sm text-slate-500 mt-2 max-w-md">
        Tài khoản của bạn không có vai trò phù hợp để truy cập chức năng này. Chức năng Quản lý Người dùng và Nhật ký Quản trị yêu cầu vai trò Quản trị viên (ADMIN).
      </p>
      <Link to="/dashboard" className="mt-6">
        <Button variant="primary">Quay về Trang Tổng quan</Button>
      </Link>
    </div>
  );
};
