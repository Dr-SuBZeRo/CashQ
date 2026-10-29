package com.cashq

import com.cashq.ui.theme.CashQTheme
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Basic unit test for CashQ — verifies the project compiles and test infrastructure works.
 */
class CashQBasicTest {

    @Test
    fun appPackage_exists() {
        // Verify the CashQ package can be referenced
        val className = MainActivity::class.qualifiedName
        assertNotNull("MainActivity should have a qualified name", className)
        assert(className!!.startsWith("com.cashq")) {
            "Package should start with com.cashq but was $className"
        }
    }
}
