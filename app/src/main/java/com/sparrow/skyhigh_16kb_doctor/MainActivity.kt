package com.sparrow.skyhigh_16kb_doctor

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var generateButton: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()

        generateButton.setOnClickListener {
            loadReport()
        }

        // Auto-load report on app start
        loadReport()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        generateButton = findViewById(R.id.btnGenerate)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupWebView() {
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // Disable caching to ensure fresh content
        webView.settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
    }

    private fun loadReport() {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    showToast("Loading SkyHigh Doctor report...")
                }

                // Try to load the report from assets (bundled during build)
                val reportContent = loadReportFromAssets()

                withContext(Dispatchers.Main) {
                    if (reportContent != null) {
                        loadReportContent(reportContent)
                        showToast("✅ Report loaded successfully!")
                    } else {
                        showToast("❌ Report not found. Please build the app first.")
                        loadInstructionsContent()
                    }
                    showLoading(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error loading report: ${e.message}")
                    loadInstructionsContent()
                    showLoading(false)
                }
            }
        }
    }

    private suspend fun loadReportFromAssets(): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("MainActivity", "Loading report from assets...")
            val inputStream = assets.open("skyhigh_report.html")
            val content = inputStream.bufferedReader().use { it.readText() }
            Log.d("MainActivity", "Report loaded from assets, size: ${content.length} characters")

            // Log a snippet of the content to verify it's correct
            val snippet = content.take(200).replace("\n", " ")
            Log.d("MainActivity", "Content snippet: $snippet...")

            // Check if the report has actual data (more than just the empty table)
            val hasData = content.contains("<tr>") && content.indexOf("<tr>") != content.lastIndexOf("<tr>")
            Log.d("MainActivity", "Report has data: $hasData")

            content
        } catch (e: Exception) {
            Log.w("MainActivity", "Could not load report from assets: ${e.message}")
            null
        }
    }

    private fun loadReportContent(htmlContent: String) {
        // Clear any existing content first
        webView.clearCache(true)
        webView.clearHistory()

        // Add timestamp to ensure fresh content
        val timestamp = System.currentTimeMillis()
        val contentWithTimestamp = htmlContent.replace(
            "<h1>SkyHigh 16KB Doctor Report</h1>",
            "<h1>SkyHigh 16KB Doctor Report</h1><p><small>Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))}</small></p>"
        )

        Log.d("MainActivity", "Loading report content with timestamp: $timestamp")
        webView.loadDataWithBaseURL(null, contentWithTimestamp, "text/html", "UTF-8", null)
    }

    private fun loadInstructionsContent() {
        val defaultHtml = """
            <html>
            <head>
                <title>SkyHigh 16KB Doctor</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        margin: 2em;
                        text-align: center;
                        background-color: #f5f5f5;
                    }
                    .container {
                        background: white;
                        padding: 2em;
                        border-radius: 8px;
                        box-shadow: 0 2px 10px rgba(0,0,0,0.1);
                        max-width: 600px;
                        margin: 0 auto;
                    }
                    .logo {
                        font-size: 2em;
                        color: #2196F3;
                        margin-bottom: 1em;
                    }
                    .message {
                        color: #666;
                        line-height: 1.6;
                    }
                    .instruction {
                        background: #e3f2fd;
                        padding: 1em;
                        border-radius: 4px;
                        margin-top: 1em;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🚀 SkyHigh 16KB Doctor</div>
                    <div class="message">
                        <h3>Welcome to SkyHigh 16KB Doctor!</h3>
                        <p>This tool analyzes your Android project to identify native libraries (.so files) that are not compatible with the 16KB alignment requirement.</p>

                        <div class="instruction">
                            <strong>To generate a report:</strong><br>
                            1. Build the app: <code>./gradlew :app:assembleDebug</code><br>
                            2. The report will be automatically generated and bundled<br>
                            3. Tap the "Generate Report" button above to reload<br><br>
                            <p><em>The report is generated during build time and embedded in the app.</em></p>
                        </div>

                        <p><em>The report will show incompatible libraries, their alignment status, and remediation suggestions.</em></p>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, defaultHtml, "text/html", "UTF-8", null)
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        generateButton.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}