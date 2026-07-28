import type { Metadata } from "next";
import { IBM_Plex_Sans_KR } from "next/font/google";
import "./globals.css";
import Providers from "./providers";

const plexKr = IBM_Plex_Sans_KR({
  variable: "--font-plex-kr",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

export const metadata: Metadata = {
  title: "ibid",
  description: "중고거래 + 검수 에스크로 플랫폼",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${plexKr.variable} h-full antialiased`}>
      <body className="min-h-full bg-white text-neutral-900">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
