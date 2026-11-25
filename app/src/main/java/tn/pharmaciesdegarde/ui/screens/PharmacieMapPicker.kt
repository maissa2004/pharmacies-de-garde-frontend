package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import tn.pharmaciesdegarde.viewmodel.MapFocus

// Pont JS → Kotlin. Les noms sont différents (callback / onMoved) pour éviter toute récursion.
class MapBridge(private val callback: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun onMoved(lat: Double, lng: Double) = callback(lat, lng)
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun PharmacieMapPicker(
    focus: MapFocus?,
    onPositionChanged: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestCallback by rememberUpdatedState(onPositionChanged)
    var pageReady by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { pageReady = true }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    Log.e("MapPicker", "Erreur ${request?.url} : ${error?.description}")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                    Log.d("MapPicker", "JS : ${m.message()} (ligne ${m.lineNumber()})")
                    return true
                }
            }
            addJavascriptInterface(MapBridge { lat, lng -> latestCallback(lat, lng) }, "Android")
            // Empêche la feuille / le scroll du formulaire de voler le glissement sur la carte
            setOnTouchListener { v, _ ->
                v.parent.requestDisallowInterceptTouchEvent(true)
                false
            }
            // baseUrl https : OpenStreetMap exige un Referer, qu'un file:// n'envoie pas
            val html = context.assets.open("map_picker.html").bufferedReader().use { it.readText() }
            loadDataWithBaseURL("https://pharmacies-de-garde.local/", html, "text/html", "utf-8", null)
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)

    // Recentre la carte uniquement quand un nouveau géocodage arrive
    LaunchedEffect(focus, pageReady) {
        if (pageReady && focus != null) {
            webView.evaluateJavascript("focusOn(${focus.lat}, ${focus.lng})", null)
        }
    }

    DisposableEffect(Unit) { onDispose { webView.destroy() } }
}