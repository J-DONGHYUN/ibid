import Link from "next/link";
import { ImageIcon } from "lucide-react";
import type { ProductSummary } from "@/lib/types";
import { formatWon } from "@/lib/format";
import StatusBadge from "./StatusBadge";

export default function ProductCard({ product }: { product: ProductSummary }) {
  const soldOut = product.status === "SOLD_OUT";

  return (
    <Link href={`/products/${product.productId}`} className="group block">
      <div className="relative mb-2 aspect-square overflow-hidden rounded-lg bg-neutral-100">
        <div className="flex h-full items-center justify-center text-neutral-300">
          <ImageIcon size={32} strokeWidth={1.5} />
        </div>
        {soldOut && (
          <div className="absolute inset-0 flex items-center justify-center bg-neutral-900/55">
            <span className="text-sm font-semibold tracking-widest text-white">품절</span>
          </div>
        )}
      </div>
      <p className="truncate text-sm text-neutral-800 group-hover:text-neutral-950">
        {product.title}
      </p>
      <p className="mt-0.5 font-bold text-neutral-900">{formatWon(product.price)}원</p>
      <div className="mt-1 flex items-center gap-2">
        <span className="text-xs text-neutral-400">재고 {product.stock}개</span>
        {product.status !== "ON_SALE" && <StatusBadge status={product.status} />}
      </div>
    </Link>
  );
}
