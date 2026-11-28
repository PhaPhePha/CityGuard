import React from 'react';
import { HelpCircle } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/common/Button';

export const NotFoundPage: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center bg-white rounded-2xl border border-slate-200 shadow-sm">
      <div className="w-16 h-16 rounded-2xl bg-slate-100 text-slate-500 flex items-center justify-center mb-4">
        <HelpCircle className="w-8 h-8" />
      </div>
      <h2 className="text-2xl font-bold text-slate-800">404 – Không Tìm Thấy Trang</h2>
      <p className="text-sm text-slate-500 mt-2 max-w-md">
        Đường dẫn bạn yêu cầu không tồn tại trong Cổng Quản Trị CityGuard.
      </p>
      <Link to="/dashboard" className="mt-6">
        <Button variant="primary">Quay về Trang Tổng quan</Button>
      </Link>
    </div>
  );
};
