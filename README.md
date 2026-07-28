# Bang Identity Dealer

A small Spring Boot REST backend for privately dealing the base-game roles in **Bang!**. Games live in memory, so restarting the application removes every lobby. Tokens are bearer credentials: use HTTPS when deploying and do not share them.

## Requirements and startup

- Java 17 or newer
- Gradle is needed once to generate the wrapper JAR. The repository keeps the
  wrapper scripts and configuration, but intentionally does not track the binary
  `gradle/wrapper/gradle-wrapper.jar`.

Generate the missing wrapper JAR locally, then start the application:

```bash
gradle wrapper --gradle-version 8.10.2
./gradlew bootRun
```

The API is then available at `http://localhost:8080/api/games`.

## API walkthrough

Create a game. Save both values; only the host token can deal roles.

```bash
curl -sS -X POST http://localhost:8080/api/games
# {"gameCode":"ABC234","hostToken":"..."}
```

Join four to seven players (repeat with a distinct name for each person). Each player should privately save their own token.

```bash
curl -sS -X POST http://localhost:8080/api/games/ABC234/players \
  -H 'Content-Type: application/json' \
  -d '{"playerName":"Alice"}'
# {"playerName":"Alice","playerToken":"..."}
```

The host or any joined player can view the lobby. It contains names only—never tokens or roles.

```bash
curl -sS http://localhost:8080/api/games/ABC234/lobby \
  -H 'Authorization: Bearer PLAYER_OR_HOST_TOKEN'
# {"players":["Alice","Bob","Carol","Dan"]}
```

Once 4–7 people have joined, the host can deal exactly once. Further joins are then closed.

```bash
curl -i -X POST http://localhost:8080/api/games/ABC234/deal \
  -H 'Authorization: Bearer HOST_TOKEN'
# HTTP/1.1 204
```

Finally, each player uses their own token to reveal only their role.

```bash
curl -sS http://localhost:8080/api/games/ABC234/role \
  -H 'Authorization: Bearer PLAYER_TOKEN'
# {"role":"OUTLAW"}
```

Invalid input and game-state errors use RFC 9457 problem-details JSON with an appropriate `400`, `401`, `404`, or `409` response status.

## Role distributions

| Players | Sheriff | Deputy | Outlaw | Renegade |
|---:|---:|---:|---:|---:|
| 4 | 1 | 0 | 2 | 1 |
| 5 | 1 | 1 | 2 | 1 |
| 6 | 1 | 1 | 3 | 1 |
| 7 | 1 | 2 | 3 | 1 |

Roles are shuffled with Java's `SecureRandom` before assignment.

## Tests

```bash
./gradlew test
```
