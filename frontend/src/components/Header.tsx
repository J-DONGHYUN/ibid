"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth";
import { useToast } from "./Toast";

const NAV = [
  { href: "/", label: "홈" },
  { href: "/products/new", label: "등록" },
  { href: "/mypage", label: "마이페이지" },
];

export default function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { isAuthenticated, logout } = useAuth();
  const { showToast } = useToast();

  const handleLogout = async () => {
    await logout();
    showToast("로그아웃 되었습니다.");
    router.push("/login");
  };

  return (
    <header className="sticky top-0 z-40 border-b border-neutral-100 bg-white">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-6 px-6">
        <Link href="/" className="text-xl font-extrabold tracking-tight">
          ibid
        </Link>
        <nav className="flex items-center gap-1">
          {NAV.map((item) => {
            const active =
              item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
                  active
                    ? "bg-neutral-100 text-neutral-900"
                    : "text-neutral-500 hover:text-neutral-900"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="ml-auto">
          {isAuthenticated ? (
            <button
              onClick={handleLogout}
              className="text-sm font-medium text-neutral-500 hover:text-neutral-900"
            >
              로그아웃
            </button>
          ) : (
            <Link
              href="/login"
              className="text-sm font-medium text-neutral-500 hover:text-neutral-900"
            >
              로그인
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
