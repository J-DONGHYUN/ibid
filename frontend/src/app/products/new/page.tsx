"use client";

import { useMemo, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Camera } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError } from "@/lib/api";
import { useToast } from "@/components/Toast";

const QUICK_PRICES = [1000, 5000, 10000, 100000];
const TITLE_MAX = 40;
const DESC_MAX = 2000;

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

  const numericOnly = (v: string) => v.replace(/[^0-9]/g, "");
  const addPrice = (amount: number) => setPrice(String((Number(price) || 0) + amount));

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
      showToast(err instanceof ApiError ? err.message : "등록에 실패했습니다.", "error");
    } finally {
      setSubmitting(false);
    }
  };

  const rowClass = "grid grid-cols-1 gap-3 border-b border-neutral-100 py-8 sm:grid-cols-[140px_1fr] sm:gap-8";
  const labelClass = "text-sm font-bold text-neutral-800";
  const inputClass =
    "w-full rounded-lg border border-neutral-200 px-4 py-3 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500";

  return (
    <main className="mx-auto max-w-4xl px-6 py-8">
      <button
        onClick={() => router.back()}
        className="mb-5 flex items-center gap-1 text-sm text-neutral-500 hover:text-neutral-900"
      >
        <ArrowLeft size={18} />
        뒤로
      </button>

      <div className="flex items-end justify-between border-b-2 border-neutral-900 pb-4">
        <h1 className="text-2xl font-extrabold">판매하기</h1>
        <span className="text-sm text-neutral-500">
          <span className="text-rose-500">*</span> 필수 입력 항목
        </span>
      </div>

      <form id="register-form" onSubmit={handleSubmit}>
        <div className={rowClass}>
          <p className={labelClass}>
            상품이미지 <span className="text-rose-500">*</span>
          </p>
          <div className="flex flex-col gap-4 sm:flex-row">
            <div className="flex h-36 w-36 shrink-0 cursor-not-allowed flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed border-neutral-200 text-neutral-400">
              <Camera size={24} strokeWidth={1.5} />
              <span className="text-xs">이미지 등록</span>
              <span className="text-xs text-neutral-300">0/12</span>
            </div>
            <div className="flex-1 rounded-lg bg-neutral-50 px-5 py-4 text-sm leading-relaxed text-neutral-500">
              · 클릭 또는 이미지를 드래그하여 등록할 수 있어요.
              <br />· 드래그하여 상품 이미지 순서를 변경할 수 있어요.
              <br />· 첫 번째 이미지가 대표 이미지로 사용돼요.
            </div>
          </div>
        </div>

        <div className={rowClass}>
          <label className={labelClass} htmlFor="title">
            상품명 <span className="text-rose-500">*</span>
          </label>
          <div>
            <input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              maxLength={TITLE_MAX}
              placeholder="상품명을 입력해 주세요."
              className={inputClass}
            />
            <p className="mt-1.5 text-right text-xs text-neutral-400">
              {title.length}/{TITLE_MAX}
            </p>
          </div>
        </div>

        <div className={rowClass}>
          <label className={labelClass} htmlFor="price">
            가격 <span className="text-rose-500">*</span>
          </label>
          <div>
            <div className="relative">
              <input
                id="price"
                value={price}
                onChange={(e) => setPrice(numericOnly(e.target.value))}
                inputMode="numeric"
                placeholder="가격을 입력해 주세요."
                className={`${inputClass} pr-10`}
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-neutral-400">원</span>
            </div>
            <div className="mt-2.5 flex gap-2">
              {QUICK_PRICES.map((amount) => (
                <button
                  key={amount}
                  type="button"
                  onClick={() => addPrice(amount)}
                  className="rounded-lg border border-neutral-200 px-3.5 py-2 text-sm text-neutral-600 hover:bg-neutral-50"
                >
                  +{amount.toLocaleString()}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className={rowClass}>
          <label className={labelClass} htmlFor="stock">
            재고 수량 <span className="text-rose-500">*</span>
          </label>
          <input
            id="stock"
            value={stock}
            onChange={(e) => setStock(numericOnly(e.target.value))}
            inputMode="numeric"
            placeholder="재고 수량을 입력해 주세요."
            className={inputClass}
          />
        </div>

        <div className={rowClass}>
          <label className={labelClass} htmlFor="desc">
            상품 설명 <span className="text-rose-500">*</span>
          </label>
          <div>
            <textarea
              id="desc"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              maxLength={DESC_MAX}
              rows={8}
              placeholder="상품에 대한 설명을 자세히 적어주세요. (구매 시기, 사용감, 하자 여부 등)"
              className={`${inputClass} resize-none`}
            />
            <p className="mt-1.5 text-right text-xs text-neutral-400">
              {description.length}/{DESC_MAX}
            </p>
          </div>
        </div>
      </form>

      <div className="mt-8 rounded-lg bg-neutral-50 px-5 py-4 text-sm text-neutral-500">
        등록된 상품은 검수 후 노출되며, 검수 전까지 <span className="font-bold text-[#f0143c]">판매대기</span> 상태로 표시됩니다.
      </div>

      <div className="sticky bottom-0 z-30 -mx-6 mt-10 flex justify-center gap-3 border-t border-neutral-100 bg-white px-6 py-4">
        <button
          type="button"
          onClick={() => showToast("임시 저장은 준비 중이에요.")}
          className="rounded-lg border border-neutral-300 px-10 py-3 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
        >
          임시 저장
        </button>
        <button
          type="submit"
          form="register-form"
          disabled={!valid || submitting}
          className="rounded-lg bg-[#f0143c] px-12 py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:bg-neutral-200 disabled:text-neutral-400"
        >
          {submitting ? "등록 중..." : "등록하기"}
        </button>
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
