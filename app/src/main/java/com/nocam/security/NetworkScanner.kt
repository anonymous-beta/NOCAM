
package com.nocam.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.time.Instant
import java.util.UUID
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object NetworkScanner {
    private val ports = listOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS/TCP",
        80 to "HTTP",
        81 to "HTTP alternate",
        110 to "POP3",
        139 to "NetBIOS",
        143 to "IMAP",
        443 to "HTTPS",
        445 to "SMB",
        554 to "RTSP",
        1883 to "MQTT",
        3306 to "MySQL",
        3389 to "RDP",
        5000 to "Device web",
        8000 to "Device service",
        8080 to "HTTP alternate",
        8443 to "HTTPS alternate",
        8554 to "RTSP alternate",
        8899 to "Device service",
        9000 to "Device service",
        37777 to "Possible DVR service"
    )

    suspend fun scan(
        scope: String,
        hosts: List<String>,
        onProgress: (completed: Int, total: Int, current: String) -> Unit
    ): ScanReport = withContext(Dispatchers.IO) {
        val started = Instant.now().toString()
        val scanId = UUID.randomUUID().toString()
        val total = hosts.size * ports.size
        var completed = 0
        val gate = Semaphore(permits = 20)

        val findings = coroutineScope {
            hosts.flatMap { host ->
                ports.map { (port, service) ->
                    async {
                        gate.withPermit {
                            val finding = probe(host, port, service)
                            val done = synchronized(ports) {
                                completed += 1
                                completed
                            }

                            withContext(Dispatchers.Main) {
                                onProgress(done, total, "$host:$port")
                            }

                            finding
                        }
                    }
                }
            }.awaitAll()
        }.sortedWith(
            compareBy<ScanFinding> { it.host }.thenBy { it.port }
        )

        ScanReport(
            scanId = scanId,
            startedAt = started,
            finishedAt = Instant.now().toString(),
            scope = scope,
            findings = findings,
            notes = listOf(
                "Targets were restricted to private RFC1918 IPv4 scope.",
                "TCP checks are non-exploitative and do not attempt credentials.",
                "A failed connection may indicate a closed, filtered, or unreachable port.",
                "ONVIF UDP discovery and SNMP UDP discovery are not implemented in this build.",
                "An open port is not proof of a vulnerability."
            )
        )
    }

    private fun probe(
        host: String,
        port: Int,
        service: String
    ): ScanFinding {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 550)
                socket.soTimeout = 650

                val detail = when (port) {
                    80, 81, 5000, 8000, 8080, 8899, 9000, 37777 ->
                        httpProbe(host, port)

                    443, 8443 -> tlsProbe(host, port)

                    554, 8554 -> rtspProbe(host, port)

                    else -> "TCP connection accepted; no active protocol identification."
                }

                ScanFinding(
                    host = host,
                    port = port,
                    service = service,
                    state = "OPEN",
                    detail = detail,
                    severity = if (port == 21 || port == 23) "REVIEW" else "INFO"
                )
            }
        } catch (e: Exception) {
            ScanFinding(
                host = host,
                port = port,
                service = service,
                state = "CLOSED_OR_FILTERED",
                detail = "${e.javaClass.simpleName}: ${e.message ?: "no response"}"
            )
        }
    }

    private fun httpProbe(host: String, port: Int): String {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
                socket.soTimeout = 700

                socket.getOutputStream().write(
                    (
                        "HEAD / HTTP/1.0\r\n" +
                            "Host: $host\r\n" +
                            "User-Agent: NOCAM-Audit/1.0\r\n" +
                            "Connection: close\r\n\r\n"
                        ).toByteArray()
                )
                socket.getOutputStream().flush()

                val line = socket.getInputStream()
                    .bufferedReader()
                    .readLine()
                    .orEmpty()

                if (line.startsWith("HTTP/")) {
                    "HTTP response: $line"
                } else {
                    "TCP service reachable; no standard HTTP status line."
                }
            }
        } catch (_: Exception) {
            "TCP connection accepted; HTTP fingerprint unavailable."
        }
    }

    private fun tlsProbe(host: String, port: Int): String {
        return try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory

            factory.createSocket().use { raw ->
                val socket = raw as SSLSocket
                socket.connect(InetSocketAddress(host, port), 700)
                socket.soTimeout = 900
                socket.startHandshake()

                val certificate = socket.session.peerCertificates.firstOrNull()
                val subject =
                    (certificate as? java.security.cert.X509Certificate)
                        ?.subjectX500Principal?.name
                        ?: "certificate unavailable"

                "TLS handshake succeeded; certificate subject: ${subject.take(180)}"
            }
        } catch (e: Exception) {
            "TLS check inconclusive: ${e.javaClass.simpleName}"
        }
    }

    private fun rtspProbe(host: String, port: Int): String {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
                socket.soTimeout = 700

                socket.getOutputStream().write(
                    (
                        "OPTIONS rtsp://$host:$port/ RTSP/1.0\r\n" +
                            "CSeq: 1\r\n" +
                            "User-Agent: NOCAM-Audit/1.0\r\n\r\n"
                        ).toByteArray()
                )
                socket.getOutputStream().flush()

                val line = socket.getInputStream()
                    .bufferedReader()
                    .readLine()
                    .orEmpty()

                if (line.startsWith("RTSP/")) {
                    "RTSP response: $line"
                } else {
                    "TCP service reachable; no RTSP status line."
                }
            }
        } catch (_: Exception) {
            "TCP connection accepted; RTSP check inconclusive."
        }
    }
}
