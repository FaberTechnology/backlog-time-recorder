package com.lambda.handlers;

import java.util.Arrays;
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
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.conf.BacklogJpConfigure;

public class IssueUpdateOrchestrator {

    private final BacklogClient client;
    private final List<UpdateStrategy> strategies;

    protected IssueUpdateOrchestrator() {
        this.client = null;
        this.strategies = null;
    }

    public IssueUpdateOrchestrator(final String apiKey) {
        this.client = new BacklogClientFactory(new BacklogJpConfigure("faber-wi").apiKey(apiKey)).newClient();
        final WorkScheduleHelper workScheduleHelper = new WorkScheduleHelper();
        final TimeTrackingHelper timeTrackingHelper = new TimeTrackingHelper(workScheduleHelper);
        final MilestoneHelper milestoneHelper = new MilestoneHelper();
        this.strategies = Arrays.asList(
                new MilestoneUpdateStrategy(milestoneHelper),
                new ActualHoursUpdateStrategy(timeTrackingHelper),
                new StartedAtUpdateStrategy(timeTrackingHelper));
    }

    public Issue updateIssue(final int issueId, final int newStatusCode) {
        final Issue rawIssue = client.getIssue(issueId);
        final IssueWrapper issueWrapper = new IssueWrapper(rawIssue, newStatusCode);
        final ProjectContext projectContext = new ProjectContext(
                rawIssue.getProjectId(), client.getMilestones(rawIssue.getProjectId()));

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
}
