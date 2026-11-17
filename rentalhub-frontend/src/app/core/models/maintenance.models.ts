export type MaintenanceStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'REJECTED';

export const MAINTENANCE_CATEGORIES = [
  'PLUMBING',
  'ELECTRICAL',
  'PAINTING',
  'CARPENTRY',
  'AC_HVAC',
  'PEST_CONTROL',
  'CLEANING',
  'SECURITY',
  'OTHER',
] as const;

export interface MaintenanceResponse {
  id: number;
  title: string;
  description: string;
  category: string | null;
  status: MaintenanceStatus;
  resolutionNotes: string | null;
  resolvedAt: string | null;
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  tenantId: number;
  tenantName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  imagePaths: string[];
  createdAt: string;
  updatedAt: string;
}

export interface MaintenanceCreateRequest {
  propertyId: number;
  title: string;
  description: string;
  category?: string;
}

export interface MaintenanceStatusUpdate {
  status: MaintenanceStatus;
  resolutionNotes?: string;
  assignedToUserId?: number;
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
