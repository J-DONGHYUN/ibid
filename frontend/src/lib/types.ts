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

export type OrderStatus =
  | "CREATED"
  | "PAID"
  | "SHIPPED_TO_INSPECTOR"
  | "UNDER_INSPECTION"
  | "COMPLETED"
  | "REFUNDED"
  | "CANCELED";

export type OrderRole = "buyer" | "seller";

export interface OrderSummary {
  orderId: number;
  productId: number;
  productTitle: string;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
}

export interface MyOrdersResponse {
  orders: OrderSummary[];
}

export interface OrderDetail {
  orderId: number;
  productId: number;
  productTitle: string;
  buyerId: number;
  sellerId: number;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
}
