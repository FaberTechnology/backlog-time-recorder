package com.lambda.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.lambda.TestContext;
import com.lambda.models.RestrictedStatusTransitionPolicy;

public class StatusChangeNotificationTest {

    private static final long PRODUCT_OWNER_ID = 100000L;
    private static final long NON_PRODUCT_OWNER_ID = 999999L;
    private static final int SETTING_PRIORITY_STATUS_ID = 10001;
    private static final String PBI_ISSUE_TYPE_NAME = "PBI";
    private static final String BUG_ISSUE_TYPE_NAME = "Bug";
    private static final String ENABLED_PROJECT_KEY = "PROJ1";
    private static final String OTHER_PROJECT_KEY = "PROJ2";
    private static final int STATUS_OPEN = 1;
    private static final int STATUS_IN_PROGRESS = 2;
    private static final int STATUS_CLOSED = 4;

    private static final IssueUpdater NO_OP_UPDATER = (issueId, newStatusCode, hasDateChange) -> null;

    @Test
    public void handleRequest_nonProductOwnerClosesIssue_notifiesOnce() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_CLOSED, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertEquals(1, notifier.calls.size());
        final long[] call = notifier.calls.get(0);
        assertEquals(STATUS_OPEN, call[1]);
        assertEquals(STATUS_CLOSED, call[2]);
        assertEquals(NON_PRODUCT_OWNER_ID, call[3]);
    }

    @Test
    public void handleRequest_productOwnerClosesIssue_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_CLOSED, PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_nonProductOwnerSetsPriority_notifiesOnce() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, SETTING_PRIORITY_STATUS_ID, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertEquals(1, notifier.calls.size());
    }

    @Test
    public void handleRequest_nonProductOwnerStartsProgress_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_IN_PROGRESS, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_nonPbiIssueClosedByNonProductOwner_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, BUG_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_CLOSED, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_nonEnabledProjectClosedByNonProductOwner_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(OTHER_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_CLOSED, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_nonProductOwnerMovesOpenToInProgress_notifiesInvalidTransitionOnly() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_IN_PROGRESS, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertEquals(1, notifier.invalidTransitionCalls.size());
        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_productOwnerMovesOpenToInProgress_notifiesInvalidTransitionToo() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, STATUS_IN_PROGRESS, PRODUCT_OWNER_ID)),
                new TestContext());

        assertEquals(1, notifier.invalidTransitionCalls.size());
        assertTrue(notifier.calls.isEmpty());
    }

    @Test
    public void handleRequest_openToSettingPriority_doesNotNotifyInvalidTransition() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(statusChangeBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, SETTING_PRIORITY_STATUS_ID, PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.invalidTransitionCalls.isEmpty());
    }

    @Test
    public void handleRequest_pbiCreatedWithNonOpenStatus_notifiesInvalidCreation() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(creationBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_IN_PROGRESS, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertEquals(1, notifier.invalidCreationCalls.size());
        final long[] call = notifier.invalidCreationCalls.get(0);
        assertEquals(STATUS_IN_PROGRESS, call[1]);
        assertEquals(NON_PRODUCT_OWNER_ID, call[2]);
    }

    @Test
    public void handleRequest_pbiCreatedWithOpenStatus_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(creationBody(ENABLED_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_OPEN, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.invalidCreationCalls.isEmpty());
    }

    @Test
    public void handleRequest_nonPbiCreatedWithNonOpenStatus_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(creationBody(ENABLED_PROJECT_KEY, BUG_ISSUE_TYPE_NAME, STATUS_IN_PROGRESS, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.invalidCreationCalls.isEmpty());
    }

    @Test
    public void handleRequest_nonEnabledProjectCreatedWithNonOpenStatus_doesNotNotify() {
        final RecordingNotifier notifier = new RecordingNotifier();
        final BacklogTimeRecorder handler = handlerFor(notifier);

        handler.handleRequest(
                event(creationBody(OTHER_PROJECT_KEY, PBI_ISSUE_TYPE_NAME, STATUS_IN_PROGRESS, NON_PRODUCT_OWNER_ID)),
                new TestContext());

        assertTrue(notifier.invalidCreationCalls.isEmpty());
    }

    private static BacklogTimeRecorder handlerFor(final StatusChangeNotifier notifier) {
        final RestrictedStatusTransitionPolicy policy = new RestrictedStatusTransitionPolicy(
                Set.of(PRODUCT_OWNER_ID), Set.of(SETTING_PRIORITY_STATUS_ID), Set.of(ENABLED_PROJECT_KEY));
        return new BacklogTimeRecorder(NO_OP_UPDATER, notifier, policy);
    }

    private static APIGatewayV2HTTPEvent event(final String body) {
        final APIGatewayV2HTTPEvent event = new APIGatewayV2HTTPEvent();
        event.setBody(body);
        event.setIsBase64Encoded(false);
        return event;
    }

    private static String statusChangeBody(final String projectKey, final String issueTypeName, final int oldStatus,
            final int newStatus, final long createdUserId) {
        return "{"
                + "\"id\":1,"
                + "\"project\":{\"id\":100000,\"projectKey\":\"" + projectKey + "\",\"name\":\"Test Project\"},"
                + "\"content\":{"
                + "\"id\":200000001,"
                + "\"summary\":\"PBI test issue\","
                + "\"issueType\":{\"id\":12345,\"name\":\"" + issueTypeName + "\"},"
                + "\"changes\":[{\"field\":\"status\",\"new_value\":\"" + newStatus + "\",\"old_value\":\""
                + oldStatus + "\",\"type\":\"standard\"}]"
                + "},"
                + "\"createdUser\":{\"id\":" + createdUserId + ",\"name\":\"Test User\",\"roleType\":2,\"lang\":\"en\"},"
                + "\"created\":\"2023-01-03T00:00:00Z\""
                + "}";
    }

    private static String creationBody(final String projectKey, final String issueTypeName, final int statusId,
            final long createdUserId) {
        return "{"
                + "\"id\":1,"
                + "\"type\":1,"
                + "\"project\":{\"id\":100000,\"projectKey\":\"" + projectKey + "\",\"name\":\"Test Project\"},"
                + "\"content\":{"
                + "\"id\":200000001,"
                + "\"summary\":\"PBI test issue\","
                + "\"issueType\":{\"id\":12345,\"name\":\"" + issueTypeName + "\"},"
                + "\"status\":{\"id\":" + statusId + ",\"name\":\"Test Status\"}"
                + "},"
                + "\"createdUser\":{\"id\":" + createdUserId + ",\"name\":\"Test User\",\"roleType\":2,\"lang\":\"en\"},"
                + "\"created\":\"2023-01-03T00:00:00Z\""
                + "}";
    }

    private static class RecordingNotifier implements StatusChangeNotifier {
        private final List<long[]> calls = new ArrayList<>();
        private final List<long[]> invalidTransitionCalls = new ArrayList<>();
        private final List<long[]> invalidCreationCalls = new ArrayList<>();

        @Override
        public void notifyUnauthorizedStatusChange(final int issueId, final int oldStatusCode, final int newStatusCode,
                final long actorUserId) {
            calls.add(new long[] {issueId, oldStatusCode, newStatusCode, actorUserId});
        }

        @Override
        public void notifyInvalidStatusTransition(final int issueId, final int oldStatusCode, final int newStatusCode,
                final long actorUserId) {
            invalidTransitionCalls.add(new long[] {issueId, oldStatusCode, newStatusCode, actorUserId});
        }

        @Override
        public void notifyInvalidCreationStatus(final int issueId, final int statusCode, final long actorUserId) {
            invalidCreationCalls.add(new long[] {issueId, statusCode, actorUserId});
        }
    }
}
