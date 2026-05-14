package com.lambda.helpers;

import com.nulabinc.backlog4j.Milestone;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MilestoneHelper {

    public List<Long> calculateRequiredMilestones(
            Date issueStart,
            Date issueDue,
            List<Milestone> projectMilestones) {

        if (issueStart == null || issueDue == null || projectMilestones == null) {
            return List.of();
        }

        List<Long> result = new ArrayList<>();
        for (Milestone milestone : projectMilestones) {
            if (overlaps(issueStart, issueDue, milestone)) {
                result.add(milestone.getId());
            }
        }
        return result;
    }

    private boolean overlaps(Date issueStart, Date issueDue, Milestone milestone) {
        Date milestoneStart = milestone.getStartDate();
        Date milestoneEnd = milestone.getReleaseDueDate();

        if (milestoneStart != null && milestoneStart.after(issueDue)) {
            return false;
        }
        if (milestoneEnd != null && milestoneEnd.before(issueStart)) {
            return false;
        }
        return milestoneStart != null || milestoneEnd != null;
    }
}
