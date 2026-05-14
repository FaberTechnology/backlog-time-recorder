package com.lambda.strategies;

import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

import java.time.Duration;
import java.util.Optional;

public class ActualHoursUpdateStrategy implements UpdateStrategy {

    private final TimeTrackingHelper timeTrackingHelper;

    public ActualHoursUpdateStrategy(TimeTrackingHelper timeTrackingHelper) {
        this.timeTrackingHelper = timeTrackingHelper;
    }

    @Override
    public boolean canApply(IssueWrapper issueWrapper, ProjectContext projectContext) {
        return issueWrapper.getStatus() == StatusType.Closed;
    }

    @Override
    public void apply(IssueWrapper issueWrapper, ProjectContext projectContext, UpdateIssueParams params) {
        Optional<String> startedAtValue = issueWrapper.getStartedAtValue();

        Duration elapsed;
        if (startedAtValue.isPresent()) {
            elapsed = timeTrackingHelper.calculateActualHours(startedAtValue.get());
        } else {
            elapsed = Duration.ZERO;
        }

        if (elapsed == Duration.ZERO) {
            elapsed = timeTrackingHelper.calculateFallbackHours(issueWrapper.getCreated());
        }

        Float actualHours = timeTrackingHelper.formatActualHours(elapsed);
        if (actualHours != null) {
            params.actualHours(actualHours);
        }
    }

    @Override
    public int getPriority() {
        return 10;
    }
}
