export interface AgreementResponse {
  id: number;
  agreementNumber: string;
  filePath: string;
  fileName: string;
  startDate: string;
  endDate: string;
  monthlyRent: number;
  depositAmount: number;
  isActive: boolean;
  isExpired: boolean;
  notes: string | null;
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  tenantId: number;
  tenantName: string;
  uploadedByName: string;
  daysUntilExpiry: number;
  expiryStatus: 'ACTIVE' | 'EXPIRING_SOON' | 'EXPIRED';
  createdAt: string;
}

export interface AgreementRequest {
  propertyId: number;
  tenantId: number;
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  monthlyRent: number;
  depositAmount: number;
  notes?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
