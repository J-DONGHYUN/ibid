"use client";

import { useEffect, useMemo, useState, type ChangeEvent, type FormEvent } from "react";
import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, Camera, X } from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import { api, ApiError, currentUserId } from "@/lib/api";
import { useToast } from "@/components/Toast";
import { CONDITION_LABEL } from "@/lib/types";
import type { ProductCondition } from "@/lib/types";

const MAX_IMAGES = 12;
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/gif"];
const TITLE_MAX = 40;
const DESC_MAX = 2000;

function EditContent({ id }: { id: number }) {
  const router = useRouter();
  const { showToast } = useToast();

  const [loaded, setLoaded] = useState(false);
  const [title, setTitle] = useState("");
  const [price, setPrice] = useState("");
  const [stock, setStock] = useState("");
  const [description, setDescription] = useState("");
  const [condition, setCondition] = useState<ProductCondition>("LIKE_NEW");
  const [images, setImages] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const p = await api.getProduct(id);
        if (!active) return;
        if (currentUserId() !== p.sellerId) {
          showToast("본인 상품만 수정할 수 있어요.", "error");
          router.replace(`/products/${id}`);
          return;
        }
        setTitle(p.title);
        setPrice(String(p.price));
        setStock(String(p.stock));
        setDescription(p.description);
        setCondition(p.productCondition);
        setImages(p.imageUrls);
        setLoaded(true);
      } catch (e) {
        showToast(e instanceof ApiError ? e.message : "상품을 불러오지 못했습니다.", "error");
      }
    })();
    return () => {
      active = false;
    };
  }, [id, router, showToast]);

  const numericOnly = (v: string) => v.replace(/[^0-9]/g, "");

  const valid = useMemo(() => {
    const p = Number(price);
    const s = Number(stock);
    return (
      title.trim().length > 0 &&
      description.trim().length > 0 &&
      images.length > 0 &&
      Number.isInteger(p) &&
      p > 0 &&
      Number.isInteger(s) &&
      s > 0
    );
  }, [title, price, stock, description, images]);

  const onAddImages = async (e: ChangeEvent<HTMLInputElement>) => {
    const picked = Array.from(e.target.files ?? []).filter((f) => ALLOWED_TYPES.includes(f.type));
    e.target.value = "";
    const files = picked.slice(0, MAX_IMAGES - images.length);
    if (files.length === 0) {
      if (picked.length > 0) showToast(`최대 ${MAX_IMAGES}장까지 등록할 수 있어요.`);
      return;
    }
    setUploading(true);
    try {
      const presigns = await api.presignImages(
        id,
        files.map((f) => ({ filename: f.name, contentType: f.type })),
      );
      await Promise.all(presigns.map((pr, i) => api.uploadToS3(pr.presignedUrl, files[i])));
      const newUrls = presigns.map((pr) => pr.imageUrl);
      await api.confirmImages(id, newUrls);
      setImages((prev) => [...prev, ...newUrls]);
      showToast("이미지를 추가했어요.");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "이미지 추가에 실패했어요.", "error");
    } finally {
      setUploading(false);
    }
  };

  const onRemoveImage = async (url: string) => {
    try {
      await api.deleteImages(id, [url]);
      setImages((prev) => prev.filter((u) => u !== url));
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "이미지 삭제에 실패했어요.", "error");
    }
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!valid || saving) return;
    setSaving(true);
    try {
      await api.updateProduct(id, {
        title: title.trim(),
        description: description.trim(),
        price: Number(price),
        stock: Number(stock),
        productCondition: condition,
      });
      showToast("수정되었습니다.");
      router.push(`/products/${id}`);
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "수정에 실패했습니다.", "error");
    } finally {
      setSaving(false);
    }
  };

  const rowClass = "grid grid-cols-1 gap-3 border-b border-neutral-100 py-8 sm:grid-cols-[140px_1fr] sm:gap-8";
  const labelClass = "text-sm font-bold text-neutral-800";
  const inputClass =
    "w-full rounded-lg border border-neutral-200 px-4 py-3 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-500";

  if (!loaded) return <p className="py-24 text-center text-sm text-neutral-400">불러오는 중...</p>;

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
        <h1 className="text-2xl font-extrabold">상품 수정</h1>
        <span className="text-sm text-neutral-500">
          이미지는 즉시 반영돼요
        </span>
      </div>

      <form id="edit-form" onSubmit={handleSave}>
        <div className={rowClass}>
          <p className={labelClass}>
            상품이미지 <span className="text-rose-500">*</span>
          </p>
          <div>
            <div className="flex flex-wrap gap-3">
              <label
                className={`flex h-28 w-28 shrink-0 flex-col items-center justify-center gap-1.5 rounded-lg border-2 border-dashed border-neutral-300 text-neutral-400 ${
                  uploading ? "cursor-wait opacity-60" : "cursor-pointer hover:border-neutral-500 hover:text-neutral-600"
                }`}
              >
                <Camera size={22} strokeWidth={1.5} />
                <span className="text-xs">{uploading ? "업로드 중..." : "이미지 추가"}</span>
                <span className="text-xs text-neutral-300">
                  {images.length}/{MAX_IMAGES}
                </span>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/gif"
                  multiple
                  disabled={uploading}
                  onChange={onAddImages}
                  className="hidden"
                />
              </label>

              {images.map((src, idx) => (
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
                    onClick={() => onRemoveImage(src)}
                    className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-neutral-900/70 text-white hover:bg-neutral-900"
                  >
                    <X size={12} />
                  </button>
                </div>
              ))}
            </div>
            <p className="mt-3 text-sm leading-relaxed text-neutral-500">
              · jpg, jpeg, png, gif · 최대 {MAX_IMAGES}장 · X를 누르면 S3에서도 즉시 삭제돼요.
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
          <div className="relative">
            <input
              id="price"
              value={price}
              onChange={(e) => setPrice(numericOnly(e.target.value))}
              inputMode="numeric"
              className={`${inputClass} pr-10`}
            />
            <span className="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-neutral-400">원</span>
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
              className={`${inputClass} resize-none`}
            />
            <p className="mt-1.5 text-right text-xs text-neutral-400">
              {description.length}/{DESC_MAX}
            </p>
          </div>
        </div>
      </form>

      <div className="sticky bottom-0 z-30 -mx-6 mt-10 flex justify-center gap-3 border-t border-neutral-100 bg-white px-6 py-4">
        <button
          type="button"
          onClick={() => router.push(`/products/${id}`)}
          className="rounded-lg border border-neutral-300 px-10 py-3 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
        >
          취소
        </button>
        <button
          type="submit"
          form="edit-form"
          disabled={!valid || saving}
          className="rounded-lg bg-[#f0143c] px-12 py-3 text-sm font-semibold text-white transition-opacity hover:opacity-90 disabled:bg-neutral-200 disabled:text-neutral-400"
        >
          {saving ? "저장 중..." : "저장"}
        </button>
      </div>
    </main>
  );
}

export default function ProductEditPage() {
  const params = useParams();
  const id = Number(params.id);
  return (
    <AuthGuard>
      <Header />
      <EditContent id={id} />
    </AuthGuard>
  );
}
