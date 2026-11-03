
package com.nocam.security

object CameraFingerprint {

    data class Fingerprint(
        val vendor: String,
        val evidence: String,
        val confidence: String
    )

    private val signatures = listOf(
        Triple("Hikvision", listOf("hikvision", "hik-connect", "webs.c"), "MEDIUM"),
        Triple("Dahua", listOf("dahua", "dhi-", "dh-ipc"), "MEDIUM"),
        Triple("Axis", listOf("axis communications", "axis-video", "axis"), "MEDIUM"),
        Triple("Reolink", listOf("reolink"), "MEDIUM"),
        Triple("Ubiquiti", listOf("ubiquiti", "unifi protect"), "MEDIUM"),
        Triple("TP-Link", listOf("tp-link", "vigi"), "MEDIUM"),
        Triple("Bosch", listOf("bosch security"), "MEDIUM"),
        Triple("Hanwha", listOf("hanwha", "wisenet"), "MEDIUM")
    )

    fun identify(response: String): Fingerprint {
        val normalized = response.lowercase()

        val match = signatures.firstOrNull { signature ->
            signature.second.any { normalized.contains(it) }
        }

        return if (match != null) {
            Fingerprint(
                vendor = match.first,
                evidence = "Matched indicator: ${match.second.firstOrNull { normalized.contains(it) }}",
                confidence = match.third
            )
        } else {
            Fingerprint(
                vendor = "Unknown",
                evidence = "No known camera vendor signature matched.",
                confidence = "LOW"
            )
        }
    }
}
