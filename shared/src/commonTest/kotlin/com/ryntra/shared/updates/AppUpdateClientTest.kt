package com.ryntra.shared.updates

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateClientTest {
    @Test
    fun newerReleaseIsDetectedAgainstBundledVersion() {
        assertTrue(AppUpdateClient.isNewerVersion("v3.2.0", "3.1.0"))
        assertTrue(AppUpdateClient.isNewerVersion("2.10.0", "2.9.9"))
    }

    @Test
    fun olderAndEqualReleasesAreIgnored() {
        assertFalse(AppUpdateClient.isNewerVersion("3.1.0", "3.1.0"))
        assertFalse(AppUpdateClient.isNewerVersion("3.0.9", "3.1.0"))
        assertFalse(AppUpdateClient.isNewerVersion("not-a-version", "3.1.0"))
    }
}
