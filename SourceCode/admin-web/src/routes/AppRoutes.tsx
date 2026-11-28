import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthLayout } from '@/layouts/AuthLayout';
import { AdminLayout } from '@/layouts/AdminLayout';
import { LoginPage } from '@/pages/Login/LoginPage';
import { DashboardPage } from '@/pages/Dashboard/DashboardPage';
import { ReportListPage } from '@/pages/Reports/ReportListPage';
import { ReportDetailPage } from '@/pages/Reports/ReportDetailPage';
import { UserListPage } from '@/pages/Users/UserListPage';
import { UserDetailPage } from '@/pages/Users/UserDetailPage';
import { CommentListPage } from '@/pages/Comments/CommentListPage';
import { AuditLogPage } from '@/pages/AuditLogs/AuditLogPage';
import { ForbiddenPage } from '@/pages/Error/ForbiddenPage';
import { NotFoundPage } from '@/pages/Error/NotFoundPage';
import { ProtectedRoute } from './ProtectedRoute';

export const AppRoutes: React.FC = () => {
  return (
    <Routes>
      {/* Public Authentication routes */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* Protected Admin Portal routes */}
      <Route
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />

        {/* Reports routes */}
        <Route path="/reports" element={<ReportListPage />} />
        <Route path="/reports/:publicId" element={<ReportDetailPage />} />

        {/* Comments routes */}
        <Route path="/comments" element={<CommentListPage />} />

        {/* Admin-only routes */}
        <Route
          path="/users"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <UserListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users/:id"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <UserDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/logs"
          element={
            <ProtectedRoute allowedRoles={['ADMIN']}>
              <AuditLogPage />
            </ProtectedRoute>
          }
        />

        {/* Error pages */}
        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
};
