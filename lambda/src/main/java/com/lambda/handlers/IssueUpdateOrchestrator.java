package com.lambda.handlers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.helpers.TimeTrackingHelper;
import com.lambda.helpers.WorkScheduleHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.lambda.strategies.ActualHoursUpdateStrategy;
import com.lambda.strategies.MilestoneUpdateStrategy;
import com.lambda.strategies.StartedAtUpdateStrategy;
import com.lambda.strategies.UpdateStrategy;
import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.BacklogClientFactory;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.AddIssueCommentParams;
import com.nulabinc.backlog4j.api.option.AddMilestoneParams;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;

public class IssueUpdateOrchestrator implements IssueUpdater, StatusChangeNotifier {

    private static final long STATUS_VIOLATION_NOTIFY_USER_ID = 399389L; // Nguyen Hoang - monitors PO-only status rule during rollout

    private final BacklogClient client;
    private final MilestoneHelper milestoneHelper;
    private final List<UpdateStrategy> strategies;

    public IssueUpdateOrchestrator(final String apiKey) {
        this.client = new BacklogClientFactory(new BacklogJpConfigure("faber-wi").apiKey(apiKey)).newClient();
        final WorkScheduleHelper workScheduleHelper = new WorkScheduleHelper();
        final TimeTrackingHelper timeTrackingHelper = new TimeTrackingHelper(workScheduleHelper);
        this.milestoneHelper = new MilestoneHelper();
        this.strategies = Arrays.asList(
                new MilestoneUpdateStrategy(milestoneHelper),
                new ActualHoursUpdateStrategy(timeTrackingHelper),
                new StartedAtUpdateStrategy(timeTrackingHelper));
    }

    @Override
    public Issue updateIssue(final int issueId, final int newStatusCode, final boolean hasDateChange) {
        final Issue rawIssue = client.getIssue(issueId);
        final IssueWrapper issueWrapper = new IssueWrapper(rawIssue, newStatusCode, hasDateChange);
        final long projectId = rawIssue.getProjectId();
        final ProjectContext projectContext = new ProjectContext(
                projectId,
                () -> client.getMilestones(projectId),
                name -> client.addMilestone(new AddMilestoneParams(projectId, name)
                        .startDate(milestoneHelper.monthStartDate(name).toString())
                        .releaseDueDate(milestoneHelper.monthEndDate(name).toString())));

        final UpdateIssueParams params = new UpdateIssueParams(issueId);
        boolean anyApplied = false;

        for (final UpdateStrategy strategy : strategies) {
            if (strategy.canApply(issueWrapper, projectContext)) {
                strategy.apply(issueWrapper, projectContext, params);
                anyApplied = true;
            }
        }

        if (!anyApplied) {
            return null;
        }

        return client.updateIssue(params);
    }

    @Override
    public void notifyUnauthorizedStatusChange(final int issueId, final int oldStatusCode, final int newStatusCode,
            final long actorUserId) {
        postViolationComment(issueId, String.format(
                "Status changed from %s to %s by user #%d without Product Owner permission. "
                        + "This change was not reverted automatically — please review.",
                describeStatus(oldStatusCode), describeStatus(newStatusCode), actorUserId));
    }

    @Override
    public void notifyInvalidStatusTransition(final int issueId, final int oldStatusCode, final int newStatusCode,
            final long actorUserId) {
        postViolationComment(issueId, String.format(
                "Status changed from %s to %s by user #%d, but Open may only move to Setting Priority or Closed. "
                        + "This change was not reverted automatically — please review.",
                describeStatus(oldStatusCode), describeStatus(newStatusCode), actorUserId));
    }

    @Override
    public void notifyInvalidCreationStatus(final int issueId, final int statusCode, final long actorUserId) {
        postViolationComment(issueId, String.format(
                "PBI created with status %s by user #%d, but PBIs must be created with status Open. "
                        + "This was not reverted automatically — please review.",
                describeStatus(statusCode), actorUserId));
    }

    private void postViolationComment(final int issueId, final String content) {
        client.addIssueComment(new AddIssueCommentParams(issueId, content)
                .notifiedUserIds(Collections.singletonList(STATUS_VIOLATION_NOTIFY_USER_ID)));
    }

    private static String describeStatus(final int statusCode) {
        final StatusType statusType = StatusType.valueOf(statusCode);
        return statusType == StatusType.Custom ? "status #" + statusCode : statusType.name();
    }
}
