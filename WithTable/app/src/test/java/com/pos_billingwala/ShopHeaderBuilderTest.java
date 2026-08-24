package com.pos_billingwala;

import com.pos_billingwala.Extra.ShopHeaderBuilder;
import com.pos_billingwala.Model.CompanyResponse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShopHeaderBuilderTest {

    @Test
    public void allFieldsFilled_printsAllLines() {
        CompanyResponse company = sample("GARGEE RESTAURANT", "Pure Veg",
                "ABC Road", "Near XYZ Chowk", "Pune, Maharashtra - 411001",
                "9876543210", "9123456780", "off", "", "");
        String details = ShopHeaderBuilder.buildShopDetailsBlock(company);
        assertEquals("Pure Veg\nABC Road\nNear XYZ Chowk\nPune, Maharashtra - 411001\n9876543210\n9123456780",
                details);
        assertEquals("GARGEE RESTAURANT", ShopHeaderBuilder.resolveShopName1(company));
    }

    @Test
    public void emptyOptionalFields_skipBlankLines() {
        CompanyResponse company = sample("GARGEE RESTAURANT", "",
                "ABC Road", "", "Pune",
                "9876543210", "", "off", "", "");
        String details = ShopHeaderBuilder.buildShopDetailsBlock(company);
        assertEquals("ABC Road\nPune\n9876543210", details);
        assertTrue(!details.contains("\n\n"));
    }

    @Test
    public void onlyShopName1_detailsEmpty() {
        CompanyResponse company = sample("Only Shop", "", "", "", "", "", "", "off", "", "");
        assertEquals("", ShopHeaderBuilder.buildShopDetailsBlock(company));
        assertEquals("Only Shop", ShopHeaderBuilder.resolveShopName1(company));
    }

    @Test
    public void legacyFallback_whenStructuredEmpty() {
        CompanyResponse company = new CompanyResponse();
        company.setCompanyName("Legacy Shop");
        company.setCompanyAddress("Old Address Lane");
        company.setCompanyMobile("9999999999");
        company.setGstStatus("off");
        assertEquals("Legacy Shop", ShopHeaderBuilder.resolveShopName1(company));
        assertEquals("Old Address Lane\n9999999999",
                ShopHeaderBuilder.buildShopDetailsBlock(company));
    }

    @Test
    public void gstAndFssai_appendedWhenPresent() {
        CompanyResponse company = sample("Shop", "", "Addr", "", "", "111", "",
                "on", "27AAAAA0000A1Z5", "12345678901234");
        String details = ShopHeaderBuilder.buildShopDetailsBlock(company);
        assertEquals("Addr\n111\nGSTIN: 27AAAAA0000A1Z5\nFSSAI No: 12345678901234", details);
    }

    @Test
    public void composeLegacyAddress_joinsNonEmptyOnly() {
        assertEquals("A\nC", ShopHeaderBuilder.composeLegacyAddress("A", "", "C"));
        assertEquals("", ShopHeaderBuilder.composeLegacyAddress(null, "  ", null));
    }

    @Test
    public void nullCompany_safe() {
        assertEquals("", ShopHeaderBuilder.resolveShopName1(null));
        assertEquals("", ShopHeaderBuilder.buildShopDetailsBlock(null));
    }

    private static CompanyResponse sample(String name1, String name2,
                                          String a1, String a2, String a3,
                                          String p1, String p2,
                                          String gstStatus, String gstNumber, String fssai) {
        CompanyResponse company = new CompanyResponse();
        company.setShopName1(name1);
        company.setShopName2(name2);
        company.setAddressLine1(a1);
        company.setAddressLine2(a2);
        company.setAddressLine3(a3);
        company.setPhoneNo1(p1);
        company.setPhoneNo2(p2);
        company.setGstStatus(gstStatus);
        company.setGstNumber(gstNumber);
        company.setCompanyFssis(fssai);
        return company;
    }
}
