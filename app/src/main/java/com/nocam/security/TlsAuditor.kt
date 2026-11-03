
package com.nocam.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object TlsAuditor {

    data class Result(
        val successful: Boolean,
        val protocol: String,
        val cipherSuite: String,
        val certificateSubject: String,
        val certificateExpiry: String,
        val detail: String
    )

    suspend fun inspect(
        host: String,
        port: Int = 443
    ): Result = withContext(Dispatchers.IO) {

        require(port in 1..65535)

        try {
            val factory = SSLSocketFactory.getDefault() as SSLSocketFactory

            factory.createSocket().use { raw ->
                val socket = raw as SSLSocket
                socket.connect(InetSocketAddress(host, port), 2000)
                socket.soTimeout = 2000
                socket.startHandshake()

                val certificate =
                    socket.session.peerCertificates.firstOrNull()
                        as? java.security.cert.X509Certificate

                Result(
                    successful = true,
                    protocol = socket.session.protocol,
                    cipherSuite = socket.session.cipherSuite,
                    certificateSubject =
                        certificate?.subjectX500Principal?.name ?: "Unknown",
                    certificateExpiry =
                        certificate?.notAfter?.toString() ?: "Unknown",
                    detail = "TLS handshake completed using Android's default trust validation."
                )
            }
        } catch (e: Exception) {
            Result(
                successful = false,
                protocol = "Unknown",
                cipherSuite = "Unknown",
                certificateSubject = "Unavailable",
                certificateExpiry = "Unavailable",
                detail = "${e.javaClass.simpleName}: ${e.message ?: "TLS check failed"}"
            )
        }
    }
}
