package com.sparrow.skyhigh_16kb_doctor

import android.content.Intent
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var downloadButton: Button
    private lateinit var shareButton: Button
    private var currentReportContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupClickListeners()
        loadReport()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        downloadButton = findViewById(R.id.btnDownload)
        shareButton = findViewById(R.id.btnShare)
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }
    }

    private fun setupClickListeners() {
        downloadButton.setOnClickListener { downloadReportAsPdf() }
        shareButton.setOnClickListener { shareReport() }
    }

    private fun loadReport() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reportContent = loadReportFromAssets()

                withContext(Dispatchers.Main) {
                    if (reportContent != null) {
                        currentReportContent = reportContent
                        loadReportContent(reportContent)
                        enableActionButtons(true)
                    } else {
                        currentReportContent = null
                        loadInstructionsContent()
                        enableActionButtons(false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentReportContent = null
                    loadInstructionsContent()
                    enableActionButtons(false)
                }
            }
        }
    }

    private suspend fun loadReportFromAssets(): String? = withContext(Dispatchers.IO) {
        try {
            assets.open("skyhigh_report.html").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not load report from assets: ${e.message}")
            null
        }
    }

    private fun loadReportContent(htmlContent: String) {
        webView.clearCache(true)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadInstructionsContent() {
        val instructionsHtml = """
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        color: white;
                    }
                    .container {
                        background: rgba(255,255,255,0.95);
                        padding: 30px;
                        border-radius: 16px;
                        box-shadow: 0 8px 32px rgba(0,0,0,0.1);
                        max-width: 500px;
                        margin: 0 auto;
                        color: #333;
                        text-align: center;
                    }
                    .logo {
                        font-size: 3em;
                        margin-bottom: 20px;
                    }
                    h2 { color: #2196F3; margin-bottom: 20px; }
                    .instruction {
                        background: #f8f9fa;
                        padding: 20px;
                        border-radius: 12px;
                        margin: 20px 0;
                        border-left: 4px solid #2196F3;
                    }
                    code {
                        background: #e9ecef;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-family: 'Courier New', monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🚀</div>
                    <h2>SkyHigh 16KB Doctor</h2>
                    <p>Analyze native libraries for 16KB page size compatibility</p>

                    <div class="instruction">
                        <strong>📋 To generate a report:</strong><br><br>
                        Run: <code>./gradlew :app:runWithFreshReport</code><br><br>
                        <em>The report will be automatically generated and displayed here.</em>
                    </div>

                    <p style="color: #666; font-size: 14px;">
                        This tool identifies .so files that may not work on Android 15+ devices with 16KB memory pages.
                    </p>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, instructionsHtml, "text/html", "UTF-8", null)
    }

    private fun downloadReportAsPdf() {
        currentReportContent?.let { content ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pdfFile = createPdfFromHtml(content)
                    withContext(Dispatchers.Main) {
                        showToast("📄 PDF saved to Downloads: ${pdfFile.name}")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("❌ Error creating PDF: ${e.message}")
                    }
                }
            }
        } ?: showToast("❌ No report available to download")
    }

    private fun shareReport() {
        currentReportContent?.let { content ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pdfFile = createPdfFromHtml(content)
                    withContext(Dispatchers.Main) {
                        sharePdfFile(pdfFile)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("❌ Error sharing report: ${e.message}")
                    }
                }
            }
        } ?: showToast("❌ No report available to share")
    }

    private suspend fun createPdfFromHtml(htmlContent: String): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SkyHigh_16KB_Report_$timestamp.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val pdfFile = File(downloadsDir, fileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1190, 842, 1).create() // A3 Landscape for more space
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint().apply {
            textSize = 9f
            color = android.graphics.Color.BLACK
            isAntiAlias = true
        }

        val titlePaint = android.graphics.Paint().apply {
            textSize = 20f
            color = android.graphics.Color.parseColor("#2196F3")
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = android.graphics.Paint().apply {
            textSize = 10f
            color = android.graphics.Color.parseColor("#2196F3")
            isFakeBoldText = true
            isAntiAlias = true
        }

        val smallPaint = android.graphics.Paint().apply {
            textSize = 8f
            color = android.graphics.Color.GRAY
            isAntiAlias = true
        }

        // Draw title
        canvas.drawText("SkyHigh 16KB Doctor Report", 30f, 40f, titlePaint)

        // Draw timestamp
        val timestampText = "Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
        canvas.drawText(timestampText, 30f, 60f, smallPaint)

        // Parse HTML content and extract all table data
        val tableData = parseHtmlTable(htmlContent)

        var y = 90f
        val lineHeight = 16f
        val rowHeight = 32f // Double height for two-line text
        val leftMargin = 30f

        // Initialize variables for multi-page support
        var currentPage = page
        var currentCanvas = canvas
        var pageNumber = 1

        if (tableData.isNotEmpty()) {
            // Draw table headers with wider spacing for full text
            canvas.drawText("APK", leftMargin, y, headerPaint)
            canvas.drawText("Library Path", leftMargin + 100f, y, headerPaint)
            canvas.drawText("ABI", leftMargin + 350f, y, headerPaint)
            canvas.drawText("p_align", leftMargin + 450f, y, headerPaint)
            canvas.drawText("16KB Compatible", leftMargin + 520f, y, headerPaint)
            canvas.drawText("Owner", leftMargin + 650f, y, headerPaint)
            canvas.drawText("Remediation", leftMargin + 850f, y, headerPaint)

            y += lineHeight + 5f

            // Draw a line under headers
            canvas.drawLine(leftMargin, y, 1150f, y, headerPaint)
            y += 10f

            // Draw all table rows (handle multiple pages if needed)

            tableData.forEach { row ->
                if (y > 720f) { // Start new page if we're near the bottom (adjusted for row height)
                    document.finishPage(currentPage)
                    pageNumber++
                    val newPageInfo = PdfDocument.PageInfo.Builder(1190, 842, pageNumber).create()
                    currentPage = document.startPage(newPageInfo)
                    currentCanvas = currentPage.canvas
                    y = 40f

                    // Redraw headers on new page
                    currentCanvas.drawText("SkyHigh 16KB Doctor Report (Page $pageNumber)", 30f, y, titlePaint)
                    y += 30f
                    currentCanvas.drawText("APK", leftMargin, y, headerPaint)
                    currentCanvas.drawText("Library Path", leftMargin + 100f, y, headerPaint)
                    currentCanvas.drawText("ABI", leftMargin + 350f, y, headerPaint)
                    currentCanvas.drawText("p_align", leftMargin + 450f, y, headerPaint)
                    currentCanvas.drawText("16KB Compatible", leftMargin + 520f, y, headerPaint)
                    currentCanvas.drawText("Owner", leftMargin + 650f, y, headerPaint)
                    currentCanvas.drawText("Remediation", leftMargin + 850f, y, headerPaint)
                    y += lineHeight + 5f
                    currentCanvas.drawLine(leftMargin, y, 1150f, y, headerPaint)
                    y += 10f
                }

                if (row.size >= 7) {
                    // APK name (show full name)
                    drawMultiLineText(currentCanvas, row[0], leftMargin, y, 90f, paint)

                    // Library path (show full path in multiple lines)
                    drawMultiLineText(currentCanvas, row[1], leftMargin + 100f, y, 240f, paint)

                    // ABI
                    currentCanvas.drawText(row[2], leftMargin + 350f, y, paint)

                    // p_align value
                    currentCanvas.drawText(row[3], leftMargin + 450f, y, paint)

                    // 16KB Compatible status with color
                    val isCompatible = row[4].lowercase() == "true"
                    val statusPaint = android.graphics.Paint().apply {
                        textSize = 9f
                        color = if (isCompatible) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#F44336")
                        isFakeBoldText = true
                        isAntiAlias = true
                    }
                    currentCanvas.drawText(if (isCompatible) "YES" else "NO", leftMargin + 520f, y, statusPaint)

                    // Owner (show full text in multiple lines)
                    drawMultiLineText(currentCanvas, row[5], leftMargin + 650f, y, 190f, paint)

                    // Remediation (show full text in multiple lines)
                    drawMultiLineText(currentCanvas, row[6], leftMargin + 850f, y, 300f, paint)
                }
                y += rowHeight
            }
        } else {
            canvas.drawText("No native libraries found or all libraries are 16KB compatible!", leftMargin, y, paint)
        }

        // Draw footer on last page
        val footerY = 800f
        currentCanvas.drawText("Generated by SkyHigh 16KB Doctor Plugin", leftMargin, footerY, smallPaint)
        currentCanvas.drawText("This report identifies native libraries that may not be compatible with Android 15+ devices using 16KB memory pages.", leftMargin, footerY + 12f, smallPaint)

        document.finishPage(currentPage)

        FileOutputStream(pdfFile).use { document.writeTo(it) }
        document.close()

        pdfFile
    }

    private fun parseHtmlTable(htmlContent: String): List<List<String>> {
        val tableData = mutableListOf<List<String>>()

        // Extract table rows, excluding header row
        val rowPattern = "<tr[^>]*>(.*?)</tr>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val cellPattern = "<td[^>]*>(.*?)</td>".toRegex(RegexOption.DOT_MATCHES_ALL)

        rowPattern.findAll(htmlContent).forEach { rowMatch ->
            val rowContent = rowMatch.groupValues[1]

            // Skip header rows (they contain <th> tags)
            if (!rowContent.contains("<th")) {
                val cells = cellPattern.findAll(rowContent).map { cellMatch ->
                    cellMatch.groupValues[1]
                        .replace("<[^>]*>".toRegex(), "") // Remove HTML tags
                        .replace("&nbsp;", " ") // Replace HTML entities
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()
                }.toList()

                // Only add rows that have the expected number of columns (7 for SkyHigh report)
                if (cells.size >= 7) {
                    tableData.add(cells)
                } else if (cells.size >= 4) {
                    // Handle cases where some columns might be missing, pad with empty strings
                    val paddedCells = cells.toMutableList()
                    while (paddedCells.size < 7) {
                        paddedCells.add("")
                    }
                    tableData.add(paddedCells)
                }
            }
        }

        return tableData
    }



    private fun sharePdfFile(pdfFile: File) {
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SkyHigh 16KB Doctor Report")
            putExtra(Intent.EXTRA_TEXT, "16KB page size compatibility report for Android app")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Report"))
    }

    private fun enableActionButtons(enabled: Boolean) {
        downloadButton.isEnabled = enabled
        shareButton.isEnabled = enabled
        downloadButton.alpha = if (enabled) 1.0f else 0.5f
        shareButton.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun drawMultiLineText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: android.graphics.Paint) {
        if (text.isEmpty()) return

        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        val lineHeight = 14f

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth <= maxWidth) {
                currentLine = testLine
            } else {
                // Draw current line and start new line
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, currentY, paint)
                    currentY += lineHeight
                    currentLine = word
                } else {
                    // Single word is too long, truncate it
                    val truncatedWord = truncateTextToFit(word, maxWidth, paint)
                    canvas.drawText(truncatedWord, x, currentY, paint)
                    return
                }
            }
        }

        // Draw the last line
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    private fun truncateTextToFit(text: String, maxWidth: Float, paint: android.graphics.Paint): String {
        if (paint.measureText(text) <= maxWidth) return text

        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isNotEmpty()) "$truncated..." else text.take(1)
    }
}