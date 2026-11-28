import { AdminUser } from '@/types/auth.types';
import { AdminReportDetail } from '@/types/report.types';
import { AdminCommentListItem } from '@/types/comment.types';
import { AdminLogListItem } from '@/types/log.types';
import { AdminDashboardSummary } from '@/types/dashboard.types';
import { AdminCategory } from '@/types/category.types';

// Fixed Development-Only Mock Credentials (Never use real passwords or database hashes)
export const MOCK_DEV_CREDENTIALS: Record<string, string> = {
  demo_admin: 'Admin@CityGuard2026!',
  'admin@example.invalid': 'Admin@CityGuard2026!',
  demo_moderator: 'Mod@CityGuard2026!',
  'moderator@example.invalid': 'Mod@CityGuard2026!',
  demo_user_a: 'User@CityGuard2026!',
  'user.a@example.invalid': 'User@CityGuard2026!',
  demo_user_b: 'User@CityGuard2026!',
  'user.b@example.invalid': 'User@CityGuard2026!',
  demo_locked: 'Locked@CityGuard2026!',
  'locked@example.invalid': 'Locked@CityGuard2026!'
};

// 6 Official Categories strictly matching CityGuard V3 PostgreSQL schema
export const mockCategories: AdminCategory[] = [
  {
    id: 1,
    code: 'TRAFFIC_JAM',
    name: 'Kẹt xe',
    color: '#EF4444'
  },
  {
    id: 2,
    code: 'ROAD_BLOCK',
    name: 'Rào chắn / Cấm đường',
    color: '#F97316'
  },
  {
    id: 3,
    code: 'ROAD_DAMAGE',
    name: 'Đường hư hỏng / Ổ gà',
    color: '#F59E0B'
  },
  {
    id: 4,
    code: 'FLOOD',
    name: 'Ngập nước',
    color: '#3B82F6'
  },
  {
    id: 5,
    code: 'BROKEN_TRAFFIC_LIGHT',
    name: 'Đèn tín hiệu hỏng',
    color: '#8B5CF6'
  },
  {
    id: 6,
    code: 'FALLEN_TREE',
    name: 'Cây đổ',
    color: '#10B981'
  }
];

// Mock Users matching CityGuard V3 demo seed
export const mockUsers: AdminUser[] = [
  {
    id: 4,
    username: 'demo_admin',
    email: 'admin@example.invalid',
    displayName: 'Quản Trị Viên (Demo Admin)',
    role: 'ADMIN',
    status: 'ACTIVE',
    avatarUrl: null
  },
  {
    id: 3,
    username: 'demo_moderator',
    email: 'moderator@example.invalid',
    displayName: 'Kiểm Duyệt Viên (Demo Moderator)',
    role: 'MODERATOR',
    status: 'ACTIVE',
    avatarUrl: null
  },
  {
    id: 1,
    username: 'demo_user_a',
    email: 'user.a@example.invalid',
    displayName: 'Nguyễn Văn A',
    role: 'USER',
    status: 'ACTIVE',
    avatarUrl: null
  },
  {
    id: 2,
    username: 'demo_user_b',
    email: 'user.b@example.invalid',
    displayName: 'Trần Thị B',
    role: 'USER',
    status: 'ACTIVE',
    avatarUrl: null
  },
  {
    id: 5,
    username: 'demo_locked',
    email: 'locked@example.invalid',
    displayName: 'Tài Khoản Bị Khóa',
    role: 'USER',
    status: 'LOCKED',
    avatarUrl: null
  }
];

