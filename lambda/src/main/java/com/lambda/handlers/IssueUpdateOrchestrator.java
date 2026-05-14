package com.lambda.handlers;

import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.lambda.strategies.UpdateStrategy;
import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

import java.util.Comparator;
import java.util.List;

public class IssueUpdateOrchestrator {

    private final BacklogClient client;
    private final List<UpdateStrategy> strategies;

    public IssueUpdateOrchestrator(BacklogClient client, List<UpdateStrategy> strategies) {
        this.client = client;
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(UpdateStrategy::getPriority))
                .toList();
    }

    public Issue updateIssue(int issueId) {
        Issue rawIssue = client.getIssue(issueId);
        IssueWrapper wrapper = new IssueWrapper(rawIssue);

        List<Milestone> projectMilestones = client.getMilestones(wrapper.getProjectId());
        ProjectContext context = new ProjectContext(wrapper.getProjectId(), projectMilestones);

        UpdateIssueParams params = new UpdateIssueParams(rawIssue.getId());

        boolean anyApplied = false;
        for (UpdateStrategy strategy : strategies) {
            if (strategy.canApply(wrapper, context)) {
                strategy.apply(wrapper, context, params);
                anyApplied = true;
            }
        }

        if (!anyApplied) {
            return null;
        }

        return client.updateIssue(params);
    }

    private boolean hasUpdates(UpdateIssueParams params) {
        return params != null;
    }
}
