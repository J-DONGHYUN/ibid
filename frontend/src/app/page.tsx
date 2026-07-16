"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ChevronDown } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import ProductCard from "@/components/ProductCard";
import { api, ApiError } from "@/lib/api";
import type { ProductSummary } from "@/lib/types";

function HomeContent() {
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [hasNext, setHasNext] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inFlight = useRef(false);

  const fetchPage = useCallback(async (pageCursor: number | null) => {
    if (inFlight.current) return;
    inFlight.current = true;
    setLoading(true);
    setError(null);
    try {
      const res = await api.getProducts(pageCursor);
      setProducts((prev) => {
        const base = pageCursor == null ? [] : prev;
        const seen = new Set(base.map((p) => p.productId));
        return [...base, ...res.products.filter((p) => !seen.has(p.productId))];
      });
      setCursor(res.nextCursor);
      setHasNext(res.hasNext);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "상품을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
      inFlight.current = false;
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchPage(null);
  }, [fetchPage]);

  const loadMore = useCallback(() => {
    if (hasNext) fetchPage(cursor);
  }, [hasNext, cursor, fetchPage]);

  const sentinelRef = useRef<HTMLDivElement | null>(null);
  useEffect(() => {
    const node = sentinelRef.current;
    if (!node || !hasNext) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) loadMore();
      },
      { rootMargin: "200px" },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [hasNext, loadMore]);

  return (
    <>
      <Header />
      <main className="mx-auto max-w-6xl px-6 py-8">
        <h1 className="mb-6 text-lg font-bold">
          전체 상품 <span className="text-neutral-400">{products.length}개</span>
        </h1>

        <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-5">
          {products.map((product) => (
            <ProductCard key={product.productId} product={product} />
          ))}
        </div>

        {error && <p className="mt-8 text-center text-sm text-rose-500">{error}</p>}

        {!loading && products.length === 0 && !error && (
          <p className="mt-16 text-center text-sm text-neutral-400">등록된 상품이 없습니다.</p>
        )}

        <div ref={sentinelRef} className="h-1" />

        {hasNext && (
          <div className="mt-10 flex justify-center">
            <button
              onClick={loadMore}
              disabled={loading}
              className="flex items-center gap-1.5 rounded-full border border-neutral-200 px-6 py-2.5 text-sm font-medium text-neutral-600 hover:bg-neutral-50 disabled:opacity-50"
            >
              {loading ? "불러오는 중..." : "더 보기"}
              {!loading && <ChevronDown size={16} />}
            </button>
          </div>
        )}
      </main>
    </>
  );
}

export default function HomePage() {
  return (
    <AuthGuard>
      <HomeContent />
    </AuthGuard>
  );
}
