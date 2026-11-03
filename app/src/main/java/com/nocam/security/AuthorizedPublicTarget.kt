
package com.nocam.security

import java.net.Inet4Address
import java.net.InetAddress
import java.net.IDN

/**
 * Validates a single public IPv4 address or hostname for an
 * explicitly authorized NOCAM audit.
 *
 * This class validates scope only. It does not perform scanning,
 * authentication, exploitation, or vulnerability testing.
 */
object AuthorizedPublicTarget {

    data class Result(
        val valid: Boolean,
        val originalTarget: String,
        val addresses: List<String> = emptyList(),
        val message: String
    )

    fun validate(target: String): Result {
        val input = target.trim()

        if (input.isBlank()) {
            return failure(input, "Enter a public IP address or hostname.")
        }

        if (input.length > 253) {
            return failure(input, "Target is too long.")
        }

        if (input.contains("/") ||
            input.contains(":") ||
            input.contains("*") ||
            input.contains(" ")) {
            return failure(
                input,
                "Enter one IPv4 address or hostname. Ranges and IPv6 are not supported."
            )
        }

        val hostname = try {
            IDN.toASCII(input).lowercase()
        } catch (_: IllegalArgumentException) {
            return failure(input, "Invalid hostname.")
        }

        if (!isValidHostnameOrIpv4(hostname)) {
            return failure(input, "Invalid IPv4 address or hostname.")
        }

        val resolved = try {
            InetAddress.getAllByName(hostname).toList()
        } catch (_: Exception) {
            return failure(input, "Could not resolve the target.")
        }

        if (resolved.isEmpty()) {
            return failure(input, "No addresses were found.")
        }

        if (resolved.size > 4) {
            return failure(
                input,
                "Hostname resolves to too many addresses. Use a specific authorized IP."
            )
        }

        if (resolved.any { it !is Inet4Address || !isPublicIpv4(it.address) }) {
            return failure(
                input,
                "Target resolves to a private, reserved, or otherwise non-public address."
            )
        }

        val addresses = resolved
            .map { it.hostAddress }
            .distinct()

        return Result(
            valid = true,
            originalTarget = input,
            addresses = addresses,
            message = "Public target validated. Confirm authorization before scanning."
        )
    }

    private fun isValidHostnameOrIpv4(value: String): Boolean {
        if (value.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            return value.split(".").all {
                it.toIntOrNull()?.let { octet -> octet in 0..255 } == true
            }
        }

        if (value.length > 253 || value.startsWith(".") || value.endsWith(".")) {
            return false
        }

        return value.split(".").all { label ->
            label.length in 1..63 &&
                label.matches(Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"))
        }
    }

    private fun isPublicIpv4(bytes: ByteArray): Boolean {
        if (bytes.size != 4) return false

        val a = bytes[0].toInt() and 0xFF
        val b = bytes[1].toInt() and 0xFF
        val c = bytes[2].toInt() and 0xFF

        // Unspecified and "this network".
        if (a == 0) return false

        // Private networks.
        if (a == 10) return false
        if (a == 172 && b in 16..31) return false
        if (a == 192 && b == 168) return false

        // Loopback and link-local.
        if (a == 127) return false
        if (a == 169 && b == 254) return false

        // Carrier-grade NAT.
        if (a == 100 && b in 64..127) return false

        // Protocol assignments and special-purpose blocks.
        if (a == 192 && b == 0 && c == 0) return false
        if (a == 192 && b == 0 && c == 2) return false
        if (a == 192 && b == 88 && c == 99) return false

        // Documentation networks.
        if (a == 198 && b == 51 && c == 100) return false
        if (a == 203 && b == 0 && c == 113) return false

        // Benchmarking.
        if (a == 198 && b in 18..19) return false

        // Multicast, reserved and broadcast.
        if (a in 224..255) return false

        return true
    }

    private fun failure(
        target: String,
        message: String
    ) = Result(
        valid = false,
        originalTarget = target,
        message = message
    )
}
