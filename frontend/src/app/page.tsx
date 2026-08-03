"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { ChevronDown, ChevronLeft, ChevronRight, ImageIcon } from "lucide-react";
import Header from "@/components/Header";
import ProductCard from "@/components/ProductCard";
import { api, ApiError } from "@/lib/api";
import type { ProductSummary } from "@/lib/types";

type SortKey = "latest" | "low" | "high";

const SORTS: { key: SortKey; label: string }[] = [
  { key: "latest", label: "최신순" },
  { key: "low", label: "낮은 가격순" },
  { key: "high", label: "높은 가격순" },
];

function HomeContent() {
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [cursor, setCursor] = useState<number | null>(null);
  const [hasNext, setHasNext] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sort, setSort] = useState<SortKey>("latest");
  const inFlight = useRef(false);
  const recRef = useRef<HTMLDivElement | null>(null);

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

  const recommended = products.slice(0, 6);

  const sorted = useMemo(() => {
    const list = [...products];
    if (sort === "low") list.sort((a, b) => a.price - b.price);
    if (sort === "high") list.sort((a, b) => b.price - a.price);
    return list;
  }, [products, sort]);

  const scrollRec = (dir: number) => recRef.current?.scrollBy({ left: dir * 320, behavior: "smooth" });

  return (
    <>
      <Header />
      <main className="mx-auto max-w-6xl px-6 py-8">
        <section className="relative mb-10 overflow-hidden rounded-2xl bg-[#E8F1FE] px-8 py-12 sm:px-12">
          <div className="max-w-md">
            <h2 className="text-2xl font-extrabold leading-snug text-neutral-900 sm:text-3xl">
              첫 거래 수수료 0원
              <br />
              지금 ibid에서 판매 시작하기
            </h2>
            <Link
              href="/products/new"
              className="mt-5 inline-flex items-center gap-1 text-sm font-bold text-neutral-800 hover:text-neutral-950"
            >
              판매하러 가기
              <ChevronRight size={16} />
            </Link>
          </div>
          <div className="pointer-events-none absolute right-12 top-1/2 hidden h-40 w-56 -translate-y-1/2 items-center justify-center rounded-xl bg-white/70 text-neutral-300 lg:flex">
            <ImageIcon size={40} strokeWidth={1.5} />
          </div>
          <span className="absolute bottom-4 right-6 rounded-full bg-neutral-500/30 px-2.5 py-0.5 text-xs font-medium text-neutral-600">
            1/3
          </span>
        </section>

        {recommended.length > 0 && (
          <section className="mb-12">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold">오늘의 추천 아이템</h2>
              <div className="flex gap-1.5">
                <button
                  onClick={() => scrollRec(-1)}
                  className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-200 text-neutral-500 hover:bg-neutral-50"
                >
                  <ChevronLeft size={16} />
                </button>
                <button
                  onClick={() => scrollRec(1)}
                  className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-200 text-neutral-500 hover:bg-neutral-50"
                >
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
            <div
              ref={recRef}
              className="flex gap-4 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            >
              {recommended.map((product) => (
                <div key={product.productId} className="w-44 shrink-0">
                  <ProductCard product={product} />
                </div>
              ))}
            </div>
          </section>
        )}

        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-lg font-bold">
            전체 상품 <span className="text-neutral-400">{products.length}개</span>
          </h1>
          <div className="flex gap-1.5 text-sm">
            {SORTS.map((s) => (
              <button
                key={s.key}
                onClick={() => setSort(s.key)}
                className={`rounded-full px-3.5 py-1.5 font-medium transition-colors ${
                  sort === s.key
                    ? "bg-neutral-900 text-white"
                    : "text-neutral-500 hover:bg-neutral-100"
                }`}
              >
                {s.label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-x-4 gap-y-8 sm:grid-cols-3 lg:grid-cols-5">
          {sorted.map((product) => (
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
  return <HomeContent />;
}
