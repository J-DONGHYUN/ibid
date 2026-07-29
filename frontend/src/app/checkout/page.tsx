"use client";

import { Suspense, useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { loadTossPayments } from "@tosspayments/tosspayments-sdk";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { useToast } from "@/components/Toast";
import { api, ApiError } from "@/lib/api";

const CLIENT_KEY = "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm";

function generateRandomString() {
  return window.btoa(Math.random().toString()).slice(0, 20);
}

function CheckoutContent({
  orderId,
  amount,
  orderName,
}: {
  orderId: number;
  amount: number;
  orderName: string;
}) {
  const { showToast } = useToast();
  const [widgets, setWidgets] = useState<Awaited<
    ReturnType<Awaited<ReturnType<typeof loadTossPayments>>["widgets"]>
  > | null>(null);
  const [ready, setReady] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      const tossPayments = await loadTossPayments(CLIENT_KEY);
      const widgets = tossPayments.widgets({ customerKey: generateRandomString() });
      if (active) setWidgets(widgets);
    })();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!widgets) return;
    let active = true;
    (async () => {
      await widgets.setAmount({ currency: "KRW", value: amount });
      await Promise.all([
        widgets.renderPaymentMethods({ selector: "#payment-method", variantKey: "DEFAULT" }),
        widgets.renderAgreement({ selector: "#agreement", variantKey: "AGREEMENT" }),
      ]);
      if (active) setReady(true);
    })();
    return () => {
      active = false;
    };
  }, [widgets, amount]);

  const handlePay = async () => {
    if (!widgets || submitting) return;
    setSubmitting(true);
    try {
      const { paymentId } = await api.createPayment(orderId);
      await widgets.requestPayment({
        orderId: generateRandomString(),
        orderName,
        successUrl: `${window.location.origin}/checkout/success?paymentId=${paymentId}&internalOrderId=${orderId}`,
        failUrl: `${window.location.origin}/checkout/fail?paymentId=${paymentId}`,
      });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "결제 준비에 실패했습니다.", "error");
      setSubmitting(false);
    }
  };

  return (
    <main className="mx-auto max-w-lg px-6 py-8">
      <h1 className="mb-6 text-xl font-bold">결제하기</h1>
      <p className="mb-4 truncate text-sm text-neutral-600">{orderName}</p>
      <div id="payment-method" />
      <div id="agreement" className="mt-4" />
      <button
        onClick={handlePay}
        disabled={!ready || submitting}
        className="mt-6 w-full rounded-xl bg-[#f0143c] py-3.5 text-base font-bold text-white transition-opacity hover:opacity-90 disabled:opacity-60"
      >
        {submitting ? "처리 중..." : "결제하기"}
      </button>
    </main>
  );
}

function CheckoutParams() {
  const searchParams = useSearchParams();
  const orderId = Number(searchParams.get("orderId"));
  const amount = Number(searchParams.get("amount"));
  const orderName = searchParams.get("orderName") ?? "상품 결제";

  if (!orderId || !amount) {
    return <p className="py-24 text-center text-sm text-rose-500">잘못된 결제 요청입니다.</p>;
  }

  return <CheckoutContent orderId={orderId} amount={amount} orderName={orderName} />;
}

export default function CheckoutPage() {
  return (
    <AuthGuard>
      <Header />
      <Suspense fallback={null}>
        <CheckoutParams />
      </Suspense>
    </AuthGuard>
  );
}
