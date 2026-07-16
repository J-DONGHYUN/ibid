import type { InputHTMLAttributes } from "react";

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  required?: boolean;
}

export default function Field({ label, error, required, ...props }: Props) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-bold text-neutral-800">
        {label}
        {required && <span className="ml-0.5 text-rose-500">*</span>}
      </label>
      <input
        className={`w-full rounded-lg border px-4 py-3 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-neutral-500 ${
          error ? "border-rose-400" : "border-neutral-200"
        }`}
        {...props}
      />
      {error && <p className="mt-1 text-xs text-rose-500">{error}</p>}
    </div>
  );
}
