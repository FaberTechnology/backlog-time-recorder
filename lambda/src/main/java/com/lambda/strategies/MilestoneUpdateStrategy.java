package com.lambda.strategies;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

public class MilestoneUpdateStrategy implements UpdateStrategy {

    private final MilestoneHelper milestoneHelper;
    private List<Long> toAdd;

    public MilestoneUpdateStrategy(final MilestoneHelper milestoneHelper) {
        this.milestoneHelper = milestoneHelper;
    }

    @Override
    public boolean canApply(final IssueWrapper issueWrapper, final ProjectContext projectContext) {
        if (!issueWrapper.isDateChanged()) {
            return false;
        }
        if (!issueWrapper.getStartDate().isPresent() || !issueWrapper.getDueDate().isPresent()) {
            return false;
        }
        final List<Long> required = milestoneHelper.calculateRequiredMilestones(
                issueWrapper.getStartDate().get(), issueWrapper.getDueDate().get(),
                projectContext.milestones());
        final List<Long> current = issueWrapper.getIssueKeyMilestones();
        toAdd = required.stream().filter(id -> !current.contains(id)).collect(Collectors.toList());
        return !toAdd.isEmpty();
    }

    @Override
    public void apply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final UpdateIssueParams params) {
        final List<Long> allIds = new ArrayList<>(issueWrapper.getIssueKeyMilestones());
        allIds.addAll(toAdd);
        params.milestoneIds(allIds);
    }
}
