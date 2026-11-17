export interface MonthlyRevenuePoint {
  month: string;
  amount: number;
}

export interface AdminDashboard {
  totalUsers: number;
  totalOwners: number;
  totalTenants: number;
  totalProperties: number;
  occupiedProperties: number;
  vacantProperties: number;
  totalRevenueThisMonth: number;
  totalRevenueThisYear: number;
  totalAgreements: number;
  activeAgreements: number;
  openMaintenanceRequests: number;
  totalMaintenanceRequests: number;
  monthlyRevenue: MonthlyRevenuePoint[];
}

export interface PropertyOccupancyPoint {
  propertyName: string;
  status: string;
  rentAmount: number;
}

export interface OwnerDashboard {
  totalProperties: number;
  occupiedProperties: number;
  vacantProperties: number;
  occupancyRate: number;
  activeTenants: number;
  collectedThisMonth: number;
  pendingAmount: number;
  overdueAmount: number;
  paidCount: number;
  pendingCount: number;
  overdueCount: number;
  activeAgreements: number;
  expiringSoonCount: number;
  openRequests: number;
  inProgressRequests: number;
  monthlyRevenue: MonthlyRevenuePoint[];
  propertyOccupancy: PropertyOccupancyPoint[];
}

export interface TenantDashboard {
  propertyId: number;
  propertyName: string;
  propertyAddress: string;
  propertyCity: string;
  monthlyRent: number;
  depositAmount: number;
  tenantId: number;
  moveInDate: string;
  daysAsTenanant: number;
  currentPaymentId: number | null;
  currentPaymentStatus: string;
  currentPaymentAmount: number;
  currentPaymentDueDate: string | null;
  currentPaymentPaidDate: string | null;
  agreementId: number | null;
  agreementStatus: string;
  agreementEndDate: string | null;
  agreementDaysLeft: number;
  openMaintenanceRequests: number;
  inProgressRequests: number;
  totalMaintenanceRequests: number;
}
