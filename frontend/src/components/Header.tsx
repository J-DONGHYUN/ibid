"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Menu, Search } from "lucide-react";
import { useAuth } from "@/lib/auth";
import { useToast } from "./Toast";

const CATEGORIES = ["여성의류", "남성의류", "스포츠/레저"];

export default function Header() {
  const router = useRouter();
  const { logout } = useAuth();
  const { showToast } = useToast();

  const handleLogout = async () => {
    await logout();
    showToast("로그아웃 되었습니다.");
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-40 bg-white">
      <div className="border-b border-neutral-100">
        <div className="mx-auto flex h-9 max-w-6xl items-center justify-end gap-4 px-6 text-xs text-neutral-500">
          <button className="hover:text-neutral-800">고객센터</button>
          <Link href="/mypage" className="hover:text-neutral-800">
            마이페이지
          </Link>
          <button className="hover:text-neutral-800">관심</button>
          <button className="relative pr-1 hover:text-neutral-800">
            알림
            <span className="absolute right-0 top-0 h-1.5 w-1.5 rounded-full bg-[#f0143c]" />
          </button>
          <button onClick={handleLogout} className="hover:text-neutral-800">
            로그아웃
          </button>
        </div>
      </div>

      <div className="mx-auto flex h-16 max-w-6xl items-center gap-6 px-6">
        <Link href="/" className="text-2xl font-extrabold tracking-tight text-neutral-900">
          ibid
        </Link>

        <div className="relative max-w-xl flex-1">
          <Search size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-neutral-400" />
          <input
            placeholder="상품명이나 상점명을 검색해주세요"
            className="h-11 w-full rounded-lg border border-neutral-200 bg-neutral-50 pl-11 pr-4 text-sm outline-none placeholder:text-neutral-400 focus:border-neutral-300 focus:bg-white"
          />
        </div>

        <nav className="ml-auto flex items-center">
          <Link
            href="/products/new"
            className="rounded-lg bg-[#f0143c] px-4 py-2 text-sm font-semibold text-white transition-opacity hover:opacity-90"
          >
            판매하기
          </Link>
        </nav>
      </div>

      <div className="border-b border-neutral-100">
        <div className="mx-auto flex h-12 max-w-6xl items-center gap-6 px-6 text-sm">
          <button className="flex items-center gap-1.5 font-bold text-neutral-900">
            <Menu size={18} />
            카테고리
          </button>
          {CATEGORIES.map((c) => (
            <button key={c} className="font-medium text-neutral-600 hover:text-neutral-900">
              {c}
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}
