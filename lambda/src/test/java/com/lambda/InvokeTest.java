package com.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.tests.annotations.Event;

public class InvokeTest {
    @ParameterizedTest
    @Event(value = "events/issue.json", type = APIGatewayV2HTTPEvent.class)
    void testApiGatewayV2(final APIGatewayV2HTTPEvent event) {
        final Context context = new TestContext();
        final BacklogTimeRecorder handler = new BacklogTimeRecorder();
        final APIGatewayV2HTTPResponse response = handler.handleRequest(event, context);
        final String expected = "Unhandled status change";
        assertEquals(expected, response.getBody());
    }
}
