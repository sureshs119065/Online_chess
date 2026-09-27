# Endgame — Frontend

React + TypeScript client for the online chess platform. Built in
sessions, same approach as the backend:

- **Session 1 (done)** — project scaffold, design system, auth (login/signup)
- **Session 2 (done)** — app shell + lobby/matchmaking screen
- **Session 3 (done)** — the chess board itself: live game screen, move animations, clocks
- **Session 4 (done)** — in-game chat + notifications
- **Session 5 (done)** — profile/stats page, polish pass, deploy notes

## Stack

Vite, React 18, TypeScript, Tailwind CSS, Framer Motion (animation),
React Router, Zustand (session state), Axios, chess.js (client-side move
legality/highlighting only - see `lib/chess.ts`).

## Run it

Requires Node 18+ and the backend running: `discovery-server`,
`auth-service`, `game-engine-service`, `matchmaking-service`,
`chat-service`, `notification-service`, and `api-gateway`.

```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

## Try it (Session 4)

With the two-window setup from Sessions 2-3 (two accounts, matched into
a game):

1. On the game screen, click the "Chat" tab next to "Moves"
2. Send a message from one window - it should appear in both within a
   moment, aligned right (yours, in brass) or left (theirs)
3. Reload the page mid-conversation - the last 50 messages reappear,
   since `chat-service` sends recent history the instant you connect
4. Play a move, then check the bell icon in the header - a "Your turn"
   notification should appear for whoever's not to move, and both
   players get a "Game ended" notification once the game finishes
5. Click a notification - it marks itself read and, if it references a
   game, jumps you straight there

**Why notifications poll instead of push:** `notification-service`
(backend Session 7) only exposes REST endpoints - there's no WebSocket
for notifications the way there is for moves, matchmaking, and chat. The
bell polls every 20 seconds (`hooks/useNotifications.ts`) rather than
pushing live. Fine for a portfolio project; a production version would
want the backend to grow a WebSocket or SSE endpoint for this instead.

## Design system

Unchanged from earlier sessions. The chat bubbles and notification bell
reuse the same tokens as everything else - `brass` for "mine"/unread,
`surface-raised` for "theirs"/read, no new colors introduced for this
session.

## Reusable pieces added this session

- `hooks/useChatSocket.ts` - the third thing built on `useWebSocket`
  (after matchmaking and the game board), handling chat-service's one
  quirk: it sends an array on connect (recent history) and single
  objects afterward (new messages), told apart with `Array.isArray`
  rather than a type discriminant, since the backend doesn't wrap either
  shape.
- `hooks/useNotifications.ts` - the polling pattern, reusable for any
  future REST-only "needs to feel live" feature.

## Try it (Session 5)

1. Click your username/rating in the header (top right) - it now links
   to `/app/profile` instead of just being a display-only readout
2. The rating shows large and centered, the way file 00's design brief
   treats ELO numbers - `font-display` (Fraunces), not the sans body font
3. Win/draw/loss render as three even segments in sage/ivory/brick - the
   same three colors already used for legal-move/neutral/check everywhere
   else in the app, so a loss reads as "bad" the same way a check does
4. "Playing since" comes from `GET /api/users/{id}` (has `createdAt`) -
   `GET /api/users/{id}/stats` doesn't carry that field, so the page reads
   from both endpoints via two small hooks (`useUserStats`,
   `useUserProfile`)

**What's deliberately not here:** a list of past games. There's no
`GET /api/users/{id}/games`-shaped endpoint anywhere in the backend (only
per-game lookups by `gameId`), so a real match-history list needs that
added first rather than being faked from what's already exposed.

## Polish pass

Went looking for anything rough across the whole app before calling
Session 5 done:

- The header's username/rating block had no affordance suggesting it was
  clickable anywhere - now it's a `Link` with a hover state, consistent
  with how every other interactive text element in the app signals itself
- Global focus-visible outlines, `prefers-reduced-motion` handling, and
  the walnut/brass token set (`index.css`, `tailwind.config.ts`) were
  already in good shape from Session 1 - nothing new needed there, this
  page just uses what's already defined rather than introducing anything
- No outstanding `TODO`/`FIXME` markers anywhere in `src/`

## Deploying this to Vercel

The frontend is a static Vite build, so Vercel's default "just point it
at the repo" flow works with one adjustment:

1. **Root directory:** set it to `frontend/` in the Vercel project
   settings - this repo's frontend isn't at the repo root
2. **Build command / output:** Vercel autodetects Vite
   (`npm run build`, output `dist/`) - no override needed
3. **Environment variables** (Project Settings → Environment Variables):
   ```
   VITE_API_BASE_URL=https://your-gateway.onrender.com
   VITE_WS_BASE_URL=wss://your-gateway.onrender.com
   ```
   Both point at wherever `api-gateway` (Session 8) ends up deployed -
   see its own "Deploying this one to Render" section in the root
   `README.md`. Without these, the production build falls back to
   same-origin requests (`lib/api.ts` / `lib/ws.ts`), which only works
   in local dev because of `vite.config.ts`'s proxy - Vercel doesn't run
   that proxy, so a deployed frontend with these unset can't reach the
   backend at all
4. **Point the backend back at this frontend:** once Vercel gives you a
   URL, set api-gateway's `FRONTEND_ORIGIN` env var (see `CorsConfig.java`)
   to that exact URL on Render and redeploy the gateway - otherwise the
   browser blocks every request with a CORS error even though the gateway
   itself is reachable
5. **WebSockets through Vercel → Render:** no special Vercel config
   needed - the frontend opens `wss://` connections directly to
   `VITE_WS_BASE_URL` (the gateway), not through any Vercel serverless
   function, so this isn't subject to Vercel's own function timeout
   limits the way an API route would be

Auth tokens live in `localStorage` via `zustand/persist`
(`store/authStore.ts`), not cookies, so there's no additional
same-site/domain cookie configuration to worry about across the
Vercel ↔ Render split.

## What's next

All 5 frontend sessions are done, matching all 8 backend sessions. From
here: a real match-history endpoint + list (see the profile page's note
above on what's missing for that), reconnect/retry handling for
WebSocket drops mid-game, and an actual production run-through end to
end - Vercel frontend talking to Render backend - to catch anything that
only shows up outside `localhost`.
