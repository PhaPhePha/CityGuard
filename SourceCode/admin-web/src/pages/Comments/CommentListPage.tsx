import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { commentService } from '@/services/commentService';
import { AdminCommentListItem, CommentFilterParams } from '@/types/comment.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import { useToast } from '@/context/ToastContext';
import { Eye, EyeOff, MessageSquare, ExternalLink, RotateCw, Search } from 'lucide-react';

export const CommentListPage: React.FC = () => {
  const { success, error } = useToast();
  const [comments, setComments] = useState<AdminCommentListItem[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [filters, setFilters] = useState<CommentFilterParams>({
    page: 0,
    size: 10,
    isHidden: undefined,
    reportPublicId: ''
  });

  // Modal State for Comment Moderation
  const [selectedComment, setSelectedComment] = useState<AdminCommentListItem | null>(null);
  const [reason, setReason] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadComments = async () => {
    setIsLoading(true);
    try {
      const data = await commentService.getComments(filters);
      setComments(data.items);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Lỗi khi tải bình luận');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadComments();
  }, [filters.page, filters.isHidden]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((prev) => ({ ...prev, page: 0 }));
    loadComments();
  };

  const handleOpenModerateModal = (comment: AdminCommentListItem) => {
    setSelectedComment(comment);
    setReason('');
    setIsModalOpen(true);
  };

  const handleConfirmToggleHide = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedComment) return;

    if (!reason.trim()) {
      error('Vui lòng nhập lý do kiểm duyệt bình luận.');
      return;
    }

    const nextHidden = !selectedComment.isHidden;
    setIsSubmitting(true);
    try {
      await commentService.updateCommentStatus(selectedComment.id, {
        isHidden: nextHidden,
        reason: reason.trim()
      });
      setIsModalOpen(false);
      success(
        `Đã ${nextHidden ? 'ẩn' : 'khôi phục'} bình luận của @${selectedComment.username} thành công!`
      );
      loadComments();
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Không thể cập nhật trạng thái bình luận');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Quản Lý & Kiểm Duyệt Bình Luận</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Kiểm duyệt nội dung bình luận vi phạm mà không xóa vật lý dữ liệu (chỉ đổi cờ is_hidden)
          </p>
        </div>
        <Button onClick={loadComments} variant="outline" size="sm">
          <RotateCw className="w-4 h-4 mr-1.5" /> Tải lại
        </Button>
      </div>

      {/* Filter toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col sm:flex-row gap-3 items-center justify-between">
        <form onSubmit={handleSearchSubmit} className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Lọc theo Report public_id..."
            value={filters.reportPublicId || ''}
            onChange={(e) => setFilters((prev) => ({ ...prev, reportPublicId: e.target.value }))}
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
          />
        </form>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          <span className="text-xs font-semibold text-slate-500 uppercase">Bộ lọc:</span>
          <select
            value={filters.isHidden === undefined ? '' : String(filters.isHidden)}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                isHidden: e.target.value === '' ? undefined : e.target.value === 'true',
                page: 0
              }))
            }
            className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
          >
            <option value="">Tất cả bình luận</option>
            <option value="false">Đang hiển thị bình thường</option>
            <option value="true">Đã bị ẩn kiểm duyệt (is_hidden = true)</option>
          </select>
        </div>
      </div>

      {/* Comment Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <LoadingSpinner message="Đang tải danh sách bình luận..." />
        ) : comments.length === 0 ? (
          <EmptyState
            title="Không có bình luận nào"
            description="Không tìm thấy bình luận phù hợp với điều kiện tìm kiếm."
            icon={<MessageSquare className="w-8 h-8" />}
          />
        ) : (
          <div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                  <tr>
                    <th className="px-5 py-3.5">ID</th>
                    <th className="px-5 py-3.5">Người viết</th>
                    <th className="px-5 py-3.5">Báo cáo (Public ID)</th>
                    <th className="px-5 py-3.5">Nội dung bình luận</th>
                    <th className="px-5 py-3.5">Trạng thái</th>
                    <th className="px-5 py-3.5">Thời gian</th>
                    <th className="px-5 py-3.5 text-right">Kiểm duyệt</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {comments.map((c) => (
                    <tr
                      key={c.id}
                      className={`hover:bg-slate-50/80 transition ${
                        c.isHidden ? 'bg-rose-50/20' : ''
                      }`}
                    >
                      <td className="px-5 py-3.5 font-mono text-xs text-slate-400">#{c.id}</td>
                      <td className="px-5 py-3.5">
                        <p className="font-semibold text-slate-800">{c.userDisplayName}</p>
                        <p className="text-xs text-slate-400">@{c.username}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <Link
                          to={`/reports/${c.reportPublicId}`}
                          className="font-mono text-xs font-semibold text-brand-600 hover:text-brand-700 flex items-center space-x-1"
                        >
                          <span>{c.reportPublicId}</span>
                          <ExternalLink className="w-3 h-3 text-brand-400" />
                        </Link>
                      </td>
                      <td className="px-5 py-3.5 max-w-sm text-slate-700">
                        <p className="line-clamp-2">{c.body}</p>
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        <Badge variant={c.isHidden ? 'danger' : 'success'}>
                          {c.isHidden ? 'Đã bị ẩn' : 'Hiển thị'}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap text-xs text-slate-400">
                        {new Date(c.createdAt).toLocaleDateString('vi-VN')}
                      </td>
                      <td className="px-5 py-3.5 text-right whitespace-nowrap">
                        <Button
                          variant={c.isHidden ? 'outline' : 'danger'}
                          size="sm"
                          onClick={() => handleOpenModerateModal(c)}
                        >
                          {c.isHidden ? (
                            <>
                              <Eye className="w-3.5 h-3.5 mr-1 text-emerald-600" /> Khôi phục
                            </>
                          ) : (
                            <>
                              <EyeOff className="w-3.5 h-3.5 mr-1" /> Ẩn bình luận
                            </>
                          )}
                        </Button>
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

      {/* Moderate Comment Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={
          selectedComment?.isHidden ? 'Khôi phục hiển thị bình luận' : 'Ẩn bình luận vi phạm'
        }
        description="Thao tác này chỉ thay đổi cờ is_hidden, không xóa vật lý dữ liệu của người dùng."
      >
        <form onSubmit={handleConfirmToggleHide} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Lý do kiểm duyệt (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={`Nhập lý do ${selectedComment?.isHidden ? 'khôi phục' : 'ẩn'} bình luận...`}
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
              variant={selectedComment?.isHidden ? 'primary' : 'danger'}
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
