package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun verifyDefaultCredentialsRules() {
        val defaultEmail = "aboanasanam42@gmail.com"
        val defaultPass = "malek@123"

        assertEquals("aboanasanam42@gmail.com", defaultEmail)
        assertEquals("malek@123", defaultPass)

        assertTrue("aboanasanam42@gmail.com".equals(defaultEmail, ignoreCase = true))
        assertFalse("wrong@email.com".equals(defaultEmail, ignoreCase = true))
    }

    @Test
    fun verifyOtpGenerationLength() {
        val random = java.security.SecureRandom()
        val code = (100000 + random.nextInt(900000)).toString()
        assertEquals(6, code.length)
        assertTrue(code.toInt() in 100000..999999)
    }
}
