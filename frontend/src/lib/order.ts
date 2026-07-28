import type { OrderRole, OrderStatus, OrderSummary } from "./types";

export function orderStatusLabel(status: OrderStatus, role: OrderRole = "buyer"): string {
  switch (status) {
    case "CREATED":
      return "결제 대기";
    case "PAID":
      return "결제 완료";
    case "SHIPPED_TO_INSPECTOR":
      return "검수업체 발송";
    case "UNDER_INSPECTION":
      return "검수 중";
    case "COMPLETED":
      return role === "seller" ? "정산완료" : "거래완료";
    case "REFUNDED":
      return "환불완료";
    case "CANCELED":
      return "취소됨";
  }
}

export function orderStatusTone(status: OrderStatus): string {
  switch (status) {
    case "PAID":
    case "SHIPPED_TO_INSPECTOR":
    case "UNDER_INSPECTION":
      return "text-[#f0143c]";
    case "COMPLETED":
      return "text-neutral-900";
    case "REFUNDED":
      return "text-neutral-500";
    case "CANCELED":
      return "text-neutral-400";
    default:
      return "text-neutral-500";
  }
}

const IN_PROGRESS: OrderStatus[] = ["PAID", "SHIPPED_TO_INSPECTOR", "UNDER_INSPECTION"];
const DONE: OrderStatus[] = ["COMPLETED", "REFUNDED", "CANCELED"];

export function orderCounts(orders: OrderSummary[]) {
  return {
    total: orders.length,
    waiting: orders.filter((o) => o.status === "CREATED").length,
    inProgress: orders.filter((o) => IN_PROGRESS.includes(o.status)).length,
    done: orders.filter((o) => DONE.includes(o.status)).length,
  };
}
