import { describe, it, expect } from 'vitest';
import { mockService } from '@/mocks/mockService';
import { mockReports, mockUsers, mockComments, MOCK_DEV_CREDENTIALS } from '@/mocks/mockData';

describe('CityGuard V3 Schema & Data Integrity Tests', () => {
  it('should have mock users matching CityGuard V3 roles and statuses', () => {
    expect(mockUsers.length).toBeGreaterThanOrEqual(5);

    const admin = mockUsers.find((u) => u.role === 'ADMIN');
    expect(admin).toBeDefined();
    expect(admin?.username).toBe('demo_admin');

    const mod = mockUsers.find((u) => u.role === 'MODERATOR');
    expect(mod).toBeDefined();
    expect(mod?.username).toBe('demo_moderator');

    const locked = mockUsers.find((u) => u.status === 'LOCKED');
    expect(locked).toBeDefined();
    expect(locked?.username).toBe('demo_locked');
  });

  it('should strictly have exactly 6 official CityGuard categories without unofficial additions', async () => {
    const categories = await mockService.getCategories();
    expect(categories.length).toBe(6);

    const expectedCodes = [
      'TRAFFIC_JAM',
      'ROAD_BLOCK',
      'ROAD_DAMAGE',
      'BROKEN_TRAFFIC_LIGHT',
      'FALLEN_TREE',
      'FLOOD'
    ];

    const actualCodes = categories.map((c) => c.code);
    expect(actualCodes.sort()).toEqual(expectedCodes.sort());

    // Verify absence of unsupported categories like 'ACCIDENT' or 'OTHER'
    expect(actualCodes).not.toContain('ACCIDENT');
    expect(actualCodes).not.toContain('OTHER');
  });

  it('should have mock reports strictly complying with public_id ULID format', () => {
    expect(mockReports.length).toBeGreaterThanOrEqual(6);

    const publicIdRegex = /^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$/;
    mockReports.forEach((r) => {
      expect(r.publicId).toMatch(publicIdRegex);
      expect(r.categoryId).toBeDefined();
      expect(r.status).toBeDefined();
      expect(typeof r.latitude).toBe('number');
      expect(typeof r.longitude).toBe('number');
    });
  });

  it('should have mock comments referencing valid report publicId and having isHidden boolean', () => {
    expect(mockComments.length).toBeGreaterThanOrEqual(5);
    const publicIdRegex = /^rpt_[0-7][0-9A-HJKMNP-TV-Z]{25}$/;

    mockComments.forEach((c) => {
      expect(c.reportPublicId).toMatch(publicIdRegex);
      expect(typeof c.isHidden).toBe('boolean');
    });
  });
});

describe('CityGuard Admin Web Auth Workflows (Hardened Mock)', () => {
  it('should successfully login as ADMIN with fixed development password', async () => {
    const res = await mockService.login({
      identifier: 'demo_admin',
      password: MOCK_DEV_CREDENTIALS.demo_admin
    });

    expect(res.token).toContain('mock-jwt-token-signature');
    expect(res.user.role).toBe('ADMIN');
    expect(res.user.status).toBe('ACTIVE');
  });

  it('should successfully login as MODERATOR with fixed development password', async () => {
    const res = await mockService.login({
      identifier: 'demo_moderator',
      password: MOCK_DEV_CREDENTIALS.demo_moderator
    });

    expect(res.user.role).toBe('MODERATOR');
    expect(res.user.status).toBe('ACTIVE');
  });

  it('should reject login when password does not match fixed mock password with 401', async () => {
    await expect(
      mockService.login({
        identifier: 'demo_admin',
        password: 'arbitrary_unauthorized_password_123'
      })
    ).rejects.toThrow(/Mật khẩu phát triển không chính xác/i);
  });

  it('should reject login for LOCKED user even with correct password with 403', async () => {
    await expect(
      mockService.login({
        identifier: 'demo_locked',
        password: MOCK_DEV_CREDENTIALS.demo_locked
      })
    ).rejects.toThrow(/bị khóa/i);
  });

  it('should reject login for regular USER role without admin privilege with 403', async () => {
    await expect(
      mockService.login({
        identifier: 'demo_user_a',
        password: MOCK_DEV_CREDENTIALS.demo_user_a
      })
    ).rejects.toThrow(/không có quyền/i);
  });

  it('should reject unknown identifier with 401', async () => {
    await expect(
      mockService.login({
        identifier: 'non_existent_user',
        password: 'Admin@CityGuard2026!'
      })
    ).rejects.toThrow(/không tồn tại/i);
  });
});

