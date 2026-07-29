import type {
  LoginResponse,
  MyOrdersResponse,
  MyTransactions,
  OrderDetail,
  OrderRole,
  ProductDetail,
  ProductListResponse,
  ProductRegisterRequest,
  SignupRequest,
} from "./types";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
const TOKEN_KEY = "ibid.accessToken";

export class ApiError extends Error {
  code: string;
  status: number;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

let accessToken: string | null = null;

export function getToken(): string | null {
  if (accessToken) return accessToken;
  if (typeof window !== "undefined") {
    accessToken = window.localStorage.getItem(TOKEN_KEY);
  }
  return accessToken;
}

export function setToken(token: string | null) {
  accessToken = token;
  if (typeof window === "undefined") return;
  if (token) {
    window.localStorage.setItem(TOKEN_KEY, token);
  } else {
    window.localStorage.removeItem(TOKEN_KEY);
  }
}

export function currentUserId(): number | null {
  const token = getToken();
  if (!token) return null;
  try {
    const part = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const sub = JSON.parse(atob(part)).sub;
    return sub != null ? Number(sub) : null;
  } catch {
    return null;
  }
}

async function parseError(res: Response): Promise<ApiError> {
  try {
    const body = await res.json();
    return new ApiError(res.status, body.code ?? "UNKNOWN", body.message ?? "요청에 실패했습니다.");
  } catch {
    return new ApiError(res.status, "UNKNOWN", "요청에 실패했습니다.");
  }
}

let refreshPromise: Promise<boolean> | null = null;

async function tryRefresh(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch(`${BASE_URL}/api/auth/refresh`, {
          method: "POST",
          credentials: "include",
        });
        if (!res.ok) {
          setToken(null);
          return false;
        }
        const body: LoginResponse = await res.json();
        setToken(body.accessToken);
        return true;
      } catch {
        setToken(null);
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
  retryOnUnauthorized?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, auth = true, retryOnUnauthorized = true } = options;

  const headers: Record<string, string> = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth) {
    const token = getToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 401 && auth && retryOnUnauthorized) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return request<T>(path, { ...options, retryOnUnauthorized: false });
    }
  }

  if (!res.ok) throw await parseError(res);

  if (res.status === 204 || res.headers.get("content-length") === "0") {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

export const api = {
  bootstrap: () => tryRefresh(),

  signup: (data: SignupRequest) =>
    request<{ userId: number }>("/api/auth/signup", { method: "POST", body: data, auth: false }),

  login: (email: string, password: string) =>
    request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password },
      auth: false,
    }),

  logout: () => request<void>("/api/auth/logout", { method: "POST", auth: false }),

  getProducts: (cursor?: number | null) =>
    request<ProductListResponse>(
      `/api/products${cursor != null ? `?cursor=${cursor}` : ""}`,
    ),

  getProduct: (id: number) => request<ProductDetail>(`/api/products/${id}`),

  registerProduct: (data: ProductRegisterRequest) =>
    request<{ productId: number }>("/api/products", { method: "POST", body: data }),

  openForSale: (productId: number) =>
    request<void>(`/api/products/${productId}/on-sale`, { method: "PATCH" }),

  purchase: (productId: number, quantity: number) =>
    request<{ orderId: number }>("/api/orders", {
      method: "POST",
      body: { productId, quantity },
    }),

  createPayment: (orderId: number) =>
    request<{ paymentId: number }>("/api/payments", {
      method: "POST",
      body: { orderId },
    }),

  confirmPayment: (
    paymentId: number,
    data: { orderId: string; amount: string; paymentKey: string },
  ) =>
    request<{ paymentKey: string }>(`/api/payments/${paymentId}/confirm`, {
      method: "POST",
      body: data,
    }),

  failPayment: (paymentId: number) =>
    request<void>(`/api/payments/${paymentId}/fail`, { method: "POST" }),

  getMyOrders: (role: OrderRole) => request<MyOrdersResponse>(`/api/orders?role=${role}`),

  getMyTransactions: () => request<MyTransactions>("/api/orders/me"),

  getMyOrder: (orderId: number) => request<OrderDetail>(`/api/orders/${orderId}`),

  inspectionReceive: (orderId: number) =>
    request<void>(`/api/inspections/${orderId}/receive`, { method: "POST" }),

  inspectionPass: (orderId: number, memo: string) =>
    request<void>(`/api/inspections/${orderId}/pass`, { method: "POST", body: { memo } }),

  inspectionFail: (orderId: number, memo: string) =>
    request<void>(`/api/inspections/${orderId}/fail`, { method: "POST", body: { memo } }),
};
