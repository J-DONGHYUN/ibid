"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronRight, ImageIcon, User } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api } from "@/lib/api";
import {
  orderCounts,
  orderStatusLabel,
  orderStatusTone,
  productStatusLabel,
  productStatusTone,
} from "@/lib/order";
import type { MyTransactions, OrderRole, OrderSummary, ProductSummary } from "@/lib/types";

const SIDEBAR: { group: string; items: string[] }[] = [
  { group: "쇼핑 정보", items: ["구매 내역", "판매 내역", "관심 상품", "후기 쓰기", "포인트", "쿠폰"] },
  { group: "내 정보", items: ["로그인 정보", "프로필 관리", "주소록", "결제 수단 관리", "정산 계좌 관리", "알림 설정"] },
];

const STATS = [
  { badge: "S", label: "판매자 등급", dot: false },
  { badge: "P", label: "OP", dot: false },
  { badge: "%", label: "쿠폰 3", dot: true },
  { badge: "★", label: "후기", dot: false },
  { badge: "+", label: "친구 초대", dot: false },
  { badge: "!", label: "공지사항", dot: true },
];

function SummaryBar({ orders, accent }: { orders: OrderSummary[]; accent: string }) {
  const c = orderCounts(orders);
  const cells = [
    { label: "전체", value: c.total, accent: true },
    { label: "결제 대기", value: c.waiting, accent: false },
    { label: "진행 중", value: c.inProgress, accent: false },
    { label: "종료", value: c.done, accent: false },
  ];
  return (
    <div className="grid grid-cols-4 rounded-xl bg-neutral-50">
      {cells.map((cell, i) => (
        <div key={cell.label} className={`py-6 text-center ${i > 0 ? "border-l border-neutral-200" : ""}`}>
          <p className="text-sm text-neutral-500">{cell.label}</p>
          <p className={`mt-1 text-xl font-bold ${cell.accent ? accent : "text-neutral-900"}`}>{cell.value}</p>
        </div>
      ))}
    </div>
  );
}

function OrderRow({ order, role }: { order: OrderSummary; role: OrderRole }) {
  return (
    <div className="flex items-center gap-4 border-b border-neutral-100 py-5">
      <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-lg bg-neutral-100 text-neutral-300">
        <ImageIcon size={24} strokeWidth={1.5} />
      </div>
      <div className="min-w-0 flex-1">
        <Link href={`/orders/${order.orderId}`} className="truncate font-medium text-neutral-900 hover:underline">
          {order.productTitle}
        </Link>
        <p className="mt-1 text-sm text-neutral-400">수량 {order.quantity}개 · {order.totalPrice.toLocaleString()}원</p>
      </div>
      <div className="shrink-0 text-right">
        <p className={`text-sm font-bold ${orderStatusTone(order.status)}`}>
          {orderStatusLabel(order.status, role)}
        </p>
      </div>
    </div>
  );
}

function OrderSection({
  title,
  role,
  accent,
  orders,
  extra,
}: {
  title: string;
  role: OrderRole;
  accent: string;
  orders: OrderSummary[] | null;
  extra?: React.ReactNode;
}) {
  return (
    <section className="mt-14">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-bold">{title}</h2>
        {extra}
      </div>
      <SummaryBar orders={orders ?? []} accent={accent} />
      <div className="mt-2">
        {orders === null && <p className="py-10 text-center text-sm text-neutral-400">불러오는 중...</p>}
        {orders !== null && orders.length === 0 && (
          <p className="py-10 text-center text-sm text-neutral-400">내역이 없습니다.</p>
        )}
        {orders?.map((o) => (
          <OrderRow key={o.orderId} order={o} role={role} />
        ))}
      </div>
    </section>
  );
}

