package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaSingleShopAddressTest {
    @Test fun checkoutNormalizesLocalDigitsAndRejectsNonPhoneCharacters() {
        assertEquals("+20 (101) 234-5678", normalizedDeliveryPhone(" +٢٠ (١٠١) ٢٣٤-٥٦٧٨ "))
        assertTrue(realCheckoutAddressValid(
            "Customer", "+٢٠ (١٠١) ٢٣٤-٥٦٧٨", "Cairo", "Building 2, Street 11",
            30.0444, 31.2357, true,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "01012345678abc", "Cairo", "Building 2, Street 11",
            30.0444, 31.2357, true,
        ))
    }

    @Test fun actualCheckoutNeedsCustomerConfirmedExactMapPin() {
        assertTrue(realCheckoutAddressValid(
            "Customer", "+201001234567", "Cairo", "Building 2, Street 11",
            30.0444, 31.2357, true,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "+201001234567", "Cairo", "Building 2, Street 11",
            null, null, false,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "+201001234567", "Cairo", "Building 2, Street 11",
            30.0444, 31.2357, false,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "+201001234567", "Cairo", "Building 2, Street 11",
            92.0, 31.2357, true,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "123", "Cairo", "Building 2, Street 11",
            30.0444, 31.2357, true,
        ))
        assertFalse(realCheckoutAddressValid(
            "Customer", "+201001234567", "", "Building 2, Street 11",
            30.0444, 31.2357, true,
        ))
    }
}
