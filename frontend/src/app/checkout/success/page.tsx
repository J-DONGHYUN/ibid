"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError } from "@/lib/api";
import { formatWon } from "@/lib/format";

function SuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [confirmed, setConfirmed] = useState(false);
  const [message, setMessage] = useState("");
  const requested = useRef(false);

  const paymentId = searchParams.get("paymentId");
  const orderId = searchParams.get("orderId");
  const amount = searchParams.get("amount");
  const paymentKey = searchParams.get("paymentKey");
  const internalOrderId = searchParams.get("internalOrderId");

  useEffect(() => {
    if (requested.current || !paymentId || !orderId || !amount || !paymentKey || !internalOrderId) return;
    requested.current = true;

    (async () => {
      try {
        await api.confirmPayment(Number(paymentId), { orderId, amount, paymentKey });
        setConfirmed(true);
        router.replace(`/orders/${internalOrderId}`);
      } catch (e) {
        setMessage(e instanceof ApiError ? e.message : "결제 승인에 실패했습니다.");
      }
    })();
  }, [paymentId, orderId, amount, paymentKey, internalOrderId, router]);

  if (message) {
    return (
      <main className="mx-auto max-w-lg px-6 py-8">
        <div className="rounded-2xl bg-white p-6 shadow-sm">
          <h2 className="text-lg font-bold">결제 실패</h2>
          <p className="mt-2 text-sm text-neutral-600">{message}</p>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-lg px-6 py-8">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-lg font-bold">{confirmed ? "결제 성공" : "결제 승인 중..."}</h2>
        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-neutral-400">주문번호</dt>
            <dd>{orderId}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-neutral-400">결제금액</dt>
            <dd>{amount ? formatWon(Number(amount)) : amount}원</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-neutral-400">paymentKey</dt>
            <dd className="break-all">{paymentKey}</dd>
          </div>
        </dl>
      </div>
    </main>
  );
}

export default function SuccessPage() {
  return (
    <AuthGuard>
      <Header />
      <Suspense fallback={null}>
        <SuccessContent />
      </Suspense>
    </AuthGuard>
  );
}
