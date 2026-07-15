"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import Field from "@/components/Field";
import { useAuth } from "@/lib/auth";
import { useToast } from "@/components/Toast";
import { ApiError } from "@/lib/api";

interface Errors {
  email?: string;
  password?: string;
  passwordConfirm?: string;
  username?: string;
}

export default function SignupPage() {
  const router = useRouter();
  const { signup } = useAuth();
  const { showToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [username, setUsername] = useState("");
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);

  const validate = (): Errors => {
    const next: Errors = {};
    if (!email.trim()) next.email = "이메일을 입력해 주세요.";
    if (password.length < 4 || password.length > 12)
      next.password = "비밀번호는 4자 이상 12자 이하여야 합니다.";
    if (password !== passwordConfirm) next.passwordConfirm = "비밀번호가 일치하지 않습니다.";
    if (username.trim().length < 4 || username.trim().length > 8)
      next.username = "닉네임은 4자 이상 8자 이하여야 합니다.";
    return next;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const next = validate();
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setSubmitting(true);
    try {
      await signup(email.trim(), password, username.trim());
      showToast("가입 완료! 로그인 화면으로 이동합니다.");
      router.push("/login");
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "회원가입에 실패했습니다.";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="mx-auto w-full max-w-md px-6 py-12">
      <button
        onClick={() => router.back()}
        className="mb-8 flex items-center gap-1.5 text-sm text-neutral-600 hover:text-neutral-900"
      >
        <ArrowLeft size={18} />
        뒤로가기
      </button>

      <h1 className="mb-8 text-3xl font-extrabold tracking-tight">회원가입</h1>

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
        <Field
          label="비밀번호 확인"
          type="password"
          placeholder="비밀번호를 다시 입력해 주세요."
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          error={errors.passwordConfirm}
        />
        <Field
          label="닉네임"
          placeholder="닉네임을 입력해 주세요."
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          error={errors.username}
        />
        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-lg bg-neutral-900 py-3.5 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-60"
        >
          {submitting ? "가입 중..." : "가입하기"}
        </button>
      </form>
    </main>
  );
}
