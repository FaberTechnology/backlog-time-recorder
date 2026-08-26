package com.lambda.models;

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

    private final RestrictedStatusTransitionPolicy policy = new RestrictedStatusTransitionPolicy(
            Collections.singleton(PRODUCT_OWNER_ID), SETTING_PRIORITY_STATUS_ID);

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
                Collections.emptySet(), SETTING_PRIORITY_STATUS_ID);

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
                Set.of(PRODUCT_OWNER_ID, OTHER_USER_ID), SETTING_PRIORITY_STATUS_ID);

        assertTrue(multiOwnerPolicy.isAuthorized(OTHER_USER_ID));
    }
}
