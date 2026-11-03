
package com.nocam.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object RtspAuditor {

    data class Result(
        val reachable: Boolean,
        val response: String,
        val methods: List<String>
    )

    suspend fun inspect(
        host: String,
        port: Int = 554
    ): Result = withContext(Dispatchers.IO) {

        require(port in 1..65535)

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 1200)
                socket.soTimeout = 1500

                val request =
                    "OPTIONS rtsp://$host:$port/ RTSP/1.0\r\n" +
                    "CSeq: 1\r\n" +
                    "User-Agent: NOCAM-Audit/1.0\r\n\r\n"

                socket.getOutputStream().write(request.toByteArray())
                socket.getOutputStream().flush()

                val reader = socket.getInputStream().bufferedReader()
                val status = reader.readLine().orEmpty()

                val headers = mutableListOf<String>()

                repeat(30) {
                    val line = reader.readLine() ?: return@repeat
                    if (line.isBlank()) return@repeat
                    headers.add(line)
                }

                val publicMethods = headers.firstOrNull {
                    it.startsWith("Public:", true)
                }?.substringAfter(":")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?: emptyList()

                Result(
                    reachable = status.startsWith("RTSP/"),
                    response = status,
                    methods = publicMethods
                )
            }
        } catch (e: Exception) {
            Result(
                reachable = false,
                response = "RTSP check inconclusive: ${e.javaClass.simpleName}",
                methods = emptyList()
            )
        }
    }
}
