"use client";

import { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";

function FailContent() {
  const searchParams = useSearchParams();
  const code = searchParams.get("code");
  const message = searchParams.get("message");

  return (
    <main className="mx-auto max-w-lg px-6 py-8">
      <div className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="text-lg font-bold">결제 실패</h2>
        <dl className="mt-4 space-y-2 text-sm">
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-neutral-400">에러 코드</dt>
            <dd>{code}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="w-20 shrink-0 text-neutral-400">실패 사유</dt>
            <dd>{message}</dd>
          </div>
        </dl>
      </div>
    </main>
  );
}

export default function FailPage() {
  return (
    <AuthGuard>
      <Header />
      <Suspense fallback={null}>
        <FailContent />
      </Suspense>
    </AuthGuard>
  );
}
