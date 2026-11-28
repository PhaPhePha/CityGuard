import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { Role } from '@/types/common.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: Role[];
}

/**
 * Lưu ý kiến trúc: ProtectedRoute ở Frontend chỉ đóng vai trò hỗ trợ trải nghiệm người dùng (UX)
 * và điều hướng hợp lý. Cơ chế bảo mật và kiểm soát truy cập thực tế (Authorization enforcement)
 * hoàn toàn do Backend REST API chịu trách nhiệm qua việc validate JWT và quyền truy cập endpoint.
 */
export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles
}) => {
  const { isAuthenticated, isLoading, user, hasRole } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <LoadingSpinner message="Đang xác thực phiên làm việc..." />
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && !hasRole(allowedRoles)) {
    return <Navigate to="/403" replace />;
  }

  return <>{children}</>;
};
