"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Minus, Plus, X } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import { formatWon } from "@/lib/format";
import type { ProductDetail } from "@/lib/types";
import { useToast } from "./Toast";

interface Props {
  product: ProductDetail;
  onClose: () => void;
  onPurchased: () => void;
}

export default function PurchaseModal({ product, onClose, onPurchased }: Props) {
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);
  const [submitting, setSubmitting] = useState(false);
  const { showToast } = useToast();

  const total = product.price * quantity;

  const changeQuantity = (delta: number) => {
    setQuantity((q) => Math.min(product.stock, Math.max(1, q + delta)));
  };

  const handleConfirm = async () => {
    setSubmitting(true);
    try {
      const { orderId } = await api.purchase(product.productId, quantity);
      const params = new URLSearchParams({
        orderId: String(orderId),
        amount: String(total),
        orderName: product.title,
      });
      onPurchased();
      router.push(`/checkout?${params.toString()}`);
    } catch (e) {
      const message = e instanceof ApiError ? e.message : "구매에 실패했습니다.";
      showToast(message, "error");
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-neutral-900/40 px-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between">
          <h2 className="text-lg font-bold">구매 확인</h2>
          <button onClick={onClose} className="text-neutral-400 hover:text-neutral-700">
            <X size={20} />
          </button>
        </div>

        <p className="mb-5 truncate text-sm text-neutral-600">{product.title}</p>

        <div className="mb-5 flex items-center justify-between">
          <span className="text-sm text-neutral-500">수량</span>
          <div className="flex items-center gap-3">
            <button
              onClick={() => changeQuantity(-1)}
              disabled={quantity <= 1}
              className="flex h-8 w-8 items-center justify-center rounded-md border border-neutral-200 text-neutral-600 disabled:opacity-40"
            >
              <Minus size={16} />
            </button>
            <span className="w-6 text-center font-medium">{quantity}</span>
            <button
              onClick={() => changeQuantity(1)}
              disabled={quantity >= product.stock}
              className="flex h-8 w-8 items-center justify-center rounded-md border border-neutral-200 text-neutral-600 disabled:opacity-40"
            >
              <Plus size={16} />
            </button>
          </div>
        </div>

        <div className="mb-6 flex items-center justify-between border-t border-neutral-100 pt-4">
          <span className="text-sm text-neutral-500">총 금액</span>
          <span className="text-lg font-bold">{formatWon(total)}원</span>
        </div>

        <div className="flex gap-2">
          <button
            onClick={onClose}
            className="flex-1 rounded-lg border border-neutral-200 py-3 text-sm font-medium text-neutral-600 hover:bg-neutral-50"
          >
            취소
          </button>
          <button
            onClick={handleConfirm}
            disabled={submitting}
            className="flex-[2] rounded-lg bg-emerald-500 py-3 text-sm font-semibold text-white hover:bg-emerald-600 disabled:opacity-60"
          >
            {submitting ? "처리 중..." : "구매하기"}
          </button>
        </div>
      </div>
    </div>
  );
}
