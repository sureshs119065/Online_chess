import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        // Walnut-and-brass tournament-hall palette. Named by role, not
        // just shade, so components read intent (bg-surface, text-ivory)
        // rather than arbitrary hex values scattered through the code.
        base: "#1B1A17", // warm near-black page background, walnut-tinted
        surface: "#24221E", // panel/card background
        "surface-raised": "#2C2A25", // one step lighter, for hover/active surfaces
        ivory: "#EDE6D6", // primary text, light-square tone
        "ivory-muted": "#B8B0A0", // secondary text
        brass: "#C9A24B", // accent - CTAs, focus states, active indicators
        "brass-dim": "#8A6F35", // brass at lower emphasis (borders, dividers)
        sage: "#6B9E78", // success / legal-move / online-status
        brick: "#B5544B", // danger / check / error
      },
      fontFamily: {
        display: ["Fraunces", "serif"],
        sans: ["IBM Plex Sans", "sans-serif"],
      },
      boxShadow: {
        // Deliberately no soft-grey-card shadow anywhere in this palette -
        // depth comes from hairline brass borders instead. This token
        // exists only for the rare case (dropdowns/toasts) that genuinely
        // need to float above content.
        float: "0 8px 30px rgba(0, 0, 0, 0.45)",
      },
    },
  },
  plugins: [],
} satisfies Config;
