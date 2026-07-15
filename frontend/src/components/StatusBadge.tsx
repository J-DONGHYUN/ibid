import type { ProductStatus } from "@/lib/types";
import { STATUS_LABEL } from "@/lib/format";

const STYLES: Record<ProductStatus, string> = {
  ON_SALE: "bg-emerald-50 text-emerald-600",
  PENDING: "bg-amber-50 text-amber-600",
  SOLD_OUT: "bg-neutral-100 text-neutral-500",
};

export default function StatusBadge({ status }: { status: ProductStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-md px-2 py-0.5 text-xs font-semibold ${STYLES[status]}`}
    >
      {STATUS_LABEL[status]}
    </span>
  );
}
