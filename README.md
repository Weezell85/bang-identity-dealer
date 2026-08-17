# Bang Identity Dealer

A small Spring Boot REST backend for privately dealing the base-game roles in **Bang!**. Games live in memory, so restarting the application removes every lobby. Tokens are bearer credentials: use HTTPS when deploying and do not share them.

## Requirements and startup

- Java 17 or newer
- Gradle is needed once to generate the wrapper JAR. The repository keeps the
  wrapper scripts and configuration, but intentionally does not track the binary
  `gradle/wrapper/gradle-wrapper.jar`.

The backend is the root Gradle project; there is no separate Maven `backend`
module. Generate the missing wrapper JAR locally, then start the application
from the repository root:

```bash
gradle wrapper --gradle-version 8.10.2
./gradlew bootRun
```

The API is then available at `http://localhost:8080/api/games`.

### Java version troubleshooting

Confirm that the JDK used by Gradle is Java 17 or newer before starting the
backend:

```bash
java -version
./gradlew --version
```

If Maven reports `release version 21 not supported`, Maven is running on a JDK
older than the Java release requested by that Maven project. That message does
not come from this repository's build: this project uses Gradle and targets
Java 17 in `build.gradle`. Run `./gradlew bootRun` from this repository's root
instead of `mvn spring-boot:run` from a `backend` directory. If you intended to
run a different Maven-based checkout, install JDK 21 and point `JAVA_HOME` at
it, or change that checkout's compiler release only if its source and
dependencies support the older JDK.

## API walkthrough

Create a game. Save both values; only the host token can deal roles.

```bash
curl -sS -X POST http://localhost:8080/api/games
# {"gameCode":"ABC234","hostToken":"..."}
```

List the codes for all games currently held in memory. The Android app uses this
endpoint to populate the game-code drop-down on its join form.

```bash
curl -sS http://localhost:8080/api/games/codes
# {"gameCodes":["ABC234"]}
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

## Android app

The `androidApp` module contains a Kotlin and Jetpack Compose companion app. It supports creating or joining a game, an automatically refreshing lobby, host-only dealing, and a private role-reveal dialog. The debug app connects to `http://10.0.2.2:8080/api/games`, which maps an Android emulator to the backend running on the development machine.

Start the backend, launch an emulator, and then build or install the app from Android Studio. From the command line (with Android SDK 35 configured):

```bash
./gradlew :androidApp:installDebug
```

For a physical device or deployed backend, update `DEFAULT_API_URL` in `androidApp/build.gradle`. Production deployments should use HTTPS; cleartext traffic is intentionally limited to emulator development hosts.

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
