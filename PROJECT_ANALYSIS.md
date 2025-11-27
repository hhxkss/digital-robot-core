# Digital Robot Core – Project Analysis

## High-level overview
- **Purpose**: Spring Boot service exposing WebSocket APIs for coordinating robot chassis control and avatar (digital human) interactions.
- **Key components**:
  - **WebSocket layer**: `WsHandler`, `WsGateway`, `WsConfig`, and `MessageParser` parse inbound JSON frames, route robot/avatar requests, and broadcast events.
  - **Robot services**: `RobotService` wraps chassis commands (real serial or simulated), while `AvatarService` relays requests/responses to connected avatar clients.
  - **Chassis drivers**: `SerialChassisDriver` delegates to `SerialLink` for real hardware IO; `SimChassisDriver` emulates navigation behavior for local testing.
  - **Report processing**: `ChassisReportDispatcher` funnels raw chassis frames to registered `ChassisReportHandler` implementations such as `NavResultHandler` for navigation outcomes.

## Message and control flow
1. **Incoming WebSocket message** → `WsHandler.handleMessage` parses payload to `WsMessage` and branches by `op`/`method`:
   - `request_robot` or `robot.*`: dispatches to `RobotService.handle`, acknowledges acceptance, and lets the chassis driver execute commands.
   - `request_avatar` or `avatar.*`: relays via `AvatarService.handle` (fire-and-forget) to connected avatar clients.
   - `avatar_response`: completes pending avatar requests through `AvatarService.onAvatarResponse` and broadcasts to all sessions through `WsGateway`.
   - Other frames are broadcast verbatim.
2. **Chassis reports** originate from drivers via `RobotService.onFrame` and are broadcast to WebSocket sessions by `WsHandler.onSerial`. They also flow into `ChassisReportDispatcher`, which iterates registered handlers in order and lets the first capable handler process a frame.
3. **Navigation auto-narration**: `NavResultHandler` watches for successful `nav_result` messages and asynchronously triggers avatar narration through `AvatarService` using the `navExecutor` thread pool defined in `AsyncConfig`.

## Configuration highlights
- `application.yaml` exposes server port and chassis settings. `robot.chassis.mode` selects `SerialChassisDriver` (`yangcheng`) or `SimChassisDriver` (`sim`), while `robot.chassis.autoOpen` controls whether drivers open automatically at startup. Navigation avatar notification can be toggled with `robot.nav.notifyAvatarOnArrival` and cooldown tuned via `arrivalCooldownMs`.
- `ChassisConfig` wires the appropriate driver beans and constructs `SerialChassisDriver` with a `SerialLink` targeting the configured port.

## Chassis driver behaviors
- **Serial driver**: `SerialLink` opens a jSerialComm port, sends ASCII frames (currently stubbed with a print), and reads framed responses with header bytes `0xAA 0x54`, checksum validation, and UTF-8 payload delivery to a callback.
- **Simulation driver**: `SimChassisDriver` emulates navigation to named points, regularly emits `nav_result` updates, supports pause/resume/cancel, enforces `max_vel` ranges, and publishes docking state transitions. It uses scheduled tasks to advance distance-to-goal metrics and stops on arrival.

## Extension points
- Implement additional `ChassisReportHandler` classes to react to other chassis frames; they are auto-discovered and ordered by `@Order`.
- `RobotService.handle` centralizes robot command names—extending it keeps WebSocket routing consistent.
- `WsGateway.publishToAvatar` standardizes the envelope for outbound avatar requests; reuse it for new avatar-side features.

## Code structure optimizations to consider
- **Centralize configuration**: chassis settings are currently injected ad hoc (for example, `RobotService` and `ChassisConfig` both read `robot.chassis.*` values individually). Introduce a dedicated `@ConfigurationProperties` class (e.g., `RobotChassisProperties`) to hold port, mode, and auto-open flags, then inject that object wherever needed to avoid duplicated `@Value` wiring and keep defaults in one place.【F:src/main/java/com/legate/digital_robot_core/service/RobotService.java†L21-L37】【F:src/main/java/com/legate/digital_robot_core/config/ChassisConfig.java†L16-L32】
- **Split WebSocket responsibilities**: `WsHandler` parses payloads, performs validation, routes robot commands, forwards avatar traffic, and formats error responses inside a single class. Extract a thin adapter that only deals with the Spring `WebSocketSession` lifecycle, delegating business routing to a dedicated `WebSocketDispatcher`/`RobotCommandRouter`. This reduces branching, makes it easier to unit test routing rules, and clarifies boundaries between transport and domain logic.【F:src/main/java/com/legate/digital_robot_core/ws/WsHandler.java†L17-L109】
- **Share serialization infrastructure**: multiple components instantiate their own `ObjectMapper` (`WsHandler`, `WsGateway`, `RobotService`, `MessageParser`), which can diverge in configuration. Define a single, injectable `ObjectMapper` bean (or a small `JsonCodec` utility) and inject it where needed to ensure consistent serialization features (modules, naming, date formats) and simplify testing mocks.【F:src/main/java/com/legate/digital_robot_core/ws/WsHandler.java†L19-L98】【F:src/main/java/com/legate/digital_robot_core/ws/WsGateway.java†L17-L43】【F:src/main/java/com/legate/digital_robot_core/service/RobotService.java†L21-L124】【F:src/main/java/com/legate/digital_robot_core/parser/MessageParser.java†L14-L40】
- **Clarify chassis event fan-out**: chassis frames currently flow directly to both `WsHandler` (for broadcast) and `ChassisReportDispatcher` via ad hoc listener registration. Introducing a lightweight domain event publisher (e.g., Spring `ApplicationEventPublisher` or a dedicated `ChassisEventBus`) would make subscriptions explicit, allow filtering by event type instead of raw strings, and keep the fan-out logic in one place.【F:src/main/java/com/legate/digital_robot_core/ws/WsHandler.java†L32-L38】【F:src/main/java/com/legate/digital_robot_core/chassis/ChassisReportDispatcher.java†L15-L42】
- **Strengthen domain typing**: handlers such as `NavResultHandler` parse raw regex groups and pass stringly-typed maps to other services. Defining small DTOs/records for chassis reports (e.g., `NavResult`, `DockStatus`) would reduce parsing brittleness, make downstream code self-documenting, and create a single place to validate protocol changes.【F:src/main/java/com/legate/digital_robot_core/chassis/handler/NavResultHandler.java†L19-L60】【F:src/main/java/com/legate/digital_robot_core/service/AvatarService.java†L14-L63】
