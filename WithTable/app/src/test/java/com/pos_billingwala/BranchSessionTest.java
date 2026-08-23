package com.pos_billingwala;

import com.pos_billingwala.Extra.BranchSession;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Multi-branch session scope — single-branch defaults (no config change required).
 */
public class BranchSessionTest {

    @Test
    public void effectiveBranchId_fallsBackToUserId() {
        com.pos_billingwala.Activity.MainActivity.branchId = "";
        com.pos_billingwala.Activity.MainActivity.userId = "42";
        assertEquals("42", BranchSession.effectiveBranchId());
    }

    @Test
    public void effectiveBranchId_prefersExplicitBranchId() {
        com.pos_billingwala.Activity.MainActivity.branchId = "99";
        com.pos_billingwala.Activity.MainActivity.userId = "42";
        assertEquals("99", BranchSession.effectiveBranchId());
    }

    @Test
    public void effectiveOrganizationId_fallsBackToOwnerId() {
        com.pos_billingwala.Activity.MainActivity.organizationId = "";
        com.pos_billingwala.Activity.MainActivity.ownerId = "7";
        assertEquals("7", BranchSession.effectiveOrganizationId());
    }

    @Test
    public void effectiveOrganizationId_prefersExplicitOrganizationId() {
        com.pos_billingwala.Activity.MainActivity.organizationId = "12";
        com.pos_billingwala.Activity.MainActivity.ownerId = "7";
        assertEquals("12", BranchSession.effectiveOrganizationId());
    }
}
