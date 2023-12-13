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

        WebhookPayload payload = Jackson.fromJsonString(body, WebhookPayload.class);
        Issue issue = payload.content;

        if (issue == null) {
            return returnText("Issue is null", 204);
        }

        if (issue.getStatus().getStatusType() != StatusType.Closed) {
            return returnText("Issue is not closed", 204);
        }

        for (var change : issue.getChanges()) {
            if (!change.getField().equals("status")
                    || Integer.parseInt(change.getNewValue()) != StatusType.Closed.getIntValue())
                continue;

            com.nulabinc.backlog4j.Issue updatedIssue = updateIssue(issue);
            logger.log(updatedIssue.getIssueKey());
            if (updatedIssue != null && updatedIssue.getActualHours() != null) {
                return returnText("Updated", 200);
            } else {
                return returnText("Failed to update", 500);
            }
        }

        return returnText(issue.getSummary(), 202);
    }

    private com.nulabinc.backlog4j.Issue updateIssue(Issue issue) {
        final String apiKey = System.getenv("BACKLOG_API_KEY");
        if (apiKey == null) {
            throw new RuntimeException("BACKLOG_API_KEY is not set");
        }
        final IssueUpdater updater = new IssueUpdater(apiKey);

        return updater.setRealHours(issue.getId());
    }

    private APIGatewayV2HTTPResponse returnText(String text, int status) {
        final HashMap<String, String> headers = new HashMap<>() {
            {
                put("Content-Type", "text/plain");
            }
        };
        final APIGatewayV2HTTPResponse response = new APIGatewayV2HTTPResponse();

        response.setBody(text);
        response.setStatusCode(status);
        response.setHeaders(headers);
        response.setIsBase64Encoded(false);
        return response;
    }
}