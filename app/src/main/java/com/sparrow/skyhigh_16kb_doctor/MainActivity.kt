package com.sparrow.skyhigh_16kb_doctor

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
        }
    }

    private fun setupClickListeners() {
        downloadButton.setOnClickListener { downloadReportAsPdf() }
        shareButton.setOnClickListener { shareReport() }
    }

    private fun loadReport() {
        CoroutineScope(Dispatchers.IO).launch {
            val reportContent = try {
                assets.open("skyhigh_report.html").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.w("MainActivity", "Could not load report: ${e.message}")
                null
            }

            withContext(Dispatchers.Main) {
                if (reportContent != null) {
                    currentReportContent = reportContent
                    webView.loadDataWithBaseURL(null, reportContent, "text/html", "UTF-8", null)
                    enableActionButtons(true)
                } else {
                    loadInstructionsContent()
                    enableActionButtons(false)
                }
            }
        }
    }

    private fun loadInstructionsContent() {
        val instructionsHtml = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 20px;
                    }
                    .container {
                        background: rgba(255,255,255,0.95);
                        padding: 40px;
                        border-radius: 20px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        max-width: 500px;
                        text-align: center;
                        backdrop-filter: blur(10px);
                    }
                    .logo { font-size: 4em; margin-bottom: 20px; }
                    h1 { color: #2196F3; margin-bottom: 10px; font-size: 1.8em; }
                    .subtitle { color: #666; margin-bottom: 30px; font-size: 1.1em; }
                    .instruction {
                        background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
                        padding: 25px;
                        border-radius: 15px;
                        margin: 25px 0;
                        border-left: 5px solid #2196F3;
                        box-shadow: 0 5px 15px rgba(0,0,0,0.05);
                    }
                    code {
                        background: #2c3e50;
                        color: #ecf0f1;
                        padding: 8px 12px;
                        border-radius: 8px;
                        font-family: 'Courier New', monospace;
                        font-weight: bold;
                        display: inline-block;
                        margin: 10px 0;
                    }
                    .footer { color: #888; font-size: 0.9em; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🚀</div>
                    <h1>SkyHigh 16KB Doctor</h1>
                    <p class="subtitle">Analyze native libraries for 16KB page size compatibility</p>

                    <div class="instruction">
                        <strong>📋 To generate a report:</strong><br><br>
                        <code>./gradlew :app:runWithFreshReport</code><br><br>
                        <em>The report will be automatically generated and displayed here.</em>
                    </div>

                    <p class="footer">
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
                        showToast("📄 PDF saved: ${pdfFile.name}")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("❌ Error creating PDF: ${e.message}")
                    }
                }
            }
        } ?: showToast("❌ No report available")
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
                        showToast("❌ Error sharing: ${e.message}")
                    }
                }
            }
        } ?: showToast("❌ No report available")
    }

    private suspend fun createPdfFromHtml(htmlContent: String): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SkyHigh_16KB_Report_$timestamp.pdf"
        val pdfFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1190, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Paint configurations
        val titlePaint = Paint().apply {
            textSize = 20f
            color = Color.parseColor("#2196F3")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#2196F3")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            textSize = 9f
            color = Color.BLACK
            isAntiAlias = true
        }
        val smallPaint = Paint().apply {
            textSize = 8f
            color = Color.GRAY
            isAntiAlias = true
        }

        // Draw content
        canvas.drawText("SkyHigh 16KB Doctor Report", 30f, 40f, titlePaint)
        canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", 30f, 60f, smallPaint)

        val tableData = parseHtmlTable(htmlContent)
        drawTable(canvas, tableData, headerPaint, textPaint)

        // Footer
        canvas.drawText("Generated by SkyHigh 16KB Doctor Plugin", 30f, 800f, smallPaint)

        document.finishPage(page)
        FileOutputStream(pdfFile).use { document.writeTo(it) }
        document.close()

        pdfFile
    }

    private fun drawTable(canvas: Canvas, tableData: List<List<String>>, headerPaint: Paint, textPaint: Paint) {
        var y = 90f
        val leftMargin = 30f
        val columnWidths = floatArrayOf(100f, 250f, 80f, 80f, 120f, 200f, 300f)
        val headers = arrayOf("APK", "Library Path", "ABI", "p_align", "16KB Compatible", "Owner", "Remediation")

        if (tableData.isNotEmpty()) {
            // Draw headers
            var x = leftMargin
            headers.forEachIndexed { index, header ->
                canvas.drawText(header, x, y, headerPaint)
                x += columnWidths[index]
            }
            y += 20f
            canvas.drawLine(leftMargin, y, 1150f, y, headerPaint)
            y += 15f

            // Draw data rows with multi-line support
            tableData.forEach { row ->
                if (row.size >= 7) {
                    val rowStartY = y
                    var maxRowHeight = 0f

                    // Calculate the height needed for this row by checking all cells
                    row.take(7).forEachIndexed { index, cell ->
                        val cellHeight = calculateTextHeight(cell, columnWidths[index], textPaint)
                        if (cellHeight > maxRowHeight) {
                            maxRowHeight = cellHeight
                        }
                    }

                    // Draw each cell with proper multi-line text
                    x = leftMargin
                    row.take(7).forEachIndexed { index, cell ->
                        drawMultiLineText(canvas, cell, x, y, columnWidths[index], textPaint)
                        x += columnWidths[index]
                    }

                    y += maxRowHeight + 10f // Add some padding between rows
                }
            }
        } else {
            canvas.drawText("No native libraries found or all libraries are 16KB compatible!", leftMargin, y, textPaint)
        }
    }

    private fun parseHtmlTable(htmlContent: String): List<List<String>> {
        val tableData = mutableListOf<List<String>>()
        val rowPattern = "<tr[^>]*>(.*?)</tr>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val cellPattern = "<td[^>]*>(.*?)</td>".toRegex(RegexOption.DOT_MATCHES_ALL)

        rowPattern.findAll(htmlContent).forEach { rowMatch ->
            val rowContent = rowMatch.groupValues[1]
            if (!rowContent.contains("<th")) {
                val cells = cellPattern.findAll(rowContent).map { cellMatch ->
                    cellMatch.groupValues[1]
                        .replace("<[^>]*>".toRegex(), "")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .trim()
                }.toList()

                if (cells.size >= 4) {
                    val paddedCells = cells.toMutableList()
                    while (paddedCells.size < 7) paddedCells.add("")
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
            putExtra(Intent.EXTRA_TEXT, "16KB page size compatibility report")
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

    private fun drawMultiLineText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        if (text.isEmpty()) return

        val words = text.split(" ")
        var currentLine = ""
        var currentY = y
        val lineHeight = 12f

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
                    // Single word is too long, break it
                    val brokenWord = breakLongWord(word, maxWidth, paint)
                    brokenWord.forEach { wordPart ->
                        canvas.drawText(wordPart, x, currentY, paint)
                        currentY += lineHeight
                    }
                    currentLine = ""
                }
            }
        }

        // Draw the last line
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    private fun calculateTextHeight(text: String, maxWidth: Float, paint: Paint): Float {
        if (text.isEmpty()) return 12f

        val words = text.split(" ")
        var currentLine = ""
        var lineCount = 0
        val lineHeight = 12f

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lineCount++
                    currentLine = word
                } else {
                    // Single word is too long, count broken parts
                    val brokenWord = breakLongWord(word, maxWidth, paint)
                    lineCount += brokenWord.size
                    currentLine = ""
                }
            }
        }

        // Count the last line
        if (currentLine.isNotEmpty()) {
            lineCount++
        }

        return lineCount * lineHeight
    }

    private fun breakLongWord(word: String, maxWidth: Float, paint: Paint): List<String> {
        val parts = mutableListOf<String>()
        var currentPart = ""

        for (char in word) {
            val testPart = currentPart + char
            if (paint.measureText(testPart) <= maxWidth) {
                currentPart = testPart
            } else {
                if (currentPart.isNotEmpty()) {
                    parts.add(currentPart)
                    currentPart = char.toString()
                } else {
                    // Even single character doesn't fit, add it anyway
                    parts.add(char.toString())
                }
            }
        }

        if (currentPart.isNotEmpty()) {
            parts.add(currentPart)
        }

        return parts.ifEmpty { listOf(word) }
    }
}