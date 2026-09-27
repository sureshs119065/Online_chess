import { useEffect, useRef } from "react";
import type { MoveResponse } from "@/types/game";

interface MoveHistoryProps {
  moves: MoveResponse[];
}

interface MovePair {
  number: number;
  white?: string;
  black?: string;
}

function toPairs(moves: MoveResponse[]): MovePair[] {
  const pairs: MovePair[] = [];
  for (const move of moves) {
    const pairNumber = Math.ceil(move.moveNumber / 2);
    let pair = pairs.find((p) => p.number === pairNumber);
    if (!pair) {
      pair = { number: pairNumber };
      pairs.push(pair);
    }
    if (move.moveNumber % 2 === 1) pair.white = move.moveSan;
    else pair.black = move.moveSan;
  }
  return pairs;
}

export function MoveHistory({ moves }: MoveHistoryProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const pairs = toPairs(moves);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [moves.length]);

  return (
    <div
      ref={scrollRef}
      className="h-48 overflow-y-auto rounded-sm border border-brass-dim/30 bg-surface p-3 text-sm sm:h-full"
    >
      {pairs.length === 0 ? (
        <p className="text-ivory-muted">No moves yet.</p>
      ) : (
        <ol className="flex flex-col gap-1">
          {pairs.map((pair) => (
            <li key={pair.number} className="grid grid-cols-[2rem_1fr_1fr] gap-2 text-ivory-muted">
              <span>{pair.number}.</span>
              <span className="text-ivory">{pair.white}</span>
              <span className="text-ivory">{pair.black}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}
