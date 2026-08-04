"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  ChevronLeft,
  ChevronRight,
  Eye,
  Heart,
  ImageIcon,
  Info,
  Share2,
  X,
} from "lucide-react";
import Header from "@/components/Header";
import AuthGuard from "@/components/AuthGuard";
import PurchaseModal from "@/components/PurchaseModal";
import { useToast } from "@/components/Toast";
import { api, ApiError, currentUserId } from "@/lib/api";
import { formatWon, sellerName, timeAgo } from "@/lib/format";
import { CONDITION_LABEL } from "@/lib/types";
import type { ProductDetail } from "@/lib/types";

const TAGS = ["#나이키", "#AirMax90", "#에어맥스", "#운동화", "#270"];
const SIMILAR = [35000, 129000, 78000, 92000, 64000, 155000];

function DetailContent({ id }: { id: number }) {
  const router = useRouter();
  const { showToast } = useToast();
  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [liked, setLiked] = useState(false);
  const [likeCount, setLikeCount] = useState(0);
  const [expanded, setExpanded] = useState(false);
  const [starting, setStarting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [imgIdx, setImgIdx] = useState(0);
  const [deleting, setDeleting] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [shareUrl, setShareUrl] = useState("");

  const handleStartSale = async () => {
    if (starting) return;
    setStarting(true);
    try {
      await api.openForSale(id);
      showToast("판매를 시작했습니다.");
      setReloadKey((k) => k + 1);
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "판매 시작에 실패했습니다.", "error");
    } finally {
      setStarting(false);
    }
  };

  const openShare = () => {
    setShareUrl(window.location.href);
    setShareOpen(true);
  };

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareUrl);
      showToast("링크를 복사했어요.");
      setShareOpen(false);
    } catch {
      showToast("복사에 실패했어요. 직접 복사해주세요.", "error");
    }
  };

  const handleDelete = async () => {
    if (deleting) return;
    if (!window.confirm("이 상품을 삭제할까요? 등록한 이미지도 함께 삭제됩니다.")) return;
    setDeleting(true);
    try {
      await api.deleteProduct(id);
      showToast("상품을 삭제했습니다.");
      router.push("/");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "삭제에 실패했습니다.", "error");
    } finally {
      setDeleting(false);
    }
  };

  const toggleLike = async () => {
    const next = !liked;
    setLiked(next);
    setLikeCount((c) => Math.max(0, c + (next ? 1 : -1)));
    try {
      if (next) await api.likeProduct(id);
      else await api.unlikeProduct(id);
    } catch (e) {
      setLiked(!next);
      setLikeCount((c) => Math.max(0, c + (next ? -1 : 1)));
      showToast(e instanceof ApiError ? e.message : "찜 처리에 실패했어요.", "error");
    }
  };

  useEffect(() => {
    let active = true;
    api
      .getLikeStatus(id)
      .then((s) => {
        if (active) {
          setLiked(s.liked);
          setLikeCount(s.count);
        }
      })
      .catch(() => {});
    return () => {
      active = false;
    };
  }, [id, reloadKey]);

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        const data = await api.getProduct(id);
        if (active) setProduct(data);
      } catch (e) {
        if (active) setError(e instanceof ApiError ? e.message : "상품을 불러오지 못했습니다.");
      }
    })();
    return () => {
      active = false;
    };
  }, [id, reloadKey]);

  if (error) return <p className="py-24 text-center text-sm text-rose-500">{error}</p>;
  if (!product) return <p className="py-24 text-center text-sm text-neutral-400">불러오는 중...</p>;

  const soldOut = product.status === "SOLD_OUT";
  const pending = product.status === "PENDING";
  const onSale = product.status === "ON_SALE";
  const isSeller = currentUserId() === product.sellerId;
  const statusView = onSale
    ? { label: "판매중", color: "text-emerald-600" }
    : pending
      ? { label: "판매대기", color: "text-amber-500" }
      : { label: "품절", color: "text-neutral-400" };

  return (
    <main className="mx-auto max-w-6xl px-6 py-6">
      <button
        onClick={() => router.push("/")}
        className="mb-5 flex items-center gap-1.5 text-sm text-neutral-500 hover:text-neutral-900"
      >
        <ArrowLeft size={18} />
        뒤로가기
      </button>

      <div className="grid gap-10 md:grid-cols-2">
        {/* 이미지 */}
        <div className="relative aspect-square overflow-hidden rounded-2xl bg-neutral-100">
          {product.imageUrls.length > 0 ? (
            <>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={product.imageUrls[imgIdx] ?? product.imageUrls[0]}
                alt={product.title}
                className="h-full w-full object-cover"
              />
              {product.imageUrls.length > 1 && (
                <>
                  <button
                    onClick={() =>
                      setImgIdx((i) => (i - 1 + product.imageUrls.length) % product.imageUrls.length)
                    }
                    className="absolute left-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/80 hover:bg-white"
                  >
                    <ChevronLeft size={20} />
                  </button>
                  <button
                    onClick={() => setImgIdx((i) => (i + 1) % product.imageUrls.length)}
                    className="absolute right-3 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full bg-white/80 hover:bg-white"
                  >
                    <ChevronRight size={20} />
                  </button>
                </>
              )}
            </>
          ) : (
            <div className="flex h-full items-center justify-center text-neutral-300">
              <ImageIcon size={48} strokeWidth={1.5} />
            </div>
          )}
          {soldOut && (
            <div className="absolute inset-0 flex items-center justify-center bg-neutral-900/55">
              <span className="text-lg font-semibold tracking-widest text-white">품절</span>
            </div>
          )}
          <span className="absolute bottom-4 left-1/2 -translate-x-1/2 rounded-full bg-neutral-800/70 px-3 py-1 text-xs text-white">
            {product.imageUrls.length > 0 ? imgIdx + 1 : 0}/{product.imageUrls.length}
          </span>
        </div>

        {/* 정보 */}
        <div>
          <h1 className="text-2xl font-bold">{product.title}</h1>
          <p className="mt-3 text-3xl font-extrabold">{formatWon(product.price)}원</p>

          <div className="mt-3 flex items-center justify-between text-sm text-neutral-400">
            <div className="flex items-center gap-3">
              <span>{timeAgo(product.createdAt)}</span>
              <span className="flex items-center gap-1">
                <Eye size={15} /> 164
              </span>
              <span className="flex items-center gap-1">
                <Heart size={15} className={liked ? "fill-rose-500 text-rose-500" : ""} /> {likeCount}
              </span>
            </div>
            <button className="hover:text-neutral-600">신고하기</button>
          </div>

          <dl className="mt-6 space-y-3 border-t border-neutral-100 pt-6 text-sm">
            <SpecRow label="판매상태">
              <span className={`font-bold ${statusView.color}`}>{statusView.label}</span>
            </SpecRow>
            <SpecRow label="상품상태">{CONDITION_LABEL[product.productCondition]}</SpecRow>
            <SpecRow label="수량">
              <span className="font-semibold">{product.stock}개</span>
            </SpecRow>
            <SpecRow label="배송비">일반 3,000원</SpecRow>
          </dl>

          <div className="mt-6 border-t border-neutral-100 pt-6">
            <p className={`whitespace-pre-wrap text-sm leading-relaxed text-neutral-700 ${expanded ? "" : "line-clamp-2"}`}>
              {product.description}
            </p>
            <button
              onClick={() => setExpanded((v) => !v)}
              className="mt-2 text-sm text-neutral-500 hover:text-neutral-800"
            >
              {expanded ? "접기" : "더보기"}
            </button>
            <div className="mt-4 flex flex-wrap gap-2">
              {TAGS.map((t) => (
                <span key={t} className="rounded-md bg-neutral-100 px-2.5 py-1 text-xs text-neutral-500">
                  {t}
                </span>
              ))}
            </div>
          </div>

          <div className="mt-6 flex items-center gap-3 border-t border-neutral-100 pt-6">
            <div className="flex h-11 w-11 items-center justify-center rounded-full bg-neutral-100 text-neutral-400">
              <ImageIcon size={18} />
            </div>
            <div>
              <p className="text-sm font-bold">{sellerName(product.sellerId)}</p>
              <p className="text-xs text-neutral-400">판매자</p>
            </div>
          </div>

          <div className="mt-6 flex gap-3">
            <IconBtn onClick={openShare} label="공유">
              <Share2 size={20} />
            </IconBtn>
            <IconBtn onClick={toggleLike} label="찜">
              <Heart size={20} className={liked ? "fill-rose-500 text-rose-500" : "text-neutral-500"} />
            </IconBtn>

            {onSale && (
              <button
                onClick={() => setModalOpen(true)}
                className="flex-1 rounded-xl bg-[#f0143c] py-3.5 text-base font-bold text-white transition-opacity hover:opacity-90"
              >
                구매하기
              </button>
            )}
            {pending && isSeller && (
              <button
                onClick={handleStartSale}
                disabled={starting}
                className="flex-1 rounded-xl bg-emerald-500 py-3.5 text-base font-bold text-white hover:bg-emerald-600 disabled:opacity-60"
              >
                {starting ? "처리 중..." : "판매 시작"}
              </button>
            )}
            {pending && !isSeller && (
              <button disabled className="flex-1 cursor-not-allowed rounded-xl bg-amber-400 py-3.5 text-base font-bold text-white">
                판매 대기중
              </button>
            )}
            {soldOut && (
              <button disabled className="flex-1 cursor-not-allowed rounded-xl bg-neutral-200 py-3.5 text-base font-bold text-neutral-400">
                품절
              </button>
            )}
          </div>

          {isSeller && (
            <div className="mt-3 flex gap-3">
              <button
                onClick={() => router.push(`/products/${id}/edit`)}
                className="flex-1 rounded-xl border border-neutral-300 py-3 text-sm font-semibold text-neutral-700 hover:bg-neutral-50"
              >
                수정
              </button>
              <button
                onClick={handleDelete}
                disabled={deleting}
                className="flex-1 rounded-xl border border-rose-200 py-3 text-sm font-semibold text-rose-600 hover:bg-rose-50 disabled:opacity-60"
              >
                {deleting ? "삭제 중..." : "삭제"}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 이 상품과 비슷해요 (정적) */}
      <section className="mt-16">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="flex items-center gap-1.5 text-lg font-bold">
            이 상품과 비슷해요 <Info size={16} className="text-neutral-300" />
          </h2>
          <div className="flex gap-1.5">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-200 text-neutral-400">
              <ChevronLeft size={16} />
            </span>
            <span className="flex h-8 w-8 items-center justify-center rounded-lg border border-neutral-200 text-neutral-400">
              <ChevronRight size={16} />
            </span>
          </div>
        </div>
        <div className="flex gap-4 overflow-x-auto pb-1 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
          {SIMILAR.map((price, i) => (
            <div key={i} className="w-44 shrink-0">
              <div className="relative mb-2 aspect-square overflow-hidden rounded-xl bg-neutral-100">
                <div className="flex h-full items-center justify-center text-neutral-300">
                  <ImageIcon size={28} strokeWidth={1.5} />
                </div>
                <span className="absolute right-3 top-3 text-neutral-400">
                  <Heart size={18} />
                </span>
              </div>
              <p className="font-bold text-neutral-900">{price.toLocaleString()}원</p>
            </div>
          ))}
        </div>
      </section>

      {modalOpen && (
        <PurchaseModal
          product={product}
          onClose={() => setModalOpen(false)}
          onPurchased={() => {
            setModalOpen(false);
            setReloadKey((k) => k + 1);
          }}
        />
      )}

      {shareOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setShareOpen(false)}
        >
          <div
            className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-1 flex items-center justify-between">
              <h3 className="text-base font-bold">공유하기</h3>
              <button
                onClick={() => setShareOpen(false)}
                className="text-neutral-400 hover:text-neutral-700"
                aria-label="닫기"
              >
                <X size={18} />
              </button>
            </div>
            <p className="mb-3 text-sm text-neutral-500">이 링크를 복사해 공유하세요.</p>
            <div className="flex items-center gap-2">
              <input
                readOnly
                value={shareUrl}
                onFocus={(e) => e.target.select()}
                className="min-w-0 flex-1 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-sm text-neutral-600"
              />
              <button
                onClick={copyLink}
                className="shrink-0 rounded-lg bg-neutral-900 px-4 py-2 text-sm font-semibold text-white hover:bg-neutral-800"
              >
                복사
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

function SpecRow({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex gap-8">
      <dt className="w-20 shrink-0 text-neutral-400">{label}</dt>
      <dd className="text-neutral-700">{children}</dd>
    </div>
  );
}

function IconBtn({
  children,
  onClick,
  label,
}: {
  children: React.ReactNode;
  onClick: () => void;
  label: string;
}) {
  return (
    <button
      onClick={onClick}
      aria-label={label}
      className="flex h-12 w-12 shrink-0 items-center justify-center rounded-xl border border-neutral-200 text-neutral-500 hover:bg-neutral-50"
    >
      {children}
    </button>
  );
}

export default function ProductDetailPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);

  return (
    <AuthGuard>
      <Header />
      {Number.isNaN(id) ? (
        <p className="py-24 text-center text-sm text-rose-500">잘못된 상품입니다.</p>
      ) : (
        <DetailContent id={id} />
      )}
    </AuthGuard>
  );
}