describe('CityGuard Admin Report Management & Trust Score Workflows', () => {
  it('should properly handle null trustScore and null trustLabel without calculating in Frontend', async () => {
    const nullReport = await mockService.getReportDetail('rpt_01M6P7Z8A9B2C3D4E5F6G7H8J9');
    expect(nullReport.trustScore).toBeNull();
    expect(nullReport.trustLabel).toBeNull();
  });

  it('should retrieve list of reports with pagination and filtering', async () => {
    const res = await mockService.getReports({ page: 0, size: 2 });
    expect(res.items.length).toBe(2);
    expect(res.page).toBe(0);
    expect(res.size).toBe(2);
    expect(res.totalItems).toBeGreaterThanOrEqual(6);
  });

  it('should update report status to HIDDEN and record HIDE_REPORT in Audit Logs', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;
    const targetPublicId = 'rpt_01M6P4YPXFBG21T29EZJ31RR0Q';

    const updated = await mockService.updateReportStatus(
      targetPublicId,
      {
        status: 'HIDDEN',
        reason: 'Vi phạm chính sách an toàn nội dung đô thị'
      },
      adminUser
    );

    expect(updated.status).toBe('HIDDEN');

    // Verify audit log entry
    const logs = await mockService.getLogs({ page: 0, size: 1 });
    const latestLog = logs.items[0];
    expect(latestLog.actorId).toBe(adminUser.id);
    expect(latestLog.targetType).toBe('REPORT');
    expect(latestLog.action).toBe('HIDE_REPORT');
  });

  it('should allow restoring HIDDEN report back to ACTIVE and record RESTORE_REPORT', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;
    const targetPublicId = 'rpt_01M6P4YPXFBG21T29EZJ31RR0Q';

    const restored = await mockService.updateReportStatus(
      targetPublicId,
      {
        status: 'ACTIVE',
        reason: 'Đã xác minh thông tin chính xác, mở lại hiển thị'
      },
      adminUser
    );

    expect(restored.status).toBe('ACTIVE');

    const logs = await mockService.getLogs({ page: 0, size: 1 });
    const latestLog = logs.items[0];
    expect(latestLog.action).toBe('RESTORE_REPORT');
  });

  it('should reject unconfirmed state transitions with 400', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;
    // Target is an EXPIRED report
    const targetPublicId = 'rpt_01M61KFWQF9E8FTP22D7ZM0PE2';

    // Trying to turn an EXPIRED report into PENDING_VERIFY is unconfirmed / pending TV5 approval
    await expect(
      mockService.updateReportStatus(
        targetPublicId,
        {
          status: 'PENDING_VERIFY',
          reason: 'Chuyển đổi bất hợp lệ'
        },
        adminUser
      )
    ).rejects.toThrow(/chưa được hỗ trợ hoặc đang chờ TV5/i);
  });
});

describe('CityGuard Admin User Management Workflows', () => {
  it('should retrieve users list with role filtering', async () => {
    const res = await mockService.getUsers({ role: 'ADMIN', page: 0, size: 10 });
    expect(res.items.every((u) => u.role === 'ADMIN')).toBe(true);
  });

  it('should retrieve user detail with totalReports and recentReports', async () => {
    const userDetail = await mockService.getUserDetail(1);
    expect(userDetail.id).toBe(1);
    expect(typeof userDetail.totalReports).toBe('number');
    expect(Array.isArray(userDetail.recentReports)).toBe(true);
  });

  it('should lock user and automatically append an Audit Log', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;
    const targetUserId = 2; // demo_user_b

    const updatedUser = await mockService.updateUserStatus(
      targetUserId,
      {
        status: 'LOCKED',
        reason: 'Spam báo cáo sai sự thật liên tục'
      },
      adminUser
    );

    expect(updatedUser.status).toBe('LOCKED');

    // Verify audit log entry
    const logs = await mockService.getLogs({ page: 0, size: 1 });
    const latestLog = logs.items[0];
    expect(latestLog.actorId).toBe(adminUser.id);
    expect(latestLog.targetType).toBe('USER');
    expect(latestLog.action).toBe('LOCK_USER');
    expect(latestLog.reason).toBe('Spam báo cáo sai sự thật liên tục');
  });

  it('should prevent admin from locking their own account', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;

    await expect(
      mockService.updateUserStatus(
        adminUser.id,
        {
          status: 'LOCKED',
          reason: 'Tự khóa'
        },
        adminUser
      )
    ).rejects.toThrow(/không thể tự khóa tài khoản của chính mình/i);
  });
});

describe('CityGuard Admin Comment Management Workflows', () => {
  it('should retrieve comments list with isHidden filtering', async () => {
    const res = await mockService.getComments({ isHidden: false, page: 0, size: 10 });
    expect(res.items.every((c) => c.isHidden === false)).toBe(true);
  });

  it('should toggle comment hide status without physical deletion', async () => {
    const adminUser = mockUsers.find((u) => u.role === 'ADMIN')!;
    const commentId = 101;

    const updatedComment = await mockService.updateCommentStatus(
      commentId,
      {
        isHidden: true,
        reason: 'Nội dung quấy rối và sử dụng từ ngữ không phù hợp'
      },
      adminUser
    );

    expect(updatedComment.isHidden).toBe(true);
    expect(updatedComment.id).toBe(commentId);

    // Verify audit log
    const logs = await mockService.getLogs({ page: 0, size: 1 });
    const latestLog = logs.items[0];
    expect(latestLog.actorId).toBe(adminUser.id);
    expect(latestLog.targetType).toBe('COMMENT');
    expect(latestLog.action).toBe('HIDE_COMMENT');
    expect(latestLog.reason).toBe('Nội dung quấy rối và sử dụng từ ngữ không phù hợp');
  });
});
