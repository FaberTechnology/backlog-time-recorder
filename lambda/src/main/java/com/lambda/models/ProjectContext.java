package com.lambda.models;

import java.util.List;
import java.util.function.Supplier;

import com.nulabinc.backlog4j.Milestone;

public class ProjectContext {

    private final long projectId;
    private final Supplier<List<Milestone>> milestonesSupplier;
    private List<Milestone> cachedMilestones;

    public ProjectContext(final long projectId, final Supplier<List<Milestone>> milestonesSupplier) {
        this.projectId = projectId;
        this.milestonesSupplier = milestonesSupplier;
    }

    public long projectId() {
        return projectId;
    }

    public List<Milestone> milestones() {
        if (cachedMilestones == null) {
            cachedMilestones = milestonesSupplier.get();
        }
        return cachedMilestones;
    }
}
