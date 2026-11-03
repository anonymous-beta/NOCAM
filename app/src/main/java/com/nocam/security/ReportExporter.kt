
package com.nocam.security

object ReportExporter {

    fun json(report: ScanReport): String =
        report.toJson()

    fun markdown(report: ScanReport): String =
        report.toMarkdown()

    fun sarif(report: ScanReport): String {

        val results = report.findings
            .filter { it.state == "OPEN" }
            .joinToString(",") { finding ->

                """
                {
                  "ruleId": "NOCAM-OPEN-PORT-${finding.port}",
                  "level": "${severity(finding.severity)}",
                  "message": {
                    "text": "${escape(finding.host)}:${finding.port} ${escape(finding.service)} — ${escape(finding.detail)}"
                  },
                  "locations": [{
                    "physicalLocation": {
                      "artifactLocation": {
                        "uri": "network://${escape(finding.host)}"
                      }
                    }
                  }]
                }
                """.trimIndent()
            }

        return """
        {
          "version": "2.1.0",
          "${'$'}schema": "https://json.schemastore.org/sarif-2.1.0.json",
          "runs": [{
            "tool": {
              "driver": {
                "name": "NOCAM",
                "informationUri": "https://github.com/anonymous-beta/NOCAM",
                "rules": []
              }
            },
            "results": [$results]
          }]
        }
        """.trimIndent()
    }

    private fun severity(value: String): String =
        when (value.uppercase()) {
            "CRITICAL", "HIGH" -> "error"
            "MEDIUM", "REVIEW" -> "warning"
            else -> "note"
        }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
