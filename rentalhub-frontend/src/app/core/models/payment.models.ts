export type PaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE' | 'WAIVED';

export interface PaymentResponse {
  id: number;
  amount: number;
  rentMonth: number;
  rentYear: number;
  monthLabel: string;
  dueDate: string;
  paidDate: string | null;
  status: PaymentStatus;
  lateFee: number | null;
  paymentReference: string | null;
  notes: string | null;
  propertyId: number;
  propertyName: string;
  tenantId: number;
  tenantName: string;
  recordedByName: string | null;
  createdAt: string;
}

export interface PaymentSummary {
  collectedThisMonth: number;
  pendingThisMonth: number;
  overdueTotal: number;
  paidCount: number;
  pendingCount: number;
  overdueCount: number;
  currentMonth: number;
  currentYear: number;
}

export interface RecordPaymentRequest {
  paidDate: string; // YYYY-MM-DD
  amount?: number;
  paymentReference?: string;
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
