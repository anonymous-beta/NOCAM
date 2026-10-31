
package com.nocam.security

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val Bg = Color(0xFF090B08)
private val CardBg = Color(0xFF171B13)
private val Olive = Color(0xFF65783A)
private val Green = Color(0xFF9BD36B)
private val Amber = Color(0xFFFFC857)
private val Muted = Color(0xFFAAB29F)
private val Line = Color(0xFF343B2B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NocamApp() }
    }
}

@Composable
private fun NocamApp() {
    val scope = rememberCoroutineScope()

    var target by remember { mutableStateOf("192.168.1.1") }
    var authorized by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("READY // waiting for an authorized target") }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentTask by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<ScanReport?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) saveDocument(uri, report?.toJson().orEmpty())
    }

    val exportMarkdown = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) saveDocument(uri, report?.toMarkdown().orEmpty())
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green,
            secondary = Amber,
            background = Bg,
            surface = CardBg,
            onSurface = Color(0xFFE8EDE2),
            onBackground = Color(0xFFE8EDE2)
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Olive, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = "NOCAM security",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "NOCAM",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Text(
                                "NETWORK & CCTV AUDIT TOOLKIT",
                                color = Green,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "CREATED BY ANONYMOUS-BETA",
                                color = Muted,
                                fontSize = 9.sp
                            )
                        }

                        Text("V1.0", color = Amber, fontSize = 12.sp)
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("SCOPE", "Private LAN", Modifier.weight(1f))
                        Metric("ENGINE", "TCP probes", Modifier.weight(1f))
                        Metric("POLICY", "Safe mode", Modifier.weight(1f))
                    }
                }

                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(CardBg, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "MISSION CONFIGURATION",
                            color = Amber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            "Enter one private IPv4 host or a small private CIDR. " +
                                "CIDR scans are capped at 64 usable hosts.",
                            color = Muted,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = { Text("Host or CIDR") },
                            placeholder = { Text("192.168.1.10 or 192.168.1.0/26") },
                            singleLine = true,
                            enabled = !scanning,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = authorized,
                                onCheckedChange = { authorized = it },
                                enabled = !scanning
                            )
                            Text(
                                "I own this network or have explicit permission.",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            "No credential guessing, exploit execution, or remote camera viewing.",
                            color = Muted,
                            fontSize = 11.sp
                        )

                        if (scanning) {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                                color = Green,
                                trackColor = Line
                            )

                            Text(
                                "$currentTask • ${(progress * 100).toInt()}%",
                                color = Green,
                                fontSize = 11.sp
                            )
                        }

                        Text(status, color = Muted, fontSize = 12.sp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (!authorized) {
                                        status = "BLOCKED // confirm authorization first"
                                        return@Button
                                    }

                                    val hosts = try {
                                        TargetScope.parse(target)
                                    } catch (e: Exception) {
                                        status = "INVALID SCOPE // ${e.message}"
                                        return@Button
                                    }

                                    report = null
                                    progress = 0f
                                    scanning = true
                                    status = "SCANNING // non-invasive checks"

                                    scanJob = scope.launch {
                                        try {
                                            report = NetworkScanner.scan(
                                                scope = target.trim(),
                                                hosts = hosts
                                            ) { done, total, current ->
                                                progress =
                                                    if (total == 0) 1f
                                                    else done.toFloat() / total
                                                currentTask = current
                                            }

                                            status =
                                                "COMPLETE // ${report?.openCount ?: 0} open TCP port(s)"
                                        } catch (_: CancellationException) {
                                            status = "CANCELLED // scan stopped"
                                        } catch (e: Exception) {
                                            status =
                                                "SCAN ERROR // " +
                                                    (e.message ?: e.javaClass.simpleName)
                                        } finally {
                                            scanning = false
                                            scanJob = null
                                        }
                                    }
                                },
                                enabled = !scanning,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Olive
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(5.dp))
                                Text("Start audit")
                            }

                            OutlinedButton(
                                onClick = { scanJob?.cancel() },
                                enabled = scanning
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Text("Stop")
                            }
                        }
                    }
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "FINDINGS",
                            color = Green,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            "${report?.openCount ?: 0} OPEN",
                            color = Amber,
                            fontSize = 11.sp
                        )
                    }
                }

                val findings = report?.findings.orEmpty()

                if (report == null) {
                    item {
                        EmptyPanel(
                            "No report yet. Run an authorized audit to populate findings."
                        )
                    }
                } else if (findings.isEmpty()) {
                    item { EmptyPanel("The scan produced no findings.") }
                } else {
                    items(findings) { finding ->
                        FindingRow(finding)
                    }
                }

                if (report != null) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "EXPORT REPORT",
                                color = Amber,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Save the findings as JSON or Markdown.",
                                color = Muted,
                                fontSize = 12.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        exportJson.launch("nocam-report.json")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Description, null)
                                    Text(" JSON")
                                }

                                OutlinedButton(
                                    onClick = {
                                        exportMarkdown.launch("nocam-report.md")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Article, null)
                                    Text(" Markdown")
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = Line)

                    Text(
                        "NOCAM V1.0 • BY ANONYMOUS-BETA • AUTHORIZED ASSESSMENT ONLY",
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(bottom = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyPanel(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(14.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun FindingRow(finding: ScanFinding) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (finding.state == "OPEN") Green else Muted,
                        RoundedCornerShape(50)
                    )
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "${finding.host}:${finding.port}",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            Text(
                finding.state,
                color = if (finding.state == "OPEN") Green else Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(finding.service, color = Amber, fontSize = 12.sp)
        Text(finding.detail, color = Muted, fontSize = 11.sp)

        if (finding.severity == "REVIEW") {
            Text(
                "REVIEW: legacy service; verify whether it is needed.",
                color = Amber,
                fontSize = 11.sp
            )
        }
    }
}

private fun ComponentActivity.saveDocument(uri: Uri, content: String) {
    try {
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        }
    } catch (_: Exception) {
        // Document provider errors are handled by Android.
    }
}
