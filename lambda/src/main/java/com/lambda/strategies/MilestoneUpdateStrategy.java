package com.lambda.strategies;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

public class MilestoneUpdateStrategy implements UpdateStrategy {

    private final MilestoneHelper milestoneHelper;
    private List<Long> nextMilestoneIds;

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

        final LocalDate start = issueWrapper.getStartDate().get();
        final LocalDate due = issueWrapper.getDueDate().get();
        if (start.isAfter(due)) {
            return false;
        }

        final Set<String> requiredNames = milestoneHelper.calculateRequiredMilestoneNames(start, due);

        final List<Milestone> current = issueWrapper.getIssueMilestones();
        final List<Milestone> kept = new ArrayList<>();
        final Set<String> keptMonthlyNames = new LinkedHashSet<>();
        boolean removedAny = false;
        for (final Milestone m : current) {
            if (!milestoneHelper.isMonthlyMilestoneName(m.getName())) {
                kept.add(m);
                continue;
            }
            if (requiredNames.contains(m.getName())) {
                kept.add(m);
                keptMonthlyNames.add(m.getName());
            } else {
                removedAny = true;
            }
        }

        final List<Milestone> toAdd = new ArrayList<>();
        for (final String name : requiredNames) {
            if (!keptMonthlyNames.contains(name)) {
                toAdd.add(projectContext.getOrCreateMilestone(name));
            }
        }

        if (toAdd.isEmpty() && !removedAny) {
            return false;
        }

        final List<Long> next = new ArrayList<>();
        for (final Milestone m : kept) {
            next.add(m.getId());
        }
        for (final Milestone m : toAdd) {
            next.add(m.getId());
        }
        nextMilestoneIds = next;
        return true;
    }

    @Override
    public void apply(final IssueWrapper issueWrapper, final ProjectContext projectContext,
            final UpdateIssueParams params) {
        params.milestoneIds(nextMilestoneIds);
    }
}