function ListingSection({ listings }: { listings: ProductSummary[] | null }) {
  const active = listings?.filter((p) => p.status === "ON_SALE" || p.status === "PENDING").length ?? 0;
  return (
    <section className="mt-8">
      <div className="mb-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-bold">판매 중인 상품</h2>
          {active > 0 && (
            <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-bold text-emerald-600">{active}</span>
          )}
        </div>
        <Link href="/products/new" className="flex items-center gap-0.5 text-sm text-neutral-500 hover:text-neutral-900">
          상품 등록 <ChevronRight size={15} />
        </Link>
      </div>
      <div className="rounded-xl border border-neutral-200">
        {listings === null && <p className="py-10 text-center text-sm text-neutral-400">불러오는 중...</p>}
        {listings !== null && listings.length === 0 && (
          <p className="py-10 text-center text-sm text-neutral-400">등록한 상품이 없습니다.</p>
        )}
        {listings?.map((p, i) => (
          <div
            key={p.productId}
            className={`flex items-center gap-4 px-5 py-4 ${i > 0 ? "border-t border-neutral-100" : ""}`}
          >
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-neutral-100 text-neutral-300">
              <ImageIcon size={20} strokeWidth={1.5} />
            </div>
            <div className="min-w-0 flex-1">
              <Link
                href={`/products/${p.productId}`}
                className="truncate font-medium text-neutral-900 hover:underline"
              >
                {p.title}
              </Link>
              <p className="mt-1 text-sm text-neutral-400">
                {p.price.toLocaleString()}원 · 재고 {p.stock}개
              </p>
            </div>
            <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-bold ${productStatusTone(p.status)}`}>
              {productStatusLabel(p.status)}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}

function MyPageContent() {
  const [data, setData] = useState<MyTransactions | null>(null);

  useEffect(() => {
    let alive = true;
    api
      .getMyTransactions()
      .then((res) => alive && setData(res))
      .catch(() => alive && setData({ purchases: [], sales: [], listings: [] }));
    return () => {
      alive = false;
    };
  }, []);

  return (
    <>
      <Header />
      <main className="mx-auto flex max-w-6xl gap-12 px-6 py-10">
        <aside className="hidden w-48 shrink-0 lg:block">
          <h1 className="mb-8 text-2xl font-bold">마이 페이지</h1>
          {SIDEBAR.map((s) => (
            <div key={s.group} className="mb-8">
              <p className="mb-3 text-sm font-bold text-neutral-900">{s.group}</p>
              <ul className="space-y-2.5 text-sm">
                {s.items.map((item, i) => (
                  <li key={item}>
                    <button
                      className={
                        s.group === "쇼핑 정보" && i === 0
                          ? "font-semibold text-neutral-900"
                          : "text-neutral-500 hover:text-neutral-900"
                      }
                    >
                      {item}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </aside>

        <div className="min-w-0 flex-1">
          <div className="flex items-center justify-between rounded-xl border border-neutral-200 px-8 py-7">
            <div className="flex items-center gap-5">
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-neutral-100 text-neutral-400">
                <User size={28} />
              </div>
              <div>
                <p className="text-lg font-bold text-neutral-900">내 계정</p>
                <p className="text-sm text-neutral-400">로그인됨</p>
              </div>
            </div>
            <div className="flex gap-2">
              <button className="rounded-lg border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50">
                프로필 관리
              </button>
              <button className="rounded-lg border border-neutral-200 px-4 py-2 text-sm font-medium text-neutral-700 hover:bg-neutral-50">
                내 상점
              </button>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-3 gap-4 rounded-xl border border-neutral-200 px-6 py-8 sm:grid-cols-6">
            {STATS.map((s) => (
              <button key={s.label} className="flex flex-col items-center gap-2.5">
                <span className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-neutral-300 text-sm font-bold text-neutral-700">
                  {s.badge}
                  {s.dot && <span className="absolute -right-0.5 -top-0.5 h-2 w-2 rounded-full bg-[#f0143c]" />}
                </span>
                <span className="text-xs text-neutral-500">{s.label}</span>
              </button>
            ))}
          </div>

          <ListingSection listings={data?.listings ?? null} />

          <OrderSection title="구매 내역" role="buyer" accent="text-[#f0143c]" orders={data?.purchases ?? null} />
          <OrderSection
            title="판매 내역"
            role="seller"
            accent="text-emerald-600"
            orders={data?.sales ?? null}
            extra={
              <Link href="/products/new" className="flex items-center gap-0.5 text-sm text-neutral-500 hover:text-neutral-900">
                판매하기 <ChevronRight size={15} />
              </Link>
            }
          />
        </div>
      </main>
    </>
  );
}

export default function MyPage() {
  return (
    <AuthGuard>
      <MyPageContent />
    </AuthGuard>
  );
}
