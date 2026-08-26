package com.lambda.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nulabinc.backlog4j.Issue.StatusType;

public class RestrictedStatusTransitionPolicyTest {

    private static final long PRODUCT_OWNER_ID = 399389L;
    private static final long OTHER_USER_ID = 111111L;
    private static final int SETTING_PRIORITY_STATUS_ID = 10001;
    private static final String PBI_ISSUE_TYPE_NAME = "PBI";
    private static final String BUG_ISSUE_TYPE_NAME = "Bug";
    private static final String ENABLED_PROJECT_KEY = "PROJ1";
    private static final String OTHER_PROJECT_KEY = "PROJ2";

    private final RestrictedStatusTransitionPolicy policy = new RestrictedStatusTransitionPolicy(
            Collections.singleton(PRODUCT_OWNER_ID), Collections.singleton(SETTING_PRIORITY_STATUS_ID),
            Collections.singleton(ENABLED_PROJECT_KEY));

    @Test
    public void isRestrictedTransition_openToSettingPriority_returnsTrue() {
        assertTrue(policy.isRestrictedTransition(StatusType.Open.getIntValue(), SETTING_PRIORITY_STATUS_ID));
    }

    @Test
    public void isRestrictedTransition_inProgressToSettingPriority_returnsFalse() {
        assertFalse(policy.isRestrictedTransition(StatusType.InProgress.getIntValue(), SETTING_PRIORITY_STATUS_ID));
    }

    @Test
    public void isRestrictedTransition_anyToClosed_returnsTrue() {
        assertTrue(policy.isRestrictedTransition(StatusType.InProgress.getIntValue(), StatusType.Closed.getIntValue()));
        assertTrue(policy.isRestrictedTransition(StatusType.Open.getIntValue(), StatusType.Closed.getIntValue()));
    }

    @Test
    public void isRestrictedTransition_openToInProgress_returnsFalse() {
        assertFalse(policy.isRestrictedTransition(StatusType.Open.getIntValue(), StatusType.InProgress.getIntValue()));
    }

    @Test
    public void isRestrictedTransition_noProductOwnersConfigured_returnsFalse() {
        final RestrictedStatusTransitionPolicy disabled = new RestrictedStatusTransitionPolicy(
                Collections.emptySet(), Collections.singleton(SETTING_PRIORITY_STATUS_ID),
                Collections.singleton(ENABLED_PROJECT_KEY));

        assertFalse(disabled.isRestrictedTransition(StatusType.Open.getIntValue(), StatusType.Closed.getIntValue()));
    }

    @Test
    public void isAuthorized_productOwner_returnsTrue() {
        assertTrue(policy.isAuthorized(PRODUCT_OWNER_ID));
    }

    @Test
    public void isAuthorized_nonProductOwner_returnsFalse() {
        assertFalse(policy.isAuthorized(OTHER_USER_ID));
    }

    @Test
    public void isAuthorized_multipleProductOwners_matchesAny() {
        final RestrictedStatusTransitionPolicy multiOwnerPolicy = new RestrictedStatusTransitionPolicy(
                Set.of(PRODUCT_OWNER_ID, OTHER_USER_ID), Collections.singleton(SETTING_PRIORITY_STATUS_ID),
                Collections.singleton(ENABLED_PROJECT_KEY));

        assertTrue(multiOwnerPolicy.isAuthorized(OTHER_USER_ID));
    }

    @Test
    public void isPbiIssueType_pbiName_returnsTrue() {
        assertTrue(policy.isPbiIssueType(PBI_ISSUE_TYPE_NAME));
    }

    @Test
    public void isPbiIssueType_nonPbiName_returnsFalse() {
        assertFalse(policy.isPbiIssueType(BUG_ISSUE_TYPE_NAME));
    }

    @Test
    public void isPbiIssueType_caseMismatch_returnsFalse() {
        assertFalse(policy.isPbiIssueType("pbi"));
    }

    @Test
    public void isPbiIssueType_null_returnsFalse() {
        assertFalse(policy.isPbiIssueType(null));
    }

    @Test
    public void parseUserIds_null_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseUserIds(null).isEmpty());
    }

    @Test
    public void parseUserIds_blank_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseUserIds("  ").isEmpty());
    }

    @Test
    public void parseUserIds_csvWithSpacesAndTrailingComma_parsesAllIds() {
        final Set<Long> ids = RestrictedStatusTransitionPolicy.parseUserIds(" 399389, 111111,");

        assertEquals(Set.of(399389L, 111111L), ids);
    }

    @Test
    public void parseStatusIds_null_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseStatusIds(null).isEmpty());
    }

    @Test
    public void parseStatusIds_blank_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseStatusIds("  ").isEmpty());
    }

    @Test
    public void parseStatusIds_csvWithSpacesAndTrailingComma_parsesAllIds() {
        final Set<Integer> ids = RestrictedStatusTransitionPolicy.parseStatusIds(" 10001, 20002,");

        assertEquals(Set.of(10001, 20002), ids);
    }

    @Test
    public void isRestrictedTransition_openToAnyConfiguredSettingPriorityId_returnsTrue() {
        final RestrictedStatusTransitionPolicy multiProjectPolicy = new RestrictedStatusTransitionPolicy(
                Collections.singleton(PRODUCT_OWNER_ID), Set.of(10001, 20002),
                Collections.singleton(ENABLED_PROJECT_KEY));

        assertTrue(multiProjectPolicy.isRestrictedTransition(StatusType.Open.getIntValue(), 10001));
        assertTrue(multiProjectPolicy.isRestrictedTransition(StatusType.Open.getIntValue(), 20002));
    }

    @Test
    public void isEnabledProject_enabledKey_returnsTrue() {
        assertTrue(policy.isEnabledProject(ENABLED_PROJECT_KEY));
    }

    @Test
    public void isEnabledProject_notEnabledKey_returnsFalse() {
        assertFalse(policy.isEnabledProject(OTHER_PROJECT_KEY));
    }

    @Test
    public void isEnabledProject_null_returnsFalse() {
        assertFalse(policy.isEnabledProject(null));
    }

    @Test
    public void isEnabledProject_multipleEnabledProjects_matchesAny() {
        final RestrictedStatusTransitionPolicy multiProjectPolicy = new RestrictedStatusTransitionPolicy(
                Collections.singleton(PRODUCT_OWNER_ID), Collections.singleton(SETTING_PRIORITY_STATUS_ID),
                Set.of(ENABLED_PROJECT_KEY, OTHER_PROJECT_KEY));

        assertTrue(multiProjectPolicy.isEnabledProject(OTHER_PROJECT_KEY));
    }

    @Test
    public void parseProjectKeys_null_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseProjectKeys(null).isEmpty());
    }

    @Test
    public void parseProjectKeys_blank_returnsEmptySet() {
        assertTrue(RestrictedStatusTransitionPolicy.parseProjectKeys("  ").isEmpty());
    }

    @Test
    public void parseProjectKeys_csvWithSpacesAndTrailingComma_parsesAllKeys() {
        final Set<String> keys = RestrictedStatusTransitionPolicy.parseProjectKeys(" PROJ1, PROJ2,");

        assertEquals(Set.of("PROJ1", "PROJ2"), keys);
    }
}
