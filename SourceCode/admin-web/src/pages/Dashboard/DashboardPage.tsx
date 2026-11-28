import React, { useEffect, useState } from 'react';
import { dashboardService } from '@/services/dashboardService';
import { reportService } from '@/services/reportService';
import { AdminDashboardSummary } from '@/types/dashboard.types';
import { AdminReportSummary } from '@/types/report.types';
import { LoadingSpinner } from '@/components/common/LoadingSpinner';
import { Badge } from '@/components/common/Badge';
import { Button } from '@/components/common/Button';
import {
  Users,
  FileText,
  Activity,
  Clock,
  RotateCw,
  AlertTriangle
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<AdminDashboardSummary | null>(null);
  const [recentReports, setRecentReports] = useState<AdminReportSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sum, rep] = await Promise.all([
        dashboardService.getSummary(),
        reportService.getReports({ size: 5 })
      ]);
      setSummary(sum);
      setRecentReports(rep.items);
    } catch (err: any) {
      setError(err.message || 'Không thể tải dữ liệu thống kê từ máy chủ.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (isLoading) {
    return <LoadingSpinner message="Đang nạp dữ liệu thống kê hệ thống..." size="lg" />;
  }

  if (error) {
    return (
      <div className="p-8 bg-rose-50 border border-rose-200 rounded-xl text-center">
        <AlertTriangle className="w-10 h-10 text-rose-500 mx-auto mb-3" />
        <h3 className="text-base font-bold text-rose-800">Lỗi kết nối API Dashboard</h3>
        <p className="text-sm text-rose-600 mt-1 mb-4">{error}</p>
        <Button onClick={fetchData} variant="outline">
          <RotateCw className="w-4 h-4 mr-2" /> Thử lại
        </Button>
      </div>
    );
  }

  const kpis = [
    {
      label: 'Tổng người dùng',
      value: summary?.totalUsers ?? 0,
      icon: Users,
      color: 'text-indigo-600',
      bgColor: 'bg-indigo-50'
    },
    {
      label: 'Tổng số báo cáo',
      value: summary?.totalReports ?? 0,
      icon: FileText,
      color: 'text-brand-600',
      bgColor: 'bg-brand-50'
    },
    {
      label: 'Báo cáo đang hoạt động',
      value: summary?.activeReports ?? 0,
      icon: Activity,
      color: 'text-emerald-600',
      bgColor: 'bg-emerald-50'
    },
    {
      label: 'Báo cáo cần kiểm duyệt',
      value: summary?.pendingReports ?? 0,
      icon: Clock,
      color: 'text-amber-600',
      bgColor: 'bg-amber-50'
    }
  ];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Bảng Điều Khiển Tổng Quan</h1>
          <p className="text-sm text-slate-500 mt-0.5">
            Theo dõi thời gian thực sự cố giao thông đô thị và trạng thái kiểm duyệt
          </p>
        </div>
        <Button onClick={fetchData} variant="outline" size="sm">
          <RotateCw className="w-4 h-4 mr-1.5" /> Làm mới
        </Button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {kpis.map((kpi, idx) => {
          const Icon = kpi.icon;
          return (
            <div
              key={idx}
              className="bg-white p-5 rounded-xl border border-slate-200/80 shadow-sm flex items-center justify-between"
            >
              <div>
                <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                  {kpi.label}
                </p>
                <h3 className="text-2xl font-bold text-slate-800 mt-1">
                  {kpi.value.toLocaleString()}
                </h3>
              </div>
              <div className={`p-3 rounded-xl ${kpi.bgColor} ${kpi.color}`}>
                <Icon className="w-6 h-6" />
              </div>
            </div>
          );
        })}
      </div>

      {/* Recent reports preview */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-5 border-b border-slate-100 flex items-center justify-between">
          <h3 className="font-semibold text-slate-800">Báo Cáo Mới Nhất Cần Lưu Ý</h3>
          <Link
            to="/reports"
            className="text-xs font-medium text-brand-600 hover:text-brand-700 hover:underline"
          >
            Xem tất cả →
          </Link>
        </div>

        <div className="divide-y divide-slate-100 overflow-x-auto">
          {recentReports.length === 0 ? (
            <p className="p-6 text-sm text-slate-500 text-center">Chưa có báo cáo nào</p>
          ) : (
            <table className="w-full text-left text-sm text-slate-600">
              <thead className="bg-slate-50 text-xs font-semibold uppercase text-slate-500">
                <tr>
                  <th className="px-5 py-3">Mã Công Khai (Public ID)</th>
                  <th className="px-5 py-3">Danh mục</th>
                  <th className="px-5 py-3">Địa điểm</th>
                  <th className="px-5 py-3">Trạng thái</th>
                  <th className="px-5 py-3 text-right">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentReports.map((r) => (
                  <tr key={r.publicId} className="hover:bg-slate-50/80">
                    <td className="px-5 py-3.5 font-mono text-xs font-semibold text-slate-800">
                      {r.publicId}
                    </td>
                    <td className="px-5 py-3.5">
                      <Badge color={r.categoryColor}>{r.categoryDisplayName}</Badge>
                    </td>
                    <td className="px-5 py-3.5 max-w-xs truncate text-xs text-slate-600">
                      {r.addressSnapshot || `${r.latitude}, ${r.longitude}`}
                    </td>
                    <td className="px-5 py-3.5">
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
                    <td className="px-5 py-3.5 text-right">
                      <Link
                        to={`/reports/${r.publicId}`}
                        className="text-xs font-medium text-brand-600 hover:text-brand-700"
                      >
                        Chi tiết
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};
