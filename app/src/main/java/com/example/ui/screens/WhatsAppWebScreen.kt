package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

private const val WHATSAPP_WEB_URL = "https://web.whatsapp.com/"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WhatsAppWebScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var currentUrl by remember { mutableStateOf(WHATSAPP_WEB_URL) }
    var showInstructions by remember { mutableStateOf(true) }
    var isDesktopUa by remember { mutableStateOf(true) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Intercept hardware / system back navigation
    BackHandler {
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column {
                    // Indian Tiranga accent line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaSaffron))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaWhite))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(IndiaGreen))
                    }

                    TopAppBar(
                        title = {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "WhatsApp Web",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = IndiaGreen.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, IndiaGreen.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "DUAL / WEB",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IndiaGreenLight,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isLoading) "Loading QR code..." else "Scan QR with WhatsApp to link",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("wa_web_back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Instruction toggle
                            IconButton(onClick = { showInstructions = !showInstructions }) {
                                Icon(
                                    imageVector = if (showInstructions) Icons.Default.Help else Icons.Default.HelpOutline,
                                    contentDescription = "Instructions",
                                    tint = IndiaSaffron
                                )
                            }

                            // Zoom In
                            IconButton(onClick = { webViewRef?.zoomIn() }) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                            }

                            // Zoom Out
                            IconButton(onClick = { webViewRef?.zoomOut() }) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                            }

                            // Reload
                            IconButton(
                                onClick = { webViewRef?.reload() },
                                modifier = Modifier.testTag("wa_web_reload_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }

                            // Clear session
                            IconButton(onClick = { showClearConfirm = true }) {
                                Icon(Icons.Default.Logout, contentDescription = "Logout / Reset Session", tint = ErrorRed)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Loading progress indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { loadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = IndiaSaffron,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Collapsible Instructions Banner
                AnimatedVisibility(visible = showInstructions) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IndiaSaffron.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🇮🇳", fontSize = 16.sp)
                                    Text(
                                        text = "How to link your WhatsApp (Companion QR):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = IndiaSaffron
                                    )
                                }
                                IconButton(
                                    onClick = { showInstructions = false },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = "1. Open WhatsApp on your primary phone (or this device's WhatsApp app).\n" +
                                        "2. Tap ⋮ (Three dots on Android) or Settings ⚙️ (iPhone).\n" +
                                        "3. Select \"Linked Devices\" → Tap \"Link a Device\".\n" +
                                        "4. Point your camera at the QR code loaded below to log in.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Android WebView for WhatsApp Web
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("wa_web_view"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            // Crucial settings for WhatsApp Web to load desktop QR code
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                allowFileAccess = true
                                allowContentAccess = true
                                setSupportZoom(true)
                                cacheMode = WebSettings.LOAD_DEFAULT

                                // Force Desktop User-Agent so WhatsApp doesn't redirect to mobile Play Store page
                                userAgentString = DESKTOP_USER_AGENT
                            }

                            // Enable cookies for session persistence
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    currentUrl = url ?: ""
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    currentUrl = url ?: ""
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url?.toString() ?: return false
                                    // Keep WhatsApp Web links inside the WebView
                                    return if (uri.contains("whatsapp.com")) {
                                        false
                                    } else {
                                        // Open external links safely
                                        false
                                    }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress / 100f
                                    if (newProgress >= 100) {
                                        isLoading = false
                                    }
                                }
                            }

                            loadUrl(WHATSAPP_WEB_URL)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        webViewRef = view
                    }
                )
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Log Out & Reset Session?") },
            text = { Text("This will clear all cookies and cache for WhatsApp Web, requiring you to scan the QR code again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        webViewRef?.let { wv ->
                            wv.clearCache(true)
                            wv.clearHistory()
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            wv.loadUrl(WHATSAPP_WEB_URL)
                        }
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Log Out & Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