// Mock Reports strictly matching CityGuard V3 public_id format (rpt_ + 26-char Crockford Base32 ULID)
// and covering all 6 categories without unofficial additions
export const mockReports: AdminReportDetail[] = [
  {
    publicId: 'rpt_01M6P4YPXFBG21T29EZJ31RR0Q',
    categoryId: 1,
    categoryCode: 'TRAFFIC_JAM',
    categoryDisplayName: 'Kẹt xe',
    categoryColor: '#EF4444',
    description: 'Kẹt xe kéo dài từ Ngã tư Hàng Xanh đến Cầu Sài Gòn, các phương tiện di chuyển rất chậm.',
    imageUrl: 'https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=600',
    latitude: 10.8015,
    longitude: 106.7118,
    addressSnapshot: 'Ngã tư Hàng Xanh, Bình Thạnh, TP.HCM',
    status: 'ACTIVE',
    confirmCount: 14,
    noLongerCount: 1,
    trustScore: 85.5,
    trustLabel: 'Cao',
    createdAt: '2026-11-20T08:15:00Z',
    expiresAt: '2026-11-20T12:15:00Z',
    reporterId: 1,
    reporterUsername: 'demo_user_a',
    reporterDisplayName: 'Nguyễn Văn A'
  },
  {
    publicId: 'rpt_01M6P63ASFNX3Z0R49VXX9BKG6',
    categoryId: 4,
    categoryCode: 'FLOOD',
    categoryDisplayName: 'Ngập nước',
    categoryColor: '#3B82F6',
    description: 'Nước ngập sâu nửa bánh xe sau cơn mưa lớn, nhiều xe máy chết máy.',
    imageUrl: 'https://images.unsplash.com/photo-1547683905-f686c993aae5?w=600',
    latitude: 10.7324,
    longitude: 106.7210,
    addressSnapshot: 'Đường Huỳnh Tấn Phát, Quận 7, TP.HCM',
    status: 'PENDING_VERIFY',
    confirmCount: 2,
    noLongerCount: 0,
    trustScore: 50.0,
    trustLabel: 'Đang xác minh',
    createdAt: '2026-11-20T09:30:00Z',
    expiresAt: '2026-11-20T15:30:00Z',
    reporterId: 2,
    reporterUsername: 'demo_user_b',
    reporterDisplayName: 'Trần Thị B'
  },
  {
    publicId: 'rpt_01M61KFWQF9E8FTP22D7ZM0PE2',
    categoryId: 3,
    categoryCode: 'ROAD_DAMAGE',
    categoryDisplayName: 'Đường hư hỏng / Ổ gà',
    categoryColor: '#F59E0B',
    description: 'Ổ gà lớn giữa làn đường ô tô nguy hiểm cho người đi xe máy vào ban đêm.',
    imageUrl: 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?w=600',
    latitude: 10.7769,
    longitude: 106.7009,
    addressSnapshot: 'Đường Nguyễn Thị Minh Khai, Quận 1, TP.HCM',
    status: 'EXPIRED',
    confirmCount: 8,
    noLongerCount: 4,
    trustScore: 62.0,
    trustLabel: 'Trung bình',
    createdAt: '2026-11-19T10:00:00Z',
    expiresAt: '2026-11-19T18:00:00Z',
    reporterId: 1,
    reporterUsername: 'demo_user_a',
    reporterDisplayName: 'Nguyễn Văn A'
  },
  {
    publicId: 'rpt_01M6P37S3F2EKY523SEXKT1KPN',
    categoryId: 2,
    categoryCode: 'ROAD_BLOCK',
    categoryDisplayName: 'Rào chắn / Cấm đường',
    categoryColor: '#F97316',
    description: 'Rào chắn thi công cống thoát nước chiếm 2/3 lòng đường nhưng thiếu đèn báo hiệu ban đêm.',
    imageUrl: undefined,
    latitude: 10.8231,
    longitude: 106.6297,
    addressSnapshot: 'Đường Quang Trung, Gò Vấp, TP.HCM',
    status: 'HIDDEN',
    confirmCount: 1,
    noLongerCount: 0,
    trustScore: 40.0,
    trustLabel: 'Thấp',
    createdAt: '2026-11-18T14:20:00Z',
    expiresAt: '2026-11-18T20:20:00Z',
    reporterId: 2,
    reporterUsername: 'demo_user_b',
    reporterDisplayName: 'Trần Thị B'
  },
  {
    publicId: 'rpt_01M6P37S3FRDQ3D6F2XMKD4P50',
    categoryId: 5,
    categoryCode: 'BROKEN_TRAFFIC_LIGHT',
    categoryDisplayName: 'Đèn tín hiệu hỏng',
    categoryColor: '#8B5CF6',
    description: 'Đèn tín hiệu giao thông mất điện cả 3 pha khiến giao lộ hỗn loạn giờ cao điểm.',
    imageUrl: undefined,
    latitude: 10.7554,
    longitude: 106.6668,
    addressSnapshot: 'Ngã tư Nguyễn Trãi - Trần Phú, Quận 5, TP.HCM',
    status: 'ACTIVE',
    confirmCount: 19,
    noLongerCount: 1,
    trustScore: 92.0,
    trustLabel: 'Cao',
    createdAt: '2026-11-20T07:45:00Z',
    expiresAt: '2026-11-20T13:45:00Z',
    reporterId: 1,
    reporterUsername: 'demo_user_a',
    reporterDisplayName: 'Nguyễn Văn A'
  },
  {
    publicId: 'rpt_01M6P7Z8A9B2C3D4E5F6G7H8J9',
    categoryId: 6,
    categoryCode: 'FALLEN_TREE',
    categoryDisplayName: 'Cây đổ',
    categoryColor: '#10B981',
    description: 'Cây me tây to đổ chắn ngang đường Nguyễn Hữu Cảnh sau cơn giông lốc.',
    imageUrl: undefined,
    latitude: 10.7925,
    longitude: 106.7156,
    addressSnapshot: 'Đường Nguyễn Hữu Cảnh, Bình Thạnh, TP.HCM',
    status: 'PENDING_VERIFY',
    confirmCount: 0,
    noLongerCount: 0,
    trustScore: null, // Test case: trustScore = null (Chưa có đánh giá)
    trustLabel: null, // Test case: trustLabel = null (Đang xác minh)
    createdAt: '2026-11-20T10:00:00Z',
    expiresAt: '2026-11-20T16:00:00Z',
    reporterId: 2,
    reporterUsername: 'demo_user_b',
    reporterDisplayName: 'Trần Thị B'
  }
];

