package com.lambda;

import com.nulabinc.backlog4j.BacklogClient;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MilestoneCalculator {
    
    private final BacklogClient client;
    private final TimeCalculator timeCalculator;

    public MilestoneCalculator(BacklogClient client, TimeCalculator timeCalculator) {
        this.client = client;
        this.timeCalculator = timeCalculator;
    }

    /**
     * Calculate milestone IDs to set based on issue dates.
     * This method requires API access to get project milestones.
     */
    public List<Long> calculateMilestoneIdsToSet(final Issue issue) {
        final Date startDate = issue.getStartDate();
        final Date dueDate = issue.getDueDate();
        
        if (startDate == null || dueDate == null) {
            return null;
        }
        
        final Set<String> requiredMilestones = timeCalculator.calculateRequiredMilestones(
            java.time.LocalDate.ofInstant(startDate.toInstant(), AppConstants.JST_ZONE),
            java.time.LocalDate.ofInstant(dueDate.toInstant(), AppConstants.JST_ZONE)
        );
        
        final Set<String> currentMilestoneNames = issue.getMilestone() != null 
            ? issue.getMilestone().stream()
                .map(Milestone::getName)
                .collect(Collectors.toSet())
            : Set.of();
        
        final Set<String> milestonesToAdd = requiredMilestones.stream()
            .filter(name -> !currentMilestoneNames.contains(name))
            .collect(Collectors.toSet());
        
        if (milestonesToAdd.isEmpty()) {
            return null;
        }
        
        final List<Milestone> projectMilestones = client.getMilestones(issue.getProjectId());
        final List<Long> newMilestoneIds = projectMilestones.stream()
            .filter(m -> milestonesToAdd.contains(m.getName()))
            .map(Milestone::getId)
            .collect(Collectors.toList());
        
        final List<Long> allMilestoneIds = new ArrayList<>();
        if (issue.getMilestone() != null) {
            allMilestoneIds.addAll(issue.getMilestone().stream()
                .map(Milestone::getId)
                .collect(Collectors.toList()));
        }
        allMilestoneIds.addAll(newMilestoneIds);
        
        return allMilestoneIds;
    }
}