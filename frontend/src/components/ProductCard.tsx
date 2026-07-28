import Link from "next/link";
import { Heart, ImageIcon } from "lucide-react";
import type { ProductSummary } from "@/lib/types";
import { formatWon } from "@/lib/format";

export default function ProductCard({ product }: { product: ProductSummary }) {
  const soldOut = product.status === "SOLD_OUT";

  return (
    <Link href={`/products/${product.productId}`} className="group block">
      <div className="relative mb-2.5 aspect-square overflow-hidden rounded-xl bg-neutral-100">
        <div className="flex h-full items-center justify-center text-neutral-300">
          <ImageIcon size={32} strokeWidth={1.5} />
        </div>
        <span className="absolute right-3 top-3 text-neutral-400">
          <Heart size={18} />
        </span>
        {soldOut && (
          <div className="absolute inset-0 flex items-center justify-center bg-neutral-900/55">
            <span className="text-sm font-semibold tracking-widest text-white">품절</span>
          </div>
        )}
      </div>
      <p className="truncate text-sm text-neutral-700 group-hover:text-neutral-950">
        {product.title}
      </p>
      <p className="mt-1 font-bold text-neutral-900">{formatWon(product.price)}원</p>
    </Link>
  );
}
