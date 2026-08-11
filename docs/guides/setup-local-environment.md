---
title: "Setup Local Development Environment"
created: 2026-08-11
author: "@HoangHades"
owner: "@HoangHades"
domain: ["Engineering-App", "Process-SDLC"]
doc_type: "How-to-Guide"
status: "active"
last-reviewed: 2026-08-11
review-cycle: "6-months"
tags: ["setup", "local-development", "onboarding", "aws-cdk"]
---

# Setup Local Development Environment

## Overview

This guide walks you through setting up a local environment for Backlog Time
Recorder, an AWS CDK (Java) project that deploys a single Lambda function.
There is no local "run the app" step in the traditional web-app sense: you
build and test locally, then deploy to AWS to exercise the webhook end to
end.

**Time to complete**: ~15 minutes

## Prerequisites

Before you begin, ensure you have:

- [ ] Git
- [ ] Java 17
- [ ] Maven
- [ ] Node.js and the AWS CDK CLI (`npm install -g aws-cdk`)
- [ ] AWS account credentials with permission to deploy Lambda functions (for the deploy step)
- [ ] A Backlog API key for the `faber-wi` space (for exercising real API calls)

**Optional**: this repo ships a [devcontainer](../../.devcontainer/devcontainer.json)
with Java 17, Maven, the AWS CDK CLI, and the AWS CLI preinstalled — open the
repo in it to skip manual tool installation.

## Steps

### Step 1: Clone the repository

```bash
git clone git@github.com:FaberTechnology/backlog-time-recorder.git
cd backlog-time-recorder
```

### Step 2: Build the Lambda module

```bash
cd lambda
mvn -B clean package
cd ..
```

This produces `lambda/target/backlog-time-recorder-0.1.jar`, the artifact the
CDK stack packages into the Lambda function
(`Code.fromAsset("lambda/target/backlog-time-recorder-0.1.jar")` in
[BacklogTimeRecorderStack.java](../../src/main/java/com/myorg/BacklogTimeRecorderStack.java)).

### Step 3: Set the Backlog API key

```bash
export BACKLOG_API_KEY=your-backlog-api-key
```

This is required both to deploy (the CDK stack injects it into the Lambda's
environment) and by the Lambda's runtime code (`IssueUpdateOrchestrator`) when
it actually calls the Backlog API.

### Step 4: Build the CDK app

```bash
mvn compile
```

### Step 5 (optional): Synthesize the CloudFormation template

```bash
cdk synth
```

Useful to sanity-check the stack without deploying anything.

## Verification

### Run the test suites

```bash
# CDK stack tests (root)
mvn test

# Lambda module tests
cd lambda && mvn test
```

All tests should pass. Notable Lambda tests include `InvokeTest` (exercises
`BacklogTimeRecorder.handleRequest` with the sample payloads in
[lambda/events/](../../lambda/events/)) and the strategy/helper-specific
tests under `lambda/src/test/java/com/lambda/`.

### Exercise the webhook handler locally

There is no local HTTP server for the Lambda. `InvokeTest` calls
`BacklogTimeRecorder.handleRequest` directly using the sample payloads in
`lambda/events/` (`issue.json`, `payload.json`) as stand-ins for a real
Backlog webhook call — read that test if you need to reproduce a webhook
scenario locally.

## Troubleshooting

### Issue: `BACKLOG_API_KEY is not set`

Thrown by `BacklogTimeRecorder.getUpdater()` when the Lambda runs without the
env var set. Set `BACKLOG_API_KEY` before running code paths that reach
`IssueUpdateOrchestrator`, or before deploying.

### Issue: CDK deploy can't find the Lambda jar

Make sure you ran `mvn -B clean package` inside `lambda/` first — the CDK
stack references the built jar at a fixed path
(`lambda/target/backlog-time-recorder-0.1.jar`) and does not build it for
you.

## Next Steps

- [ ] Review the [System Architecture](../architecture/system-architecture.md)
- [ ] Read the [Deployment Guide](deployment.md) to deploy your changes
- [ ] Check `CONTRIBUTING.md` once it exists — run `/faber-docs:sync --apply` to scaffold it

## FAQ

### Do I need a real Backlog API key to run the unit tests?

No — the existing tests use the sample JSON payloads in `lambda/events/` and
don't call the live Backlog API for most cases. `BACKLOG_API_KEY` is only
required to actually deploy or to run code paths that call
`IssueUpdateOrchestrator` against real Backlog.

### Can I run the Lambda locally as an HTTP server?

Not currently — there's no local HTTP harness (e.g. SAM Local) configured in
this repo. To test end to end, deploy to AWS (see the
[Deployment Guide](deployment.md)) and point a Backlog webhook at the
Function URL.
