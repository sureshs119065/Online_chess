import { forwardRef, useId } from "react";
import type { InputHTMLAttributes } from "react";
import { AnimatePresence, motion } from "framer-motion";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(
  ({ label, error, id, className = "", ...props }, ref) => {
    const generatedId = useId();
    const inputId = id ?? generatedId;

    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={inputId} className="text-sm text-ivory-muted">
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          className={`rounded-sm border bg-surface px-4 py-3 text-ivory placeholder:text-ivory-muted/50 transition-colors duration-150 focus:outline-none ${
            error ? "border-brick" : "border-brass-dim/60 focus:border-brass"
          } ${className}`}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...props}
        />
        <AnimatePresence>
          {error && (
            <motion.p
              id={`${inputId}-error`}
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.15 }}
              className="text-sm text-brick"
            >
              {error}
            </motion.p>
          )}
        </AnimatePresence>
      </div>
    );
  },
);

TextField.displayName = "TextField";
