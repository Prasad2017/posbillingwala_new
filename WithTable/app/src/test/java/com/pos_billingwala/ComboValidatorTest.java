package com.pos_billingwala;

import com.pos_billingwala.Extra.ComboValidator;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ComboValidatorTest {

    @Test
    public void validateCombo_rejectsBlankName() {
        assertEquals(ComboValidator.ERR_NAME, ComboValidator.validateCombo("  ", "99", 1));
    }

    @Test
    public void validateCombo_rejectsNonPositivePrice() {
        assertEquals(ComboValidator.ERR_PRICE, ComboValidator.validateCombo("Lunch", "0", 1));
        assertEquals(ComboValidator.ERR_PRICE, ComboValidator.validateCombo("Lunch", "abc", 1));
    }

    @Test
    public void validateCombo_requiresAtLeastOneItem() {
        assertEquals(ComboValidator.ERR_ITEMS, ComboValidator.validateCombo("Lunch", "199", 0));
    }

    @Test
    public void validateCombo_acceptsValidMaster() {
        assertNull(ComboValidator.validateCombo("Lunch Combo", "199.50", 2));
    }

    @Test
    public void validateComboItem_requiresExistingProduct() {
        assertEquals(ComboValidator.ERR_PRODUCT,
                ComboValidator.validateComboItem("", true, true, false, null, true, "1"));
        assertEquals(ComboValidator.ERR_PRODUCT,
                ComboValidator.validateComboItem("10", false, true, false, null, true, "1"));
    }

    @Test
    public void validateComboItem_rejectsInactiveProduct() {
        assertEquals(ComboValidator.ERR_PRODUCT_INACTIVE,
                ComboValidator.validateComboItem("10", true, false, false, null, true, "1"));
    }

    @Test
    public void validateComboItem_requiresPortionWhenProductHasPortions() {
        assertEquals(ComboValidator.ERR_PORTION_REQUIRED,
                ComboValidator.validateComboItem("10", true, true, true, "", false, "1"));
    }

    @Test
    public void validateComboItem_rejectsPortionFromAnotherProduct() {
        assertEquals(ComboValidator.ERR_PORTION_MISMATCH,
                ComboValidator.validateComboItem("10", true, true, true, "5", false, "1"));
    }

    @Test
    public void validateComboItem_rejectsNonPositiveQuantity() {
        assertEquals(ComboValidator.ERR_QUANTITY,
                ComboValidator.validateComboItem("10", true, true, false, null, true, "0"));
    }

    @Test
    public void validateComboItem_acceptsProductWithoutPortion() {
        assertNull(ComboValidator.validateComboItem("10", true, true, false, null, true, "2"));
    }

    @Test
    public void formatComponentLine_includesPortionWhenPresent() {
        assertEquals("  - Chicken / Half x 1",
                ComboValidator.formatComponentLine("Chicken", "Half", "1"));
        assertEquals("  - Coke x 2",
                ComboValidator.formatComponentLine("Coke", "", "2"));
    }

    @Test
    public void buildComponentSnapshot_joinsNonEmptyLines() {
        String snapshot = ComboValidator.buildComponentSnapshot(Arrays.asList(
                "  - Chicken / Half x 1",
                "",
                "  - Coke x 1"));
        assertEquals("  - Chicken / Half x 1\n  - Coke x 1", snapshot);
        assertEquals("", ComboValidator.buildComponentSnapshot(Collections.emptyList()));
    }

    @Test
    public void isValidSellingPrice_andQuantity() {
        assertTrue(ComboValidator.isValidSellingPrice("10"));
        assertFalse(ComboValidator.isValidSellingPrice("0"));
        assertTrue(ComboValidator.isValidQuantity("0.5"));
        assertFalse(ComboValidator.isValidQuantity("-1"));
    }
}
