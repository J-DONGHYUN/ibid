"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import { ApiError } from "@/lib/api";

export default function AdminLoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password) {
      showToast("운영자 ID와 비밀번호를 입력해 주세요.", "error");
      return;
    }
    setSubmitting(true);
    try {
      await login(email.trim(), password);
      router.push("/admin");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "로그인에 실패했습니다.", "error");
    } finally {
      setSubmitting(false);
    }
  };

  const inputClass =
    "w-full rounded-lg border border-neutral-200 px-4 py-3.5 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500";

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-6">
      <div className="mb-10 text-center">
        <div className="flex items-center justify-center gap-2">
          <span className="text-3xl font-extrabold tracking-tight">ibid</span>
          <span className="rounded-md bg-neutral-900 px-2 py-1 text-xs font-bold tracking-wide text-white">
            ADMIN
          </span>
        </div>
        <p className="mt-3 text-sm text-neutral-400">검수 운영 백오피스</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">운영자 ID</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="admin@ibid.co.kr"
            className={inputClass}
          />
        </div>
        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력해 주세요."
            className={inputClass}
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-neutral-900 py-4 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-60"
        >
          {submitting ? "로그인 중..." : "운영자 로그인"}
        </button>
      </form>

      <div className="mt-5 rounded-lg bg-neutral-50 px-5 py-4 text-sm leading-relaxed text-neutral-500">
        검수 권한 계정만 접근할 수 있습니다. 일반 회원은{" "}
        <Link href="/login" className="font-bold text-neutral-700 underline underline-offset-2">
          일반 로그인
        </Link>
        을 이용해 주세요.
      </div>
    </main>
  );
}
