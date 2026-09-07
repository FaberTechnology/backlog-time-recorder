---
title: "Backlog Time Recorder Overview"
created: 2026-08-11
author: "@HoangHades"
owner: "@HoangHades"
domain: ["Engineering-App"]
doc_type: "Overview-Handbook"
status: "active"
last-reviewed: 2026-09-07
review-cycle: "6-months"
tags: ["project-overview", "backlog", "aws-lambda"]
---

# Project Overview

## Introduction

Backlog Time Recorder is a small AWS Lambda service that keeps [Backlog](https://backlog.com/)
issue metadata in sync automatically. It listens for Backlog issue-update
webhooks and reacts to status and date changes on issues in the `faber-wi`
space, updating the issue back through the Backlog API.

## Background

### Problem Statement

Without this service, monthly milestones, actual hours, and a "Started at"
custom field on a Backlog issue all have to be maintained by hand whenever an
issue's dates or status change — easy to forget and inconsistent across
issues. Separately, nothing stops any user from moving a PBI issue through
status transitions reserved for the Product Owner, or creating one in the
wrong starting status.

### Goals and Objectives

- **Milestone accuracy**: keep the monthly milestones on an issue in sync with
  its start/due dates, without manual edits.
- **Actual hours capture**: automatically calculate and record actual hours
  when an issue is closed.
- **Started-at tracking**: automatically stamp a "Started at" custom field
  when work begins on an issue.
- **PBI status governance**: flag (via an issue comment, without reverting)
  PBI status changes or creations that violate the Product-Owner-only
  transition rules, once enabled for a project.

### Success Metrics

No metrics are currently tracked for this automation (e.g. number of
manual corrections avoided). TODO: define metrics if the team wants to
measure impact.

## Target Audience

### Primary Users

- **Backlog users in the `faber-wi` space**: see their issues' milestones,
  actual hours, and "Started at" field kept current without manual work.

### Stakeholders

- **Project managers / PMO**: rely on milestone and actual-hours data in
  Backlog for planning and reporting.

## Key Features

1. **Milestone auto-management**: recalculates and applies the monthly
   milestones an issue should belong to whenever its start/due date changes,
   creating missing milestones and removing ones no longer needed
   ([`MilestoneUpdateStrategy`](../../lambda/src/main/java/com/lambda/strategies/MilestoneUpdateStrategy.java)).
2. **Actual hours calculation**: on close, if no actual hours are recorded,
   computes them from the issue's creation date and its "Started at" field
   ([`ActualHoursUpdateStrategy`](../../lambda/src/main/java/com/lambda/strategies/ActualHoursUpdateStrategy.java)).
3. **Started-at stamping**: on Open/In Progress, stamps or updates the
   "Started at" custom field
   ([`StartedAtUpdateStrategy`](../../lambda/src/main/java/com/lambda/strategies/StartedAtUpdateStrategy.java)).
4. **PBI status validation** (opt-in per project): once `PRODUCT_OWNER_USER_IDS`,
   `SETTING_PRIORITY_STATUS_IDS`, and `ENABLED_PROJECT_KEYS` are configured,
   flags Open→Setting Priority/Closed transitions by non-Product-Owner users,
   invalid transitions out of Open, and PBIs created in a status other than
   Open — each as a Backlog issue comment, never a revert
   ([`RestrictedStatusTransitionPolicy`](../../lambda/src/main/java/com/lambda/models/RestrictedStatusTransitionPolicy.java),
   [`StatusChangeNotifier`](../../lambda/src/main/java/com/lambda/handlers/StatusChangeNotifier.java)).

## System Context

### Related Systems

- **Backlog (`faber-wi` space)**: source of the issue-update webhook events
  and the target of all API updates, via [backlog4j](https://github.com/nulab/backlog4j).

### Dependencies

- **Backlog API key** (`BACKLOG_API_KEY`): scoped to the `faber-wi` space;
  required both to deploy and to run.
- **AWS Lambda Function URL**: the public HTTPS endpoint Backlog's webhook
  posts to (no API Gateway in front of it).
- **PBI status validation env vars** (`PRODUCT_OWNER_USER_IDS`,
  `SETTING_PRIORITY_STATUS_IDS`, `ENABLED_PROJECT_KEYS`): all optional; the
  status-validation feature stays fully disabled until they're set. See
  [System Architecture — Security Architecture](../architecture/system-architecture.md#security-architecture).

## Technology Stack

| Layer          | Technology                                       |
| -------------- | ------------------------------------------------- |
| Language       | Java 17                                            |
| Framework      | AWS CDK 2.253.0 (Java)                             |
| Backlog client | backlog4j 2.6.0                                    |
| Infrastructure | AWS Lambda (Function URL, SnapStart), no database  |

There is no frontend or database — the service is a stateless backend
automation that reads/writes directly against the Backlog API on each
webhook call.

## Current Status

**Status**: Live — deployed automatically to AWS on every push to `master`
(see [Deployment Guide](../guides/deployment.md)).

**Version**: `0.1` (per [pom.xml](../../pom.xml))

**Last Updated**: 2026-09-07 (most recent commit at the time this doc was updated)

### Roadmap

No roadmap is currently documented for this project. TODO: add planned work
here (e.g. webhook signature verification, see
[System Architecture — Known Limitations](../architecture/system-architecture.md#known-limitations)).

## Team

No formal team roles (product owner / tech lead) are documented for this
repository. By commit history, the most active contributors are HoangHades
and Masayuki Sugahara.

## Resources

- [System Architecture](../architecture/system-architecture.md)
- [Deployment Guide](../guides/deployment.md)
- <!-- TODO: add links to the Backlog project/space and any Slack channel used for this service -->

## Getting Started

For developers joining this project:

1. Read the [README.md](../../README.md) in the root directory
2. Review the [System Architecture](../architecture/system-architecture.md)
3. Check [CONTRIBUTING.md](../../CONTRIBUTING.md) once it exists

## FAQ

### What happens if `BACKLOG_API_KEY` isn't set?

The Lambda throws a `RuntimeException("BACKLOG_API_KEY is not set")` the
first time it needs to call the Backlog API (see
[`BacklogTimeRecorder.getOrchestrator()`](../../lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java)).

### Which issue statuses actually trigger an update?

Open, In Progress, and Closed
([`BacklogTimeRecorder.isHandledStatus()`](../../lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java)).
Any other status change is ignored unless the issue's start/due date also
changed, in which case milestones are still recalculated.

### Does the PBI status check ever revert a change?

No. It only posts a Backlog issue comment describing the violation and
notifying a fixed reviewer user ID — a human has to review and fix the
issue manually
([`IssueUpdateOrchestrator.postViolationComment()`](../../lambda/src/main/java/com/lambda/handlers/IssueUpdateOrchestrator.java)).

### How do I enable the PBI status check for a project?

Set all three of `PRODUCT_OWNER_USER_IDS`, `SETTING_PRIORITY_STATUS_IDS`, and
`ENABLED_PROJECT_KEYS`, and add the project's key to
`ENABLED_PROJECT_KEYS`. It only ever applies to issues whose Backlog issue
type is named exactly `PBI`. See the root
[README's Configuration section](../../README.md#configuration).

## Glossary

| Term | Definition |
| ---- | ---------- |
| Webhook | The HTTP callback Backlog sends to the Lambda Function URL whenever a subscribed issue event occurs. |
| PBI | Product Backlog Item — the Backlog issue type name the status-validation feature targets. |
| Product Owner | A user listed in `PRODUCT_OWNER_USER_IDS`, authorized to move a PBI to Setting Priority or Closed. |
| Setting Priority | A per-project custom status; its numeric ID must be listed in `SETTING_PRIORITY_STATUS_IDS` (each Backlog project can assign it a different ID). |
| Actual hours | A Backlog issue field recording how many hours were actually spent on the issue. |
| Milestone | A Backlog project-level date range (here, generated monthly) that issues can be assigned to. |
| "Started at" custom field | A text custom field on Backlog issues that this service stamps when work begins. |
| Function URL | An AWS Lambda feature that exposes a function directly over HTTPS without needing API Gateway. |
