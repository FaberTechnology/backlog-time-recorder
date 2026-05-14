package com.lambda.models;

import com.nulabinc.backlog4j.Milestone;

import java.util.List;

public record ProjectContext(
    long projectId,
    List<Milestone> milestones
) {}
