package com.lambda.handlers;

import java.util.Base64;
import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.lambda.WebhookPayload;
import com.lambda.models.Issue;
import com.lambda.models.RestrictedStatusTransitionPolicy;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.internal.json.Jackson;

public class BacklogTimeRecorder implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private IssueUpdater updater;
    private StatusChangeNotifier notifier;
    private RestrictedStatusTransitionPolicy statusTransitionPolicy;
    private IssueUpdateOrchestrator orchestrator;

    BacklogTimeRecorder(final IssueUpdater updater) {
        this(updater, (issueId, oldStatusCode, newStatusCode, actorUserId) -> { });
    }

    BacklogTimeRecorder(final IssueUpdater updater, final StatusChangeNotifier notifier) {
        this(updater, notifier, null);
    }

    BacklogTimeRecorder(final IssueUpdater updater, final StatusChangeNotifier notifier,
            final RestrictedStatusTransitionPolicy statusTransitionPolicy) {
        this.updater = updater;
        this.notifier = notifier;
        this.statusTransitionPolicy = statusTransitionPolicy;
    }

    public BacklogTimeRecorder() {
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(final APIGatewayV2HTTPEvent event, final Context context) {
        final LambdaLogger logger = context.getLogger();
        String body = event.getBody();
        if (event.getIsBase64Encoded()) {
            final byte[] decodedBytes = Base64.getDecoder().decode(body);
            body = new String(decodedBytes);
        }
        logger.log(body, LogLevel.DEBUG);

        final WebhookPayload payload = Jackson.fromJsonString(body, WebhookPayload.class);
        final Issue issue = payload.getContent();

        if (issue == null) {
            return returnText("Issue is null", 204);
        }

        final boolean hasDateChange = issue.getChanges().stream()
                .anyMatch(change -> change.getField().equals("startDate") || change.getField().equals("limitDate"));

        final int oldStatus = issue.getChanges().stream()
                .filter(change -> change.getField().equals("status"))
                .findFirst()
                .map(change -> change.getOldValue())
                .filter(value -> value != null)
                .map(Integer::parseInt)
                .orElse(0);

        final int newStatus = issue.getChanges().stream()
                .filter(change -> change.getField().equals("status"))
                .findFirst()
                .map(change -> Integer.parseInt(change.getNewValue()))
                .orElse(0);

        final String issueTypeName = issue.getIssueType() != null ? issue.getIssueType().getName() : null;

        if (newStatus != 0 && getStatusTransitionPolicy().isPbiIssueType(issueTypeName)
                && getStatusTransitionPolicy().isRestrictedTransition(oldStatus, newStatus)) {
            final long actorUserId = payload.getCreatedUser() != null ? payload.getCreatedUser().getId() : 0;
            if (!getStatusTransitionPolicy().isAuthorized(actorUserId)) {
                getNotifier().notifyUnauthorizedStatusChange(issue.getId(), oldStatus, newStatus, actorUserId);
            }
        }

        if (newStatus != 0 && isHandledStatus(newStatus)) {
            final com.nulabinc.backlog4j.Issue updatedIssue = getUpdater().updateIssue(issue.getId(), newStatus, hasDateChange);
            if (updatedIssue == null) {
                return returnText("No issue to update", 200);
            }
            return returnText(issue.getSummary(), 202);
        }

        if (hasDateChange) {
            try {
                getUpdater().updateIssue(issue.getId(), 0, true);
            } catch (Exception e) {
                logger.log("Failed to update milestones for issue " + issue.getId() + ": " + e.getMessage(),
                        LogLevel.ERROR);
            }
        }
        return returnText(newStatus == 0 ? "Status did not change" : "Unhandled status change", 204);
    }

    private boolean isHandledStatus(final int statusCode) {
        final StatusType statusType = StatusType.valueOf(statusCode);
        return statusType == StatusType.Closed || statusType == StatusType.InProgress || statusType == StatusType.Open;
    }

    private IssueUpdater getUpdater() {
        if (updater == null) {
            updater = getOrchestrator();
        }
        return updater;
    }

    private StatusChangeNotifier getNotifier() {
        if (notifier == null) {
            notifier = getOrchestrator();
        }
        return notifier;
    }

    private IssueUpdateOrchestrator getOrchestrator() {
        if (orchestrator == null) {
            final String apiKey = System.getenv("BACKLOG_API_KEY");
            if (apiKey == null) {
                throw new RuntimeException("BACKLOG_API_KEY is not set");
            }
            orchestrator = new IssueUpdateOrchestrator(apiKey);
        }
        return orchestrator;
    }

    private RestrictedStatusTransitionPolicy getStatusTransitionPolicy() {
        if (statusTransitionPolicy == null) {
            statusTransitionPolicy = RestrictedStatusTransitionPolicy.fromEnv();
        }
        return statusTransitionPolicy;
    }

    private APIGatewayV2HTTPResponse returnText(final String text, final int status) {
        final HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        final APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();
        response.setBody(text);
        response.setStatusCode(status);
        response.setHeaders(headers);
        response.setIsBase64Encoded(false);
        return response;
    }
}
