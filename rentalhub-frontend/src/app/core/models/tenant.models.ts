export interface TenantResponse {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  idProofNumber: string | null;
  idProofType: string | null;
  moveInDate: string;
  moveOutDate: string | null;
  isCurrentTenant: boolean;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  notes: string | null;
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  createdAt: string;
}

export interface TenantRequest {
  fullName: string;
  email: string;
  phone: string;
  idProofNumber?: string;
  idProofType?: string;
  moveInDate: string;
  propertyId: number;
  emergencyContactName?: string;
  emergencyContactPhone?: string;
  notes?: string;
}

export interface MoveOutRequest {
  moveOutDate: string;
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
