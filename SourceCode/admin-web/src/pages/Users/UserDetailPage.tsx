import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { userService } from '@/services/userService';
import { AdminUserDetail, UserStatus } from '@/types/user.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { useToast } from '@/context/ToastContext';
import {
  ArrowLeft,
  Lock,
  Unlock,
  Mail,
  Shield,
  Calendar,
  FileText,
  ExternalLink,
  AlertTriangle
} from 'lucide-react';

export const UserDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { success, error } = useToast();

  const [user, setUser] = useState<AdminUserDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Lock/Unlock Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [targetStatus, setTargetStatus] = useState<UserStatus>('LOCKED');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadUser = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const data = await userService.getUserDetail(Number(id));
      setUser(data);
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Không thể tải chi tiết người dùng');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUser();
  }, [id]);

  const handleOpenLockModal = () => {
    if (!user) return;
    const nextStatus = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    setTargetStatus(nextStatus);
    setReason('');
    setIsModalOpen(true);
  };

  const handleSubmitLockStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    if (!reason.trim()) {
      error('Vui lòng nhập lý do khóa hoặc mở khóa tài khoản.');
      return;
    }

    setIsSubmitting(true);
    try {
      await userService.updateUserStatus(user.id, {
        status: targetStatus,
        reason: reason.trim()
      });
      setIsModalOpen(false);
      success(
        `Đã ${targetStatus === 'LOCKED' ? 'khóa' : 'mở khóa'} tài khoản @${user.username} thành công!`
      );
      loadUser();
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Cập nhật trạng thái thất bại');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Đang tải thông tin tài khoản người dùng..." />;
  }

  if (!user) {
    return (
      <div className="space-y-4">
        <Button variant="outline" size="sm" onClick={() => navigate('/users')}>
          <ArrowLeft className="w-4 h-4 mr-1.5" /> Quay lại danh sách
        </Button>
        <EmptyState
          title="Không tìm thấy người dùng"
          description={`Không tìm thấy tài khoản với ID: ${id}`}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Header Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="sm" onClick={() => navigate('/users')}>
            <ArrowLeft className="w-4 h-4 mr-1.5" /> Quản lý tài khoản
          </Button>
          <div className="h-4 w-px bg-slate-200" />
          <span className="text-xs text-slate-400 font-mono">User ID: #{user.id}</span>
        </div>

        <div>
          <Button
            variant={user.status === 'ACTIVE' ? 'danger' : 'outline'}
            size="sm"
            onClick={handleOpenLockModal}
          >
            {user.status === 'ACTIVE' ? (
              <>
                <Lock className="w-4 h-4 mr-1.5" /> Khóa tài khoản
              </>
            ) : (
              <>
                <Unlock className="w-4 h-4 mr-1.5 text-emerald-600" /> Mở khóa tài khoản
              </>
            )}
          </Button>
        </div>
      </div>

      {/* User Information Card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 pb-6 border-b border-slate-100">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 rounded-2xl bg-brand-50 border border-brand-100 flex items-center justify-center text-brand-700 font-bold text-2xl">
              {user.displayName?.[0] || 'U'}
            </div>
            <div>
              <div className="flex items-center space-x-3">
                <h2 className="text-xl font-bold text-slate-800">{user.displayName}</h2>
                <Badge variant={user.status === 'ACTIVE' ? 'success' : 'danger'}>
                  {user.status}
                </Badge>
                <Badge variant="info">{user.role}</Badge>
              </div>
              <p className="text-sm text-slate-400 mt-1">@{user.username}</p>
            </div>
          </div>

          {/* Quick Stat */}
          <div className="flex items-center space-x-4 bg-slate-50 px-5 py-3 rounded-xl border border-slate-100">
            <div>
              <p className="text-xs font-semibold text-slate-400 uppercase">Tổng báo cáo</p>
              <p className="text-2xl font-bold text-brand-600">{user.totalReports ?? 0}</p>
            </div>
            <FileText className="w-8 h-8 text-brand-300" />
          </div>
        </div>

        {/* Detailed Info Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-6">
          <div className="space-y-1">
            <span className="text-xs font-semibold text-slate-400 uppercase flex items-center">
              <Mail className="w-3.5 h-3.5 mr-1 text-slate-400" /> Địa chỉ Email
            </span>
            <p className="text-sm font-medium text-slate-800">{user.email}</p>
          </div>

          <div className="space-y-1">
            <span className="text-xs font-semibold text-slate-400 uppercase flex items-center">
              <Shield className="w-3.5 h-3.5 mr-1 text-slate-400" /> Vai trò hệ thống
            </span>
            <p className="text-sm font-medium text-slate-800">
              {user.role === 'ADMIN'
                ? 'Quản trị viên (Toàn quyền)'
                : user.role === 'MODERATOR'
                ? 'Kiểm duyệt viên (Moderator)'
                : 'Người dùng phổ thông'}
            </p>
          </div>

          <div className="space-y-1">
            <span className="text-xs font-semibold text-slate-400 uppercase flex items-center">
              <Calendar className="w-3.5 h-3.5 mr-1 text-slate-400" /> Ngày tham gia
            </span>
            <p className="text-sm font-medium text-slate-800">
              {new Date(user.createdAt).toLocaleDateString('vi-VN')}
            </p>
          </div>
        </div>
      </div>

      {/* User's Recent Reports Section */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-slate-800">
              Báo cáo gần đây của người dùng ({user.recentReports?.length || 0})
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Danh sách các sự cố do người dùng này tạo trên hệ thống
            </p>
          </div>
        </div>

        {!user.recentReports || user.recentReports.length === 0 ? (
          <div className="p-8 text-center text-slate-400 text-sm">
            Người dùng chưa tạo báo cáo sự cố nào.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3.5">Mã Public ID</th>
                  <th className="px-5 py-3.5">Danh mục</th>
                  <th className="px-5 py-3.5">Mô tả</th>
                  <th className="px-5 py-3.5">Địa chỉ</th>
                  <th className="px-5 py-3.5">Trạng thái</th>
                  <th className="px-5 py-3.5 text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {user.recentReports.map((r) => (
                  <tr key={r.publicId} className="hover:bg-slate-50/80 transition">
                    <td className="px-5 py-3.5">
                      <Link
                        to={`/reports/${r.publicId}`}
                        className="font-mono text-xs font-bold text-brand-600 hover:text-brand-700 flex items-center space-x-1"
                      >
                        <span>{r.publicId}</span>
                        <ExternalLink className="w-3 h-3 text-brand-400" />
                      </Link>
                    </td>
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <span className="font-medium text-slate-800">{r.categoryDisplayName}</span>
                    </td>
                    <td className="px-5 py-3.5 max-w-xs truncate text-slate-700">
                      {r.description || '—'}
                    </td>
                    <td className="px-5 py-3.5 max-w-[200px] truncate text-slate-400 text-xs">
                      {r.addressSnapshot || '—'}
                    </td>
                    <td className="px-5 py-3.5 whitespace-nowrap">
                      <Badge
                        variant={
                          r.status === 'ACTIVE'
                            ? 'success'
                            : r.status === 'PENDING_VERIFY'
                            ? 'warning'
                            : r.status === 'HIDDEN'
                            ? 'danger'
                            : 'default'
                        }
                      >
                        {r.status}
                      </Badge>
                    </td>
                    <td className="px-5 py-3.5 text-right whitespace-nowrap">
                      <Link to={`/reports/${r.publicId}`}>
                        <Button variant="ghost" size="sm">
                          Xem báo cáo
                        </Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Lock/Unlock Reason Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={targetStatus === 'LOCKED' ? 'Khóa tài khoản người dùng' : 'Mở khóa tài khoản'}
        description={`Xác nhận thay đổi trạng thái tài khoản @${user.username}. Hành động này sẽ được ghi vào nhật ký kiểm toán.`}
      >
        <form onSubmit={handleSubmitLockStatus} className="space-y-4">
          {targetStatus === 'LOCKED' && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2 text-rose-700 text-xs">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>
                Khi tài khoản bị khóa, người dùng sẽ bị từ chối đăng nhập và không thể tạo báo cáo hoặc bình luận mới.
              </span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Lý do thực hiện (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={`Nhập lý do ${targetStatus === 'LOCKED' ? 'khóa' : 'mở khóa'} tài khoản này...`}
              rows={3}
              required
              className="w-full px-3.5 py-2 text-sm border border-slate-300 rounded-xl focus:outline-none focus:border-brand-500"
            />
          </div>

          <div className="flex items-center justify-end space-x-3 pt-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setIsModalOpen(false)}
              disabled={isSubmitting}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              variant={targetStatus === 'LOCKED' ? 'danger' : 'primary'}
              size="sm"
              isLoading={isSubmitting}
            >
              {targetStatus === 'LOCKED' ? 'Xác nhận khóa' : 'Xác nhận mở khóa'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
