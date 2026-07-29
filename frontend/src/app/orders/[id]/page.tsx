"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, ImageIcon } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError, currentUserId } from "@/lib/api";
import { useToast } from "@/components/Toast";
import { formatWon } from "@/lib/format";
import type { OrderDetail, OrderStatus } from "@/lib/types";

const HEADLINE: Record<OrderStatus, { title: string; desc: string }> = {
  CREATED: { title: "결제 대기", desc: "결제를 완료하면 거래가 시작돼요." },
  PAID: { title: "결제 완료", desc: "판매자의 발송을 기다리고 있어요." },
  SHIPPED_TO_INSPECTOR: { title: "발송 완료", desc: "상품이 검수업체로 발송되었어요." },
  UNDER_INSPECTION: { title: "검수 중", desc: "검수업체가 상품을 확인하고 있어요." },
  COMPLETED: { title: "종료된 거래", desc: "배송이 완료되어 거래가 종료되었어요." },
  REFUNDED: { title: "환불된 거래", desc: "검수 불합격으로 환불되었어요." },
  CANCELED: { title: "취소된 거래", desc: "결제가 완료되지 않아 취소되었어요." },
};

const STEPS = ["발송완료", "입고완료", "검수합격", "배송완료"];

function doneSteps(status: OrderStatus): number {
  switch (status) {
    case "SHIPPED_TO_INSPECTOR":
      return 1;
    case "UNDER_INSPECTION":
      return 2;
    case "COMPLETED":
      return 4;
    default:
      return 0;
  }
}

function DetailContent({ id }: { id: number }) {
  const router = useRouter();
  const { showToast } = useToast();
  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const handleShip = async () => {
    if (!order || busy) return;
    setBusy(true);
    try {
      await api.ship(order.orderId);
      setOrder({ ...order, status: "SHIPPED_TO_INSPECTOR" });
      showToast("발송 처리했습니다. 검수센터 입고를 기다려 주세요.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "발송 처리에 실패했습니다.", "error");
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    let active = true;
    api
      .getMyOrder(id)
      .then((o) => active && setOrder(o))
      .catch((e) => active && setError(e instanceof ApiError ? e.message : "거래를 불러오지 못했습니다."));
    return () => {
      active = false;
    };
  }, [id]);

  if (error) return <p className="py-24 text-center text-sm text-rose-500">{error}</p>;
  if (!order) return <p className="py-24 text-center text-sm text-neutral-400">불러오는 중...</p>;

  const head = HEADLINE[order.status];
  const done = doneSteps(order.status);

  return (
    <main className="mx-auto max-w-3xl px-6 py-8">
      <button
        onClick={() => router.push("/mypage")}
        className="mb-6 flex items-center gap-1.5 text-sm text-neutral-500 hover:text-neutral-900"
      >
        <ArrowLeft size={18} />
        마이페이지
      </button>

      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">{head.title}</h1>
          <p className="mt-1 text-sm text-neutral-500">{head.desc}</p>
        </div>
        <div className="flex gap-4 pt-1 text-sm text-neutral-400">
          <button className="hover:text-neutral-700">거래 내역 전체</button>
          <button className="hover:text-neutral-700">1:1 문의하기</button>
        </div>
      </div>

      {currentUserId() === order.sellerId && order.status === "PAID" && (
        <div className="mt-6 rounded-2xl bg-neutral-900 p-6 text-white">
          <p className="font-bold">구매자가 결제를 완료했어요</p>
          <p className="mt-1 text-sm text-neutral-300">상품을 검수센터로 발송한 뒤 아래 버튼을 눌러 주세요.</p>
          <button
            onClick={handleShip}
            disabled={busy}
            className="mt-4 w-full rounded-lg bg-[#f0143c] py-3 text-sm font-bold text-white hover:opacity-90 disabled:opacity-50"
          >
            {busy ? "처리 중..." : "발송 완료"}
          </button>
        </div>
      )}

      <p className="mt-8 text-sm font-bold">주문번호 #{order.orderId}</p>
      <div className="mt-3 border-t border-neutral-900" />

      {/* 상품 */}
      <div className="flex gap-4 py-6">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-lg bg-neutral-100 text-neutral-300">
          <ImageIcon size={28} strokeWidth={1.5} />
        </div>
        <div>
          <p className="font-bold">{order.productTitle}</p>
          <p className="mt-2 text-sm font-bold">수량 {order.quantity}개 / 일반배송</p>
        </div>
      </div>
      <Link
        href={`/products/${order.productId}`}
        className="block rounded-lg border border-neutral-200 py-3.5 text-center text-sm font-medium text-neutral-700 hover:bg-neutral-50"
      >
        상품 상세
      </Link>

      {/* 진행 상황 */}
      <div className="mt-6 rounded-2xl bg-neutral-50 p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-bold">진행 상황</h2>
          <span className="text-sm text-neutral-400">{head.title}</span>
        </div>
        <div className="grid grid-cols-4 gap-2">
          {STEPS.map((s, i) => {
            const active = i < done;
            return (
              <div key={s}>
                <div className={`h-1 rounded-full ${active ? "bg-neutral-900" : "bg-neutral-200"}`} />
                <p className={`mt-2 text-sm ${active ? "font-bold text-neutral-900" : "text-neutral-400"}`}>{s}</p>
              </div>
            );
          })}
        </div>
      </div>

      {/* 결제 내역 */}
      <Section title="결제 내역">
        <SpecRow label="즉시 구매가">
          <span className="font-bold">{formatWon(order.totalPrice)}원</span>
        </SpecRow>
        <SpecRow label="검수비">무료</SpecRow>
        <SpecRow label="수수료">-</SpecRow>
        <SpecRow label="배송비">-</SpecRow>
        <SpecRow label="쿠폰 사용">-</SpecRow>
      </Section>
      <button className="mt-3 block w-full rounded-lg border border-neutral-200 py-3.5 text-center text-sm font-medium text-neutral-700 hover:bg-neutral-50">
        결제 내역 상세보기
      </button>

      {/* 배송 정보 (정적) */}
      <Section title="배송 정보">
        <p className="text-sm text-neutral-400">배송 정보는 결제/배송 연동 후 표시됩니다.</p>
      </Section>

      <Section title="배송 주소">
        <SpecRow label="받는 사람">-</SpecRow>
        <SpecRow label="휴대폰 번호">-</SpecRow>
        <SpecRow label="주소">-</SpecRow>
      </Section>
    </main>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-10">
      <h2 className="mb-3 font-bold">{title}</h2>
      <div className="border-t border-neutral-900 pt-4">
        <div className="space-y-3">{children}</div>
      </div>
    </section>
  );
}

function SpecRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-8 text-sm">
      <dt className="w-24 shrink-0 text-neutral-400">{label}</dt>
      <dd className="text-neutral-700">{children}</dd>
    </div>
  );
}

export default function OrderDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  return (
    <AuthGuard>
      <Header />
      {Number.isNaN(id) ? (
        <p className="py-24 text-center text-sm text-rose-500">잘못된 거래입니다.</p>
      ) : (
        <DetailContent id={id} />
      )}
    </AuthGuard>
  );
}
