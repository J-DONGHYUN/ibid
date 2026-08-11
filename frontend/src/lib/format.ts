import type { ProductStatus } from "./types";

export function formatWon(value: number): string {
  return value.toLocaleString("ko-KR");
}

export function sellerName(sellerId: number): string {
  return `user_${String(sellerId).padStart(3, "0")}`;
}

export function timeAgo(iso: string): string {
  const seconds = Math.floor((Date.now() - new Date(iso).getTime()) / 1000);
  if (seconds < 60) return "방금 전";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}일 전`;
  const months = Math.floor(days / 30);
  if (months < 12) return `${months}개월 전`;
  return `${Math.floor(months / 12)}년 전`;
}

export const STATUS_LABEL: Record<ProductStatus, string> = {
  PENDING: "판매대기",
  ON_SALE: "판매중",
  SOLD_OUT: "품절",
};
