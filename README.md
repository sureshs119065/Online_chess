# Online Chess Platform — Sessions 1–8 (all backend services complete)

No Docker required anywhere in this plan. Everything runs as a plain Spring
Boot jar, locally now and on a free-tier host later. The database is plain
PostgreSQL throughout — a locally installed instance for offline/local dev,
and Render's own free Postgres once deployed — no Supabase, no third-party
DB host.

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

## Database setup — plain PostgreSQL, local + Render

Every service reads the same five env vars for its database connection:
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (see any
service's `application.yml` — they're all identical on this point). Which
values you set just depends on where the service is running.

### Local install (Windows)

1. Install PostgreSQL natively — either the official installer from
   [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
   (includes pgAdmin4, a GUI) or via winget:
   ```powershell
   winget install PostgreSQL.PostgreSQL
   ```
2. During setup you'll be asked to set a password for the `postgres`
   superuser — remember it, that becomes `DB_PASSWORD`.
3. Create the database this project uses. Easiest via pgAdmin4 (right-click
   Databases → Create → Database, name it `chess_platform`), or from a
   terminal:
   ```powershell
   psql -U postgres -c "CREATE DATABASE chess_platform;"
   ```
4. In `.env`, set `DB_PASSWORD` to what you chose in step 2. `DB_HOST`
   (`localhost`), `DB_PORT` (`5432`), `DB_NAME` (`chess_platform`), and
   `DB_USER` (`postgres`) already default correctly in every service's
   `application.yml`, so you usually only need that one line.

**macOS/Linux:** `brew install postgresql@16` (then `brew services start
postgresql@16`) or `sudo apt install postgresql`, then
`createdb chess_platform` and set a password for the `postgres` role with
`psql -c "ALTER USER postgres PASSWORD 'yourpassword';"` if it doesn't
already have one.

No `CREATE EXTENSION` steps needed — every table uses `gen_random_uuid()`,
which has been built into PostgreSQL core since version 13, and both a
fresh install and Render's Postgres are well past that.

### Render (production)

1. In Render's dashboard, create a new **PostgreSQL** instance on the
   **Free** plan.
2. Once it's up, Render shows you its connection info — Hostname, Port,
   Database, Username, Password. Use the **Internal** values (not
   External), since every backend service is also deployed on Render and
   internal connections are faster and don't count against bandwidth.
3. Set those five values as `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` /
   `DB_PASSWORD` in **each** backend service's environment variables in
   Render's dashboard (auth-service, game-engine-service,
   matchmaking-service, chat-service, notification-service — all five
   point at the same database).

**Know this going in:** Render's free Postgres expires 30 days after
creation (14-day grace period to upgrade before deletion), and only one
free instance is allowed per account. Fine for demoing a portfolio project
over a few weeks; if you want this to keep running indefinitely, you'll
eventually need to either upgrade that one database to a paid instance, or
recreate a free one periodically (which means re-running every service's
Flyway migrations against the new instance and losing prior data). Worth
knowing before you invest a lot of demo data into it.

### One database, five services: Flyway history tables

All five services with a `db/migration` folder (auth, game-engine,
matchmaking, chat, notification) connect to the same physical database and
the same default `public` schema. Flyway tracks which migrations it's
already applied in a history table — by default just one table named
`flyway_schema_history` — which would end up **shared across every
service** if left unconfigured, since they're all pointed at the same
database. Each service's own `V1__...sql` file is a completely different
migration that happens to also be "version 1", so the second service to
start up would find a "version 1" already recorded (from the first
service) with a different checksum, and Flyway correctly refuses to
proceed rather than risk applying the wrong thing.

The fix: each service's `application.yml` sets its own
`spring.flyway.table` (e.g. `flyway_history_auth`,
`flyway_history_game_engine`) so their migration histories stay
completely independent, even though the actual tables they create all
land in the same database. This is already done in every service's
`application.yml` — nothing for you to configure — but if you ever add a
new service with its own migrations, give it a distinct
`spring.flyway.table` value too, or you'll hit the same checksum-mismatch
error the moment two services both ship a "version 1".

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

Nothing here talks to Postgres yet — that starts in Session 3 (auth-service).

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
  local `.env` and again in Render's dashboard when you deploy)

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
- **Database:** Render's own free Postgres — see "Database setup" above.
- **Railway:** similar GitHub-connect flow, if you'd rather use it instead
  of Render for the app services. As of 2026 Railway's free tier is
  effectively a time-boxed trial (30 days / ~$5 credit) rather than a
  permanent free plan — check railway.com/pricing before relying on it for a
  long-lived deployment.

### Should you even deploy discovery-server / config-server?
For a portfolio deployment, honestly — maybe not. They add another
always-on process to keep alive on a free host, and a spun-down registry
breaks every dependent service's startup. See the doc comments in
`DiscoveryServerApplication.java` and `ConfigServerApplication.java` for the
two options (keep Eureka/Config always-on on a paid-enough tier, or drop them
in prod and use plain env vars per service). Keep using both locally either
way — no downside there.

