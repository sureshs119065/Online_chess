import { forwardRef } from "react";
import type { ButtonHTMLAttributes } from "react";
import { motion } from "framer-motion";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: "primary" | "ghost";
  isLoading?: boolean;
}

/**
 * The only animation here is whileTap (a person pressing the button) -
 * no idle pulse, no hover glow beyond a plain color shift. Motion answers
 * an action, per the design brief's restraint principle.
 */
export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = "primary", isLoading, disabled, className = "", children, ...props }, ref) => {
    const base =
      "group relative overflow-hidden inline-flex items-center justify-center gap-2 rounded-sm px-5 py-3 text-sm font-medium tracking-wide transition-colors duration-150 disabled:cursor-not-allowed disabled:opacity-50";

    const variants = {
      primary:
        "bg-brass text-base shadow-[0_0_28px_-8px_rgba(201,162,75,0.7)] hover:bg-[#DBB35D] hover:shadow-[0_0_34px_-6px_rgba(201,162,75,0.9)] disabled:hover:bg-brass",
      ghost:
        "bg-transparent text-ivory border border-brass-dim hover:border-brass hover:text-brass",
    };

    return (
      <motion.button
        ref={ref}
        whileHover={{ y: -1 }}
        whileTap={{ scale: 0.97 }}
        transition={{ duration: 0.12 }}
        disabled={disabled || isLoading}
        className={`${base} ${variants[variant]} ${className}`}
        {...props}
      >
        {isLoading ? (
          <>
            <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
            <span>Please wait</span>
          </>
        ) : (
          <>
            {variant === "primary" && (
              <span aria-hidden className="pointer-events-none absolute inset-y-0 left-0 w-1/4 bg-white/30 opacity-0 group-hover:animate-[sweep_0.8s_ease] group-hover:opacity-100" />
            )}
            <span className="relative">{children}</span>
          </>
        )}
      </motion.button>
    );
  },
);

Button.displayName = "Button";
