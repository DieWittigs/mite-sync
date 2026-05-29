# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

Maven wrapper (`./mvnw`) is committed; prefer it over a system `mvn`.

- Run the app locally: `./mvnw spring-boot:run` (listens on 8080)
- Build jar: `./mvnw package`
- Full verify (tests + JaCoCo coverage check): `./mvnw verify`
- Single test class: `./mvnw test -Dtest=BookingProposalServiceTest`
- Single test method: `./mvnw test -Dtest=BookingProposalServiceTest#methodName`
- Docker build: `docker build -t mite-sync .` (multi-stage, Maven 3.9 + Temurin 25)
- Docker Compose (uses Docker Hub image by default): `docker compose up` — needs `.env` (see `.env.example`). Port 8888 only needs to be exposed during the first Google OAuth setup.

JaCoCo enforces **≥80% instruction coverage at BUNDLE level** as part of `verify`. `GoogleCalendarService` and `AzureDevOpsService` are excluded (external I/O). When adding code in other classes, expect the coverage gate to fail builds if untested.

Java version is pinned to **25** (`.java-version`, `pom.xml`). Spring Boot **3.5.14**.

## Big-Picture Architecture

The app exposes two independent workflows over the Mite time-tracking API, both as REST endpoints (no UI, no DB). See `HELP.md` and `mite-sync.http` for example payloads.

### Two Mite instances, two directions

There are **two** `MiteClient` beans (`MiteSyncConfig`):
- `sourceMiteClient` → the billing-relevant Mite where hours actually must end up
- `targetMiteClient` → a mirror Mite instance

Hosts and credentials come from env vars (`MITE_SYNC_SOURCE_*` / `MITE_SYNC_TARGET_*`). These are wired by **bean name** into services — never assume which client a service uses without checking. In particular:
- `MiteSyncService` (used by `/sync-jobs`) reads from SOURCE and writes to TARGET (delete-then-recreate).
- `MiteBookingService` (used by `/daily-reports/*`) reads **and writes** to SOURCE. Daily-Reports book into the billing system; `/sync-jobs` later mirrors them to TARGET.

### Workflow 1 — `/sync-jobs` (classic mirror)

`MiteSyncController` → `MiteSyncFacade` → `MiteSyncService`. Idempotent: deletes all TARGET entries in the date range, then recreates from SOURCE. Field translation between the two Mite instances goes through `TimeEntryConverter` (a Spring `Converter<TimeEntries.TimeEntry, TimeEntry>`), which rewrites `project-id` and `service-id` to the TARGET values from `application.yml`. Date format on this endpoint is **`dd.MM.yyyy`** (not ISO).

### Workflow 2 — `/daily-reports/{date}/preview` and `/book`

`DailyReportController` → `DailyReportFacade`, which fans out to four services and composes a proposal:
1. `GoogleCalendarService` — meetings of the day (rounded up to 15-min steps)
2. `AzureDevOpsService` — WIQL queries for "changed by me today" and "open work items assigned to me"
3. `MiteBookingService` — already-booked entries for the day (to avoid duplicates)
4. `BookingProposalService` — pure logic that combines all of the above with the user-supplied `mainPbiId` / `targetHours`

The `preview` step is read-only and **safe to re-run**. `book` actually creates entries in SOURCE Mite. Clients are expected to call `preview`, optionally edit the returned `ProposalEntryModel` list, then post it to `book`. Date format on these endpoints is **ISO (`yyyy-MM-dd`)**.

Booking rules (in `BookingProposalService`):
- "Daily" event (configurable summary) is always booked at a fixed duration (default 15 min), regardless of calendar length.
- Other meetings are rounded up to the next 15-min step.
- Meeting entries get note `#<meeting-collector-pbi> <summary>`; remaining hours are filled onto the main PBI: `#<mainPbiId> <title>`.
- `targetHours` from the request body overrides the default daily target (375 min = 6.25 h). Already-booked minutes count toward the target.
- Duplicate guard: an entry whose note matches an already-booked Mite note (case-insensitive, trimmed) is skipped.
- `skipped` events and `declined` calendar events are filtered out; `needsAction` stays in (user removes manually before `/book`).

