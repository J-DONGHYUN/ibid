"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ImageIcon } from "lucide-react";
import AuthGuard from "@/components/AuthGuard";
import { useToast } from "@/components/Toast";
import { useAuth } from "@/lib/auth";
import { api, ApiError } from "@/lib/api";
import type { InspectionQueueItem, OrderStatus } from "@/lib/types";

type QueueStatus = "SHIPPED_TO_INSPECTOR" | "UNDER_INSPECTION";

const STATUS: Record<QueueStatus, { label: string; badge: string; text: string }> = {
  SHIPPED_TO_INSPECTOR: { label: "수령 대기", badge: "bg-amber-100 text-amber-700", text: "text-amber-600" },
  UNDER_INSPECTION: { label: "검수 중", badge: "bg-indigo-100 text-indigo-700", text: "text-indigo-600" },
};

const CHECKLIST = ["정품 여부 확인", "외관 하자 확인", "구성품 일치", "사이즈/스펙 일치"];
const MENU = ["검수 대기열", "검수 진행", "상품 승인", "신고 관리", "정산 관리"];
const FILTERS: { key: "ALL" | QueueStatus; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "SHIPPED_TO_INSPECTOR", label: "수령 대기" },
  { key: "UNDER_INSPECTION", label: "검수 중" },
];

function statusOf(status: OrderStatus) {
  return STATUS[status as QueueStatus] ?? { label: status, badge: "bg-neutral-100 text-neutral-500", text: "text-neutral-500" };
}

