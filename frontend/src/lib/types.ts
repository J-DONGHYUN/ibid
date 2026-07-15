export type ProductStatus = "PENDING" | "ON_SALE" | "SOLD_OUT";

export interface ProductSummary {
  productId: number;
  title: string;
  price: number;
  stock: number;
  status: ProductStatus;
}

export interface ProductListResponse {
  products: ProductSummary[];
  nextCursor: number | null;
  hasNext: boolean;
}

export interface ProductDetail {
  productId: number;
  sellerId: number;
  title: string;
  description: string;
  price: number;
  stock: number;
  status: ProductStatus;
}

export interface LoginResponse {
  accessToken: string;
}

export interface SignupRequest {
  email: string;
  password: string;
  username: string;
}

export interface ProductRegisterRequest {
  title: string;
  description: string;
  price: number;
  stock: number;
}
