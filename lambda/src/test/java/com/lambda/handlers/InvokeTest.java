package com.lambda.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.lambda.TestContext;

public class InvokeTest {

    private static final IssueUpdateOrchestrator NO_OP_ORCHESTRATOR = new IssueUpdateOrchestrator() {
        @Override
        public com.nulabinc.backlog4j.Issue updateIssue(final int issueId, final int newStatusCode) {
            return null;
        }
    };

    @ParameterizedTest
    @Event(value = "events/issue.json", type = APIGatewayV2HTTPEvent.class)
    void testApiGatewayV2(final APIGatewayV2HTTPEvent event) {
        final Context context = new TestContext();
        final BacklogTimeRecorder handler = new BacklogTimeRecorder(NO_OP_ORCHESTRATOR);
        final APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);
        assertEquals("No issue to update", response.getBody());
    }
}
