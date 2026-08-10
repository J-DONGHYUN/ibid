"use client";

import { useEffect, useState } from "react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import ProductCard from "@/components/ProductCard";
import { api, ApiError } from "@/lib/api";
import type { ProductSummary } from "@/lib/types";

function LikesContent() {
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const data = await api.myLikes();
        if (active) setProducts(data);
      } catch (e) {
        if (active) setError(e instanceof ApiError ? e.message : "관심 목록을 불러오지 못했습니다.");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  return (
    <>
      <Header />
      <main className="mx-auto max-w-6xl px-6 py-8">
        <div className="mb-6 flex items-end justify-between border-b-2 border-neutral-900 pb-4">
          <h1 className="text-2xl font-extrabold">관심 목록</h1>
          <span className="text-sm text-neutral-500">{products.length}개</span>
        </div>

        {loading && <p className="py-24 text-center text-sm text-neutral-400">불러오는 중...</p>}

        {error && <p className="py-24 text-center text-sm text-rose-500">{error}</p>}

        {!loading && !error && products.length === 0 && (
          <p className="py-24 text-center text-sm text-neutral-400">아직 찜한 상품이 없어요.</p>
        )}

        {!loading && !error && products.length > 0 && (
          <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-5">
            {products.map((product) => (
              <ProductCard key={product.productId} product={product} />
            ))}
          </div>
        )}
      </main>
    </>
  );
}

export default function LikesPage() {
  return (
    <AuthGuard>
      <LikesContent />
    </AuthGuard>
  );
}
