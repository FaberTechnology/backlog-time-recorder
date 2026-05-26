package com.lambda.strategies;

import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;

public interface UpdateStrategy {
    boolean canApply(IssueWrapper issueWrapper, ProjectContext projectContext);
    void apply(IssueWrapper issueWrapper, ProjectContext projectContext, UpdateIssueParams params);
}