## Session 3 — auth-service

The first service that actually talks to Postgres.

### Before running it — one-time setup

1. Complete the "Database setup" steps above (local Postgres installed,
   `chess_platform` database created).
2. Copy `.env.example` to `.env` in the repo root and fill in `DB_PASSWORD`
   (and `JWT_SECRET`, `INTERNAL_SERVICE_KEY` — any long random strings work
   for local dev). Load it into your shell before running:
   - macOS/Linux: `export $(grep -v '^#' .env | xargs)`
   - Windows: `. .\load-env.ps1`

   (plain `mvn spring-boot:run` doesn't auto-load `.env` files either way).

### Run it

```bash
cd auth-service
mvn spring-boot:run
# → http://localhost:8081
```

Flyway runs `V1__create_users_table.sql` automatically against your
Postgres database on first boot — check with `psql -U postgres -d
chess_platform -c "\dt"` (or pgAdmin's table browser) afterward to confirm
the `users` table exists.

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
- Environment variables to set in Render's dashboard: `DB_HOST`, `DB_PORT`,
  `DB_NAME`, `DB_USER`, `DB_PASSWORD` (from Render's Postgres instance —
  see "Database setup"), `JWT_SECRET`, `INTERNAL_SERVICE_KEY`, and — if
  you're skipping discovery-server in prod per the Session 1 note —
  `EUREKA_ENABLED=false`.

## Session 4 — game-engine-service (the core)

Move validation via `chesslib`, live play over WebSocket, ELO updates on
game end. Depends on `common-lib` and calls back into `auth-service` over
HTTP for ratings.

### Before running it

Same `.env` as Session 3 — `INTERNAL_SERVICE_KEY` must be identical in
both `auth-service` and `game-engine-service`. It's how `auth-service`'s
internal endpoint verifies a rating update really came from
`game-engine-service` and not some random caller. It's never exposed
through api-gateway (see `InternalUserController`'s javadoc), so a simple
shared secret is enough here rather than full JWT auth.

### Run it

```bash
# auth-service needs to be running too, since game-engine calls it for
# ELO reads/writes on every game end
cd auth-service && mvn spring-boot:run &
cd game-engine-service && mvn spring-boot:run
# → http://localhost:8083
```

Flyway's `V1__create_games_moves_tables.sql` references `users(id)`, so
run `auth-service` at least once first — its migration has to create that
table before this one can add the foreign keys.

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
`ws://localhost:8083/ws/game/<gameId>?token=<accessToken>` (Postman's
WebSocket client or `wscat` both work) and send:
```json
{"type":"MOVE","moveSan":"e4"}
```
`GameIdHandshakeInterceptor` verifies `token` against the same JWT secret
auth-service issued it with and rejects the handshake (401) without a
valid one — the mover is whoever that token belongs to (and must actually
be one of `whitePlayerId`/`blackPlayerId` for this game), not a `playerId`
field in the message. Every connected client for that game receives the
updated `GameStateDto` back. Send `{"type":"RESIGN"}` to end the game
early — same rule, no `playerId` needed or trusted in the payload.

The server also now enforces the clock itself (`GameService.checkTimeout`
+ `GameTimeoutScheduler`'s periodic sweep) — `whiteTimeMs`/`blackTimeMs`
tick down against real elapsed time and a flag-fall ends the game with
`resultReason: TIMEOUT`, rather than those fields just being set once at
creation and left for the client to count down on its own.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl game-engine-service -am clean package -DskipTests`
- Start command: `java -jar game-engine-service/target/game-engine-service.jar`
- Env vars: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` (same
  Render Postgres instance as auth-service), `INTERNAL_SERVICE_KEY` (must
  match auth-service's deployed value), `AUTH_SERVICE_URL` set to
  auth-service's Render URL, `EUREKA_ENABLED=false` if skipping
  discovery-server in prod. `JWT_SECRET` — must match auth-service's
  exact value — is now needed too: `GameIdHandshakeInterceptor` verifies
  it against the `token` query param on every `/ws/game` connection.
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
`ws://localhost:8082/ws/matchmaking/<userId>?token=<user1AccessToken>`
**before** joining the queue to see the push live:
```json
{"type":"MATCH_FOUND","gameId":"...","opponentId":"...","yourColor":"WHITE"}
```
`UserIdHandshakeInterceptor` verifies `token` against the same JWT secret
auth-service issued it with, and rejects the handshake unless the token's
own subject matches the `<userId>` in the path — you can't subscribe to
someone else's match-found notifications just by connecting to their id.
From there, connect to `ws://localhost:8083/ws/game/<gameId>?token=<accessToken>`
(Session 4) to actually play.

To leave the queue instead:
```bash
curl -X DELETE http://localhost:8082/api/matchmaking/leave \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user1Id>"}'
```

### Deploying this one to Render (free tier)

- Build command: `mvn -pl matchmaking-service -am clean package -DskipTests`
- Start command: `java -jar matchmaking-service/target/matchmaking-service.jar`
- Env vars: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` (same
  Render Postgres instance), `AUTH_SERVICE_URL`, `GAME_ENGINE_SERVICE_URL`
  (both pointed at their deployed Render URLs), `EUREKA_ENABLED=false` if
  skipping discovery-server. `JWT_SECRET` — must match auth-service's
  exact value — is needed too: `UserIdHandshakeInterceptor` verifies it
  against the `token` query param on every `/ws/matchmaking` connection.
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

Connect a WebSocket client to `ws://localhost:8084/ws/chat/<gameId>?token=<accessToken>`
and send:
```json
{"message": "gg, good game!"}
```
`GameIdHandshakeInterceptor` verifies `token` against the same JWT secret
auth-service issued it with and rejects the handshake (401) without a
valid one — the sender is whoever that token belongs to, not a `senderId`
field in the message (a client can no longer post as someone else just by
changing that field). Every connection for that `gameId` — including the
sender — receives the saved `ChatMessageResponse` back. On connect, a
client is immediately sent the last 50 messages (oldest-first) so joining
mid-game isn't a blank screen.

### Deploying this one to Render (free tier)

- Build command: `mvn -pl chat-service -am clean package -DskipTests`
- Start command: `java -jar chat-service/target/chat-service.jar`
- Env vars: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` (same
  Render Postgres instance), `EUREKA_ENABLED=false` if skipping
  discovery-server, and `JWT_SECRET` (must match auth-service's exact
  value — `GameIdHandshakeInterceptor` verifies it on every `/ws/chat`
  connection). No other service URLs needed — chat-service doesn't call
  anyone else.

## Session 7 — notification-service

The first piece needing a message broker. Given the "free tier, no Docker"
goal, the recommended setup skips local RabbitMQ entirely:

### RabbitMQ setup — CloudAMQP free tier (no Docker, no local install)

1. Create a free account at [cloudamqp.com](https://www.cloudamqp.com) and
   spin up a **"Little Lemur" (free)** instance.
2. Open the instance's dashboard and copy its connection details. Use the
   **individual fields** (host, port, username, password, vhost), not the
   single `amqp://` URL string — Spring Boot's config doesn't reliably
   parse embedded credentials out of a full URI, so decompose it.
3. Add those to `.env` (see the `RABBITMQ_*` lines in `.env.example`).
   CloudAMQP's free tier uses TLS on port 5671, so
   `RABBITMQ_SSL_ENABLED=true`.
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
- Env vars: `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` (same
  Render Postgres instance), all six `RABBITMQ_*` vars (same CloudAMQP
  instance as local, or a fresh one — your call), `EUREKA_ENABLED=false`
  if skipping discovery-server. Also add the `RABBITMQ_*` vars to
  `game-engine-service`'s Render environment, or it'll silently fail to
  publish (logged, not fatal — see `RabbitMqGameEventPublisher`).

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
all read-only), `/actuator/**`, and `/ws/**` at the gateway layer — though
`/ws/**` is authenticated one hop downstream, not actually open; see
`JwtAuthFilter`'s javadoc.
Everything else needs `Authorization: Bearer <token>` or the gateway
returns 401 before the request goes anywhere.

**Both hardening gaps once documented here are now closed.** WebSocket
handshakes (`/ws/game`, `/ws/chat`, `/ws/matchmaking`) verify a JWT passed
as a `token` query param before accepting the connection — see each
service's `*HandshakeInterceptor`. And every controller that used to read
the caller's id only from the request body/path (`ResignRequest.playerId`,
`JoinQueueRequest`/`LeaveQueueRequest.userId`,
`NotificationController`'s `{userId}` path variable) now resolves it via
common-lib's `RequestUserResolver`, which prefers the gateway's
`X-User-Id` header and rejects a body-supplied id that doesn't match it
(falling back to the body only when there's no header at all, i.e. a
direct-to-service call bypassing this gateway for local testing).

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
- **Postgres** is a locally installed instance for dev and Render's own
  free Postgres in production — neither is a local container either.

With all three of those already hosted for free or installed natively, a
`docker-compose.yml` would mostly just be starting Java processes in
containers instead of directly — which `start-all-local.sh`
(`start-all-local.ps1` on Windows) above already does, more simply.
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

## All 8 backend sessions are done
That's every module from the original plan. From here: build the frontend
(file `04-IMPLEMENTATION-ROADMAP.md`'s Phase 8), or start deploying
services one at a time to Render using each session's deploy notes above.
The two gaps once flagged here (WebSocket JWT auth, controllers preferring
`X-User-Id` over body-supplied ids) are now closed — see the callout
above the "Run it" section.

## Frontend

The React client lives in `frontend/` and is built the same way as the
backend — split into sessions, each one a complete runnable piece. See
`frontend/README.md` for the run instructions, design system notes, and
session plan. Sessions 1 through 4 (project scaffold, design system,
auth, the lobby/matchmaking screen, the live chess board, and in-game
chat + notifications) are done as of this point in the build.
