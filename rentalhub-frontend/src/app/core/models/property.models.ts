// ─────────────────────────────────────────────────────────────────
// property.models.ts — matches PropertyRequestDTO / PropertyResponseDTO
// ─────────────────────────────────────────────────────────────────

export const PropertyType = {
  APARTMENT: 'APARTMENT',
  HOUSE: 'HOUSE',
  VILLA: 'VILLA',
  STUDIO: 'STUDIO',
  COMMERCIAL: 'COMMERCIAL',
  SHOP: 'SHOP',
  WAREHOUSE: 'WAREHOUSE',
  OTHER: 'OTHER',
} as const;
export type PropertyTypeValue =
  (typeof PropertyType)[keyof typeof PropertyType];

export const PropertyStatus = {
  AVAILABLE: 'AVAILABLE',
  OCCUPIED: 'OCCUPIED',
  MAINTENANCE: 'MAINTENANCE',
  INACTIVE: 'INACTIVE',
} as const;
export type PropertyStatusValue =
  (typeof PropertyStatus)[keyof typeof PropertyStatus];

export interface PropertyImage {
  id: number;
  filePath: string;
  isPrimary: boolean;
}

export interface PropertyResponse {
  id: number;
  propertyName: string;
  propertyType: PropertyTypeValue;
  addressLine1: string;
  addressLine2: string | null;
  city: string;
  state: string;
  pincode: string;
  rentAmount: number;
  depositAmount: number;
  bedrooms: number;
  bathrooms: number;
  areaSqft: number | null;
  description: string | null;
  status: PropertyStatusValue;
  ownerId: number;
  ownerName: string;
  images: PropertyImage[];
  createdAt: string;
}

// Sent as JSON string inside the "property" multipart part
export interface PropertyRequest {
  propertyName: string;
  propertyType: PropertyTypeValue;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state: string;
  pincode: string;
  rentAmount: number;
  depositAmount: number;
  bedrooms: number;
  bathrooms: number;
  areaSqft?: number;
  description?: string;
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
