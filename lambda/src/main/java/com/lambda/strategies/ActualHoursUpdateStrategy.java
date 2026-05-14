package com.lambda.strategies;

import java.util.Optional;

import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

public class ActualHoursUpdateStrategy implements UpdateStrategy {

    private static final String CUSTOM_FIELD_STARTED_AT = "Started at";

    private final TimeTrackingHelper timeTrackingHelper;

    public ActualHoursUpdateStrategy(final TimeTrackingHelper timeTrackingHelper) {
        this.timeTrackingHelper = timeTrackingHelper;
    }

    @Override
    public boolean canApply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final int newStatusCode) {
        return newStatusCode == StatusType.Closed.getIntValue();
    }

    @Override
    public void apply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final UpdateIssueParams params) {
        final Optional<CustomField> startedAtField = issueWrapper.getCustomField(CUSTOM_FIELD_STARTED_AT);
        String startedAtValue = null;
        if (startedAtField.isPresent() && startedAtField.get() instanceof TextCustomField) {
            startedAtValue = ((TextCustomField) startedAtField.get()).getValue();
        }

        final float actualHours = timeTrackingHelper.calculateActualHours(
                issueWrapper.getCreatedAt(), startedAtValue);

        if (actualHours > 999 || actualHours < 0) {
            return;
        }

        params.actualHours(actualHours);
    }
}
