"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Field from "@/components/Field";
import { useAuth } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import { ApiError } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next: typeof errors = {};
    if (!email.trim()) next.email = "이메일을 입력해 주세요.";
    if (!password) next.password = "비밀번호를 입력해 주세요.";
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSubmitting(true);
    try {
      await login(email.trim(), password);
      router.push("/");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "로그인에 실패했습니다.";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-md flex-col justify-center px-6">
      <div className="mb-10 text-center">
        <h1 className="text-4xl font-extrabold tracking-tight">ibid</h1>
        <p className="mt-2 text-sm text-neutral-400">중고거래 플랫폼</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <Field
          label="이메일"
          type="email"
          placeholder="이메일을 입력해 주세요."
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={errors.email}
        />
        <Field
          label="비밀번호"
          type="password"
          placeholder="비밀번호를 입력해 주세요."
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          error={errors.password}
        />
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-neutral-900 py-3.5 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-60"
        >
          {submitting ? "로그인 중..." : "로그인"}
        </button>
      </form>

      <div className="my-6 flex items-center gap-4 text-xs text-neutral-300">
        <span className="h-px flex-1 bg-neutral-100" />
        또는
        <span className="h-px flex-1 bg-neutral-100" />
      </div>

      <div className="text-center">
        <Link href="/signup" className="text-sm font-bold underline underline-offset-4">
          회원가입
        </Link>
      </div>

      <div className="mt-8 text-center">
        <Link
          href="/admin/login"
          className="text-xs text-neutral-400 underline underline-offset-4 hover:text-neutral-600"
        >
          운영자 로그인
        </Link>
      </div>
    </main>
  );
}
