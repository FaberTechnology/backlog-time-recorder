package com.lambda.strategies;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

public class MilestoneUpdateStrategy implements UpdateStrategy {

    private final MilestoneHelper milestoneHelper;

    public MilestoneUpdateStrategy(final MilestoneHelper milestoneHelper) {
        this.milestoneHelper = milestoneHelper;
    }

    @Override
    public boolean canApply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final int newStatusCode) {
        if (!issueWrapper.getStartDate().isPresent() || !issueWrapper.getDueDate().isPresent()) {
            return false;
        }
        final LocalDate start = issueWrapper.getStartDate().get();
        final LocalDate due = issueWrapper.getDueDate().get();
        final List<Long> required = milestoneHelper.calculateRequiredMilestones(
                start, due, projectContext.milestones());
        final List<Long> current = issueWrapper.getIssueKeyMilestones();
        return required.stream().anyMatch(id -> !current.contains(id));
    }

    @Override
    public void apply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final UpdateIssueParams params) {
        final LocalDate start = issueWrapper.getStartDate().get();
        final LocalDate due = issueWrapper.getDueDate().get();
        final List<Long> required = milestoneHelper.calculateRequiredMilestones(
                start, due, projectContext.milestones());
        final List<Long> current = issueWrapper.getIssueKeyMilestones();
        final List<Long> toAdd = required.stream()
                .filter(id -> !current.contains(id))
                .collect(Collectors.toList());

        final List<Long> allIds = new ArrayList<>(current);
        allIds.addAll(toAdd);
        params.milestoneIds(allIds);
    }
}
