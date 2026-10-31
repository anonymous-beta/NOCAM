
package com.nocam.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetScopeTest {

    @Test
    fun allowsPrivateSingleHost() {
        assertEquals(
            listOf("192.168.1.10"),
            TargetScope.parse("192.168.1.10")
        )

        assertTrue(TargetScope.parse("10.0.0.1").isNotEmpty())
        assertTrue(TargetScope.parse("172.16.0.1").isNotEmpty())
    }

    @Test
    fun rejectsPublicHosts() {
        assertFalse(TargetScope.isPrivateIpv4("8.8.8.8"))
        assertFalse(TargetScope.isPrivateIpv4("172.32.0.1"))
        assertFalse(TargetScope.isPrivateIpv4("192.0.2.1"))
    }

    @Test
    fun limitsSubnetSize() {
        val hosts = TargetScope.parse("192.168.1.0/26")
        assertEquals(62, hosts.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLargeSubnet() {
        TargetScope.parse("192.168.0.0/24")
    }
}