### External-system integration notes

- **Google Calendar OAuth**: `GoogleCalendarService` uses **lazy `ensureClient()`**, not `@PostConstruct`. `LocalServerReceiver` would otherwise block startup. First `/preview` call triggers a browser popup; refresh token is persisted to `~/.mite-sync/google-tokens/`. Setup steps live in `HELP.md`. The `skip-summaries` list must stay a **comma-separated string** in `application.yml` and be split via SpEL (`@Value("#{'${...}'.split(',')}")`) — Spring's plain `@Value` does not reliably bind YAML block lists to `List<String>`.
- **Azure DevOps**: Plain JDK `HttpClient` + Jackson — no SDK. Auth = PAT in Basic header (`":" + pat`, base64). When building the WIQL URL, project names with spaces must use **percent-encoding (`%20`)**, not form-encoding (`+`). The code does `URLEncoder.encode(project, UTF_8).replace("+", "%20")` for that reason.
- **mite-java JAXB asymmetry** (`io.seventytwo.oss:mite-java:1.1.0`): read and write types differ.
  - Read side (`TimeEntries.TimeEntry`): `getId().getValue()` → `long`, `getMinutes().getValue()` → `short`, `getNote()` → `Object` (call `.toString()` after null-check).
  - Write side (`TimeEntry` with inner classes): `Minutes.setValue(short)` requires an explicit `(short)` cast; `DateAt.setValue(LocalDate)` takes a `LocalDate`, not a String; `setNote(Object)` takes a plain `String` directly (no `Note` wrapper).
  - This is why `MiteBookingService.buildTimeEntry` and `TimeEntryConverter.convert` look more verbose than seems necessary.

### Configuration & secrets

All runtime config lives in `src/main/resources/application.yml` and is overridden via env vars at runtime (Spring relaxed binding — see `docker-compose.yml` for the mapping `MITE_SYNC_SOURCE_API_KEY` → `mite-sync.source.api-key`, etc.). The committed `application.yml` has **empty/placeholder values**; real secrets live in `.env` (gitignored) and are surfaced to the container via `docker-compose.yml`. When editing config, keep the env-var override path working — `docker-compose.yml` is the contract.

Google OAuth artifacts (`google-client-secret.json`, `google-tokens/`) live under `~/.mite-sync/` on the host and are bind-mounted into the container.

### Validation & error handling

- Request DTOs use Jakarta validation (`@Valid` on controllers).
- `GlobalExceptionHandler` turns `MethodArgumentNotValidException` into a flat `{field: message}` JSON body.
- Custom date-range validation: `@ValidDateRange` annotation + `DateRangeValidator` (used on `SyncJobModel`).
- `MiteBookingService.book` is **best-effort**: per-entry failures are collected in `BookingResultModel.failed` and don't abort the run.

## Conventions worth knowing

- Package root: `org.twittig.mite.mitesync`. Layering is `web.controller` → `facade` → `service` (+ `converter`, `web.model`, `web.annotation`).
- Tests mirror the main package structure under `src/test/java`. `GoogleCalendarService` and `AzureDevOpsService` are intentionally untested (excluded from JaCoCo) because they hit live APIs — keep new external-I/O code likewise excluded only when you can't reasonably mock it, and prefer to put the testable logic into a sibling pure-logic class (the way `BookingProposalService` is split out of `DailyReportFacade`).
- Javadoc and inline comments in this repo are in **German**. Match the existing style when editing.
- The CI workflow (`.github/workflows/docker.yml`) builds and pushes `mite-sync:latest` to Docker Hub on every push to `main`. No tests run in CI — `./mvnw verify` locally is the only gate.
