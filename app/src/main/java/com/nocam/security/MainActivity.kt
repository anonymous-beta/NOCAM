package com.nocam.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

private val NocamBlack = Color(0xFF080A09)
private val NocamPanel = Color(0xFF141915)
private val NocamGreen = Color(0xFF8BAF42)
private val NocamAmber = Color(0xFFFFB74D)
private val NocamText = Color(0xFFE8EDE7)
private val NocamMuted = Color(0xFF9AA69B)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NocamTheme {
                NocamScreen()
            }
        }
    }
}

@Composable
private fun NocamTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = NocamGreen,
        secondary = NocamAmber,
        background = NocamBlack,
        surface = NocamPanel,
        onPrimary = Color.Black,
        onBackground = NocamText,
        onSurface = NocamText
    )

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

@Composable
private fun NocamScreen() {
    val activity = androidx.compose.ui.platform.LocalContext.current
        as ComponentActivity

    val scope = rememberCoroutineScope()

    var target by remember { mutableStateOf("192.168.1.1") }
    var allowNetworkScan by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf("READY • AUTHORIZED NETWORKS ONLY")
    }
    var findings by remember {
        mutableStateOf<List<ScanFinding>>(emptyList())
    }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var reportText by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    activity.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(reportText.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("Could not open the selected file.")

                    status = "REPORT EXPORTED"
                } catch (exception: Exception) {
                    status = "EXPORT FAILED: ${exception.message ?: "I/O error"}"
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NocamBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .height(54.dp)
                        .background(
                            NocamGreen.copy(alpha = 0.15f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "NOCAM security",
                        tint = NocamGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOCAM",
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        color = NocamText
                    )
                    Text(
                        text = "NETWORK CAMERA AUDIT",
                        fontSize = 11.sp,
                        color = NocamGreen,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = "v1.0",
                    color = NocamMuted,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = NocamGreen.copy(alpha = 0.35f))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NocamPanel
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TARGET CONFIGURATION",
                        color = NocamGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Private IP or CIDR") },
                        placeholder = { Text("192.168.1.1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allowNetworkScan,
                            onCheckedChange = {
                                allowNetworkScan = it
                            }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "I am authorized to scan this network",
                                fontSize = 13.sp,
                                color = NocamText
                            )
                            Text(
                                "Private IPv4 targets only",
                                fontSize = 11.sp,
                                color = NocamMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!allowNetworkScan) {
                                    status = "CONFIRM AUTHORIZATION FIRST"
                                } else if (running) {
                                    status = "SCAN ALREADY RUNNING"
                                } else {
                                    running = true
                                    findings = emptyList()
                                    status = "SCANNING AUTHORIZED TARGET"

                                    scanJob = scope.launch {
                                        try {
                                            val hosts = TargetScope.parse(target)

                                            val scanner = NetworkScanner()
                                            val results = scanner.scan(
                                                hosts = hosts,
                                                onProgress = { host, port ->
                                                    status = "CHECKING $host:$port"
                                                }
                                            )

                                            findings = results
                                            status = "SCAN COMPLETE • ${results.size} RESULTS"

                                            reportText = buildString {
                                                appendLine("# NOCAM Scan Report")
                                                appendLine()
                                                appendLine("Target: $target")
                                                appendLine("Results: ${results.size}")
                                                appendLine()
                                                results.forEach { finding ->
                                                    appendLine(
                                                        "- ${finding.host}:${finding.port} " +
                                                            "| ${finding.service} " +
                                                            "| ${finding.state} " +
                                                            "| ${finding.detail}"
                                                    )
                                                }
                                            }
                                        } catch (exception: Exception) {
                                            status = "SCAN FAILED: " +
                                                (exception.message ?: "Invalid target")
                                        } finally {
                                            running = false
                                            scanJob = null
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !running
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("START SCAN")
                        }

                        Button(
                            onClick = {
                                scanJob?.cancel()
                                scanJob = null
                                running = false
                                status = "SCAN CANCELLED"
                            },
                            enabled = running
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Cancel scan"
                            )
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = NocamPanel
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (running) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(20.dp),
                                color = NocamGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .height(9.dp)
                                    .background(
                                        NocamGreen,
                                        RoundedCornerShape(50)
                                    )
                            )
                        }

                        Text(
                            text = status,
                            color = if (status.contains("FAILED")) {
                                NocamAmber
                            } else {
                                NocamGreen
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "RESULTS • ${findings.size}",
                        color = NocamMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (reportText.isBlank()) {
                            status = "RUN A SCAN BEFORE EXPORTING"
                        } else {
                            exportLauncher.launch("nocam-report.md")
                        }
                    },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("EXPORT MARKDOWN REPORT")
                }
            }

            Text(
                text = "FINDINGS",
                color = NocamGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            if (findings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (running) {
                            "Scanning authorized targets…"
                        } else {
                            "No results yet. Configure a private target and start a scan."
                        },
                        color = NocamMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(findings) { finding ->
                        FindingRow(finding)
                    }
                }
            }

            Text(
                text = "NOCAM • AUTHORIZED SECURITY TESTING ONLY",
                color = NocamMuted,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun FindingRow(finding: ScanFinding) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = NocamPanel
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${finding.host}:${finding.port}",
                    color = NocamText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = finding.state,
                    color = if (finding.state.equals("OPEN", true)) {
                        NocamGreen
                    } else {
                        NocamMuted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = finding.service,
                color = NocamAmber,
                fontSize = 12.sp
            )

            Text(
                text = finding.detail,
                color = NocamMuted,
                fontSize = 12.sp
            )
        }
    }
}