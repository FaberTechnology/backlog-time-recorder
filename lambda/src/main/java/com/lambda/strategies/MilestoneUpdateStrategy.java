package com.lambda.strategies;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.Issue.StatusType;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

import java.util.List;

public class MilestoneUpdateStrategy implements UpdateStrategy {

    private final MilestoneHelper milestoneHelper;

    public MilestoneUpdateStrategy(MilestoneHelper milestoneHelper) {
        this.milestoneHelper = milestoneHelper;
    }

    @Override
    public boolean canApply(IssueWrapper issueWrapper, ProjectContext projectContext) {
        StatusType status = issueWrapper.getStatus();
        return (status == StatusType.InProgress || status == StatusType.Open)
                && issueWrapper.getStartDate() != null
                && issueWrapper.getDueDate() != null;
    }

    @Override
    public void apply(IssueWrapper issueWrapper, ProjectContext projectContext, UpdateIssueParams params) {
        List<Long> milestoneIds = milestoneHelper.calculateRequiredMilestones(
                issueWrapper.getStartDate(),
                issueWrapper.getDueDate(),
                projectContext.milestones());

        if (!milestoneIds.isEmpty()) {
            params.milestoneIds(milestoneIds);
        }
    }

    @Override
    public int getPriority() {
        return 30;
    }
}
