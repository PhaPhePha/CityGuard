import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { userService } from '@/services/userService';
import { AdminUserListItem, UserFilterParams, UserRole, UserStatus } from '@/types/user.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import { useToast } from '@/context/ToastContext';
import { Search, Lock, Unlock, ExternalLink, Filter, RotateCw } from 'lucide-react';

export const UserListPage: React.FC = () => {
  const { success, error } = useToast();
  const [users, setUsers] = useState<AdminUserListItem[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [filters, setFilters] = useState<UserFilterParams>({
    page: 0,
    size: 10,
    search: '',
    role: undefined,
    status: undefined
  });

  // Modal Lock/Unlock state
  const [selectedUser, setSelectedUser] = useState<AdminUserListItem | null>(null);
  const [reason, setReason] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await userService.getUsers(filters);
      setUsers(data.items);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Lỗi khi tải danh sách người dùng');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [filters.page, filters.role, filters.status]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((prev) => ({ ...prev, page: 0 }));
    loadUsers();
  };

  const handleOpenLockModal = (user: AdminUserListItem) => {
    setSelectedUser(user);
    setReason('');
    setIsModalOpen(true);
  };

  const handleConfirmToggleStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser) return;
    if (!reason.trim()) {
      error('Vui lòng nhập lý do thực hiện thao tác.');
      return;
    }

    const nextStatus: UserStatus = selectedUser.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
    setIsSubmitting(true);
    try {
      await userService.updateUserStatus(selectedUser.id, {
        status: nextStatus,
        reason: reason.trim()
      });
      setIsModalOpen(false);
      success(
        `Đã ${nextStatus === 'LOCKED' ? 'khóa' : 'mở khóa'} tài khoản @${selectedUser.username} thành công!`
      );
      loadUsers();
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Không thể cập nhật trạng thái');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Quản Lý Tài Khoản Người Dùng</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Chỉ dành riêng cho Quản trị viên (ADMIN) – Theo dõi và khóa/mở khóa tài khoản vi phạm
          </p>
        </div>
        <Button onClick={loadUsers} variant="outline" size="sm">
          <RotateCw className="w-4 h-4 mr-1.5" /> Tải lại
        </Button>
      </div>

      {/* Filter toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col lg:flex-row gap-3 items-center justify-between">
        <form onSubmit={handleSearchSubmit} className="relative w-full lg:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Tìm theo username, email, tên..."
            value={filters.search || ''}
            onChange={(e) => setFilters((prev) => ({ ...prev, search: e.target.value }))}
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
          />
        </form>

        <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
          {/* Role Filter */}
          <div className="flex items-center space-x-1.5">
            <span className="text-xs text-slate-500 font-medium">Vai trò:</span>
            <select
              value={filters.role || ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  role: (e.target.value as UserRole) || undefined,
                  page: 0
                }))
              }
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
            >
              <option value="">Tất cả vai trò</option>
              <option value="ADMIN">ADMIN (Quản trị viên)</option>
              <option value="MODERATOR">MODERATOR (Kiểm duyệt viên)</option>
              <option value="USER">USER (Người dùng)</option>
            </select>
          </div>

          {/* Status Filter */}
          <div className="flex items-center space-x-1.5">
            <Filter className="w-4 h-4 text-slate-400" />
            <span className="text-xs text-slate-500 font-medium">Trạng thái:</span>
            <select
              value={filters.status || ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  status: (e.target.value as UserStatus) || undefined,
                  page: 0
                }))
              }
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">ACTIVE (Hoạt động)</option>
              <option value="LOCKED">LOCKED (Đã khóa)</option>
            </select>
          </div>
        </div>
      </div>

      {/* User Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <LoadingSpinner message="Đang tải danh sách người dùng..." />
        ) : users.length === 0 ? (
          <EmptyState
            title="Không tìm thấy người dùng"
            description="Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm."
          />
        ) : (
          <div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                  <tr>
                    <th className="px-5 py-3.5">ID</th>
                    <th className="px-5 py-3.5">Người dùng</th>
                    <th className="px-5 py-3.5">Email</th>
                    <th className="px-5 py-3.5">Vai trò (Role)</th>
                    <th className="px-5 py-3.5">Trạng thái</th>
                    <th className="px-5 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {users.map((u) => (
                    <tr key={u.id} className="hover:bg-slate-50/80 transition">
                      <td className="px-5 py-3.5 font-mono text-xs font-semibold text-slate-500">
                        #{u.id}
                      </td>
                      <td className="px-5 py-3.5">
                        <Link
                          to={`/users/${u.id}`}
                          className="group block hover:text-brand-600 transition"
                        >
                          <p className="font-semibold text-slate-800 group-hover:text-brand-600 flex items-center space-x-1">
                            <span>{u.displayName}</span>
                            <ExternalLink className="w-3 h-3 opacity-0 group-hover:opacity-100 transition" />
                          </p>
                          <p className="text-xs text-slate-400">@{u.username}</p>
                        </Link>
                      </td>
                      <td className="px-5 py-3.5 text-slate-500 text-xs">{u.email}</td>
                      <td className="px-5 py-3.5">
                        <Badge
                          variant={
                            u.role === 'ADMIN'
                              ? 'info'
                              : u.role === 'MODERATOR'
                              ? 'warning'
                              : 'default'
                          }
                        >
                          {u.role}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge variant={u.status === 'ACTIVE' ? 'success' : 'danger'}>
                          {u.status}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end space-x-2">
                          <Link to={`/users/${u.id}`}>
                            <Button variant="ghost" size="sm">
                              Chi tiết
                            </Button>
                          </Link>
                          <Button
                            variant={u.status === 'ACTIVE' ? 'outline' : 'primary'}
                            size="sm"
                            onClick={() => handleOpenLockModal(u)}
                          >
                            {u.status === 'ACTIVE' ? (
                              <>
                                <Lock className="w-3.5 h-3.5 mr-1 text-rose-500" /> Khóa
                              </>
                            ) : (
                              <>
                                <Unlock className="w-3.5 h-3.5 mr-1" /> Mở khóa
                              </>
                            )}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              currentPage={filters.page || 0}
              totalPages={totalPages}
              totalItems={totalItems}
              pageSize={filters.size || 10}
              onPageChange={(page) => setFilters((prev) => ({ ...prev, page }))}
            />
          </div>
        )}
      </div>

      {/* Lock/Unlock Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={
          selectedUser?.status === 'ACTIVE'
            ? `Khóa tài khoản @${selectedUser?.username}`
            : `Mở khóa tài khoản @${selectedUser?.username}`
        }
        description="Thao tác này sẽ cập nhật trạng thái người dùng trong cơ sở dữ liệu và ghi nhật ký kiểm toán."
      >
        <form onSubmit={handleConfirmToggleStatus} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Lý do thực hiện (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do khóa hoặc mở khóa tài khoản này..."
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
              variant={selectedUser?.status === 'ACTIVE' ? 'danger' : 'primary'}
              size="sm"
              isLoading={isSubmitting}
            >
              Xác nhận
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
