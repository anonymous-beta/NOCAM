
package com.nocam.security

import java.net.Inet4Address
import java.net.InetAddress

object TargetScope {
    private const val MAX_HOSTS = 64

    fun parse(input: String): List<String> {
        val raw = input.trim()

        require(raw.isNotEmpty()) {
            "Enter a private IPv4 address or CIDR range."
        }

        if (!raw.contains('/')) {
            require(isPrivateIpv4(raw)) {
                "Only RFC1918 private IPv4 addresses are allowed."
            }
            return listOf(raw)
        }

        val parts = raw.split('/')
        require(parts.size == 2) {
            "CIDR must look like 192.168.1.0/26."
        }

        val address = parts[0]
        val prefix = parts[1].toIntOrNull()
            ?: throw IllegalArgumentException(
                "CIDR prefix must be a number."
            )

        require(prefix in 16..30) {
            "CIDR prefix must be between /16 and /30."
        }

        require(isPrivateIpv4(address)) {
            "CIDR base must be a private IPv4 address."
        }

        val base = ipv4ToLong(address)
        val mask =
            (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = base and mask
        val broadcast = network or (mask xor 0xFFFFFFFFL)
        val count = (broadcast - network - 1).toInt()

        require(count in 1..MAX_HOSTS) {
            "CIDR scans are limited to $MAX_HOSTS hosts. Use a smaller subnet."
        }

        return (network + 1 until broadcast)
            .map(::longToIpv4)
            .filter(::isPrivateIpv4)
    }

    fun isPrivateIpv4(input: String): Boolean {
        if (input.isBlank()) return false
        if (input.any { it !in "0123456789." }) return false

        val address = try {
            InetAddress.getByName(input)
        } catch (_: Exception) {
            return false
        }

        if (address !is Inet4Address) return false
        if (address.hostAddress != input) return false

        val b = address.address.map { it.toInt() and 0xff }

        return b[0] == 10 ||
            (b[0] == 172 && b[1] in 16..31) ||
            (b[0] == 192 && b[1] == 168)
    }

    private fun ipv4ToLong(ip: String): Long {
        val parts = ip.split('.')
        require(parts.size == 4) { "Invalid IPv4 address." }

        val octets = parts.map {
            it.toIntOrNull()?.takeIf { n -> n in 0..255 }
                ?: throw IllegalArgumentException(
                    "Invalid IPv4 address."
                )
        }

        return octets.fold(0L) { acc, n ->
            (acc shl 8) or n.toLong()
        }
    }

    private fun longToIpv4(value: Long): String =
        "${(value shr 24) and 255}." +
            "${(value shr 16) and 255}." +
            "${(value shr 8) and 255}." +
            "${value and 255}"
}
