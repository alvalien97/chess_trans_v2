*This project has been created as part of the 42 curriculum by alvalien, mmembril.*

# Transcendence Chess

## Description

Transcendence Chess is a browser-based chess application. The frontend is built with Angular, the backend with Spring Boot, and PostgreSQL stores user accounts and game metadata. Chess rules are validated on the backend with Chesslib; the browser renders the board and sends moves over WebSocket.

The playable flow is one authenticated account per color. A player joins matchmaking and waits for a different account; the server assigns colors and checks that each move comes from the account assigned to the color whose turn it is. Users can also create and enter single-elimination tournaments for 2, 4, or 8 players. This is a work in progress, not a claim that every ft_transcendence requirement or optional module has passed evaluation.

## Features

- Register and log in with an email and password. Passwords are hashed with BCrypt.
- Authenticated REST and STOMP sessions using the login token.
- One-versus-one matchmaking with a waiting queue, persistent player/color assignment, and server-enforced turns.
- Play chess with server-side legal move validation, including castling, en passant, promotion to queen, check, checkmate, and draw rules.
- Receive board updates and shared chat messages through STOMP over WebSocket. The server derives chat senders from the authenticated account.
- Create and enter 2/4/8-player tournaments with bracket tracking, automatic advancement after wins, and organizer resolution of drawn knockout matches.
- Search for users by partial username, add or remove friends, and view their session-based online status from Home.
- Upload an avatar from Settings; avatar-only changes do not require entering the current password.
- Read the project-specific Privacy Policy and Terms of Service from the login screen or the authenticated app footer.
- Run the application and its dependencies with Docker Compose.

## Technical Stack

- Frontend: Angular 20, TypeScript, HTML, and CSS.
- Backend: Java 21, Spring Boot 4, Spring MVC, Spring Data JPA, and WebSocket/STOMP.
- Chess rules: Chesslib 1.3.7.
- Database: PostgreSQL 16, accessed through Spring Data JPA.
- Deployment: Docker Compose and Nginx. Browser traffic is served over HTTPS using a local self-signed certificate.

Angular provides the browser UI; Spring Boot owns game state and validates moves so clients cannot decide which moves are legal. PostgreSQL is used for persistent account and game metadata storage.

## Requirements

- Docker Engine and the Docker Compose plugin.
- A browser compatible with the current stable Google Chrome release.

Java and Node.js are not required on the host when running the application through Docker Compose.

## Instructions

1. From the project root, create the local environment file if it does not exist:

   ```sh
   cp .env.example .env
   ```

2. Build and start the application:

   ```sh
   docker compose up --build
   ```

   On Linux installations where the current user cannot access Docker directly, prefix the command with `sudo`.

