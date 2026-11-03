package com.nocam.security

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

// ----------------------------------------------------
// NOCAM DARK RED THEME
// ----------------------------------------------------

private val NocamBlack = Color(0xFF090707)
private val NocamBackground = Color(0xFF100A0B)
private val NocamPanel = Color(0xFF1B1113)
private val NocamPanelLight = Color(0xFF271518)
private val NocamRed = Color(0xFFB51F32)
private val NocamBrightRed = Color(0xFFEF3349)
private val NocamDarkRed = Color(0xFF64101D)
private val NocamAmber = Color(0xFFFFB74D)
private val NocamText = Color(0xFFF4EDEE)
private val NocamMuted = Color(0xFFAA999C)
private val NocamBorder = Color(0xFF49232A)

// ----------------------------------------------------
// ACTIVITY
// ----------------------------------------------------

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

// ----------------------------------------------------
// THEME
// ----------------------------------------------------

@Composable
private fun NocamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = NocamBrightRed,
            onPrimary = Color.White,
            secondary = NocamAmber,
            background = NocamBlack,
            surface = NocamPanel,
            onBackground = NocamText,
            onSurface = NocamText,
            error = NocamBrightRed
        ),
        content = content
    )
}

// ----------------------------------------------------
// MAIN SCREEN
// ----------------------------------------------------

