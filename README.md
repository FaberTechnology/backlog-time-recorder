# Backlog Time Recorder

> AWS Lambda service that reacts to Backlog issue-update webhooks to automatically manage milestones, actual hours, and the "Started at" custom field.

## About

This project deploys a single AWS Lambda function (via AWS CDK) that receives
[Backlog](https://backlog.com/) issue-update webhook events through a public
Lambda Function URL. On each webhook it applies a small set of update rules to
the issue via the Backlog API:

- **Milestones** — when an issue's start/due date changes, recalculates and
  applies the monthly milestones that should cover that date range (creating
  missing milestones, removing ones that no longer apply).
- **Actual hours** — when an issue is closed and has no actual hours recorded
  yet, calculates them from the issue's creation date and its `Started at`
  custom field.
- **Started at** — when an issue moves to Open or In Progress, stamps/updates
  the `Started at` custom field.

See [`lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java`](lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java)
for the webhook entry point and [`lambda/src/main/java/com/lambda/strategies/`](lambda/src/main/java/com/lambda/strategies/)
for the individual update rules.

### Tech Stack

| Layer          | Technology                                              |
| -------------- | -------------------------------------------------------- |
| Language       | Java 17                                                   |
| Framework      | AWS CDK 2.114.0 (Java)                                    |
| Backlog client | [backlog4j](https://github.com/nulab/backlog4j) 2.6.0, targeting the `faber-wi` Backlog space |
| Infrastructure | AWS Lambda (Java 17 runtime, SnapStart, public Function URL, no API Gateway) |
| Build          | Maven (root CDK app + `lambda/` module, packaged with `maven-shade-plugin`) |

### Project Structure

```
.
├── src/main/java/com/myorg/          # CDK app (infrastructure)
│   ├── BacklogTimeRecorderApp.java   # CDK app entry point
│   └── BacklogTimeRecorderStack.java # Lambda + Function URL stack definition
├── lambda/                           # Lambda function source, built as its own jar
│   ├── src/main/java/com/lambda/
│   │   ├── handlers/                 # Webhook entry point + issue-update orchestration
│   │   ├── strategies/               # One strategy per update rule (milestone / actual hours / started-at)
│   │   ├── helpers/                  # Milestone, time-tracking, work-schedule calculations
│   │   └── models/                   # Webhook payload / issue models
│   └── events/                       # Sample Backlog webhook payloads for local testing
├── cdk.json                          # CDK app entry point config
└── pom.xml                           # Root Maven project (CDK app)
```

## System Requirements

- Java 17
- Maven
- AWS CDK CLI (`npm install -g aws-cdk`)
- AWS account credentials with permission to deploy Lambda

A ready-to-use [devcontainer](.devcontainer/devcontainer.json) is provided with
Java 17, Maven, the AWS CDK CLI, and the AWS CLI preinstalled.

## Quick Start

### 1. Clone the repository

```bash
git clone git@github.com:FaberTechnology/backlog-time-recorder.git
cd backlog-time-recorder
```

### 2. Configure environment

Set the `BACKLOG_API_KEY` environment variable to a Backlog API key for the
`faber-wi` space. It is required both to deploy (the CDK stack injects it into
the Lambda's environment) and to run the Lambda's tests/build locally.

Optionally, set `PRODUCT_OWNER_USER_IDS`, `SETTING_PRIORITY_STATUS_IDS`, and
`ENABLED_PROJECT_KEYS` to enable the PBI status-transition check (Open ->
Setting Priority, or any status -> Closed, requires a Product Owner). This
check only applies to issues whose Backlog issue type name is exactly "PBI"
(hardcoded) in projects listed in `ENABLED_PROJECT_KEYS`, so the rule can be
rolled out to a few projects first. See the Configuration table below.

### 3. Run the application

This service has no local "run" mode — it is deployed as a Lambda and invoked
by Backlog's webhook. To deploy it:

```bash
cd lambda && mvn -B clean package && cd ..
cdk deploy --require-approval never
```

After deploying, point a Backlog webhook (issue created/updated) at the
printed Lambda Function URL.

## Development

### Build

```bash
# Lambda module (produces lambda/target/backlog-time-recorder-0.1.jar, consumed by the CDK stack)
cd lambda && mvn -B clean package

# CDK app
mvn compile
```

### Run Tests

```bash
# Root (CDK stack tests)
mvn test

# Lambda module
cd lambda && mvn test
```

### Lint / Static Analysis

No linter or static analysis tool is currently configured for this project.

## Configuration

| Variable                     | Description                                              | Required |
| ----------------------------- | --------------------------------------------------------- | -------- |
| `BACKLOG_API_KEY`             | API key for the `faber-wi` Backlog space, used by both the deployed Lambda and the CDK/Maven build | Yes      |
| `PRODUCT_OWNER_USER_IDS`      | Comma-separated Backlog user IDs allowed to move a PBI from Open to Setting Priority, or to Closed. If unset, the status-transition check is disabled | No       |
| `SETTING_PRIORITY_STATUS_IDS` | Comma-separated numeric Backlog status IDs of the "Setting Priority" status, one per project (each Backlog project can assign it a different ID) | No (required only to enforce the Open -> Setting Priority rule) |
| `ENABLED_PROJECT_KEYS`        | Comma-separated Backlog project keys (project codes) the status-transition check applies to. Lets the rule be rolled out to a few projects first; if unset, the check is disabled everywhere | No (required to enable the status-transition check) |

## Architecture

A single CDK stack (`BacklogTimeRecorderStack`) provisions one Lambda function
with a public Function URL (no auth) as the webhook receiver. Backlog sends
issue-update webhook events to that URL; the Lambda parses the payload,
decides which update strategies apply, and calls back into the Backlog API via
`backlog4j` to update the issue. See
[docs/architecture/system-architecture.md](docs/architecture/system-architecture.md)
for the detailed architecture write-up.

## API Documentation

This service does not expose its own REST API; it only receives Backlog
webhook events on its Lambda Function URL. Sample payloads used for local
testing are in [`lambda/events/`](lambda/events/).

## Documentation

All project documentation is in the [docs/](docs/) directory:

- [Overview](docs/overview/) -- project context and goals
- [Architecture](docs/architecture/) -- system design and diagrams
- [Guides](docs/guides/) -- how-to guides (currently: [deployment](docs/guides/deployment.md))

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines and
contribution workflow.

## Security

See [SECURITY.md](SECURITY.md) for the security policy and how to report vulnerabilities.

## Links

<!-- TODO: add project-specific links (Backlog space, Slack channel, wiki, etc.) -->
