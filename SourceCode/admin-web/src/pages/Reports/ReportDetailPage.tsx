import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { reportService } from '@/services/reportService';
import { AdminReportDetail, ReportStatus } from '@/types/report.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Modal } from '@/components/common/Modal';
import { useToast } from '@/context/ToastContext';
import {
  ArrowLeft,
  MapPin,
  Clock,
  User,
  ShieldCheck,
  ThumbsUp,
  ThumbsDown,
  ExternalLink,
  Copy,
  Check,
  EyeOff,
  Sliders,
  AlertTriangle,
  Calendar,
  RotateCw
} from 'lucide-react';

export const ReportDetailPage: React.FC = () => {
  const { publicId } = useParams<{ publicId: string }>();
  const navigate = useNavigate();
  const { success, error } = useToast();

  const [report, setReport] = useState<AdminReportDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  // Moderation Modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newStatus, setNewStatus] = useState<ReportStatus>('ACTIVE');
  const [reason, setReason] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadReport = async () => {
    if (!publicId) return;
    setIsLoading(true);
    try {
      const data = await reportService.getReportDetail(publicId);
      setReport(data);
      setNewStatus(data.status);
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Không thể tải chi tiết báo cáo');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadReport();
  }, [publicId]);

  const handleCopyPublicId = () => {
    if (!report?.publicId) return;
    navigator.clipboard.writeText(report.publicId);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

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
    return [{ value: 'HIDDEN', label: 'HIDDEN (Ẩn vi phạm / spam)' }];
  };

  const handleOpenStatusModal = (statusToSet?: ReportStatus) => {
    if (!report) return;
    const allowed = getAllowedTargetStatuses(report.status);
    if (statusToSet && allowed.some((a) => a.value === statusToSet)) {
      setNewStatus(statusToSet);
    } else {
      setNewStatus(allowed[0]?.value || report.status);
    }
    setReason('');
    setIsModalOpen(true);
  };

  const handleSubmitStatus = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!publicId || !report) return;

    if (!reason.trim()) {
      error('Vui lòng nhập lý do kiểm duyệt hoặc thay đổi trạng thái.');
      return;
    }

    setIsSubmitting(true);
    try {
      const updated = await reportService.updateReportStatus(publicId, {
        status: newStatus,
        reason: reason.trim()
      });
      setReport(updated);
      setIsModalOpen(false);
      success(
        `Đã cập nhật trạng thái báo cáo thành "${newStatus}" thành công!`
      );
    } catch (err: any) {
      error(err.response?.data?.message || err.message || 'Cập nhật trạng thái thất bại');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return <LoadingSpinner message="Đang tải thông tin chi tiết sự cố..." />;
  }

  if (!report) {
    return (
      <div className="space-y-4">
        <Button variant="outline" size="sm" onClick={() => navigate('/reports')}>
          <ArrowLeft className="w-4 h-4 mr-1.5" /> Quay lại danh sách
        </Button>
        <EmptyState
          title="Không tìm thấy báo cáo"
          description={`Không tồn tại sự cố với định danh công khai ${publicId}`}
        />
      </div>
    );
  }

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
      {/* Top Bar Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="sm" onClick={() => navigate('/reports')}>
            <ArrowLeft className="w-4 h-4 mr-1.5" /> Danh sách báo cáo
          </Button>
          <div className="h-4 w-px bg-slate-200" />
          <span className="text-xs text-slate-400 font-mono">public_id:</span>
          <span className="font-mono text-xs font-semibold text-slate-800 bg-slate-100 px-2 py-1 rounded">
            {report.publicId}
          </span>
          <button
            onClick={handleCopyPublicId}
            className="text-slate-400 hover:text-slate-600 transition"
            title="Sao chép public_id"
          >
            {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
          </button>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center space-x-2.5">
          {report.status === 'HIDDEN' ? (
            <Button
              variant="outline"
              size="sm"
              onClick={() => handleOpenStatusModal('ACTIVE')}
            >
              <RotateCw className="w-4 h-4 mr-1.5 text-emerald-600" /> Khôi phục hiển thị
            </Button>
          ) : (
            <Button
              variant="danger"
              size="sm"
              onClick={() => handleOpenStatusModal('HIDDEN')}
            >
              <EyeOff className="w-4 h-4 mr-1.5" /> Ẩn báo cáo
            </Button>
          )}
          <Button
            variant="primary"
            size="sm"
            onClick={() => handleOpenStatusModal()}
          >
            <Sliders className="w-4 h-4 mr-1.5" /> Chuyển trạng thái
          </Button>
        </div>
      </div>

      {/* Main Content Details */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (2 Cols) - Report Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Main Card */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
            {/* Header / Badges */}
            <div className="p-6 border-b border-slate-100 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center space-x-2.5">
                <span
                  className="w-3 h-3 rounded-full flex-shrink-0"
                  style={{ backgroundColor: report.categoryColor || '#3B82F6' }}
                />
                <span className="text-base font-bold text-slate-800">
                  {report.categoryDisplayName}
                </span>
                <span className="text-xs text-slate-400 font-mono">({report.categoryCode})</span>
              </div>
              <Badge variant={getStatusBadgeVariant(report.status)} size="md">
                {report.status}
              </Badge>
            </div>

            {/* Image Preview */}
            {report.imageUrl ? (
              <div className="relative bg-slate-900 border-b border-slate-100 flex items-center justify-center max-h-96 overflow-hidden">
                <img
                  src={report.imageUrl}
                  alt={report.categoryDisplayName}
                  className="w-full h-auto max-h-96 object-contain"
                />
              </div>
            ) : (
              <div className="p-8 text-center bg-slate-50 border-b border-slate-100 text-slate-400 text-sm">
                Không có hình ảnh đính kèm
              </div>
            )}

            {/* Description */}
            <div className="p-6 space-y-4">
              <div>
                <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                  Mô tả sự cố
                </h4>
                <p className="text-slate-800 text-base leading-relaxed bg-slate-50/70 p-4 rounded-xl border border-slate-100">
                  {report.description || 'Không có mô tả chi tiết.'}
                </p>
              </div>

              {/* Location details */}
              <div className="pt-2">
                <h4 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                  Vị trí phản ánh
                </h4>
                <div className="flex items-start space-x-3 p-4 bg-sky-50/60 rounded-xl border border-sky-100">
                  <MapPin className="w-5 h-5 text-sky-600 flex-shrink-0 mt-0.5" />
                  <div className="flex-1">
                    <p className="text-sm font-semibold text-slate-800">
                      {report.addressSnapshot || 'Chưa cập nhật địa chỉ'}
                    </p>
                    <p className="text-xs text-slate-500 font-mono mt-1">
                      Tọa độ: {report.latitude}, {report.longitude}
                    </p>
                  </div>
                  <a
                    href={`https://www.google.com/maps?q=${report.latitude},${report.longitude}`}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center text-xs font-medium text-sky-700 hover:text-sky-800 bg-white px-2.5 py-1.5 rounded-lg border border-sky-200 shadow-sm"
                  >
                    Google Maps <ExternalLink className="w-3.5 h-3.5 ml-1" />
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column (1 Col) - Metadata & Verification */}
        <div className="space-y-6">
          {/* Trust & Community Verification */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-5">
            <h3 className="text-sm font-bold text-slate-800 flex items-center">
              <ShieldCheck className="w-4 h-4 text-brand-600 mr-2" />
              Độ tin cậy & Bình chọn
            </h3>

            {/* Trust score visual */}
            <div>
              <div className="flex items-center justify-between text-sm mb-1.5">
                <span className="text-slate-500">Điểm tin cậy (Trust score):</span>
                {typeof report.trustScore === 'number' && report.trustScore !== null ? (
                  <span className="font-bold text-slate-800">{report.trustScore}/100</span>
                ) : (
                  <span className="text-xs font-medium text-slate-400 italic">Chưa có đánh giá</span>
                )}
              </div>

              {typeof report.trustScore === 'number' && report.trustScore !== null && (
                <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden my-2">
                  <div
                    className="bg-brand-600 h-2.5 rounded-full transition-all duration-500"
                    style={{ width: `${Math.min(100, Math.max(0, report.trustScore))}%` }}
                  />
                </div>
              )}

              <div className="flex items-center justify-between text-xs text-slate-400 mt-1">
                <span>Nhãn tin cậy:</span>
                <span className="font-semibold text-brand-600">
                  {report.trustLabel || 'Đang xác minh'}
                </span>
              </div>
            </div>

            {/* Votes breakdown */}
            <div className="grid grid-cols-2 gap-3 pt-2">
              <div className="p-3 bg-emerald-50 rounded-xl border border-emerald-100 text-center">
                <div className="flex items-center justify-center space-x-1.5 text-emerald-700 mb-1">
                  <ThumbsUp className="w-4 h-4" />
                  <span className="text-xs font-semibold">Xác nhận</span>
                </div>
                <div className="text-xl font-bold text-emerald-800">{report.confirmCount}</div>
                <div className="text-[11px] text-emerald-600">người xác nhận</div>
              </div>

              <div className="p-3 bg-rose-50 rounded-xl border border-rose-100 text-center">
                <div className="flex items-center justify-center space-x-1.5 text-rose-700 mb-1">
                  <ThumbsDown className="w-4 h-4" />
                  <span className="text-xs font-semibold">Đã hết</span>
                </div>
                <div className="text-xl font-bold text-rose-800">{report.noLongerCount}</div>
                <div className="text-[11px] text-rose-600">báo cáo đã hết</div>
              </div>
            </div>
          </div>

          {/* Reporter details */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-800 flex items-center">
              <User className="w-4 h-4 text-slate-600 mr-2" />
              Người tạo báo cáo
            </h3>

            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 font-semibold text-sm">
                {report.reporterDisplayName?.[0] || 'U'}
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-800">
                  {report.reporterDisplayName}
                </p>
                <p className="text-xs text-slate-400">@{report.reporterUsername}</p>
              </div>
            </div>

            <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
              <span>Mã người dùng (ID):</span>
              <Link
                to={`/users/${report.reporterId}`}
                className="font-mono text-brand-600 hover:text-brand-700 font-semibold"
              >
                #{report.reporterId}
              </Link>
            </div>
          </div>

          {/* Timestamps */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-3">
            <h3 className="text-sm font-bold text-slate-800 flex items-center">
              <Clock className="w-4 h-4 text-slate-600 mr-2" />
              Thời gian
            </h3>

            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between text-slate-500">
                <span className="flex items-center">
                  <Calendar className="w-3.5 h-3.5 mr-1 text-slate-400" /> Ngày tạo:
                </span>
                <span className="font-medium text-slate-700">
                  {new Date(report.createdAt).toLocaleString('vi-VN')}
                </span>
              </div>

              {report.expiresAt && (
                <div className="flex items-center justify-between text-slate-500">
                  <span className="flex items-center">
                    <Clock className="w-3.5 h-3.5 mr-1 text-amber-500" /> Tự động đóng:
                  </span>
                  <span className="font-medium text-slate-700">
                    {new Date(report.expiresAt).toLocaleString('vi-VN')}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Status Update / Moderation Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Kiểm duyệt & Chuyển trạng thái sự cố"
        description={`Cập nhật trạng thái cho báo cáo ${report.publicId}. Mọi thao tác đều được tự động lưu vào Audit Logs.`}
      >
        <form onSubmit={handleSubmitStatus} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Trạng thái sự cố chuyển đổi
            </label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as ReportStatus)}
              className="w-full px-3.5 py-2.5 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
            >
              {getAllowedTargetStatuses(report?.status).map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {newStatus === 'HIDDEN' && (
            <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl flex items-start space-x-2 text-rose-700 text-xs">
              <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>
                Cảnh báo: Khi chuyển sang <strong>HIDDEN</strong>, sự cố này sẽ không còn xuất hiện trên ứng dụng người dùng.
              </span>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-slate-700 uppercase mb-1.5">
              Lý do kiểm duyệt (Bắt buộc) <span className="text-rose-500">*</span>
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do thực hiện thao tác kiểm duyệt này..."
              rows={3}
              required
              className="w-full px-3.5 py-2.5 text-sm border border-slate-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-brand-500/20 focus:border-brand-500"
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
              Xác nhận thay đổi
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
