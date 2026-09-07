package com.lambda.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

public class IssueUpdateOrchestratorDescribeStatusTest {

    private static final int STATUS_OPEN = 1;
    private static final int CUSTOM_STATUS_ID = 40366;

    @Test
    public void describeStatus_standardStatus_returnsStatusName() {
        assertEquals("Open", IssueUpdateOrchestrator.describeStatus(STATUS_OPEN));
    }

    @Test
    public void describeStatus_customStatus_omitsId() {
        final String description = IssueUpdateOrchestrator.describeStatus(CUSTOM_STATUS_ID);

        assertEquals("a custom status", description);
        assertFalse(description.contains(String.valueOf(CUSTOM_STATUS_ID)));
    }
}
