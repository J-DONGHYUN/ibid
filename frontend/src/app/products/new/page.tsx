"use client";

import { useMemo, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Camera } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError } from "@/lib/api";
import { useToast } from "@/components/Toast";

function RegisterContent() {
  const router = useRouter();
  const { showToast } = useToast();

  const [title, setTitle] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const valid = useMemo(() => {
    const p = Number(price);
    const s = Number(stock);
    return (
      title.trim().length > 0 &&
      description.trim().length > 0 &&
      Number.isInteger(p) &&
      p > 0 &&
      Number.isInteger(s) &&
      s > 0
    );
  }, [title, price, stock, description]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!valid || submitting) return;
    setSubmitting(true);
    try {
      const { productId } = await api.registerProduct({
        title: title.trim(),
        description: description.trim(),
        price: Number(price),
        stock: Number(stock),
      });
      showToast("검수 대기 상태로 등록되었습니다.");
      router.push(`/products/${productId}`);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "등록에 실패했습니다.";
      showToast(message, "error");
    } finally {
      setSubmitting(false);
    }
  };

  const numericOnly = (v: string) => v.replace(/[^0-9]/g, "");

  return (
    <main className="mx-auto max-w-2xl px-6 py-8 pb-28">
      <h1 className="mb-8 text-2xl font-extrabold">판매하기</h1>

      <form id="register-form" onSubmit={handleSubmit} className="space-y-8">
        <div>
          <p className="mb-2 text-sm font-bold text-neutral-800">
            상품이미지 <span className="text-rose-500">*</span>{" "}
            <span className="font-normal text-neutral-400">(0/12)</span>
          </p>
          <div className="flex h-36 w-36 cursor-not-allowed flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-neutral-200 text-neutral-400">
            <Camera size={24} strokeWidth={1.5} />
            <span className="text-xs">이미지 등록</span>
          </div>
          <div className="mt-3 rounded-lg bg-neutral-50 px-4 py-3 text-xs leading-relaxed text-neutral-400">
            · 이미지 업로드는 아직 지원되지 않아요 (백엔드 미구현)
            <br />· 상품명·가격·재고·설명만 등록됩니다
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">
            상품명 <span className="text-rose-500">*</span>
          </label>
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            maxLength={100}
            placeholder="상품명을 입력해 주세요."
            className="w-full rounded-lg border border-neutral-200 px-4 py-3 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">
            가격 <span className="text-rose-500">*</span>
          </label>
          <div className="relative">
            <input
              value={price}
              onChange={(e) => setPrice(numericOnly(e.target.value))}
              inputMode="numeric"
              placeholder="가격을 입력해 주세요."
              className="w-full rounded-lg border border-neutral-200 px-4 py-3 pr-10 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500"
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-neutral-400">
              원
            </span>
          </div>
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">
            재고 수량 <span className="text-rose-500">*</span>
          </label>
          <input
            value={stock}
            onChange={(e) => setStock(numericOnly(e.target.value))}
            inputMode="numeric"
            placeholder="재고 수량을 입력해 주세요."
            className="w-full rounded-lg border border-neutral-200 px-4 py-3 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500"
          />
        </div>

        <div>
          <label className="mb-1.5 block text-sm font-bold text-neutral-800">
            상품 설명 <span className="text-rose-500">*</span>
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={2000}
            rows={5}
            placeholder="상품 설명을 입력해 주세요."
            className="w-full resize-none rounded-lg border border-neutral-200 px-4 py-3 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500"
          />
        </div>
      </form>

      <div className="fixed inset-x-0 bottom-0 border-t border-neutral-100 bg-white">
        <div className="mx-auto flex max-w-2xl justify-center gap-3 px-6 py-4">
          <button
            type="button"
            onClick={() => showToast("임시 저장은 준비 중이에요.")}
            className="rounded-lg bg-rose-50 px-8 py-3 text-sm font-semibold text-rose-500 hover:bg-rose-100"
          >
            임시 저장
          </button>
          <button
            type="submit"
            form="register-form"
            disabled={!valid || submitting}
            className="rounded-lg bg-neutral-900 px-10 py-3 text-sm font-semibold text-white hover:bg-neutral-800 disabled:bg-neutral-200 disabled:text-neutral-400"
          >
            {submitting ? "등록 중..." : "등록하기"}
          </button>
        </div>
      </div>
    </main>
  );
}

export default function ProductRegisterPage() {
  return (
    <AuthGuard>
      <Header />
      <RegisterContent />
    </AuthGuard>
  );
}
