package com.lambda.strategies;

import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

import java.util.Optional;

public class StartedAtUpdateStrategy implements UpdateStrategy {

    private final TimeTrackingHelper timeTrackingHelper;

    public StartedAtUpdateStrategy(TimeTrackingHelper timeTrackingHelper) {
        this.timeTrackingHelper = timeTrackingHelper;
    }

    @Override
    public boolean canApply(IssueWrapper issueWrapper, ProjectContext projectContext) {
        StatusType status = issueWrapper.getStatus();
        return status == StatusType.InProgress || status == StatusType.Open;
    }

    @Override
    public void apply(IssueWrapper issueWrapper, ProjectContext projectContext, UpdateIssueParams params) {
        Optional<Long> fieldId = issueWrapper.getStartedAtFieldId();
        if (fieldId.isEmpty()) {
            return;
        }

        String existingValue = issueWrapper.getStartedAtValue().orElse(null);
        String newValue = timeTrackingHelper.appendTimestamp(existingValue);
        params.textCustomField(fieldId.get(), newValue);
    }

    @Override
    public int getPriority() {
        return 20;
    }
}
