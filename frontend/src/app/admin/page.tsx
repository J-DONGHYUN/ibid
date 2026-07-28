"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { ImageIcon } from "lucide-react";
import AuthGuard from "@/components/AuthGuard";
import { useToast } from "@/components/Toast";
import { useAuth } from "@/lib/auth";
import { api, ApiError } from "@/lib/api";

type Status = "WAITING" | "INSPECTING" | "PASSED" | "FAILED";

interface Row {
  id: string;
  orderId: number;
  title: string;
  price: number;
  seller: string;
  date: string;
  status: Status;
}

const INITIAL_ROWS: Row[] = [
  { id: "IN-2411", orderId: 2411, title: "나이키 에어맥스 90 화이트", price: 89000, seller: "user_042", date: "26/07/28", status: "WAITING" },
  { id: "IN-2410", orderId: 2410, title: "무인양품 오크 원목 책상", price: 145000, seller: "user_119", date: "26/07/28", status: "WAITING" },
  { id: "IN-2409", orderId: 2409, title: "소니 WH-1000XM5 헤드폰", price: 268000, seller: "user_301", date: "26/07/27", status: "INSPECTING" },
  { id: "IN-2408", orderId: 2408, title: "맥북 프로 14인치 M3", price: 1780000, seller: "user_077", date: "26/07/27", status: "INSPECTING" },
  { id: "IN-2407", orderId: 2407, title: "스탠리 텀블러 887ml", price: 32000, seller: "user_042", date: "26/07/26", status: "PASSED" },
  { id: "IN-2406", orderId: 2406, title: "레고 테크닉 포르쉐 911", price: 210000, seller: "user_255", date: "26/07/26", status: "FAILED" },
];

const STATUS: Record<Status, { label: string; badge: string; text: string }> = {
  WAITING: { label: "검수대기", badge: "bg-amber-100 text-amber-700", text: "text-amber-600" },
  INSPECTING: { label: "검수중", badge: "bg-indigo-100 text-indigo-700", text: "text-indigo-600" },
  PASSED: { label: "합격", badge: "bg-emerald-100 text-emerald-700", text: "text-emerald-600" },
  FAILED: { label: "불합격", badge: "bg-rose-100 text-rose-700", text: "text-rose-600" },
};

const CHECKLIST = ["정품 여부 확인", "외관 하자 확인", "구성품 일치", "사이즈/스펙 일치"];
const MENU = ["검수 대기열", "검수 진행", "상품 승인", "신고 관리", "정산 관리"];
const FILTERS: { key: "ALL" | Status; label: string }[] = [
  { key: "ALL", label: "전체" },
  { key: "WAITING", label: "검수대기" },
  { key: "INSPECTING", label: "검수중" },
  { key: "PASSED", label: "합격" },
];

