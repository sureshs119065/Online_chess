import type { ReactNode } from "react";
import { AnimatedBoardHero } from "@/components/board/AnimatedBoardHero";

interface AuthLayoutProps {
  children: ReactNode;
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Hero side - hidden below lg, since the board doesn't earn its
          space on a phone-width screen and the form should get full focus. */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-surface p-12 lg:flex">
        <a href="/" className="font-display text-xl text-ivory">
          Endgame
        </a>

        <div className="flex flex-col items-center gap-8">
          <AnimatedBoardHero />
          <p className="max-w-sm text-center font-display text-2xl leading-snug text-ivory">
            Every move is recorded, rated, and remembered.
          </p>
        </div>

        <p className="text-sm text-ivory-muted">
          Real-time chess, played properly.
        </p>
      </div>

      {/* Form side */}
      <div className="flex flex-col justify-center px-6 py-16 sm:px-12 lg:px-20">
        <a href="/" className="mb-10 font-display text-xl text-ivory lg:hidden">
          Endgame
        </a>
        <div className="w-full max-w-sm">{children}</div>
      </div>
    </div>
  );
}
