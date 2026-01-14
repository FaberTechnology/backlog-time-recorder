package com.lambda;

import java.util.Base64;
import java.util.HashMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.lambda.models.Issue;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.internal.json.Jackson;

public class BacklogTimeRecorder implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        final LambdaLogger logger = context.getLogger();
        String body = event.getBody();
        if (event.getIsBase64Encoded()) {
            byte[] decodedBytes = Base64.getDecoder().decode(body);
            body = new String(decodedBytes);
        }
        logger.log(body, LogLevel.DEBUG);

        final WebhookPayload payload = Jackson.fromJsonString(body, WebhookPayload.class);
        final Issue issue = payload.content;

        if (issue == null) {
            return returnText("Issue is null", 204);
        }

        final String apiKey = System.getenv("BACKLOG_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("BACKLOG_API_KEY is not set");
        }
        final IssueUpdater updater = new IssueUpdater(apiKey);

        // Check for start date or due date changes
        boolean hasDateChange = issue.getChanges().stream()
            .anyMatch(change -> change.getField().equals("startDate") || change.getField().equals("limitDate"));

        int newStatus = issue.getChanges().stream()
            .filter(change -> change.getField().equals("status"))
            .findFirst()
            .map(change -> Integer.parseInt(change.getNewValue()))
            .orElse(0);
        
        if (!hasDateChange && newStatus == 0) {
            return returnText("No relevant changes", 204);
        }

        // Determine what updates to perform
        boolean shouldUpdateMilestones = hasDateChange;
        boolean shouldSetActualHours = false;
        boolean shouldSetStartedAt = false;
        
        if (newStatus != 0) {
            switch (StatusType.valueOf(newStatus)) {
                case Closed:
                    shouldSetActualHours = true;
                    break;
                case InProgress, Open:
                    shouldSetStartedAt = true;
                    break;
                default:
                    return returnText("Unhandled status change", 204);
            }
        }
        
        // Perform all updates in a single API call
        com.nulabinc.backlog4j.Issue updatedIssue = updater.updateIssueFields(
            issue.getId(), 
            shouldUpdateMilestones, 
            shouldSetActualHours, 
            shouldSetStartedAt
        );

        if (updatedIssue == null) {
            return returnText("No issue to update", 200);
        }

        return returnText(issue.getSummary(), 202);
    }

    private APIGatewayV2HTTPResponse returnText(String text, int status) {
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
