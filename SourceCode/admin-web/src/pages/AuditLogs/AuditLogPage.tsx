import React, { useEffect, useState } from 'react';
import { logService } from '@/services/logService';
import { AdminLogListItem, LogFilterParams } from '@/types/log.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { EmptyState } from '@/components/common/EmptyState';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import { Pagination } from '@/components/common/Pagination';
import { History, RotateCw, Filter } from 'lucide-react';
import { isAxiosError } from 'axios';

export const AuditLogPage: React.FC = () => {
  const [logs, setLogs] = useState<AdminLogListItem[]>([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [filters, setFilters] = useState<LogFilterParams>({
    page: 0,
    size: 15,
    targetType: undefined,
    action: undefined
  });

  const loadLogs = async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      const data = await logService.getLogs(filters);
      setLogs(data.items);
      setTotalItems(data.totalItems);
      setTotalPages(data.totalPages);
    } catch (err: unknown) {
      const fallback = 'Không thể tải nhật ký quản trị. Vui lòng thử lại.';
      const message = isAxiosError<{ message?: string }>(err)
          ? err.response?.data?.message || fallback
          : fallback;

      setLoadError(message);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [filters.page, filters.targetType, filters.action]);

  const getActionBadgeVariant = (action: string) => {
    if (action.includes('HIDE') || action.includes('LOCK')) {
      return 'danger';
    }
    if (action.includes('RESTORE') || action.includes('UNLOCK')) {
      return 'success';
    }
    return 'warning';
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Nhật Ký Quản Trị Hệ Thống (Audit Logs)</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Ghi nhận toàn bộ thao tác nhạy cảm (Khóa/Mở User, Ẩn/Khôi phục Report & Comment) của Admin/Moderator
          </p>
        </div>
        <Button onClick={loadLogs} variant="outline" size="sm">
          <RotateCw className="w-4 h-4 mr-1.5" /> Làm mới
        </Button>
      </div>

      {/* Filter toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-sm flex flex-wrap items-center gap-4">
        <div className="flex items-center space-x-2">
          <Filter className="w-4 h-4 text-slate-400" />
          <span className="text-xs font-semibold text-slate-500 uppercase">Đối tượng:</span>
          <select
            value={filters.targetType || ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                targetType: (e.target.value as any) || undefined,
                page: 0
              }))
            }
            className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
          >
            <option value="">Tất cả đối tượng</option>
            <option value="REPORT">REPORT (Sự cố)</option>
            <option value="USER">USER (Người dùng)</option>
            <option value="COMMENT">COMMENT (Bình luận)</option>
            <option value="CATEGORY">CATEGORY (Danh mục)</option>
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-xs font-semibold text-slate-500 uppercase">Hành động:</span>
          <select
            value={filters.action || ''}
            onChange={(e) =>
              setFilters((prev) => ({
                ...prev,
                action: e.target.value || undefined,
                page: 0
              }))
            }
            className="text-sm border border-slate-300 rounded-lg px-3 py-1.5 focus:outline-none focus:border-brand-500"
          >
            <option value="">Tất cả hành động</option>
            <option value="LOCK_USER">LOCK_USER (Khóa user)</option>
            <option value="UNLOCK_USER">UNLOCK_USER (Mở khóa user)</option>
            <option value="HIDE_REPORT">HIDE_REPORT (Ẩn sự cố)</option>
            <option value="UPDATE_REPORT_STATUS">UPDATE_REPORT_STATUS (Đổi trạng thái sự cố)</option>
            <option value="HIDE_COMMENT">HIDE_COMMENT (Ẩn bình luận)</option>
            <option value="RESTORE_COMMENT">RESTORE_COMMENT (Khôi phục bình luận)</option>
          </select>
        </div>
      </div>

      {/* Log Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        {isLoading ? (
          <LoadingSpinner message="Đang tải nhật ký kiểm duyệt..." />
        ) : loadError ? (
          <div role="alert" className="px-6 py-5 text-sm text-rose-700">
            {loadError}
          </div>
        ) : logs.length === 0 ? (
          <EmptyState title="Chưa có nhật ký nào" icon={<History className="w-8 h-8" />} />
        ) : (
          <div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-600">
                <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                  <tr>
                    <th className="px-5 py-3.5">ID Log</th>
                    <th className="px-5 py-3.5">Người thực hiện</th>
                    <th className="px-5 py-3.5">Hành động</th>
                    <th className="px-5 py-3.5">Đối tượng tác động</th>
                    <th className="px-5 py-3.5">Lý do kiểm duyệt</th>
                    <th className="px-5 py-3.5 text-right">Thời gian</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {logs.map((log) => (
                    <tr key={log.id} className="hover:bg-slate-50/80 transition">
                      <td className="px-5 py-3.5 font-mono text-xs text-slate-400">#{log.id}</td>
                      <td className="px-5 py-3.5">
                        <p className="font-semibold text-slate-800">{log.actorDisplayName}</p>
                        <p className="text-xs text-slate-400">@{log.actorUsername}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <Badge variant={getActionBadgeVariant(log.action)}>{log.action}</Badge>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="font-semibold text-slate-700">{log.targetType}</span>
                        <span className="text-xs text-slate-400 ml-1.5 font-mono">
                          ID: {log.targetId}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-slate-700 max-w-sm truncate">
                        {log.reason || '—'}
                      </td>
                      <td className="px-5 py-3.5 text-right text-xs text-slate-500 whitespace-nowrap">
                        {new Date(log.createdAt).toLocaleString('vi-VN')}
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
              pageSize={filters.size || 15}
              onPageChange={(page) => setFilters((prev) => ({ ...prev, page }))}
            />
          </div>
        )}
      </div>
    </div>
  );
};
