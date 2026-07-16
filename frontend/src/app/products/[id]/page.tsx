"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Heart, ImageIcon } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import StatusBadge from "@/components/StatusBadge";
import PurchaseModal from "@/components/PurchaseModal";
import { api, ApiError } from "@/lib/api";
import { formatWon, sellerName } from "@/lib/format";
import type { ProductDetail } from "@/lib/types";

function DetailContent({ id }: { id: number }) {
  const router = useRouter();
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [liked, setLiked] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const data = await api.getProduct(id);
        if (active) setProduct(data);
      } catch (e) {
        if (active) setError(e instanceof ApiError ? e.message : "상품을 불러오지 못했습니다.");
      }
    })();
    return () => {
      active = false;
    };
  }, [id, reloadKey]);

  if (error) {
    return <p className="py-24 text-center text-sm text-rose-500">{error}</p>;
  }
  if (!product) {
    return <p className="py-24 text-center text-sm text-neutral-400">불러오는 중...</p>;
  }

  const soldOut = product.status === "SOLD_OUT";
  const pending = product.status === "PENDING";
  const onSale = product.status === "ON_SALE";

  return (
    <main className="mx-auto max-w-6xl px-6 py-6">
      <button
        onClick={() => router.back()}
        className="mb-5 flex items-center gap-1.5 text-sm text-neutral-500 hover:text-neutral-900"
      >
        <ArrowLeft size={18} />
        뒤로가기
      </button>

      <div className="grid gap-8 md:grid-cols-2">
        <div className="relative aspect-square overflow-hidden rounded-xl bg-neutral-100">
          <div className="flex h-full items-center justify-center text-neutral-300">
            <ImageIcon size={48} strokeWidth={1.5} />
          </div>
          {soldOut && (
            <div className="absolute inset-0 flex items-center justify-center bg-neutral-900/55">
              <span className="text-lg font-semibold tracking-widest text-white">품절</span>
            </div>
          )}
        </div>

        <div>
          <StatusBadge status={product.status} />
          <h1 className="mt-3 text-2xl font-bold">{product.title}</h1>
          <p className="mt-2 text-3xl font-extrabold">
            {formatWon(product.price)}
            <span className="ml-0.5 text-lg font-bold">원</span>
          </p>

          <dl className="mt-8 space-y-3 text-sm">
            <SpecRow label="제품상태" value="중고" />
            <SpecRow label="배송비" value="별도" />
            <SpecRow label="재고" value={`${product.stock}개`} bold />
            <SpecRow label="판매자" value={sellerName(product.sellerId)} />
          </dl>

          <div className="mt-8 flex gap-3">
            <button
              onClick={() => setLiked((v) => !v)}
              className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl border border-neutral-200 hover:bg-neutral-50"
              aria-label="찜하기"
            >
              <Heart
                size={22}
                className={liked ? "fill-rose-500 text-rose-500" : "text-neutral-400"}
              />
            </button>

            {onSale && (
              <button
                onClick={() => setModalOpen(true)}
                className="flex-1 rounded-xl bg-emerald-500 text-base font-bold text-white hover:bg-emerald-600"
              >
                즉시 구매
              </button>
            )}
            {pending && (
              <button
                disabled
                className="flex-1 cursor-not-allowed rounded-xl bg-amber-400 text-base font-bold text-white"
              >
                판매 대기중
              </button>
            )}
            {soldOut && (
              <button
                disabled
                className="flex-1 cursor-not-allowed rounded-xl bg-neutral-200 text-base font-bold text-neutral-400"
              >
                품절
              </button>
            )}
          </div>
        </div>
      </div>

      <section className="mt-12 border-t border-neutral-100 pt-8">
        <h2 className="mb-3 font-bold">상품정보</h2>
        <p className="whitespace-pre-wrap text-sm leading-relaxed text-neutral-600">
          {product.description}
        </p>
      </section>

      <section className="mt-10 border-t border-neutral-100 pt-8">
        <h2 className="mb-4 font-bold">판매자 정보</h2>
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-full bg-neutral-100" />
          <p className="text-sm font-semibold">{sellerName(product.sellerId)}</p>
        </div>
      </section>

      {modalOpen && (
        <PurchaseModal
          product={product}
          onClose={() => setModalOpen(false)}
          onPurchased={() => {
            setModalOpen(false);
            setReloadKey((k) => k + 1);
          }}
        />
      )}
    </main>
  );
}

function SpecRow({ label, value, bold }: { label: string; value: string; bold?: boolean }) {
  return (
    <div className="flex gap-8">
      <dt className="w-16 shrink-0 text-neutral-400">{label}</dt>
      <dd className={bold ? "font-semibold" : "text-neutral-700"}>{value}</dd>
    </div>
  );
}

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  return (
    <AuthGuard>
      <Header />
      {Number.isNaN(id) ? (
        <p className="py-24 text-center text-sm text-rose-500">잘못된 상품입니다.</p>
      ) : (
        <DetailContent id={id} />
      )}
    </AuthGuard>
  );
}
