package com.lambda.handlers;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.lambda.WebhookPayload;
import com.lambda.helpers.MilestoneHelper;
import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.helpers.WorkScheduleHelper;
import com.lambda.models.Issue;
import com.lambda.strategies.ActualHoursUpdateStrategy;
import com.lambda.strategies.MilestoneUpdateStrategy;
import com.lambda.strategies.StartedAtUpdateStrategy;
import com.lambda.strategies.UpdateStrategy;
import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;
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

        BacklogConfigure configure = new BacklogJpConfigure("faber-wi").apiKey(apiKey);
        BacklogClient client = new BacklogClientFactory(configure).newClient();

        // Check for start date or due date changes
        boolean hasDateChange = issue.getChanges().stream()
                .anyMatch(change -> change.getField().equals("startDate") || change.getField().equals("limitDate"));

        if (hasDateChange) {
            try {
                MilestoneHelper milestoneHelper = new MilestoneHelper();
                IssueUpdateOrchestrator dateOrchestrator = new IssueUpdateOrchestrator(client,
                        List.of(new MilestoneUpdateStrategy(milestoneHelper)));
                dateOrchestrator.updateIssue(issue.getId());
            } catch (Exception e) {
                logger.log("Failed to update milestones for issue " + issue.getId() + ": " + e.getMessage(),
                        LogLevel.ERROR);
            }
        }

        int newStatus = issue.getChanges().stream()
                .filter(change -> change.getField().equals("status"))
                .findFirst()
                .map(change -> Integer.parseInt(change.getNewValue()))
                .orElse(0);
        if (newStatus == 0) {
            return returnText("Status did not change", 204);
        }

        StatusType statusType = StatusType.valueOf(newStatus);
        if (statusType != StatusType.Closed
                && statusType != StatusType.InProgress
                && statusType != StatusType.Open) {
            return returnText("Unhandled status change", 204);
        }

        WorkScheduleHelper workScheduleHelper = new WorkScheduleHelper();
        TimeTrackingHelper timeTrackingHelper = new TimeTrackingHelper(workScheduleHelper);
        MilestoneHelper milestoneHelper = new MilestoneHelper();

        List<UpdateStrategy> strategies = List.of(
                new ActualHoursUpdateStrategy(timeTrackingHelper),
                new StartedAtUpdateStrategy(timeTrackingHelper),
                new MilestoneUpdateStrategy(milestoneHelper)
        );

        IssueUpdateOrchestrator orchestrator = new IssueUpdateOrchestrator(client, strategies);
        com.nulabinc.backlog4j.Issue updatedIssue = orchestrator.updateIssue(issue.getId());

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