@Composable
private fun NocamScreen() {

    val activity = androidx.compose.ui.platform.LocalContext.current
        as ComponentActivity

    val coroutineScope = rememberCoroutineScope()

    var target by remember {
        mutableStateOf("192.168.1.1")
    }

    var publicMode by remember {
        mutableStateOf(false)
    }

    var authorized by remember {
        mutableStateOf(false)
    }

    var running by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("SYSTEM READY")
    }

    var progress by remember {
        mutableStateOf(0)
    }

    var totalChecks by remember {
        mutableStateOf(0)
    }

    var findings by remember {
        mutableStateOf<List<ScanFinding>>(emptyList())
    }

    var scanJob by remember {
        mutableStateOf<Job?>(null)
    }

    var report by remember {
        mutableStateOf<ScanReport?>(null)
    }

    var exportFormat by remember {
        mutableStateOf("MARKDOWN")
    }

    var reportText by remember {
        mutableStateOf("")
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->

        if (uri != null) {
            coroutineScope.launch {
                try {
                    val output =
                        activity.contentResolver.openOutputStream(uri)
                            ?: throw IOException(
                                "Unable to open selected file."
                            )

                    output.use {
                        it.write(
                            reportText.toByteArray(Charsets.UTF_8)
                        )
                    }

                    status = "REPORT EXPORTED SUCCESSFULLY"

                } catch (exception: Exception) {
                    status = "EXPORT FAILED: ${
                        exception.message ?: "I/O error"
                    }"
                }
            }
        }
    }

    // ------------------------------------------------
    // SCREEN LAYOUT
    // ------------------------------------------------

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = NocamBackground
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NocamBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {

            // HEADER WITH ACTUAL NOCAM LOGO

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .width(55.dp)
                        .height(55.dp)
                        .background(
                            NocamDarkRed,
                            RoundedCornerShape(15.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Image(
                        painter = painterResource(
                            id = R.drawable.nocam_logo
                        ),
                        contentDescription = "NOCAM Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(45.dp)
                            .height(45.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "NOCAM",
                        color = NocamText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "NETWORK CAMERA AUDIT",
                        color = NocamBrightRed,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "VERSION",
                        color = NocamMuted,
                        fontSize = 9.sp
                    )

                    Text(
                        "1.0.0",
                        color = NocamAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(
                color = NocamBorder,
                thickness = 1.dp
            )

            // TARGET CONFIGURATION

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = NocamPanel
                )
            ) {

                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    SectionTitle("TARGET CONFIGURATION")

                    Text(
                        "AUDIT MODE",
                        color = NocamMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected = !publicMode,
                            onClick = {
                                publicMode = false
                                authorized = false
                            },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = NocamBrightRed
                            )
                        )

                        Text(
                            "PRIVATE NETWORK",
                            color = NocamText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.width(10.dp))

                        RadioButton(
                            selected = publicMode,
                            onClick = {
                                publicMode = true
                                authorized = false
                            },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                selectedColor = NocamBrightRed
                            )
                        )

                        Text(
                            "PUBLIC TARGET",
                            color = NocamText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = target,
                        onValueChange = {
                            target = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                if (publicMode) {
                                    "Public IP or hostname"
                                } else {
                                    "Private IP or CIDR"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                if (publicMode) {
                                    "203.0.113.10"
                                } else {
                                    "192.168.1.1"
                                }
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        )
                    )

                    Text(
                        text = if (publicMode) {
                            "Single public IPv4 address or hostname. Public CIDR scanning is disabled."
                        } else {
                            "Private RFC1918 IPv4 address or supported CIDR range."
                        },
                        color = NocamMuted,
                        fontSize = 10.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Checkbox(
                            checked = authorized,
                            onCheckedChange = {
                                authorized = it
                            },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(
                                checkedColor = NocamBrightRed,
                                uncheckedColor = NocamMuted
                            )
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                "I HAVE AUTHORIZATION TO AUDIT THIS TARGET",
                                color = NocamText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Required before any network checks.",
                                color = NocamMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // START AND STOP

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {

                        Button(
                            onClick = {

                                if (!authorized) {
                                    status = "AUTHORIZATION REQUIRED"

                                } else if (!running) {

                                    running = true
                                    findings = emptyList()
                                    report = null
                                    reportText = ""
                                    progress = 0
                                    totalChecks = 0
                                    status = "VALIDATING TARGET"

                                    scanJob = coroutineScope.launch {

                                        try {

                                            val scanScope = target.trim()

                                            val hosts = if (publicMode) {

                                                val validation =
                                                    withContext(Dispatchers.IO) {
                                                        AuthorizedPublicTarget
                                                            .validate(scanScope)
                                                    }

                                                if (!validation.valid) {
                                                    throw IllegalArgumentException(
                                                        validation.message
                                                    )
                                                }

                                                validation.addresses

                                            } else {

                                                TargetScope.parse(scanScope)
                                            }

                                            status = "STARTING AUDIT"

                                            val scanReport =
                                                NetworkScanner.scan(
                                                    scope = scanScope,
                                                    hosts = hosts,
                                                    onProgress = {
                                                            completed,
                                                            total,
                                                            current ->

                                                        progress = completed
                                                        totalChecks = total

                                                        status =
                                                            "CHECKING $current"
                                                    }
                                                )

                                            report = scanReport
                                            findings = scanReport.findings

                                            reportText =
                                                ReportExporter.markdown(
                                                    scanReport
                                                )

                                            status =
                                                "AUDIT COMPLETE • " +
                                                "${scanReport.openCount} OPEN PORTS"

                                        } catch (
                                            exception: CancellationException
                                        ) {

                                            status = "AUDIT CANCELLED"
                                            throw exception

                                        } catch (
                                            exception: Exception
                                        ) {

                                            status =
                                                "AUDIT FAILED: " +
                                                (
                                                    exception.message
                                                        ?: "Unknown error"
                                                )

                                        } finally {

                                            running = false
                                            scanJob = null
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !running,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NocamDarkRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {

                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(5.dp))

                            Text(
                                "START AUDIT",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                scanJob?.cancel()
                                status = "CANCELLING AUDIT"
                            },
                            enabled = running,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NocamPanelLight,
                                contentColor = NocamBrightRed
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {

                            Icon(
                                Icons.Default.Stop,
                                contentDescription = "Stop audit"
                            )
                        }
                    }
                }
            }

            // LIVE STATUS

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NocamPanel
                ),
                shape = RoundedCornerShape(14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        if (running) {

                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(17.dp)
                                    .height(17.dp),
                                color = NocamBrightRed,
                                strokeWidth = 2.dp
                            )

                        } else {

                            Box(
                                modifier = Modifier
                                    .width(9.dp)
                                    .height(9.dp)
                                    .background(
                                        NocamBrightRed,
                                        RoundedCornerShape(50)
                                    )
                            )
                        }

                        Text(
                            status,
                            color = NocamBrightRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    if (running && totalChecks > 0) {

                        LinearProgressIndicator(
                            progress = progress.toFloat() /
                                totalChecks.coerceAtLeast(1),
                            modifier = Modifier.fillMaxWidth(),
                            color = NocamBrightRed,
                            trackColor = NocamPanelLight
                        )

                        Text(
                            "$progress / $totalChecks checks",
                            color = NocamMuted,
                            fontSize = 10.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Metric(
                            "FINDINGS",
                            findings.size.toString()
                        )

                        Metric(
                            "OPEN PORTS",
                            findings.count {
                                it.state == "OPEN"
                            }.toString()
                        )

                        Metric(
                            "HOSTS",
                            findings.map {
                                it.host
                            }.distinct().size.toString()
                        )
                    }
                }
            }

            // EXPORT SECTION

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = NocamPanel
                ),
                shape = RoundedCornerShape(14.dp)
            ) {

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    SectionTitle("REPORT EXPORT")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {

                        listOf(
                            "MARKDOWN",
                            "JSON",
                            "SARIF"
                        ).forEach { format ->

                            Button(
                                onClick = {
                                    exportFormat = format
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (exportFormat == format) {
                                            NocamDarkRed
                                        } else {
                                            NocamPanelLight
                                        },
                                    contentColor = NocamText
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {

                                Text(
                                    format,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {

                            val currentReport = report

                            if (currentReport == null) {
                                status = "COMPLETE AN AUDIT FIRST"
                            } else {

                                reportText = when (exportFormat) {
                                    "JSON" ->
                                        ReportExporter.json(currentReport)

                                    "SARIF" ->
                                        ReportExporter.sarif(currentReport)

                                    else ->
                                        ReportExporter.markdown(currentReport)
                                }

                                val fileName = when (exportFormat) {
                                    "JSON" -> "nocam-report.json"
                                    "SARIF" -> "nocam-report.sarif"
                                    else -> "nocam-report.md"
                                }

                                exportLauncher.launch(fileName)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !running && report != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NocamDarkRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(9.dp)
                    ) {

                        Icon(
                            Icons.Default.Article,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(7.dp))

                        Text("EXPORT REPORT")
                    }
                }
            }

            // FINDINGS

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    Icons.Default.BugReport,
                    contentDescription = null,
                    tint = NocamBrightRed
                )

                Spacer(Modifier.width(7.dp))

                Text(
                    "AUDIT FINDINGS",
                    color = NocamText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${findings.size} RESULTS",
                    color = NocamMuted,
                    fontSize = 10.sp
                )
            }

            if (findings.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        if (running) {
                            "AUDIT IN PROGRESS..."
                        } else {
                            "NO FINDINGS YET\n\nConfigure a target to begin."
                        },
                        color = NocamMuted,
                        fontSize = 12.sp
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    items(
                        items = findings,
                        key = {
                            "${it.host}:${it.port}:${it.state}"
                        }
                    ) { finding ->

                        FindingRow(finding)
                    }
                }
            }

            // BRANDING FOOTER

            HorizontalDivider(
                color = NocamBorder
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {

                Text(
                    "NOCAM • NETWORK CAMERA AUDIT",
                    color = NocamMuted,
                    fontSize = 9.sp
                )

                Text(
                    "MADE BY ANONYMOUS-BETA",
                    color = NocamBrightRed,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )

                Text(
                    "github.com/anonymous-beta",
                    color = NocamAmber,
                    fontSize = 10.sp,
                    modifier = Modifier.clickable {
                        activity.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    "https://github.com/anonymous-beta"
                                )
                            )
                        )
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// REUSABLE UI COMPONENTS
// ----------------------------------------------------

@Composable
private fun SectionTitle(title: String) {

    Text(
        title,
        color = NocamBrightRed,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        letterSpacing = 1.1.sp
    )
}

@Composable
private fun Metric(
    label: String,
    value: String
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            value,
            color = NocamBrightRed,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            label,
            color = NocamMuted,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun FindingRow(
    finding: ScanFinding
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(
            containerColor = NocamPanel
        )
    ) {

        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "${finding.host}:${finding.port}",
                    color = NocamText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    finding.state,
                    color = if (finding.state == "OPEN") {
                        NocamBrightRed
                    } else {
                        NocamMuted
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                finding.service,
                color = NocamAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                finding.detail,
                color = NocamMuted,
                fontSize = 11.sp
            )

            if (finding.severity != "INFO") {

                Text(
                    "REVIEW: ${finding.severity}",
                    color = NocamBrightRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}