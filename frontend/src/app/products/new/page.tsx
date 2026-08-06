"use client";

import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Camera, X } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError } from "@/lib/api";
import { useToast } from "@/components/Toast";
import { CONDITION_LABEL } from "@/lib/types";
import type { ProductCondition } from "@/lib/types";

const QUICK_PRICES = [1000, 5000, 10000, 100000];
const TITLE_MAX = 40;
const DESC_MAX = 2000;
const MAX_IMAGES = 12;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/gif"];

function RegisterContent() {
  const router = useRouter();
  const { showToast } = useToast();

  const [title, setTitle] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [description, setDescription] = useState("");
  const [condition, setCondition] = useState<ProductCondition>("LIKE_NEW");
  const [tags, setTags] = useState<string[]>([]);
  const [tagInput, setTagInput] = useState("");
  const [shippingFee, setShippingFee] = useState("");
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const previews = useMemo(() => files.map((f) => URL.createObjectURL(f)), [files]);
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews]);

  const onSelectFiles = (e: ChangeEvent<HTMLInputElement>) => {
    const picked = Array.from(e.target.files ?? []).filter((f) => ALLOWED_TYPES.includes(f.type));
    setFiles((prev) => [...prev, ...picked].slice(0, MAX_IMAGES));
    e.target.value = "";
  };
  const removeFile = (idx: number) => setFiles((prev) => prev.filter((_, i) => i !== idx));

  const valid = useMemo(() => {
    const p = Number(price);
    const s = Number(stock);
    return (
      title.trim().length > 0 &&
      description.trim().length > 0 &&
      files.length > 0 &&
      Number.isInteger(p) &&
      p > 0 &&
      Number.isInteger(s) &&
      s > 0 &&
      shippingFee.trim().length > 0
    );
  }, [title, price, stock, description, files, shippingFee]);

  const numericOnly = (v: string) => v.replace(/[^0-9]/g, "");
  const addPrice = (amount: number) => setPrice(String((Number(price) || 0) + amount));

  const commitTags = () => {
    const t = tagInput.trim().replace(/^#+/, "");
    const next = t && !tags.includes(t) && tags.length < 10 ? [...tags, t] : tags;
    setTags(next);
    setTagInput("");
    return next;
  };
  const removeTag = (t: string) => setTags((prev) => prev.filter((x) => x !== t));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!valid || submitting) return;
    const finalTags = commitTags();
    setSubmitting(true);
    try {
      const { productId } = await api.registerProduct({
        title: title.trim(),
        description: description.trim(),
        price: Number(price),
        stock: Number(stock),
        productCondition: condition,
        tags: finalTags,
        shippingFee: Number(shippingFee),
      });

      if (files.length > 0) {
        const presigns = await api.presignImages(
          productId,
          files.map((f) => ({ filename: f.name, contentType: f.type })),
        );
        await Promise.all(presigns.map((p, i) => api.uploadToS3(p.presignedUrl, files[i])));
        await api.confirmImages(productId, presigns.map((p) => p.imageUrl));
      }

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
          <div>
            <div className="flex flex-wrap gap-3">
              <label className="flex h-28 w-28 shrink-0 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-lg border-2 border-dashed border-neutral-300 text-neutral-400 hover:border-neutral-500 hover:text-neutral-600">
                <Camera size={22} strokeWidth={1.5} />
                <span className="text-xs">이미지 등록</span>
                <span className="text-xs text-neutral-300">
                  {files.length}/{MAX_IMAGES}
                </span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/gif"
                  multiple
                  onChange={onSelectFiles}
                  className="hidden"
                />
              </label>

              {previews.map((src, idx) => (
                <div
                  key={src}
                  className="relative h-28 w-28 shrink-0 overflow-hidden rounded-lg border border-neutral-200"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img src={src} alt={`상품 이미지 ${idx + 1}`} className="h-full w-full object-cover" />
                  {idx === 0 && (
                    <span className="absolute left-1 top-1 rounded bg-neutral-900/80 px-1.5 py-0.5 text-[10px] font-semibold text-white">
                      대표
                    </span>
                  )}
                  <button
                    type="button"
                    onClick={() => removeFile(idx)}
                    className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-neutral-900/70 text-white hover:bg-neutral-900"
                  >
                    <X size={12} />
                  </button>
                </div>
              ))}
            </div>
            <p className="mt-3 text-sm leading-relaxed text-neutral-500">
              · jpg, jpeg, png, gif · 최대 {MAX_IMAGES}장 · 첫 번째 이미지가 대표로 사용돼요.
            </p>
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
          <p className={labelClass}>
            상품상태 <span className="text-rose-500">*</span>
          </p>
          <div className="flex gap-2">
            {(["NEW", "LIKE_NEW", "USED"] as ProductCondition[]).map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setCondition(c)}
                className={`rounded-lg border px-4 py-2.5 text-sm ${
                  condition === c
                    ? "border-neutral-900 bg-neutral-900 text-white"
                    : "border-neutral-200 text-neutral-600 hover:bg-neutral-50"
                }`}
              >
                {CONDITION_LABEL[c]}
              </button>
            ))}
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
          <label className={labelClass} htmlFor="shippingFee">
            배송비 <span className="text-rose-500">*</span>
          </label>
          <div>
            <div className="relative">
              <input
                id="shippingFee"
                value={shippingFee}
                onChange={(e) => setShippingFee(numericOnly(e.target.value))}
                inputMode="numeric"
                placeholder="0 (무료배송)"
                className={`${inputClass} pr-10`}
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-neutral-400">원</span>
            </div>
            <p className="mt-2 text-sm text-neutral-400">결제 시 상품 금액과 합산돼요. 무료배송이면 0을 입력하세요.</p>
          </div>
        </div>

        <div className={rowClass}>
          <label className={labelClass} htmlFor="tags">
            태그
          </label>
          <div>
            {tags.length > 0 && (
              <div className="mb-2 flex flex-wrap gap-2">
                {tags.map((t) => (
                  <span
                    key={t}
                    className="flex items-center gap-1 rounded-md bg-neutral-100 px-2.5 py-1 text-xs text-neutral-600"
                  >
                    #{t}
                    <button
                      type="button"
                      onClick={() => removeTag(t)}
                      className="text-neutral-400 hover:text-neutral-700"
                    >
                      <X size={12} />
                    </button>
                  </span>
                ))}
              </div>
            )}
            <div className="flex gap-2">
              <input
                id="tags"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    commitTags();
                  }
                }}
                placeholder="예: 나이키, 후드"
                className={`${inputClass} flex-1`}
              />
              <button
                type="button"
                onClick={commitTags}
                className="shrink-0 rounded-lg border border-neutral-300 px-5 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
              >
                추가
              </button>
            </div>
            <p className="mt-2 text-sm text-neutral-400">입력 후 추가 버튼 또는 Enter · 최대 10개.</p>
          </div>
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
        등록하면 <span className="font-bold text-[#f0143c]">판매대기</span> 상태로 저장돼요. 상세 페이지에서 <span className="font-bold text-neutral-800">판매 시작</span>을 누르면 판매중으로 전환됩니다.
      </div>

      <div className="sticky bottom-0 z-30 -mx-6 mt-10 flex justify-end border-t border-neutral-100 bg-white px-6 py-4">
        <button
          type="submit"
          form="register-form"
          disabled={!valid || submitting}
          className="rounded-lg bg-[#f0143c] px-14 py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:bg-neutral-200 disabled:text-neutral-400"
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
