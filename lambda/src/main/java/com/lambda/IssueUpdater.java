package com.lambda;

import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.CustomField;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogConfigure;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;
import com.nulabinc.backlog4j.internal.json.customFields.TextCustomField;

import com.lambda.utils.IssueUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class IssueUpdater {
    private final BacklogClient client;
    private final TimeCalculator timeCalculator;
    private final MilestoneCalculator milestoneCalculator;

    public enum UpdateField {
        MILESTONES,
        ACTUAL_HOURS,
        STARTED_AT
    }

    public IssueUpdater(final String apiKey) {
        BacklogConfigure configure = new BacklogJpConfigure(AppConstants.BACKLOG_SPACE).apiKey(apiKey);
        this.client = new BacklogClientFactory(configure).newClient();
        this.timeCalculator = new TimeCalculator();
        this.milestoneCalculator = new MilestoneCalculator(client, timeCalculator);
    }

    /**
     * Update issue with specified fields in a single API call.
     * This reduces number of comments created on issue.
     * 
     * @param issueId the issue ID
     * @param fieldsToUpdate set of fields to update
     * @return the updated issue, or null if no updates were made
     */
    public Issue updateIssueFields(final int issueId, final EnumSet<UpdateField> fieldsToUpdate) {
        final Issue issue = client.getIssue(issueId);
        final TimeCalculator.TimeUpdateData timeData = timeCalculator.calculateTimeUpdates(issue, fieldsToUpdate);
        
        // Check for time updates
        boolean hasUpdates = timeData.hasTimeUpdates();
        
        // Check for milestone updates
        List<Long> milestoneIds = null;
        if (fieldsToUpdate.contains(UpdateField.MILESTONES)) {
            milestoneIds = milestoneCalculator.calculateMilestoneIdsToSet(issue);
            if (milestoneIds != null) {
                hasUpdates = true;
            }
        }
        
        if (!hasUpdates) {
            return null;
        }

        final UpdateIssueParams params = new UpdateIssueParams(issue.getId());
        
        if (milestoneIds != null) {
            params.milestoneIds(milestoneIds);
        }
        
        if (timeData.getActualHours() != null) {
            params.actualHours(timeData.getActualHours());
        }
        
        if (timeData.getStartedAt() != null) {
            final Optional<CustomField> field = IssueUtils.customField(issue, AppConstants.CUSTOM_FIELD_STARTED_AT);
            if (field.isPresent()) {
                params.textCustomField(field.get().getId(), timeData.getStartedAt());
            }
        }
        
        return client.updateIssue(params);
    }
}