3. Open [https://localhost](https://localhost). The local certificate is self-signed, so the browser will ask you to confirm the security exception.

4. Create an account or log in, then select **Buscar rival**. Keep the page open while a second account logs in and joins the queue; the first account waits until another player arrives.

5. To play a tournament, create one, have the required number of accounts join, then have the organizer start the bracket. Open assigned matches with **Jugar**. A drawn game pauses until the organizer records which participant advances.

Press `Ctrl+C` to stop the foreground Compose process. `docker compose down` stops and removes the containers but preserves the PostgreSQL volume. `docker compose down -v` also deletes the database volume and its data.

Development credentials in `.env.example` are for local use only and must not be used in a production deployment.

## Database Schema

- `users`: generated numeric ID, unique username, unique email, BCrypt password hash, optional `avatar_url`, and creation timestamp.
- `friendships`: directed user/friend relations. Adding a friend creates both directions; removing one deletes both.
- `games`: string ID, White and Black user references, status, and creation timestamp.
- `tournaments`: name, organizer, bracket capacity, status, and creation timestamp.
- `tournament_participants`: tournament/user registration and seed/order.
- `tournament_matches`: tournament round/position, players, linked game, result, and status.

The current Chesslib board position and move history are held in backend memory. Match assignments and tournament bracket records persist, but a backend restart currently resets in-progress chess positions and invalidates in-memory login tokens. The matchmaking waiting queue is also in memory.

## Modules

The team has not yet confirmed which optional modules it intends to claim or its final point calculation. The current code suggests candidates, but they must not be counted until the team verifies every criterion and demonstrates them:

- Web frameworks: Angular and Spring Boot.
- Real-time features: authenticated WebSocket game updates and shared chat; reconnect behavior and disconnect handling still need evaluation.
- Gaming: browser-based chess and two-player matchmaking; remote-player reconnection is not implemented.
- Tournament system: registration, 2/4/8-player brackets, assigned matches, results, and advancement. This is a candidate for the PDF's minor Tournament module, subject to evaluation.
- User management: secure sign-up/login, editable account details/avatar, public player profiles, friend search/add/remove, and STOMP-session presence. This remains a candidate module until the team validates and demonstrates every criterion.
- ORM: Spring Data JPA is used for PostgreSQL persistence.

The PDF requires 14 points from fully implemented, demonstrated modules. The team must record its selected modules and point calculation before evaluation; do not double-count overlapping criteria. Multilingual support is a one-point optional module and requires three complete translations, a language switcher, and all user-facing text to be translatable.

## Team Information

The following 42 logins were provided for the project. Roles and responsibilities still need to be agreed and recorded.

| 42 login | Role(s) | Responsibilities |
| --- | --- | --- |
| alvalien | To be confirmed | Admin |
| mmembril | To be confirmed | Admin |

## Project Management

Task distribution, meeting cadence, project tracking tools, and communication channels have not yet been documented. Add the team's actual process here before evaluation.

## Individual Contributions

The implementation owner for each feature and each member's individual contributions have not yet been recorded. Add an honest breakdown here before evaluation; do not infer ownership from the test accounts above.

## Testing

Run backend tests from the project root:

```sh
./chess-backend/gradlew -p chess-backend test --no-daemon
```

Build the frontend with Docker as part of `docker compose up --build`, or from the frontend directory with `npm run build` after installing its dependencies.

## Known Limitations and Work Remaining

- Responsive layouts, keyboard focus styles, a skip link, reduced-motion handling, and labeled forms are present, but WCAG 2.1 AA has not been audited or certified. Test with keyboard, screen readers, and supported viewport sizes before claiming that module.
- Privacy Policy and Terms of Service are available in-app and describe the current academic demo. Before any public/production deployment, the team must establish the actual data controller/contact, retention process, and applicable legal review.
- Friend presence is tracked from authenticated STOMP connections and broadcast when a user's first connection opens or last connection closes. Account deletion removes the avatar and friendship rows, revokes sessions, and leaves matchmaking; it anonymizes the account while retaining game/tournament history to preserve results.
- Session tokens are stored in memory on the backend, have no automatic expiry, and become invalid after a backend restart. Logout revokes the current token; deleting an account revokes all of that account's tokens.
- Active board positions and move history are not persisted, although match assignments and tournament brackets are.
- Multilingual support is not implemented. The PDF's optional module requires at least three complete languages, a language switcher, and translatable user-facing text.
- Tournaments are limited to 2, 4, or 8 players; drawn knockout matches pause until the organizer selects who advances.
- The original chess piece image files are in `chess-frontend/src/assets/pieces`, but the board currently renders Unicode symbols instead.
- Team roles, project-management records, selected modules/points, and feature ownership still need to be completed in this document.

## File and Folder Guide

This guide covers maintained source, configuration, and documentation. Generated output such as `chess-backend/build`, `.gradle`, `chess-frontend/.angular`, `node_modules`, `dist`, and `comienzos/Chess_Transcendence/out` is generated or cached and is not runtime source.

### Project Root

- `README.md`: project description, setup, schema, status, and this file map.
- `transcendence.pdf`: 42 project specification.
- `.env.example`: sample PostgreSQL settings copied to local `.env`.
- `.env`: local environment values; do not commit or publish.
- `.gitignore`: root-level generated and secret exclusions.
- `Makefile`: `up`, `down`, and `logs` shortcuts for Compose.
- `docker-compose.yml`: PostgreSQL, backend, frontend, Nginx, health check, and persistent database volume.
- `.vscode/settings.json`: workspace-specific VS Code settings.
- `nginx/nginx.conf`: HTTP-to-HTTPS redirect and proxy routes for frontend, REST, and WebSocket.
- `nginx/certs/nginx-selfsigned.crt`: local HTTPS certificate.
- `nginx/certs/nginx-selfsigned.key`: local private key; never use in production.

### `chess-backend`

- `build.gradle`: Java/Spring dependencies, Chesslib, repositories, and test configuration.
- `settings.gradle`: Gradle project name.
- `gradlew`, `gradlew.bat`: Gradle wrapper launchers for Unix and Windows.
- `gradle/wrapper/gradle-wrapper.properties`: pinned Gradle distribution settings.
- `gradle/wrapper/gradle-wrapper.jar`: Gradle wrapper implementation.
- `Dockerfile`: builds and runs the Spring Boot executable JAR.
- `.dockerignore`: excludes local/generated files from the backend build context.
- `.gitignore`: backend-specific generated/IDE exclusions.
- `.gitattributes`: Git text-file normalization rules.
- `.idea/compiler.xml`, `.idea/gradle.xml`, `.idea/misc.xml`, `.idea/workspace.xml`, and `.idea/.gitignore`: IntelliJ compiler, Gradle, project, workspace, and ignore settings; not required at runtime.
- `HELP.md`: Spring-generated help document.
- `src/main/resources/application.properties`: server port, PostgreSQL environment defaults, and JPA settings.

`src/main/java/com/transcendence/chess/`

- `ChessApplication.java`: Spring Boot entry point.
- `config/CorsConfig.java`: HTTP CORS policy and BCrypt encoder bean.
- `config/RestExceptionHandler.java`: REST validation and application error responses.
- `config/StompAuthInterceptor.java`: validates STOMP tokens and restricts game subscriptions to participants.
- `config/WebSocketConfig.java`: STOMP broker, `/ws` endpoint, and inbound authentication interceptor.
- `controller/AuthController.java`: signup/login/logout, account updates/deletion, public profiles, and friend search/list/add/remove endpoints.
- `controller/ChatController.java`: receives chat and sets the sender from the authenticated user.
- `controller/ChessController.java`: processes WebSocket moves and broadcasts accepted board states.
- `controller/GameRestController.java`: returns a participant-authorized game state.
- `controller/MatchmakingController.java`: join, poll, and leave the one-versus-one queue.
- `controller/TournamentController.java`: list, create, join, start, and resolve drawn tournament matches.
- `dto/AuthRequest.java`: validated signup/login input.
- `dto/AuthResponse.java`: session token and public user data.
- `dto/FriendView.java`, `dto/AddFriendRequest.java`: friend search/list and add responses/requests.
- `dto/PublicProfileView.java`, `dto/PresenceUpdate.java`: public player data and live connection-state broadcasts.
- `dto/MoveDTO.java`: submitted board coordinates.
- `dto/MatchmakingStatus.java`: queue state, game ID, assigned color, and opponent.
- `dto/TournamentRequest.java`: tournament name and bracket capacity.
- `dto/TournamentView.java`: tournament, participant, and bracket response model.
- `dto/ResolveTournamentMatchRequest.java`: selected winner for a drawn knockout match.
- `model/User.java`: persistent account, unique username/email, password hash, avatar data, and creation date.
- `model/Friendship.java`: persistent directed friend relation.
- `model/Game.java`: persistent match ID, White/Black users, status, and creation date.
- `model/GameState.java`: board JSON, side to move, and game status sent to Angular.
- `model/Tournament.java`: tournament identity, organizer, capacity, and status.
- `model/TournamentParticipant.java`: registration order/seed for one tournament user.
- `model/TournamentMatch.java`: bracket round, players, linked game, result, and status.
- `model/ChatMessage.java`: chat sender and message content.
- `model/Piece.java`, `Type.java`, `Pawn.java`, `Rook.java`, `Knight.java`, `Bishop.java`, `Queen.java`, and `King.java`: board-response piece objects. `ChessRules` maps Chesslib pieces to these objects; legal move decisions come from Chesslib rather than the legacy `canMove` methods.
- `model/Board.java`: earlier board helper; the active match state is managed by `ChessRules`.
- `repository/UserRepository.java`: user lookup, partial case-insensitive search, and uniqueness queries.
- `repository/FriendshipRepository.java`: friend list and relationship operations.
- `repository/GameRepository.java`: match persistence and active-game lookup by user.
- `repository/TournamentRepository.java`: tournament persistence/listing.
- `repository/TournamentParticipantRepository.java`: registration and seed lookup.
- `repository/TournamentMatchRepository.java`: bracket lookup by game and round.
- `service/AuthService.java`: account/profile updates, friend search and management, deletion cleanup, BCrypt verification, and in-memory token validation.
- `service/PresenceService.java`: authenticated STOMP session tracking and online/offline topic broadcasts.
- `service/ChessRules.java`: adapts board coordinates to Chesslib legal moves and builds frontend board state.
- `service/GameService.java`: authorizes match participants, enforces assigned-color turns, applies moves, and reports results.
- `service/MatchmakingService.java`: synchronized in-memory waiting queue that creates persisted two-user games.
- `service/TournamentService.java`: registration, bracket creation, advancement, winner tracking, and draw resolution.

`src/test/java/com/transcendence/chess/service/`

- `ChessRulesTest.java`: legal/illegal and special chess moves, king safety, and checkmate.
- `GameServiceTest.java`: current-color ownership and rejection of nonparticipants.
- `MatchmakingServiceTest.java`: waiting, no self-pairing, and color assignment.
- `TournamentServiceTest.java`: seeded first-round pairings and advancement after a complete round.

### `chess-frontend`

- `package.json`, `package-lock.json`: frontend dependencies, scripts, and pinned versions.
- `angular.json`: Angular build, serve, asset, and test targets.
- `tsconfig.json`, `tsconfig.app.json`, `tsconfig.spec.json`: TypeScript options for app and tests.
- `Dockerfile`: builds Angular assets and serves them with Nginx.
- `nginx.default.conf`: static-file and Angular route fallback configuration.
- `proxy.conf.json`: local development proxy to backend HTTP and WebSocket endpoints.
- `.dockerignore`, `.gitignore`: Docker and Git exclusions.
- `.editorconfig`: editor formatting defaults.
- `README.md`: original Angular CLI notes.
- `public/favicon.ico`: browser tab icon.
- `.vscode/extensions.json`: recommended VS Code extensions.
- `.vscode/launch.json`: frontend debugger setup.
- `.vscode/tasks.json`: frontend build/run tasks.
- `src/index.html`: document shell and early `global` alias required by SockJS.
- `src/main.ts`: bootstraps Angular in the browser.
- `src/main.server.ts`: SSR bootstrap entry point.
- `src/server.ts`: optional Angular SSR/Express server; Docker Nginx serves the browser build as static files.
- `src/styles.css`: global document defaults.
- `src/app/app.ts`, `app.html`, `app.css`: root component, login/register, authenticated layout, and page styling.
- `src/app/components/legal-notice/`: in-app Privacy Policy and Terms of Service views.
- `src/app/services/presence-websocket.ts`: app-wide subscription to online/offline presence updates.
- `src/app/app.config.ts`: browser HTTP, router, and zoneless providers.
- `src/app/app.config.server.ts`: server-rendering providers.
- `src/app/app.routes.ts`, `app.routes.server.ts`: browser/server route definitions.
- `src/app/app.spec.ts`: root-component smoke test.
- `src/app/components/board/board.component.ts`: queue polling, match/color assignment, state loading, and color/turn-restricted input.
- `src/app/components/board/board.component.html`: matchmaking states, turn/status display, and board template.
- `src/app/components/board/board.component.css`: board squares and selection/matchmaking styles.
- `src/app/components/board/board.spec.ts`: board-component smoke test.
- `src/app/components/chat/chat.component.ts`: shared-chat subscription and message publishing.
- `src/app/components/chat/chat.component.html`: transcript and send form.
- `src/app/components/chat/chat.component.css`: chat presentation.
- `src/app/components/tournaments/tournaments.component.ts`: tournament listing, registration, start, game selection, and draw resolution.
- `src/app/components/tournaments/tournaments.component.html`: tournament form, participants, bracket, and actions.
- `src/app/components/tournaments/tournaments.component.css`: tournament layout and small-screen adjustments.
- `src/app/services/auth.ts`: signup/login, browser session storage, and bearer-header access.
- `src/app/services/chess-websocket.ts`: authenticated STOMP, chat/game subscriptions, move publishing, and error callbacks.
- `src/app/services/chess-websocket.spec.ts`: WebSocket-service smoke test.
- `src/assets/pieces/white_pawn.jpg`, `white_horse.jpg`, `white_bishop.jpg`, `white_tower.jpg`, `white_queen.jpg`, `white_king.jpg`: original white-piece images.
- `src/assets/pieces/black_pawn.jpg`, `black_horse.jpg`, `black_bishop.jpg`, `black_tower.jpg`, `black_queen.jpg`, `black_king.jpg`: original black-piece images.

The image assets are copied from the original project but are not wired into the board; it currently renders Unicode symbols. The Angular asset list currently copies `public/`, not `src/assets/`, so these JPEG files are also not included in the built site.

### `comienzos/Chess_Transcendence`

The original Java/Swing desktop chess project, retained as a reference and not launched by the web app.

- `Chess_Transcendence.iml`: IntelliJ module metadata.
- `.gitignore`: original-project generated-file exclusions.
- `.idea/modules.xml`: IntelliJ module registration.
- `.idea/misc.xml`: IntelliJ project/compiler settings.
- `.idea/workspace.xml`: machine-specific IntelliJ state.
- `.idea/.gitignore`: IntelliJ-generated-file exclusions.
- `src/main/Main.java`: desktop application entry point.
- `src/main/GamePanel.java`: Swing game loop, selection, simulation, check/mate/stalemate, castling, promotion, and rendering.
- `src/main/Board.java`: Swing board drawing and square sizing.
- `src/main/Mouse.java`: mouse input adapter.
- `src/main/Type.java`: original piece-type enum.
- `src/piece/Piece.java`: shared piece state, image loading, occupancy, and movement helpers.
- `src/piece/Pawn.java`: pawn moves, double-step, captures, and en passant.
- `src/piece/Rook.java`: rook movement.
- `src/piece/Knight.java`: knight movement.
- `src/piece/Bishop.java`: bishop movement.
- `src/piece/Queen.java`: queen movement.
- `src/piece/King.java`: king movement and castling checks.
- `res/piece/white_pawn.jpg`, `white_horse.jpg`, `white_bishop.jpg`, `white_tower.jpg`, `white_queen.jpg`, `white_king.jpg`: original white-piece sprites.
- `res/piece/black_pawn.jpg`, `black_horse.jpg`, `black_bishop.jpg`, `black_tower.jpg`, `black_queen.jpg`, `black_king.jpg`: original black-piece sprites.
- `out/production/Chess_Transcendence/`: generated `.class` files and copied sprites from an old IDE build; not source or required by the web app.

## Resources

- [Angular documentation](https://angular.dev/)
- [Spring Boot documentation](https://docs.spring.io/spring-boot/index.html)
- [Chesslib repository and documentation](https://github.com/bhlangonijr/chesslib)
- [Docker Compose documentation](https://docs.docker.com/compose/)

AI-use disclosure: an AI assistant was used to help inspect the project specification, investigate and fix defects in friend search/removal and avatar updates, add regression tests, and draft/update project documentation. The team must review and understand these changes and disclose any additional AI use elsewhere in the project.