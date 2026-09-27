# Online Chess Platform — Sessions 1–8 (all backend services complete)

No Docker required anywhere in this plan. Everything runs as a plain Spring
Boot jar, locally now and on a free-tier host later.

**On Windows?** Every `export ...` / `.sh` command in this README is bash
syntax and does nothing in PowerShell — it'll silently leave env vars
unset, which shows up as confusing errors like Postgres rejecting an empty
password. Use `load-env.ps1`, `start-all-local.ps1`, and
`stop-all-local.ps1` instead of their `.sh`/bash-command counterparts
everywhere below; they do the same thing. Load your `.env` at the start of
every new terminal session with:
```powershell
. .\load-env.ps1
```
(the leading `. ` matters — see the script's comment for why).

## Run locally

Requires JDK 17+ and Maven (or use the `./mvnw` wrapper once you add one via
`mvn -N wrapper:wrapper` from the repo root).

```bash
# from the repo root — builds every module in the parent pom
mvn clean install

# terminal 1
cd discovery-server
mvn spring-boot:run
# → http://localhost:8761  (Eureka dashboard)

# terminal 2
cd config-server
mvn spring-boot:run
# → http://localhost:8888
# sanity check: http://localhost:8888/application/default
```

Nothing here talks to Supabase yet — that starts in Session 3 (auth-service).

`common-lib` (Session 2) is a plain library module, not a runnable service —
`mvn clean install` from the repo root builds and installs it into your local
`.m2` so auth-service and every later service can depend on it via:

```xml
<dependency>
  <groupId>com.chess</groupId>
  <artifactId>common-lib</artifactId>
  <version>1.0.0</version>
</dependency>
```

It currently provides:
- `com.chess.common.dto.ErrorResponse` — the one error shape every service returns
- `com.chess.common.exception.ApiException` (+ `notFound()`/`badRequest()`/`conflict()`/... factories)
- `com.chess.common.exception.GlobalExceptionHandlerBase` — extend it with
  `@RestControllerAdvice` in each service
- `com.chess.common.security.JwtUtil` — `generateToken`, `generateRefreshToken`,
  `validateToken`, `extractUserId`, `extractUsername`, reading the signing
  secret from the `JWT_SECRET` env var (never hardcode it — set it in your
  local `.env` and again in Render/Railway's dashboard when you deploy)

## Moving to free-tier hosting later

You don't need to decide this now, but so the code doesn't box you in:

- **Render (recommended default):** connect your GitHub repo, create a "Web
  Service" per module, set the build command to
  `mvn -pl discovery-server -am clean package -DskipTests` and the start
  command to `java -jar discovery-server/target/discovery-server.jar`. Render
  sets `$PORT` automatically — `application.yml` above already reads it via
  `server.port: ${PORT:8761}`, so no code changes needed. Free instances spin
  down after idle time and cold-start on the next request; fine for a demo,
  not for anything latency-sensitive.
- **Railway:** similar GitHub-connect flow. As of 2026 Railway's free tier is
  effectively a time-boxed trial (30 days / ~$5 credit) rather than a
  permanent free plan — check railway.com/pricing before relying on it for a
  long-lived deployment.
- **Supabase:** unaffected by either choice above — it's just your Postgres
  host, wired in starting with auth-service (Session 3).

### Should you even deploy discovery-server / config-server?
For a portfolio deployment, honestly — maybe not. They add another
always-on process to keep alive on a free host, and a spun-down registry
breaks every dependent service's startup. See the doc comments in
`DiscoveryServerApplication.java` and `ConfigServerApplication.java` for the
two options (keep Eureka/Config always-on on a paid-enough tier, or drop them
in prod and use plain env vars per service). Keep using both locally either
way — no downside there.

## Session 3 — auth-service

The first service that actually talks to Supabase. Two Spring profiles ship
with it, matching the local-first → free-tier-deploy workflow:

- **`local`** (default) — direct Supabase connection (port 5432), for
  everyday dev.
- **`render`** — Supabase via the connection pooler (port 6543), meant for
  Render/Railway where you might have several short-lived connections.

### Before running it — one-time setup

1. Create a free Supabase project at supabase.com if you haven't yet, and
   grab its DB password + project ref.
2. Copy `.env.example` to `.env` in the repo root and fill in:
   ```
   SUPABASE_DB_PASSWORD=your_actual_supabase_password
   JWT_SECRET=a_long_random_string_at_least_32_chars
   ```
   (Add a JWT_SECRET line to `.env.example` if it isn't there yet — see the
   file 05 reference.) Export these into your shell before running
   (`export $(cat .env | xargs)` on macOS/Linux, or use an IDE run
   config / direnv), since plain `mvn spring-boot:run` doesn't auto-load
   `.env` files.
3. Edit `auth-service/src/main/resources/application.yml` and replace
   `<PROJECT-REF>` in the `local` profile's datasource URL with your actual
   Supabase project ref.

### Run it

```bash
cd auth-service
mvn spring-boot:run
# → http://localhost:8081
```

Flyway runs `V1__create_users_table.sql` automatically against Supabase on
first boot — check the Supabase dashboard's Table Editor afterward to
confirm the `users` table exists.

### Try it (curl)

```bash
curl -X POST http://localhost:8081/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"username":"magnus","email":"magnus@example.com","password":"hunter22"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"magnus","password":"hunter22"}'

# copy the accessToken from the login response into $TOKEN
curl http://localhost:8081/api/users/<userId>/stats

curl -X PUT http://localhost:8081/api/users/<userId> \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"magnus_carlsen"}'
```

### Deploying this one to Render (free tier)

- Build command: `mvn -pl auth-service -am clean package -DskipTests`
- Start command: `java -jar auth-service/target/auth-service.jar`
- Environment variables to set in Render's dashboard: `SUPABASE_DB_PASSWORD`,
  `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=render`, and — if you're skipping
  discovery-server in prod per the Session 1 note — `EUREKA_ENABLED=false`.
- `SUPABASE_DB_URL`/`SUPABASE_DB_USER` are optional overrides if you don't
  want to hardcode `<PROJECT-REF>` into the committed `application.yml` —
  set them as env vars instead and they take precedence.

## Session 4 — game-engine-service (the core)

Move validation via `chesslib`, live play over WebSocket, ELO updates on
game end. Depends on `common-lib` and calls back into `auth-service` over
HTTP for ratings.

### Before running it

Same `.env` as Session 3, plus one more line:
```
INTERNAL_SERVICE_KEY=change_this_to_another_long_random_string
```
This must be identical in both `auth-service` and `game-engine-service` —
it's how `auth-service`'s new internal endpoint verifies a rating update
really came from `game-engine-service` and not some random caller. It's
never exposed through api-gateway (see `InternalUserController`'s javadoc),
so a simple shared secret is enough here rather than full JWT auth.

Also swap `<PROJECT-REF>` in `game-engine-service/src/main/resources/application.yml`'s
`local` profile, same as you did for auth-service.

### Run it

```bash
# auth-service needs to be running too, since game-engine calls it for
# ELO reads/writes on every game end
cd auth-service && mvn spring-boot:run &
cd game-engine-service && mvn spring-boot:run
# → http://localhost:8083
```

Flyway's `V1__create_games_moves_tables.sql` references `users(id)`, so
run `auth-service` at least once first against a fresh Supabase project —
its migration has to create that table before this one can add the
foreign keys.

### Try it (curl + a WebSocket client)

```bash
# 1. Create a game directly (normally matchmaking-service does this in Session 5)
curl -X POST http://localhost:8083/api/games \
  -H "Content-Type: application/json" \
  -d '{"whitePlayerId":"<uuid>","blackPlayerId":"<uuid>","timeControl":"blitz_5"}'

# 2. Check state / move history
curl http://localhost:8083/api/games/<gameId>
curl http://localhost:8083/api/games/<gameId>/moves
```

Moves themselves go over the WebSocket, not REST — connect to
`ws://localhost:8083/ws/game/<gameId>` (Postman's WebSocket client or
`wscat` both work) and send:
```json
{"type":"MOVE","playerId":"<whitePlayerId>","moveSan":"e4"}
```
Every connected client for that game receives the updated `GameStateDto`
back. Send `{"type":"RESIGN","playerId":"<uuid>"}` to end the game early.

**Known simplification, read before deploying this anywhere public:** the
WebSocket trusts whichever `playerId` the client sends in the message —
it isn't verified against a JWT during the handshake yet. Fine for local
testing; `GameWebSocketHandler`'s javadoc explains exactly what to add
(a handshake-time JWT check) before this is internet-facing.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl game-engine-service -am clean package -DskipTests`
- Start command: `java -jar game-engine-service/target/game-engine-service.jar`
- Env vars: `SUPABASE_DB_PASSWORD`, `JWT_SECRET` isn't needed here directly,
  but `INTERNAL_SERVICE_KEY` is (must match auth-service's deployed value),
  plus `AUTH_SERVICE_URL` set to auth-service's Render URL,
  `SPRING_PROFILES_ACTIVE=render`, and `EUREKA_ENABLED=false` if skipping
  discovery-server in prod.
- WebSockets work on Render's free web services without extra config, but
  remember: free instances spin down when idle, which will drop any open
  game connections — fine for a demo, worth knowing about before you rely
  on it for anything real.

## Session 5 — matchmaking-service

Queue join/leave, ELO-band pairing every 2 seconds, calls
`game-engine-service` via Feign to create the game once two players are
matched, then pushes a "match found" event over WebSocket to both.

### Run it

Needs `auth-service` and `game-engine-service` both running (it calls both).

```bash
cd matchmaking-service
mvn spring-boot:run
# → http://localhost:8082
```

Same `local`/`render` profile split, same `<PROJECT-REF>` swap needed in
the `local` profile's datasource URL as every service before it.

### Try it

```bash
# Two different users join the same time-control queue
curl -X POST http://localhost:8082/api/matchmaking/join \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user1Id>","timeControl":"blitz_5"}'

curl -X POST http://localhost:8082/api/matchmaking/join \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user2Id>","timeControl":"blitz_5"}'
```

Within ~2 seconds `QueueMatcherScheduler` should pair them (their ELOs
need to be within 100 points to start — see `MatchmakingService.bandFor`
for how that widens the longer someone waits) and call `game-engine-service`
to create the game. Connect a WebSocket client to
`ws://localhost:8082/ws/matchmaking/<userId>` **before** joining the queue
to see the push live:
```json
{"type":"MATCH_FOUND","gameId":"...","opponentId":"...","yourColor":"WHITE"}
```
From there, connect to `ws://localhost:8083/ws/game/<gameId>` (Session 4)
to actually play.

To leave the queue instead:
```bash
curl -X DELETE http://localhost:8082/api/matchmaking/leave \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user1Id>"}'
```

### Deploying this one to Render (free tier)

- Build command: `mvn -pl matchmaking-service -am clean package -DskipTests`
- Start command: `java -jar matchmaking-service/target/matchmaking-service.jar`
- Env vars: `SUPABASE_DB_PASSWORD`, `AUTH_SERVICE_URL`,
  `GAME_ENGINE_SERVICE_URL` (both pointed at their deployed Render URLs),
  `SPRING_PROFILES_ACTIVE=render`, `EUREKA_ENABLED=false` if skipping
  discovery-server.
- The `@Scheduled` matcher keeps running as long as the instance is awake —
  on a free tier that spins down when idle, a queued player might not get
  matched until something else wakes the instance back up. Worth knowing,
  not fixable without a paid always-on tier.

## Session 6 — chat-service

Simple per-game WebSocket chat, same session-group-per-gameId shape as
game-engine-service's live play — no chess-rules complexity here.

### Run it

```bash
cd chat-service
mvn spring-boot:run
# → http://localhost:8084
```

Needs `auth-service` and `game-engine-service` migrations to have run at
least once first (foreign keys to `users` and `games`).

### Try it

```bash
curl http://localhost:8084/api/games/<gameId>/chat
```

Connect a WebSocket client to `ws://localhost:8084/ws/chat/<gameId>` and
send:
```json
{"senderId": "<userId>", "message": "gg, good game!"}
```
Every connection for that `gameId` — including the sender — receives the
saved `ChatMessageResponse` back. On connect, a client is immediately sent
the last 50 messages (oldest-first) so joining mid-game isn't a blank
screen.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl chat-service -am clean package -DskipTests`
- Start command: `java -jar chat-service/target/chat-service.jar`
- Env vars: `SUPABASE_DB_PASSWORD`, `SPRING_PROFILES_ACTIVE=render`,
  `EUREKA_ENABLED=false` if skipping discovery-server. No other service
  URLs needed — chat-service doesn't call anyone else.

## Session 7 — notification-service

The first piece needing a message broker. Given the "free tier, no Docker"
goal, the recommended setup skips local RabbitMQ entirely:

### RabbitMQ setup — CloudAMQP free tier (no Docker, no local install)

1. Create a free account at [cloudamqp.com](https://www.cloudamqp.com) and
   spin up a **"Little Lemur" (free)** instance — same idea as using
   Supabase instead of running your own Postgres.
2. Open the instance's dashboard and copy its connection details. Use the
   **individual fields** (host, port, username, password, vhost), not the
   single `amqp://` URL string — Spring Boot's config doesn't reliably
   parse embedded credentials out of a full URI, so decompose it.
3. Add those to `.env` (see the new lines in `.env.example`). CloudAMQP's
   free tier uses TLS on port 5671, so `RABBITMQ_SSL_ENABLED=true`.
4. **Both** `game-engine-service` (the publisher) and `notification-service`
   (the consumer) need the exact same `RABBITMQ_*` values — they only see
   each other's messages if they're pointed at the same broker/vhost.

One CloudAMQP free instance is enough for local dev AND your eventual
Render deployment — you don't need two. Prefer running RabbitMQ yourself
instead? Install it natively (`brew install rabbitmq` / `apt install
rabbitmq-server`) and point `RABBITMQ_HOST=localhost`,
`RABBITMQ_SSL_ENABLED=false`, default port 5672, `guest`/`guest` — no
Docker needed either way.

### What changed in game-engine-service

`LoggingGameEventPublisher` (the Session 4 placeholder) is gone, replaced
by `RabbitMqGameEventPublisher`, which publishes plain JSON text (not
Java-serialized objects — see its javadoc for why) onto a `chess.events`
topic exchange. `GameService`'s code didn't change at all — it only ever
depended on the `GameEventPublisher` interface, exactly as planned back in
Session 4.

### Run it

```bash
cd notification-service
mvn spring-boot:run
# → http://localhost:8085
```

Needs `auth-service`'s migration to have run at least once (foreign key to
`users`). Restart `game-engine-service` too, since it now needs the
`RABBITMQ_*` env vars to actually publish.

### Try it

Play a game to checkmate/resignation (Sessions 4/5), then:
```bash
curl http://localhost:8085/api/notifications/<userId>
```
You should see a `GAME_ENDED` entry, and a `MOVE_ALERT` entry for every
move where that user was the one waiting on their opponent. Mark one read:
```bash
curl -X PUT http://localhost:8085/api/notifications/<notificationId>/read
```

Note: nothing currently publishes to `game.invite` — the matchmaking flow
pairs players automatically rather than one inviting another by name — but
the queue/consumer are wired up per the original architecture doc, ready
for a future direct-challenge feature.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl notification-service -am clean package -DskipTests`
- Start command: `java -jar notification-service/target/notification-service.jar`
- Env vars: `SUPABASE_DB_PASSWORD`, all six `RABBITMQ_*` vars (same
  CloudAMQP instance as local, or a fresh one — your call),
  `SPRING_PROFILES_ACTIVE=render`, `EUREKA_ENABLED=false` if skipping
  discovery-server. Also add the `RABBITMQ_*` vars to `game-engine-service`'s
  Render environment, or it'll silently fail to publish (logged, not
  fatal — see `RabbitMqGameEventPublisher`).

## Session 8 — api-gateway

Single entry point in front of every service, plus a JWT filter that
rejects unauthenticated requests to protected routes before they ever
reach a backend. This is also where the "no Docker" decision from Session
1 gets revisited — see below.

### What it routes

| Path | Routes to |
|---|---|
| `/api/auth/**`, `/api/users/**` | auth-service |
| `/api/games/*/chat/**` | chat-service (checked before the games route) |
| `/api/games/**` | game-engine-service |
| `/api/matchmaking/**` | matchmaking-service |
| `/api/notifications/**` | notification-service |
| `/ws/game/**`, `/ws/chat/**`, `/ws/matchmaking/**` | same services, as WebSocket upgrades |

Public (no JWT needed): all of `/api/auth/**`, `GET /api/users/**`,
`GET /api/games/**` (covers game state, move history, and chat history —
all read-only), `/actuator/**`, and `/ws/**` for now (see
`JwtAuthFilter`'s javadoc for why WebSocket auth is still a known gap).
Everything else needs `Authorization: Bearer <token>` or the gateway
returns 401 before the request goes anywhere.

**One thing worth knowing before you lean on this:** every service built
in Sessions 3–7 reads the caller's id from the request body (e.g.
`ResignRequest.playerId`), not from the `X-User-Id` header this gateway
now injects on every authenticated request. The header is there and
correct, but no controller prefers it yet over the body — that's the
natural next hardening pass once the gateway is always in front of
everything, and `JwtAuthFilter`'s javadoc spells out exactly what that
change looks like.

### Run it

```bash
cd api-gateway
mvn spring-boot:run
# → http://localhost:8080
```

From here on, hit everything through port 8080 instead of each service's
own port directly — e.g. `curl http://localhost:8080/api/auth/login`
instead of `:8081/api/auth/login`.

### Running everything at once, still no Docker

**macOS/Linux:**
```bash
export $(grep -v '^#' .env | xargs)   # load your .env into the shell
./start-all-local.sh                  # starts all 8 services in the background
tail -f logs/*.log                    # watch them come up
./stop-all-local.sh                   # tear it all down
```

**Windows (PowerShell):**
```powershell
. .\load-env.ps1                            # load your .env into the shell
.\start-all-local.ps1                       # starts all 8 services as background jobs
Get-Content logs\auth-service.log -Wait     # watch one come up (repeat per service, or check Get-Job)
.\stop-all-local.ps1                        # tear it all down
```

### About Docker, finally

The original 6-file plan called for a `docker-compose.yml` wiring Redis,
RabbitMQ, and every service together, plus a `Dockerfile` per module. We
skipped that on purpose the whole way through:

- **Redis** was never actually wired into anything — no service ended up
  needing a cache — so there's nothing to containerize there.
- **RabbitMQ** is CloudAMQP (hosted), not a local container, per Session 7.
- **Postgres** is Supabase (hosted), not a local container, per Session 1.

With all three of those already hosted for free, a `docker-compose.yml`
would mostly just be starting Java processes in containers instead of
directly — which `start-all-local.sh` above already does, more simply.
`Dockerfile.template` at the repo root is there if you ever deploy to a
host that specifically requires a container (Fly.io, Cloud Run, Railway's
Docker flow) — copy it in and adjust `SERVICE_NAME` when that day comes.
Render, the deployment target used throughout this README, never needs it.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl api-gateway -am clean package -DskipTests`
- Start command: `java -jar api-gateway/target/api-gateway.jar`
- Env vars: `JWT_SECRET` (must match auth-service's exactly), every
  `*_SERVICE_URL` pointed at each service's deployed Render URL, the
  matching `*_SERVICE_WS_URL` variants with `wss://` instead of `ws://`
  once you're on HTTPS, `FRONTEND_ORIGIN` once you have a real frontend
  URL, `EUREKA_ENABLED=false` if skipping discovery-server.
- This is the one service worth keeping always-on if you can — everything
  else being asleep is just slow; the gateway itself being asleep means
  nothing reaches your app at all.

## All 8 sessions are done
That's every module from the original plan. From here: build the frontend
(file `04-IMPLEMENTATION-ROADMAP.md`'s Phase 8), or start deploying
services one at a time to Render using each session's deploy notes above,
or go back and close the two documented gaps (WebSocket JWT auth,
controllers preferring `X-User-Id` over body-supplied ids) before this
goes anywhere more public than your own testing.
