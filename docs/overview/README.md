---
title: "Backlog Time Recorder Overview"
created: 2026-08-11
author: "@HoangHades"
owner: "@HoangHades"
domain: ["Engineering-App"]
doc_type: "Overview-Handbook"
status: "active"
last-reviewed: 2026-08-11
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
issues.

### Goals and Objectives

- **Milestone accuracy**: keep the monthly milestones on an issue in sync with
  its start/due dates, without manual edits.
- **Actual hours capture**: automatically calculate and record actual hours
  when an issue is closed.
- **Started-at tracking**: automatically stamp a "Started at" custom field
  when work begins on an issue.

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

## System Context

### Related Systems

- **Backlog (`faber-wi` space)**: source of the issue-update webhook events
  and the target of all API updates, via [backlog4j](https://github.com/nulab/backlog4j).

### Dependencies

- **Backlog API key** (`BACKLOG_API_KEY`): scoped to the `faber-wi` space;
  required both to deploy and to run.
- **AWS Lambda Function URL**: the public HTTPS endpoint Backlog's webhook
  posts to (no API Gateway in front of it).

## Technology Stack

| Layer          | Technology                                       |
| -------------- | ------------------------------------------------- |
| Language       | Java 17                                            |
| Framework      | AWS CDK 2.114.0 (Java)                             |
| Backlog client | backlog4j 2.6.0                                    |
| Infrastructure | AWS Lambda (Function URL, SnapStart), no database  |

There is no frontend or database — the service is a stateless backend
automation that reads/writes directly against the Backlog API on each
webhook call.

## Current Status

**Status**: Live — deployed automatically to AWS on every push to `master`
(see [Deployment Guide](../guides/deployment.md)).

**Version**: `0.1` (per [pom.xml](../../pom.xml))

**Last Updated**: 2026-07-24 (most recent commit at the time this doc was written)

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
[`BacklogTimeRecorder.getUpdater()`](../../lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java)).

### Which issue statuses actually trigger an update?

Open, In Progress, and Closed
([`BacklogTimeRecorder.isHandledStatus()`](../../lambda/src/main/java/com/lambda/handlers/BacklogTimeRecorder.java)).
Any other status change is ignored unless the issue's start/due date also
changed, in which case milestones are still recalculated.

## Glossary

| Term | Definition |
| ---- | ---------- |
| Webhook | The HTTP callback Backlog sends to the Lambda Function URL whenever a subscribed issue event occurs. |
| Actual hours | A Backlog issue field recording how many hours were actually spent on the issue. |
| Milestone | A Backlog project-level date range (here, generated monthly) that issues can be assigned to. |
| "Started at" custom field | A text custom field on Backlog issues that this service stamps when work begins. |
| Function URL | An AWS Lambda feature that exposes a function directly over HTTPS without needing API Gateway. |
