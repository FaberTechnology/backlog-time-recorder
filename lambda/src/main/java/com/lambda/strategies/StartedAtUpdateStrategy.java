package com.lambda.strategies;

import java.util.Optional;

import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

public class StartedAtUpdateStrategy implements UpdateStrategy {

    private static final String CUSTOM_FIELD_STARTED_AT = "Started at";

    private final TimeTrackingHelper timeTrackingHelper;

    public StartedAtUpdateStrategy(final TimeTrackingHelper timeTrackingHelper) {
        this.timeTrackingHelper = timeTrackingHelper;
    }

    @Override
    public boolean canApply(final IssueWrapper issueWrapper, final ProjectContext projectContext) {
        final int status = issueWrapper.getNewStatusCode();
        if (status != StatusType.InProgress.getIntValue() && status != StatusType.Open.getIntValue()) {
            return false;
        }
        return issueWrapper.getCustomField(CUSTOM_FIELD_STARTED_AT).isPresent();
    }

    @Override
    public void apply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final UpdateIssueParams params) {
        final Optional<CustomField> field = issueWrapper.getCustomField(CUSTOM_FIELD_STARTED_AT);
        if (!field.isPresent()) return;

        String existingValue = null;
        if (field.get() instanceof TextCustomField) {
            existingValue = ((TextCustomField) field.get()).getValue();
        }

        final String newValue = timeTrackingHelper.formatStartedAt(existingValue);
        params.textCustomField(field.get().getId(), newValue);
    }
}