// Mock Comments
export const mockComments: AdminCommentListItem[] = [
  {
    id: 101,
    reportPublicId: 'rpt_01M6P4YPXFBG21T29EZJ31RR0Q',
    reportId: 1,
    userId: 2,
    username: 'demo_user_b',
    userDisplayName: 'Trần Thị B',
    body: 'Đến 8h45 vẫn chưa thông xe, mọi người nên rẽ sang đường D2 để tránh.',
    isHidden: false,
    deletedAt: null,
    createdAt: '2026-11-20T08:30:00Z'
  },
  {
    id: 102,
    reportPublicId: 'rpt_01M6P4YPXFBG21T29EZJ31RR0Q',
    reportId: 1,
    userId: 1,
    username: 'demo_user_a',
    userDisplayName: 'Nguyễn Văn A',
    body: 'Có CSGT đang điều tiết rồi nhé mọi người.',
    isHidden: false,
    deletedAt: null,
    createdAt: '2026-11-20T08:45:00Z'
  },
  {
    id: 103,
    reportPublicId: 'rpt_01M6P37S3F2EKY523SEXKT1KPN',
    reportId: 4,
    userId: 5,
    username: 'demo_locked',
    userDisplayName: 'Tài Khoản Bị Khóa',
    body: 'Spam quảng cáo dịch vụ chở hàng giá rẻ liên hệ 090xxxxxxx',
    isHidden: true,
    deletedAt: null,
    createdAt: '2026-11-18T15:00:00Z'
  },
  {
    id: 104,
    reportPublicId: 'rpt_01M6P63ASFNX3Z0R49VXX9BKG6',
    reportId: 2,
    userId: 1,
    username: 'demo_user_a',
    userDisplayName: 'Nguyễn Văn A',
    body: 'Đoạn này ngập sâu hơn 30cm rồi, xe gầm thấp không nên đi qua.',
    isHidden: false,
    deletedAt: null,
    createdAt: '2026-11-20T09:40:00Z'
  },
  {
    id: 105,
    reportPublicId: 'rpt_01M61KFWQF9E8FTP22D7ZM0PE2',
    reportId: 3,
    userId: 2,
    username: 'demo_user_b',
    userDisplayName: 'Trần Thị B',
    body: 'Công nhân môi trường đô thị đã rào chắn cảnh báo rồi.',
    isHidden: false,
    deletedAt: null,
    createdAt: '2026-11-19T10:30:00Z'
  }
];

// Mock Admin Logs
export const mockAdminLogs: AdminLogListItem[] = [
  {
    id: 501,
    actorId: 4,
    actorUsername: 'demo_admin',
    actorDisplayName: 'Quản Trị Viên (Demo Admin)',
    action: 'LOCK_USER',
    targetType: 'USER',
    targetId: 5,
    reason: 'Phát hiện spam bình luận quảng cáo trái phép liên tục',
    createdAt: '2026-11-18T15:30:00Z'
  },
  {
    id: 502,
    actorId: 3,
    actorUsername: 'demo_moderator',
    actorDisplayName: 'Kiểm Duyệt Viên (Demo Moderator)',
    action: 'HIDE_REPORT',
    targetType: 'REPORT',
    targetId: 4,
    reason: 'Báo cáo thông tin không chính xác về địa điểm vật cản',
    createdAt: '2026-11-18T16:00:00Z'
  },
  {
    id: 503,
    actorId: 3,
    actorUsername: 'demo_moderator',
    actorDisplayName: 'Kiểm Duyệt Viên (Demo Moderator)',
    action: 'HIDE_COMMENT',
    targetType: 'COMMENT',
    targetId: 103,
    reason: 'Nội dung bình luận mang tính chất quảng cáo rác',
    createdAt: '2026-11-18T16:05:00Z'
  }
];

// Mock Dashboard Summary
export const mockDashboardSummary: AdminDashboardSummary = {
  totalUsers: 142,
  totalReports: 580,
  activeReports: 42,
  pendingReports: 12
};
