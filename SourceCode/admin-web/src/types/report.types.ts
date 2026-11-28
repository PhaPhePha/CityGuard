import { ReportStatus } from './common.types';

export type { ReportStatus };

export interface AdminReportSummary {
  publicId: string; // rpt_ + 26-char Crockford Base32 ULID
  categoryId: number;
  categoryCode: string;
  categoryDisplayName: string;
  categoryColor: string;
  description?: string;
  imageUrl?: string;
  latitude: number;
  longitude: number;
  addressSnapshot?: string;
  status: ReportStatus;
  confirmCount: number;
  noLongerCount: number;
  trustScore?: number | null;
  trustLabel?: string | null;
  createdAt: string;
  expiresAt: string;
}

export interface AdminReportDetail extends AdminReportSummary {
  reporterId?: number;
  reporterUsername?: string;
  reporterDisplayName?: string;
  reporterAvatarUrl?: string;
}

export interface AdminUpdateReportStatusRequest {
  status: ReportStatus;
  reason: string;
}

export interface ReportFilterParams {
  keyword?: string;
  categoryId?: number;
  status?: ReportStatus;
  page?: number;
  size?: number;
}
