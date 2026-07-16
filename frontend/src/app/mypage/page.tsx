"use client";

import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";

export default function MyPage() {
  return (
    <AuthGuard>
      <Header />
      <main className="mx-auto max-w-6xl px-6 py-24 text-center">
        <h1 className="text-lg font-bold">마이페이지</h1>
        <p className="mt-2 text-sm text-neutral-400">준비 중입니다.</p>
      </main>
    </AuthGuard>
  );
}
