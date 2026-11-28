import {
  mockUsers,
  mockReports,
  mockComments,
  mockAdminLogs,
  mockDashboardSummary,
  mockCategories,
  MOCK_DEV_CREDENTIALS
} from './mockData';
import { AdminAuthResponse, AdminLoginRequest, AdminUser } from '@/types/auth.types';
import { AdminCategory } from '@/types/category.types';
import { AdminDashboardSummary } from '@/types/dashboard.types';
import {
  AdminReportDetail,
  AdminReportSummary,
  AdminUpdateReportStatusRequest,
  ReportFilterParams
} from '@/types/report.types';
import {
  AdminUserDetail,
  AdminUserListItem,
  AdminUpdateUserStatusRequest,
  UserFilterParams
} from '@/types/user.types';
import {
  AdminCommentListItem,
  AdminUpdateCommentStatusRequest,
  CommentFilterParams
} from '@/types/comment.types';
import { AdminLogListItem, LogFilterParams } from '@/types/log.types';
import { PageResponse } from '@/types/common.types';

const delay = (ms = 350) => new Promise((resolve) => setTimeout(resolve, ms));

// In-memory clone to allow state updates during testing/demo
let reportsState = [...mockReports];
let usersState = [...mockUsers];
let commentsState = [...mockComments];
let logsState = [...mockAdminLogs];