function AdminContent() {
  const router = useRouter();
  const { logout } = useAuth();
  const { showToast } = useToast();

  const [rows, setRows] = useState<InspectionQueueItem[] | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [filter, setFilter] = useState<"ALL" | QueueStatus>("ALL");
  const [checks, setChecks] = useState<boolean[]>(CHECKLIST.map(() => false));
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      const res = await api.inspectionQueue();
      setRows(res.items);
      setSelectedId((prev) =>
        res.items.some((i) => i.orderId === prev) ? prev : res.items[0]?.orderId ?? null,
      );
    } catch (e) {
      setRows([]);
      showToast(e instanceof ApiError ? e.message : "대기열을 불러오지 못했습니다.", "error");
    }
  }, [showToast]);

  useEffect(() => {
    load();
  }, [load]);

  const selected = rows?.find((r) => r.orderId === selectedId) ?? null;
  const visible = filter === "ALL" ? rows ?? [] : (rows ?? []).filter((r) => r.status === filter);
  const counts = {
    total: rows?.length ?? 0,
    waiting: (rows ?? []).filter((r) => r.status === "SHIPPED_TO_INSPECTOR").length,
    inspecting: (rows ?? []).filter((r) => r.status === "UNDER_INSPECTION").length,
  };

  const receive = async () => {
    if (!selected || busy) return;
    setBusy(true);
    try {
      await api.inspectionReceive(selected.orderId);
      await load();
      showToast("수령 처리했습니다. 검수를 진행하세요.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "처리에 실패했습니다.", "error");
    } finally {
      setBusy(false);
    }
  };

  const judge = async (result: "PASS" | "FAIL") => {
    if (!selected || busy) return;
    setBusy(true);
    const memo = CHECKLIST.filter((_, i) => checks[i]).join(", ") || "검수 완료";
    try {
      if (result === "PASS") await api.inspectionPass(selected.orderId, memo);
      else await api.inspectionFail(selected.orderId, memo);
      setChecks(CHECKLIST.map(() => false));
      await load();
      showToast(result === "PASS" ? "합격 처리했습니다." : "불합격 처리했습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "처리에 실패했습니다.", "error");
    } finally {
      setBusy(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    router.push("/admin/login");
  };

  return (
    <div className="min-h-screen bg-neutral-50">
      <header className="flex h-14 items-center justify-between bg-neutral-900 px-6 text-white">
        <div className="flex items-center gap-2">
          <span className="text-lg font-extrabold">ibid</span>
          <span className="rounded bg-[#f0143c] px-1.5 py-0.5 text-[10px] font-bold tracking-wide">ADMIN</span>
        </div>
        <div className="flex items-center gap-4 text-sm">
          <span className="text-neutral-400">검수센터 · 성수 1센터</span>
          <span className="font-bold">검수 백오피스</span>
          <button onClick={handleLogout} className="rounded-md border border-neutral-600 px-3 py-1 text-xs hover:bg-neutral-800">
            로그아웃
          </button>
        </div>
      </header>

      <div className="mx-auto flex max-w-7xl gap-8 px-6 py-8">
        <aside className="hidden w-44 shrink-0 md:block">
          <ul className="space-y-1 text-sm">
            {MENU.map((m, i) => (
              <li key={m}>
                <button
                  className={`flex w-full items-center justify-between rounded-lg px-3 py-2.5 ${
                    i === 0 ? "bg-neutral-100 font-bold text-neutral-900" : "text-neutral-500 hover:text-neutral-900"
                  }`}
                >
                  {m}
                  {i === 0 && <span className="text-[#f0143c]">{counts.waiting}</span>}
                  {i === 1 && <span className="text-neutral-400">{counts.inspecting}</span>}
                </button>
              </li>
            ))}
          </ul>
          <div className="mt-6 border-t border-neutral-200 pt-4">
            <button onClick={() => router.push("/")} className="px-3 text-sm text-neutral-400 hover:text-neutral-700">
              서비스 홈으로
            </button>
          </div>
        </aside>

        <main className="min-w-0 flex-1">
          <div className="flex items-end justify-between">
            <div>
              <h1 className="text-2xl font-bold">검수 대기열</h1>
              <p className="mt-1 text-sm text-neutral-500">접수된 상품을 수령하고 합격 여부를 처리합니다.</p>
            </div>
            <button onClick={load} className="text-xs text-neutral-400 hover:text-neutral-700">
              새로고침
            </button>
          </div>

          <div className="mt-6 grid grid-cols-3 gap-4">
            <StatCard label="전체 대기열" value={counts.total} />
            <StatCard label="수령 대기" value={counts.waiting} tone="text-amber-600" />
            <StatCard label="검수 중" value={counts.inspecting} tone="text-indigo-600" />
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_360px]">
            <div className="rounded-2xl border border-neutral-200 bg-white p-5">
              <div className="mb-4 flex items-center justify-between">
                <div className="flex gap-1.5">
                  {FILTERS.map((f) => (
                    <button
                      key={f.key}
                      onClick={() => setFilter(f.key)}
                      className={`rounded-full px-3.5 py-1.5 text-sm font-medium ${
                        filter === f.key ? "bg-neutral-900 text-white" : "text-neutral-500 hover:bg-neutral-100"
                      }`}
                    >
                      {f.label}
                    </button>
                  ))}
                </div>
                <span className="text-sm text-neutral-400">{visible.length}건</span>
              </div>

              <div className="grid grid-cols-[90px_1fr_90px_80px] gap-3 border-b border-neutral-100 pb-2 text-xs text-neutral-400">
                <span>주문번호</span>
                <span>상품</span>
                <span>판매자</span>
                <span className="text-right">상태</span>
              </div>

              {rows === null && <p className="py-16 text-center text-sm text-neutral-400">불러오는 중...</p>}
              {rows !== null && visible.length === 0 && (
                <p className="py-16 text-center text-sm text-neutral-400">대기열이 비어 있습니다.</p>
              )}
              {visible.map((r) => (
                <button
                  key={r.orderId}
                  onClick={() => setSelectedId(r.orderId)}
                  className={`grid w-full grid-cols-[90px_1fr_90px_80px] items-center gap-3 border-b border-neutral-50 py-3 text-left text-sm ${
                    selectedId === r.orderId ? "bg-neutral-50" : "hover:bg-neutral-50"
                  }`}
                >
                  <span className="text-neutral-500">#{r.orderId}</span>
                  <span className="flex min-w-0 items-center gap-2">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded bg-neutral-100 text-neutral-300">
                      <ImageIcon size={16} />
                    </span>
                    <span className="min-w-0">
                      <span className="block truncate font-medium text-neutral-800">{r.productTitle}</span>
                      <span className="block text-xs text-neutral-400">{r.price.toLocaleString()}원</span>
                    </span>
                  </span>
                  <span className="text-neutral-500">user_{r.sellerId}</span>
                  <span className="text-right">
                    <span className={`rounded-full px-2 py-1 text-xs font-medium ${statusOf(r.status).badge}`}>
                      {statusOf(r.status).label}
                    </span>
                  </span>
                </button>
              ))}
            </div>

            <div className="h-fit rounded-2xl border border-neutral-200 bg-white p-5">
              {selected === null ? (
                <p className="py-16 text-center text-sm text-neutral-400">처리할 주문을 선택하세요.</p>
              ) : (
                <>
                  <div className="mb-4 flex items-center justify-between">
                    <h2 className="font-bold">검수 처리</h2>
                    <span className="text-xs text-neutral-400">#{selected.orderId}</span>
                  </div>
                  <div className="flex aspect-video items-center justify-center rounded-xl bg-neutral-100 text-neutral-300">
                    <ImageIcon size={36} strokeWidth={1.5} />
                  </div>
                  <p className="mt-4 font-bold">{selected.productTitle}</p>
                  <p className="font-bold">{selected.price.toLocaleString()}원</p>

                  <dl className="mt-4 space-y-2 text-sm">
                    <div className="flex justify-between">
                      <dt className="text-neutral-400">판매자</dt>
                      <dd className="text-neutral-700">user_{selected.sellerId}</dd>
                    </div>
                    <div className="flex justify-between">
                      <dt className="text-neutral-400">현재 상태</dt>
                      <dd className={`font-bold ${statusOf(selected.status).text}`}>{statusOf(selected.status).label}</dd>
                    </div>
                  </dl>

                  {selected.status === "SHIPPED_TO_INSPECTOR" ? (
                    <div className="mt-6">
                      <p className="mb-3 text-sm text-neutral-500">상품이 도착했습니다. 수령 처리하면 검수를 시작합니다.</p>
                      <button
                        onClick={receive}
                        disabled={busy}
                        className="w-full rounded-xl bg-neutral-900 py-3 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-50"
                      >
                        검수 수령
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="mt-5">
                        <p className="mb-2 text-sm font-bold">검수 체크리스트</p>
                        <ul className="space-y-2">
                          {CHECKLIST.map((c, i) => (
                            <li key={c}>
                              <label className="flex items-center gap-2 text-sm text-neutral-600">
                                <input
                                  type="checkbox"
                                  checked={checks[i]}
                                  onChange={() => setChecks((prev) => prev.map((v, idx) => (idx === i ? !v : v)))}
                                  className="h-4 w-4 rounded border-neutral-300"
                                />
                                {c}
                              </label>
                            </li>
                          ))}
                        </ul>
                      </div>

                      <div className="mt-6 flex gap-2">
                        <button
                          onClick={() => judge("FAIL")}
                          disabled={busy}
                          className="flex-1 rounded-xl border border-[#f0143c] py-3 text-sm font-bold text-[#f0143c] hover:bg-rose-50 disabled:opacity-50"
                        >
                          불합격
                        </button>
                        <button
                          onClick={() => judge("PASS")}
                          disabled={busy}
                          className="flex-1 rounded-xl bg-neutral-900 py-3 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-50"
                        >
                          합격 처리
                        </button>
                      </div>
                    </>
                  )}
                </>
              )}
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

function StatCard({ label, value, tone }: { label: string; value: number; tone?: string }) {
  return (
    <div className="rounded-2xl border border-neutral-200 bg-white px-6 py-5">
      <p className="text-sm text-neutral-500">{label}</p>
      <p className={`mt-2 text-2xl font-bold ${tone ?? "text-neutral-900"}`}>{value}</p>
    </div>
  );
}

export default function AdminPage() {
  return (
    <AuthGuard>
      <AdminContent />
    </AuthGuard>
  );
}
