package com.lambda.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;
import com.lambda.TestContext;

public class InvokeTest {

    private static final IssueUpdater NO_OP_UPDATER = (issueId, newStatusCode) -> null;

    @ParameterizedTest
    @Event(value = "events/issue.json", type = APIGatewayV2HTTPEvent.class)
    void testApiGatewayV2(final APIGatewayV2HTTPEvent event) {
        final Context context = new TestContext();
        final BacklogTimeRecorder handler = new BacklogTimeRecorder(NO_OP_UPDATER);
        final APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);
        assertEquals("Unhandled status change", response.getBody());
    }
}
