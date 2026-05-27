package com.lambda.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.nulabinc.backlog4j.Milestone;

public class ProjectContext {

    private final long projectId;
    private final Supplier<List<Milestone>> milestonesSupplier;
    private final Function<String, Milestone> milestoneCreator;
    private List<Milestone> cachedMilestones;

    public ProjectContext(
            final long projectId,
            final Supplier<List<Milestone>> milestonesSupplier,
            final Function<String, Milestone> milestoneCreator) {
        this.projectId = projectId;
        this.milestonesSupplier = milestonesSupplier;
        this.milestoneCreator = milestoneCreator;
    }

    public long projectId() {
        return projectId;
    }

    public List<Milestone> milestones() {
        if (cachedMilestones == null) {
            cachedMilestones = new ArrayList<>(milestonesSupplier.get());
        }
        return cachedMilestones;
    }

    public Milestone getOrCreateMilestone(final String name) {
        final Optional<Milestone> existing = milestones().stream()
                .filter(m -> name.equals(m.getName()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }
        final Milestone created = milestoneCreator.apply(name);
        milestones().add(created);
        return created;
    }
}
