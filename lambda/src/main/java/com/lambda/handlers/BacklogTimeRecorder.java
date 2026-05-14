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
import com.nulabinc.backlog4j.internal.json.Jackson;

public class BacklogTimeRecorder implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private IssueUpdateOrchestrator orchestrator;

    BacklogTimeRecorder(final IssueUpdateOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
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

        final int newStatus = issue.getChanges().stream()
                .filter(change -> change.getField().equals("status"))
                .findFirst()
                .map(change -> Integer.parseInt(change.getNewValue()))
                .orElse(0);

        final com.nulabinc.backlog4j.Issue updatedIssue = getOrchestrator().updateIssue(issue.getId(), newStatus);

        if (updatedIssue == null) {
            return returnText("No issue to update", 200);
        }

        return returnText(issue.getSummary(), 202);
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
