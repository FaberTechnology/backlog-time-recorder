package com.lambda.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.lambda.helpers.MilestoneHelper;
import com.lambda.models.IssueWrapper;
import com.lambda.models.ProjectContext;
import com.nulabinc.backlog4j.Issue;
import com.nulabinc.backlog4j.Milestone;
import com.nulabinc.backlog4j.api.option.UpdateIssueParams;
import com.nulabinc.backlog4j.http.NameValuePair;
import com.nulabinc.backlog4j.internal.json.Jackson;
import com.nulabinc.backlog4j.internal.json.IssueJSONImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MilestoneUpdateStrategyTest {

    private final MilestoneHelper helper = new MilestoneHelper();
    private final MilestoneUpdateStrategy strategy = new MilestoneUpdateStrategy(helper);

    @Test
    public void canApply_AddsAndRemovesObsoleteMonthly() {
        final Milestone apr = milestone(1L, "2026-Apr");
        final Milestone may = milestone(2L, "2026-May");
        final Milestone jun = milestone(3L, "2026-Jun");

        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-06-20", Arrays.asList(apr, may));
        final ProjectContext ctx = projectContextWith(Arrays.asList(apr, may, jun));

        assertTrue(strategy.canApply(issue, ctx));

        final UpdateIssueParams params = new UpdateIssueParams(1);
        strategy.apply(issue, ctx, params);

        assertEquals(Arrays.asList("2", "3"), milestoneIdParams(params));
    }

    @Test
    public void canApply_PreservesManualMilestones() {
        final Milestone sprint = milestone(10L, "Sprint 1");
        final Milestone apr = milestone(1L, "2026-Apr");
        final Milestone may = milestone(2L, "2026-May");

        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-05-20", Arrays.asList(sprint, apr));
        final ProjectContext ctx = projectContextWith(Arrays.asList(apr, may, sprint));

        assertTrue(strategy.canApply(issue, ctx));

        final UpdateIssueParams params = new UpdateIssueParams(1);
        strategy.apply(issue, ctx, params);

        assertEquals(Arrays.asList("10", "2"), milestoneIdParams(params));
    }

    @Test
    public void canApply_AutoCreatesMissingMonthlyMilestone() {
        final Milestone may = milestone(2L, "2026-May");
        final List<String> created = new ArrayList<>();

        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-06-20", Arrays.asList(may));
        final ProjectContext ctx = projectContextWithCreator(
                new ArrayList<>(Arrays.asList(may)),
                name -> {
                    created.add(name);
                    return milestone(999L, name);
                });

        assertTrue(strategy.canApply(issue, ctx));

        final UpdateIssueParams params = new UpdateIssueParams(1);
        strategy.apply(issue, ctx, params);

        assertEquals(Arrays.asList("2026-Jun"), created);
        assertEquals(Arrays.asList("2", "999"), milestoneIdParams(params));
    }

    @Test
    public void canApply_FalseWhenNoChangesNeeded() {
        final Milestone may = milestone(2L, "2026-May");

        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-05-20", Arrays.asList(may));
        final ProjectContext ctx = projectContextWith(Arrays.asList(may));

        assertFalse(strategy.canApply(issue, ctx));
    }

    @Test
    public void canApply_FalseWhenDateNotChanged() {
        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-06-20", new ArrayList<>(), false);
        final ProjectContext ctx = projectContextWith(new ArrayList<>());

        assertFalse(strategy.canApply(issue, ctx));
    }

    @Test
    public void canApply_DoesNotCreateMilestone() {
        final Milestone may = milestone(2L, "2026-May");
        final List<String> created = new ArrayList<>();

        final IssueWrapper issue = issueWith(
                "2026-05-10", "2026-06-20", Arrays.asList(may));
        final ProjectContext ctx = projectContextWithCreator(
                new ArrayList<>(Arrays.asList(may)),
                name -> {
                    created.add(name);
                    return milestone(999L, name);
                });

        assertTrue(strategy.canApply(issue, ctx));

        assertTrue(created.isEmpty(), "canApply must not trigger milestone creation");
    }

    @Test
    public void canApply_FalseWhenStartAfterDue() {
        final Milestone apr = milestone(1L, "2026-Apr");
        final Milestone may = milestone(2L, "2026-May");

        final IssueWrapper issue = issueWith(
                "2026-05-20", "2026-04-10", Arrays.asList(apr, may));
        final ProjectContext ctx = projectContextWith(Arrays.asList(apr, may));

        assertFalse(strategy.canApply(issue, ctx));
    }

    @Test
    public void canApply_FalseWhenDatesMissing() {
        final IssueWrapper issue = issueWith(null, null, new ArrayList<>());
        final ProjectContext ctx = projectContextWith(new ArrayList<>());

        assertFalse(strategy.canApply(issue, ctx));
    }

    private IssueWrapper issueWith(
            final String startDate, final String dueDate, final List<Milestone> milestones) {
        return issueWith(startDate, dueDate, milestones, true);
    }

    private IssueWrapper issueWith(
            final String startDate, final String dueDate,
            final List<Milestone> milestones, final boolean dateChanged) {
        final String milestoneArray = milestones.stream()
                .map(m -> "{\"id\":" + m.getId() + ",\"name\":\"" + m.getName() + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
        final String startField = startDate == null ? "null" : "\"" + startDate + "T00:00:00Z\"";
        final String dueField = dueDate == null ? "null" : "\"" + dueDate + "T00:00:00Z\"";
        final String json = "{"
                + "\"id\":1,"
                + "\"projectId\":100,"
                + "\"startDate\":" + startField + ","
                + "\"dueDate\":" + dueField + ","
                + "\"milestone\":" + milestoneArray
                + "}";
        final Issue issue = Jackson.fromJsonString(json, IssueJSONImpl.class);
        return new IssueWrapper(issue, 0, dateChanged);
    }

    private ProjectContext projectContextWith(final List<Milestone> available) {
        return new ProjectContext(100L, () -> available, name -> {
            throw new AssertionError("Unexpected creator call for " + name);
        });
    }

    private ProjectContext projectContextWithCreator(
            final List<Milestone> available,
            final java.util.function.Function<String, Milestone> creator) {
        return new ProjectContext(100L, () -> available, creator);
    }

    private static Milestone milestone(final long id, final String name) {
        return new StubMilestone(id, name);
    }

    private static List<String> milestoneIdParams(final UpdateIssueParams params) {
        final List<String> values = new ArrayList<>();
        for (final NameValuePair p : params.getParamList()) {
            if ("milestoneId[]".equals(p.getName())) {
                values.add(p.getValue());
            }
        }
        return values;
    }

    private static final class StubMilestone implements Milestone {
        private final long id;
        private final String name;

        StubMilestone(final long id, final String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public long getId() { return id; }
        @Override
        public String getIdAsString() { return String.valueOf(id); }
        @Override
        public long getProjectId() { return 100L; }
        @Override
        public String getProjectIdAsString() { return "100"; }
        @Override
        public String getName() { return name; }
        @Override
        public String getDescription() { return null; }
        @Override
        public java.util.Date getStartDate() { return null; }
        @Override
        public java.util.Date getReleaseDueDate() { return null; }
        @Override
        public Boolean getArchived() { return Boolean.FALSE; }
    }
}
