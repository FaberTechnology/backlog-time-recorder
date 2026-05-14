package com.lambda.models;

import java.util.List;

import com.nulabinc.backlog4j.Milestone;

public class ProjectContext {

    private final long projectId;
    private final List<Milestone> milestones;

    public ProjectContext(final long projectId, final List<Milestone> milestones) {
        this.projectId = projectId;
        this.milestones = milestones;
    }

    public long projectId() {
        return projectId;
    }

    public List<Milestone> milestones() {
        return milestones;
    }
}
