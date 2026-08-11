export type ProductStatus = "PENDING" | "ON_SALE" | "SOLD_OUT";

export type ProductCondition = "NEW" | "LIKE_NEW" | "USED";

export interface ProductSummary {
  productId: number;
  title: string;
  price: number;
  stock: number;
  status: ProductStatus;
  thumbnailUrl: string | null;
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
  viewCount: number;
  productCondition: ProductCondition;
  imageUrls: string[];
  createdAt: string;
  tags: string[];
  shippingFee: number;
}

export interface ImagePresignRequest {
  filename: string;
  contentType: string;
}

export interface ImagePresignResponse {
  presignedUrl: string;
  key: string;
  imageUrl: string;
}

export interface ImageConfirmRequest {
  imageUrls: string[];
}

export interface ProductLikeStatus {
  count: number;
  liked: boolean;
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
  productCondition: ProductCondition;
  tags: string[];
  shippingFee: number;
}

export interface ProductUpdateRequest {
  title: string;
  description: string;
  price: number;
  stock: number;
  productCondition: ProductCondition;
  tags: string[];
  shippingFee: number;
}

export const CONDITION_LABEL: Record<ProductCondition, string> = {
  NEW: "새 상품",
  LIKE_NEW: "거의 새 것",
  USED: "사용감 있음",
};

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

export interface MyTransactions {
  purchases: OrderSummary[];
  sales: OrderSummary[];
  listings: ProductSummary[];
}

export interface InspectionQueueItem {
  orderId: number;
  productTitle: string;
  price: number;
  sellerId: number;
  status: OrderStatus;
}

export interface InspectionQueueResponse {
  items: InspectionQueueItem[];
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
