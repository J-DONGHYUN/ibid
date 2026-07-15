import type { ProductStatus } from "./types";

export function formatWon(value: number): string {
  return value.toLocaleString("ko-KR");
}

export function sellerName(sellerId: number): string {
  return `user_${String(sellerId).padStart(3, "0")}`;
}

export const STATUS_LABEL: Record<ProductStatus, string> = {
  PENDING: "판매대기",
  ON_SALE: "판매중",
  SOLD_OUT: "품절",
};
