import React, { useEffect, useState } from 'react';
import { reportService } from '@/services/reportService';
import { categoryService } from '@/services/categoryService';
import { AdminReportSummary, ReportFilterParams, ReportStatus } from '@/types/report.types';
import { AdminCategory } from '@/types/category.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { Pagination } from '@/components/common/Pagination';
import { useToast } from '@/context/ToastContext';
import { Search, Filter, RotateCw, ExternalLink, EyeOff, Sliders } from 'lucide-react';
import { Link } from 'react-router-dom';

export const ReportListPage: React.FC = () => {
  const { success, error } = useToast();
  const [reports, setReports] = useState<AdminReportSummary[]>([]);
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [filters, setFilters] = useState<ReportFilterParams>({
    page: 0,
    size: 10,
    keyword: '',
    categoryId: undefined,
    status: undefined
  });

  // Moderation Modal state for quick table action
  const [selectedReport, setSelectedReport] = useState<AdminReportSummary | null>(null);
  const [newStatus, setNewStatus] = useState<ReportStatus>('ACTIVE');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Load categories from service (avoids hardcoding)
  useEffect(() => {
    categoryService
      .getCategories()
      .then((data) => setCategories(data))
      .catch((err) => console.error('Lỗi nạp danh mục:', err));
  }, []);

  const loadReports = async () => {
    setIsLoading(true);
    try {
      const data = await reportService.getReports(filters);
      setReports(data.items);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Lỗi khi tải danh sách báo cáo');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReports();
  }, [filters.page, filters.status, filters.categoryId]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters((prev) => ({ ...prev, page: 0 }));
    loadReports();
  };

  // Determine allowed status transitions for the modal
  const getAllowedTargetStatuses = (current?: ReportStatus): { value: ReportStatus; label: string }[] => {
    if (!current) {
      return [{ value: 'ACTIVE', label: 'ACTIVE (Đang hoạt động)' }];
    }
    if (current === 'HIDDEN') {
      return [{ value: 'ACTIVE', label: 'ACTIVE (Khôi phục hiển thị)' }];
    }
    if (current === 'ACTIVE' || current === 'PENDING_VERIFY') {
      return [
        { value: 'HIDDEN', label: 'HIDDEN (Ẩn vi phạm / spam)' },
        { value: 'RESOLVED', label: 'RESOLVED (Đã xử lý / giải quyết)' }
      ];
    }
    // For EXPIRED or RESOLVED
    return [{ value: 'HIDDEN', label: 'HIDDEN (Ẩn vi phạm / spam)' }];
  };

  const handleOpenModal = (report: AdminReportSummary, initialStatus?: ReportStatus) => {
    setSelectedReport(report);
    const allowed = getAllowedTargetStatuses(report.status);
    if (initialStatus && allowed.some((a) => a.value === initialStatus)) {
      setNewStatus(initialStatus);
    } else {
      setNewStatus(allowed[0]?.value || report.status);
    }
    setReason('');
    setIsModalOpen(true);
  };

  const handleSubmitStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedReport) return;
    if (!reason.trim()) {
      error('Vui lòng nhập lý do kiểm duyệt.');
      return;
    }

    setIsSubmitting(true);
    try {
      await reportService.updateReportStatus(selectedReport.publicId, {
        status: newStatus,
        reason: reason.trim()
      });
      setIsModalOpen(false);
      success(`Cập nhật trạng thái báo cáo ${selectedReport.publicId} thành công!`);
      loadReports();
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Cập nhật trạng thái thất bại');
    } finally {
      setIsSubmitting(false);
    }
  };

  const getStatusBadgeVariant = (st: ReportStatus) => {
    switch (st) {
      case 'ACTIVE':
        return 'success';
      case 'PENDING_VERIFY':
        return 'warning';
      case 'RESOLVED':
        return 'info';
      case 'EXPIRED':
        return 'default';
      case 'HIDDEN':
        return 'danger';
      default:
        return 'default';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Kiểm Duyệt Báo Cáo Sự Cố</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Quản lý, tìm kiếm và chuyển đổi trạng thái báo cáo bằng định danh công khai (publicId)
          </p>
        </div>
        <Button onClick={loadReports} variant="outline" size="sm">
          <RotateCw className="w-4 h-4 mr-1.5" /> Tải lại
        </Button>
      </div>

      {/* Filter toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-col lg:flex-row gap-3 items-center justify-between">
        <form onSubmit={handleSearchSubmit} className="relative w-full lg:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-3" />
          <input
            type="text"
            placeholder="Tìm theo mô tả, public_id..."
            value={filters.keyword || ''}
            onChange={(e) => setFilters((prev) => ({ ...prev, keyword: e.target.value }))}
            className="w-full pl-9 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
          />
        </form>

        <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
          {/* Dynamic Category Filter from Service */}
          <div className="flex items-center space-x-1.5">
            <span className="text-xs text-slate-500 font-medium">Danh mục:</span>
            <select
              value={filters.categoryId || ''}
              onChange={(e) =>
                setFilters((prev) => ({
                  ...prev,
                  categoryId: e.target.value ? Number(e.target.value) : undefined,
                  page: 0
                }))
              }
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
            >
              <option value="">Tất cả danh mục ({categories.length})</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.code})
                </option>
              ))}
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
                  status: (e.target.value as any) || undefined,
                  page: 0
                }))
              }
              className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PENDING_VERIFY">PENDING_VERIFY (Chờ xác minh)</option>
              <option value="ACTIVE">ACTIVE (Đang hoạt động)</option>
              <option value="RESOLVED">RESOLVED (Đã giải quyết)</option>
              <option value="EXPIRED">EXPIRED (Hết hạn)</option>
              <option value="HIDDEN">HIDDEN (Đã ẩn)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Report Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <LoadingSpinner message="Đang tải danh sách sự cố..." />
        ) : reports.length === 0 ? (
          <EmptyState
            title="Không tìm thấy báo cáo"
            description="Thử thay đổi từ khóa hoặc điều kiện lọc để hiển thị kết quả."
          />
        ) : (
          <div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                  <tr>
                    <th className="px-5 py-3.5">Mã Public ID</th>
                    <th className="px-5 py-3.5">Danh mục</th>
                    <th className="px-5 py-3.5">Mô tả sự cố</th>
                    <th className="px-5 py-3.5">Địa điểm</th>
                    <th className="px-5 py-3.5">Độ tin cậy</th>
                    <th className="px-5 py-3.5">Trạng thái</th>
                    <th className="px-5 py-3.5">Thời gian</th>
                    <th className="px-5 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {reports.map((report) => (
                    <tr key={report.publicId} className="hover:bg-slate-50/80 transition">
                      <td className="px-5 py-3.5">
                        <Link
                          to={`/reports/${report.publicId}`}
                          className="font-mono text-xs font-bold text-brand-600 hover:text-brand-700 hover:underline flex items-center space-x-1"
                        >
                          <span>{report.publicId}</span>
                          <ExternalLink className="w-3 h-3 text-brand-400" />
                        </Link>
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        <div className="flex items-center space-x-1.5">
                          <span
                            className="w-2.5 h-2.5 rounded-full flex-shrink-0"
                            style={{ backgroundColor: report.categoryColor || '#3B82F6' }}
                          />
                          <span className="font-medium text-slate-800">
                            {report.categoryDisplayName}
                          </span>
                        </div>
                      </td>
                      <td className="px-5 py-3.5 max-w-xs truncate text-slate-700">
                        {report.description || '—'}
                      </td>
                      <td className="px-5 py-3.5 max-w-[200px] truncate text-slate-500 text-xs">
                        {report.addressSnapshot || '—'}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        {typeof report.trustScore === 'number' && report.trustScore !== null ? (
                          <>
                            <span className="text-xs font-semibold text-slate-700">
                              {report.trustScore}
                            </span>
                            <span className="text-[11px] text-slate-400 ml-1">
                              ({report.trustLabel || 'Đang xác minh'})
                            </span>
                          </>
                        ) : (
                          <span className="text-xs text-slate-400 italic">
                            Chưa có đánh giá ({report.trustLabel || 'Đang xác minh'})
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap">
                        <Badge variant={getStatusBadgeVariant(report.status)}>
                          {report.status}
                        </Badge>
                      </td>
                      <td className="px-5 py-3.5 whitespace-nowrap text-xs text-slate-400">
                        {new Date(report.createdAt).toLocaleDateString('vi-VN')}
                      </td>
                      <td className="px-5 py-3.5 text-right whitespace-nowrap">
                        <div className="flex items-center justify-end space-x-1.5">
                          <Link to={`/reports/${report.publicId}`}>
                            <Button variant="ghost" size="sm">
                              Chi tiết
                            </Button>
                          </Link>
                          {report.status !== 'HIDDEN' && (
                            <button
                              onClick={() => handleOpenModal(report, 'HIDDEN')}
                              className="p-1.5 text-rose-500 hover:bg-rose-50 rounded-lg transition"
                              title="Ẩn vi phạm"
                            >
                              <EyeOff className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            onClick={() => handleOpenModal(report)}
                            className="p-1.5 text-slate-500 hover:bg-slate-100 rounded-lg transition"
                            title="Chuyển trạng thái"
                          >
                            <Sliders className="w-4 h-4" />
                          </button>
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

      {/* Moderation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Kiểm duyệt báo cáo sự cố"
        description={
          selectedReport ? `Cập nhật trạng thái cho ${selectedReport.publicId}` : undefined
        }
      >
        <form onSubmit={handleSubmitStatus} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Trạng thái chuyển đổi cho phép
            </label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as ReportStatus)}
              className="w-full px-3.5 py-2 text-sm border border-slate-300 rounded-xl focus:outline-none focus:border-brand-500"
            >
              {getAllowedTargetStatuses(selectedReport?.status).map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Lý do kiểm duyệt (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do thực hiện kiểm duyệt..."
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
              variant={newStatus === 'HIDDEN' ? 'danger' : 'primary'}
              size="sm"
              isLoading={isSubmitting}
            >
              Lưu thay đổi
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