function AdminContent() {
  const router = useRouter();
  const { logout } = useAuth();
  const { showToast } = useToast();

  const [rows, setRows] = useState<Row[]>(INITIAL_ROWS);
  const [selectedId, setSelectedId] = useState<string>(INITIAL_ROWS[0].id);
  const [filter, setFilter] = useState<"ALL" | Status>("ALL");
  const [checks, setChecks] = useState<boolean[]>(CHECKLIST.map(() => false));
  const [busy, setBusy] = useState(false);

  const selected = rows.find((r) => r.id === selectedId) ?? rows[0];
  const visible = filter === "ALL" ? rows : rows.filter((r) => r.status === filter);
  const counts = useMemo(
    () => ({
      today: rows.length,
      waiting: rows.filter((r) => r.status === "WAITING").length,
      passed: rows.filter((r) => r.status === "PASSED").length,
      failed: rows.filter((r) => r.status === "FAILED").length,
    }),
    [rows],
  );

  const judge = async (result: "PASSED" | "FAILED") => {
    if (busy) return;
    setBusy(true);
    const memo = CHECKLIST.filter((_, i) => checks[i]).join(", ") || "검수 완료";
    try {
      if (result === "PASSED") await api.inspectionPass(selected.orderId, memo);
      else await api.inspectionFail(selected.orderId, memo);
      setRows((prev) => prev.map((r) => (r.id === selected.id ? { ...r, status: result } : r)));
      showToast(result === "PASSED" ? "합격 처리했습니다." : "불합격 처리했습니다.");
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
          <span className="font-bold">검수관 김지훈</span>
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
                  {i === 1 && <span className="text-neutral-400">2</span>}
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
              <p className="mt-1 text-sm text-neutral-500">접수된 상품을 확인하고 합격 여부를 처리합니다.</p>
            </div>
            <p className="text-xs text-neutral-400">최근 갱신 2026-07-28 09:41</p>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard label="오늘 접수" value={counts.today} />
            <StatCard label="검수 대기" value={counts.waiting} tone="text-amber-600" />
            <StatCard label="합격" value={counts.passed} tone="text-emerald-600" />
            <StatCard label="불합격" value={counts.failed} tone="text-[#f0143c]" />
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

              <div className="grid grid-cols-[80px_1fr_80px_70px_70px] gap-3 border-b border-neutral-100 pb-2 text-xs text-neutral-400">
                <span>접수번호</span>
                <span>상품</span>
                <span>판매자</span>
                <span>접수일</span>
                <span className="text-right">상태</span>
              </div>
              {visible.map((r) => (
                <button
                  key={r.id}
                  onClick={() => setSelectedId(r.id)}
                  className={`grid w-full grid-cols-[80px_1fr_80px_70px_70px] items-center gap-3 border-b border-neutral-50 py-3 text-left text-sm ${
                    selectedId === r.id ? "bg-neutral-50" : "hover:bg-neutral-50"
                  }`}
                >
                  <span className="text-neutral-500">{r.id}</span>
                  <span className="flex items-center gap-2 min-w-0">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded bg-neutral-100 text-neutral-300">
                      <ImageIcon size={16} />
                    </span>
                    <span className="min-w-0">
                      <span className="block truncate font-medium text-neutral-800">{r.title}</span>
                      <span className="block text-xs text-neutral-400">{r.price.toLocaleString()}원</span>
                    </span>
                  </span>
                  <span className="text-neutral-500">{r.seller}</span>
                  <span className="text-neutral-400">{r.date}</span>
                  <span className="text-right">
                    <span className={`rounded-full px-2 py-1 text-xs font-medium ${STATUS[r.status].badge}`}>
                      {STATUS[r.status].label}
                    </span>
                  </span>
                </button>
              ))}
            </div>

            <div className="h-fit rounded-2xl border border-neutral-200 bg-white p-5">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="font-bold">검수 처리</h2>
                <span className="text-xs text-neutral-400">{selected.id}</span>
              </div>
              <div className="flex aspect-video items-center justify-center rounded-xl bg-neutral-100 text-neutral-300">
                <ImageIcon size={36} strokeWidth={1.5} />
              </div>
              <p className="mt-4 font-bold">{selected.title}</p>
              <p className="font-bold">{selected.price.toLocaleString()}원</p>

              <dl className="mt-4 space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-neutral-400">판매자</dt>
                  <dd className="text-neutral-700">{selected.seller}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-neutral-400">접수일</dt>
                  <dd className="text-neutral-700">{selected.date}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-neutral-400">현재 상태</dt>
                  <dd className={`font-bold ${STATUS[selected.status].text}`}>{STATUS[selected.status].label}</dd>
                </div>
              </dl>

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
                  onClick={() => judge("FAILED")}
                  disabled={busy}
                  className="flex-1 rounded-xl border border-[#f0143c] py-3 text-sm font-bold text-[#f0143c] hover:bg-rose-50 disabled:opacity-50"
                >
                  불합격
                </button>
                <button
                  onClick={() => judge("PASSED")}
                  disabled={busy}
                  className="flex-1 rounded-xl bg-neutral-900 py-3 text-sm font-bold text-white hover:bg-neutral-800 disabled:opacity-50"
                >
                  합격 처리
                </button>
              </div>
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