export const mockService = {
  // 1. Auth Login Mock
  async login(request: AdminLoginRequest): Promise<AdminAuthResponse> {
    await delay(400);

    const user = usersState.find(
      (u) =>
        u.username.toLowerCase() === request.identifier.toLowerCase() ||
        u.email.toLowerCase() === request.identifier.toLowerCase()
    );

    if (!user) {
      const error: any = new Error('Thông tin tài khoản không tồn tại (Mock)');
      error.response = { status: 401, data: { message: error.message } };
      throw error;
    }

    // Strict Password Validation for Mock Environment (Dev credentials only)
    const expectedPassword = MOCK_DEV_CREDENTIALS[request.identifier.toLowerCase()];
    if (!expectedPassword || request.password !== expectedPassword) {
      const error: any = new Error(
        'Mật khẩu phát triển không chính xác. Môi trường mock yêu cầu mật khẩu demo cố định.'
      );
      error.response = { status: 401, data: { message: error.message } };
      throw error;
    }

    if (user.status === 'LOCKED') {
      const error: any = new Error('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên. (Mock)');
      error.response = { status: 403, data: { message: error.message } };
      throw error;
    }

    if (user.role === 'USER') {
      const error: any = new Error('Người dùng không có quyền truy cập vào Admin Web (Mock 403)');
      error.response = { status: 403, data: { message: error.message } };
      throw error;
    }

    return {
      token: 'mock-jwt-token-signature-' + user.id + '-' + Date.now(),
      tokenType: 'Bearer',
      user: { ...user }
    };
  },

  // 1.1 Categories Mock
  async getCategories(): Promise<AdminCategory[]> {
    await delay(150);
    return [...mockCategories];
  },

  // 2. Dashboard Mock
  async getDashboardSummary(): Promise<AdminDashboardSummary> {
    await delay(300);
    return {
      ...mockDashboardSummary,
      totalUsers: usersState.length,
      totalReports: reportsState.length,
      activeReports: reportsState.filter((r) => r.status === 'ACTIVE').length,
      pendingReports: reportsState.filter((r) => r.status === 'PENDING_VERIFY').length
    };
  },

  // 3. Reports Mock
  async getReports(params: ReportFilterParams): Promise<PageResponse<AdminReportSummary>> {
    await delay(350);
    let filtered = [...reportsState];

    if (params.keyword) {
      const kw = params.keyword.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          r.publicId.toLowerCase().includes(kw) ||
          r.description?.toLowerCase().includes(kw) ||
          r.addressSnapshot?.toLowerCase().includes(kw)
      );
    }

    if (params.categoryId) {
      filtered = filtered.filter((r) => r.categoryId === Number(params.categoryId));
    }

    if (params.status) {
      filtered = filtered.filter((r) => r.status === params.status);
    }

    const page = params.page || 0;
    const size = params.size || 10;
    const start = page * size;
    const items = filtered.slice(start, start + size);

    return {
      items,
      page,
      size,
      totalItems: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      isLast: start + size >= filtered.length
    };
  },

  async getReportDetail(publicId: string): Promise<AdminReportDetail> {
    await delay(300);
    const report = reportsState.find((r) => r.publicId === publicId);
    if (!report) {
      const error: any = new Error(`Không tìm thấy báo cáo với mã công khai: ${publicId}`);
      error.response = { status: 404, data: { message: error.message } };
      throw error;
    }
    return { ...report };
  },

  async updateReportStatus(
    publicId: string,
    request: AdminUpdateReportStatusRequest,
    currentAdmin: AdminUser
  ): Promise<AdminReportDetail> {
    await delay(400);
    const idx = reportsState.findIndex((r) => r.publicId === publicId);
    if (idx === -1) {
      const error: any = new Error(`Không tìm thấy báo cáo với mã công khai: ${publicId}`);
      error.response = { status: 404, data: { message: error.message } };
      throw error;
    }

    const currentStatus = reportsState[idx].status;
    const targetStatus = request.status;

    // State transition rules:
    // Allowed:
    // - ANY -> HIDDEN (Moderator hide)
    // - HIDDEN -> ACTIVE (Moderator restore)
    // - ACTIVE / PENDING_VERIFY -> RESOLVED (Incident handled)
   const isAllowedTransition =
       currentStatus != targetStatus &&
       (
           targetStatus === 'HIDDEN' ||
           (currentStatus === 'HIDDEN' && targetStatus === 'ACTIVE') ||
           (
               currentStatus === 'ACTIVE' || currentStatus === 'PENDING_VERIFY'
           ) &&
               targetStatus === 'RESOLVED'
       );

    if (!isAllowedTransition) {
      const error: any = new Error(
        `Chuyển đổi trạng thái từ "${currentStatus}" sang "${targetStatus}" chưa được hỗ trợ hoặc đang chờ TV5 xác nhận quy tắc chuyển đổi.`
      );
      error.response = { status: 400, data: { message: error.message } };
      throw error;
    }

    reportsState[idx] = {
      ...reportsState[idx],
      status: targetStatus
    };

    let actionName = 'UPDATE_REPORT_STATUS';
    if (targetStatus === 'HIDDEN') {
      actionName = 'HIDE_REPORT';
    } else if (currentStatus === 'HIDDEN' && targetStatus === 'ACTIVE') {
      actionName = 'RESTORE_REPORT';
    }

    // Auto-record to mock logs
    logsState.unshift({
      id: Date.now(),
      actorId: currentAdmin.id,
      actorUsername: currentAdmin.username,
      actorDisplayName: currentAdmin.displayName,
      action: actionName,
      targetType: 'REPORT',
      targetId: idx + 1,
      reason: request.reason,
      createdAt: new Date().toISOString()
    });

    return { ...reportsState[idx] };
  },

  // 4. Users Mock
  async getUsers(params: UserFilterParams): Promise<PageResponse<AdminUserListItem>> {
    await delay(350);
    let filtered = [...usersState];

    if (params.search) {
      const s = params.search.toLowerCase();
      filtered = filtered.filter(
        (u) =>
          u.username.toLowerCase().includes(s) ||
          u.email.toLowerCase().includes(s) ||
          u.displayName.toLowerCase().includes(s)
      );
    }

    if (params.role) {
      filtered = filtered.filter((u) => u.role === params.role);
    }

    if (params.status) {
      filtered = filtered.filter((u) => u.status === params.status);
    }

    const page = params.page || 0;
    const size = params.size || 10;
    const start = page * size;
    const items = filtered.slice(start, start + size).map((u) => ({
      ...u,
      createdAt: '2026-11-15T00:00:00Z'
    }));

    return {
      items,
      page,
      size,
      totalItems: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      isLast: start + size >= filtered.length
    };
  },

  async getUserDetail(id: number): Promise<AdminUserDetail> {
    await delay(300);
    const user = usersState.find((u) => u.id === id);
    if (!user) {
      const error: any = new Error(`Không tìm thấy người dùng với ID: ${id}`);
      error.response = { status: 404, data: { message: error.message } };
      throw error;
    }

    const userReports = reportsState.filter((r) => r.reporterId === id);

    return {
      ...user,
      createdAt: '2026-11-15T00:00:00Z',
      totalReports: userReports.length,
      recentReports: userReports.slice(0, 5)
    };
  },

  async updateUserStatus(
    id: number,
    request: AdminUpdateUserStatusRequest,
    currentAdmin: AdminUser
  ): Promise<AdminUserListItem> {
    await delay(400);

    if (currentAdmin.id === id) {
      const error: any = new Error('Bạn không thể tự khóa tài khoản của chính mình (Mock rule)');
      error.response = { status: 400, data: { message: error.message } };
      throw error;
    }

    const idx = usersState.findIndex((u) => u.id === id);
    if (idx === -1) {
      const error: any = new Error(`Không tìm thấy người dùng với ID: ${id}`);
      error.response = { status: 404, data: { message: error.message } };
      throw error;
    }

    usersState[idx] = {
      ...usersState[idx],
      status: request.status
    };

    logsState.unshift({
      id: Date.now(),
      actorId: currentAdmin.id,
      actorUsername: currentAdmin.username,
      actorDisplayName: currentAdmin.displayName,
      action: request.status === 'LOCKED' ? 'LOCK_USER' : 'UNLOCK_USER',
      targetType: 'USER',
      targetId: id,
      reason: request.reason,
      createdAt: new Date().toISOString()
    });

    return {
      ...usersState[idx],
      createdAt: '2026-11-15T00:00:00Z'
    };
  },

  // 5. Comments Mock
  async getComments(params: CommentFilterParams): Promise<PageResponse<AdminCommentListItem>> {
    await delay(350);
    let filtered = [...commentsState];

    if (params.reportPublicId) {
      filtered = filtered.filter((c) => c.reportPublicId === params.reportPublicId);
    }

    if (typeof params.isHidden === 'boolean') {
      filtered = filtered.filter((c) => c.isHidden === params.isHidden);
    }

    const page = params.page || 0;
    const size = params.size || 10;
    const start = page * size;
    const items = filtered.slice(start, start + size);

    return {
      items,
      page,
      size,
      totalItems: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      isLast: start + size >= filtered.length
    };
  },

  async updateCommentStatus(
    id: number,
    request: AdminUpdateCommentStatusRequest,
    currentAdmin: AdminUser
  ): Promise<AdminCommentListItem> {
    await delay(350);
    const idx = commentsState.findIndex((c) => c.id === id);
    if (idx === -1) {
      const error: any = new Error(`Không tìm thấy bình luận với ID: ${id}`);
      error.response = { status: 404, data: { message: error.message } };
      throw error;
    }

    commentsState[idx] = {
      ...commentsState[idx],
      isHidden: request.isHidden
    };

    logsState.unshift({
      id: Date.now(),
      actorId: currentAdmin.id,
      actorUsername: currentAdmin.username,
      actorDisplayName: currentAdmin.displayName,
      action: request.isHidden ? 'HIDE_COMMENT' : 'RESTORE_COMMENT',
      targetType: 'COMMENT',
      targetId: id,
      reason: request.reason,
      createdAt: new Date().toISOString()
    });

    return { ...commentsState[idx] };
  },

  // 6. Logs Mock
  async getLogs(params: LogFilterParams): Promise<PageResponse<AdminLogListItem>> {
    await delay(350);
    let filtered = [...logsState];

    if (params.actorId) {
      filtered = filtered.filter((l) => l.actorId === Number(params.actorId));
    }

    if (params.action) {
      filtered = filtered.filter((l) => l.action === params.action);
    }

    if (params.targetType) {
      filtered = filtered.filter((l) => l.targetType === params.targetType);
    }

    const page = params.page || 0;
    const size = params.size || 10;
    const start = page * size;
    const items = filtered.slice(start, start + size);

    return {
      items,
      page,
      size,
      totalItems: filtered.length,
      totalPages: Math.ceil(filtered.length / size) || 1,
      isLast: start + size >= filtered.length
    };
  }
};
