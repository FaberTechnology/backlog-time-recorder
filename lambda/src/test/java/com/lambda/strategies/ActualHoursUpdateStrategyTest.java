package com.lambda.strategies;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.helpers.WorkScheduleHelper;
import com.lambda.models.IssueWrapper;
import com.nulabinc.backlog4j.Issue.StatusType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActualHoursUpdateStrategyTest {

    private final ActualHoursUpdateStrategy strategy =
            new ActualHoursUpdateStrategy(new TimeTrackingHelper(new WorkScheduleHelper()));

    @Test
    public void canApply_closedStatus_andActualHoursNotSet_returnsTrue() {
        final IssueWrapper wrapper = new StubIssueWrapper(
                StatusType.Closed.getIntValue(), Optional.empty());

        assertTrue(strategy.canApply(wrapper, null));
    }

    @Test
    public void canApply_closedStatus_butActualHoursAlreadySet_returnsFalse() {
        final IssueWrapper wrapper = new StubIssueWrapper(
                StatusType.Closed.getIntValue(), Optional.of(3.5f));

        assertFalse(strategy.canApply(wrapper, null));
    }

    @Test
    public void canApply_closedStatus_actualHoursIsZero_returnsFalse() {
        final IssueWrapper wrapper = new StubIssueWrapper(
                StatusType.Closed.getIntValue(), Optional.of(0f));

        assertFalse(strategy.canApply(wrapper, null));
    }

    @Test
    public void canApply_nonClosedStatus_returnsFalse() {
        final IssueWrapper wrapper = new StubIssueWrapper(
                StatusType.InProgress.getIntValue(), Optional.empty());

        assertFalse(strategy.canApply(wrapper, null));
    }

    private static class StubIssueWrapper extends IssueWrapper {
        private final int statusCode;
        private final Optional<Float> actualHours;

        StubIssueWrapper(final int statusCode, final Optional<Float> actualHours) {
            super(null, statusCode, false);
            this.statusCode = statusCode;
            this.actualHours = actualHours;
        }

        @Override
        public int getNewStatusCode() {
            return statusCode;
        }

        @Override
        public Optional<Float> getActualHours() {
            return actualHours;
        }
    }
}
