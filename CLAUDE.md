# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

An AWS Lambda webhook receiver for Backlog (Nulab's project management tool). It listens for issue-update
webhooks and automatically applies bookkeeping updates to the issue: monthly milestones spanning its
start/due dates, actual-hours tracking based on a "Started at" custom field, and started-at timestamps
when work begins. The infrastructure (a single Lambda behind a Function URL) is defined with AWS CDK in Java.

The repo has two independent Maven modules:

- **Root (`/`)** — the CDK app (`com.myorg`) that defines and deploys the `BacklogTimeRecorderStack`.
- **`lambda/`** — the actual Lambda handler code (`com.lambda`), built into a shaded jar that the CDK
  stack references directly by path (`lambda/target/backlog-time-recorder-0.1.jar`). The Lambda module
  must be built *before* `cdk deploy`/`cdk synth`, since the CDK app loads that jar as a file asset.

## Common commands

Build/test the CDK app (root):
```bash
mvn package
```

Build/test the Lambda module (must happen first, produces the shaded jar CDK deploys):
```bash
cd lambda && mvn package
```

Run a single test class (from within the relevant module directory):
```bash
mvn test -Dtest=MilestoneUpdateStrategyTest
```

Run a single test method:
```bash
mvn test -Dtest=MilestoneUpdateStrategyTest#someTestMethod
```

CDK commands (run from repo root; `cdk.json` invokes `mvn -e -q compile exec:java` as the app entrypoint):
```bash
cdk synth
cdk diff
cdk deploy --require-approval never
```

Deployment is automated via [.github/workflows/deploy.yml](.github/workflows/deploy.yml) on every push to
`master`: it builds the `lambda/` module with `BACKLOG_API_KEY` injected, then runs `cdk deploy`. There is
no separate staging environment — pushing to `master` deploys to production.

## Architecture (lambda module)

Request flow: `BacklogTimeRecorder` (the Lambda `RequestHandler`) → `IssueUpdateOrchestrator` → a list of
`UpdateStrategy` implementations, each independently deciding whether it applies and mutating a shared
`UpdateIssueParams` before a single `client.updateIssue(params)` call.

- **`handlers/BacklogTimeRecorder`** — entry point (`handleRequest`). Decodes the webhook body (base64 if
  needed) into a `WebhookPayload`, inspects the issue's `changes` list for a `status` field change and/or a
  `startDate`/`limitDate` change, and only proceeds for status transitions to Open/InProgress/Closed
  (`Issue.StatusType`) or any date change. `IssueUpdater` is lazily constructed from `BACKLOG_API_KEY` (env
  var) so it can be swapped for a test double via the package-private constructor.
- **`handlers/IssueUpdateOrchestrator`** (implements `IssueUpdater`) — fetches the raw issue from Backlog,
  wraps it in `IssueWrapper`, builds a `ProjectContext` (lazy milestone lookup/creation for the issue's
  project), then runs each `UpdateStrategy` in order: `MilestoneUpdateStrategy`,
  `ActualHoursUpdateStrategy`, `StartedAtUpdateStrategy`. If no strategy applies, no Backlog update call is
  made at all (`updateIssue` returns `null`).
- **`strategies/*`** — each strategy has `canApply(IssueWrapper, ProjectContext)` (decides eligibility, may
  cache state needed by `apply`) and `apply(...)` (mutates the shared `UpdateIssueParams`). Adding a new
  automated update means adding a new `UpdateStrategy` and registering it in
  `IssueUpdateOrchestrator`'s constructor list — strategies are independent and composable, not
  mutually exclusive.
  - `MilestoneUpdateStrategy`: on a date change, computes the set of monthly (`yyyy-MMM`) milestones the
    issue's start→due range should belong to via `MilestoneHelper`, keeps non-monthly milestones untouched,
    drops monthly milestones no longer in range, and creates any missing ones through
    `ProjectContext.getOrCreateMilestone`.
  - `ActualHoursUpdateStrategy`: on transition to Closed, if actual hours aren't already set, computes them
    from the "Started at" custom field (a `;`-delimited list of start/end timestamp pairs) or falls back to
    issue creation time, via `TimeTrackingHelper`. Skips applying if the computed value is out of the
    sane `[0, 999]` range.
  - `StartedAtUpdateStrategy`: on transition to Open/InProgress, appends a new timestamp to the "Started
    at" custom field (semicolon-delimited running log of start/stop times).
- **`helpers/TimeTrackingHelper`** — parses/formats the "Started at" custom field and sums elapsed
  *working* time across its start/end pairs (delegating to `WorkScheduleHelper`), rounded to one decimal
  hour.
- **`helpers/WorkScheduleHelper`** — defines the work calendar: weekdays only, 09:30–19:30 JST. All
  actual-hours math excludes weekends and outside-hours time.
- **`helpers/MilestoneHelper`** — pure date/name logic for monthly milestones (`yyyy-MMM` format, e.g.
  `2026-Aug`), independent of the Backlog API.
- **`models/IssueWrapper`** — adapts a raw `backlog4j` `Issue` plus the webhook's new status code and
  date-changed flag into the typed accessors strategies use (dates converted to JST `LocalDate`).
- **`models/ProjectContext`** — lazily-cached per-project milestone list with get-or-create semantics, so
  multiple strategies/lookups within one invocation don't refetch or duplicate-create milestones.
- **`models/Issue`** — a `backlog4j` issue subtype extended to deserialize the webhook's `changes` array
  (field/old/new value triples), which the stock `backlog4j` model doesn't expose.

Status codes referenced throughout (`Issue.StatusType` from `backlog4j`): the handler only reacts to Open,
InProgress, and Closed — other custom workflow statuses are ignored.

## Testing conventions

- `TestContext`/`TestLogger` in `lambda/src/test/java/com/lambda` are hand-written fakes for the Lambda
  `Context`/`LambdaLogger` interfaces, used to invoke `BacklogTimeRecorder.handleRequest` directly in tests
  without a real Lambda runtime.
- `InvokeTest` and `IssueDeserializationTest` exercise the handler/model layer against sample payloads in
  `lambda/events/*.json`.
- Strategy and helper tests (`MilestoneUpdateStrategyTest`, `ActualHoursUpdateStrategyTest`,
  `WorkScheduleHelperTest`, `MilestoneHelperTest`) test each unit independently with fakes/mocks rather than
  hitting the real Backlog API.
