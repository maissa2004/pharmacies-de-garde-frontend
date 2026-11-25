package tn.pharmaciesdegarde.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDateTime
import org.json.JSONArray
import org.json.JSONObject
import tn.pharmaciesdegarde.data.remote.GardeDto
import tn.pharmaciesdegarde.viewmodel.UserLocation


private class GardesMapBridge(private val onClick: (Long) -> Unit) {
    private val main = Handler(Looper.getMainLooper())

    // Le pont JS tourne sur un autre thread : la navigation doit se faire sur le thread principal
    @JavascriptInterface
    fun onMarkerClick(id: Long) {
        main.post { onClick(id) }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun GardesMap(
    gardes: List<GardeDto>,
    now: LocalDateTime,
    onPharmacieClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,
    focusIds: Set<Long>? = null
) {
    val latestClick by rememberUpdatedState(onPharmacieClick)
    var pageReady by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Une pharmacie = un marqueur (vert si au moins une de ses gardes est en cours).
    // Le JSON sert de clé : la carte ne se recentre que si son contenu change vraiment.
    val json = remember(gardes, now, userLocation, focusIds) {        val points = JSONArray()
        gardes.groupBy { it.pharmacie.id }.forEach { (_, list) ->
            val p = list.first().pharmacie
            val lat = p.latitude?.toDouble()
            val lng = p.longitude?.toDouble()
            if (lat == null || lng == null || !lat.isFinite() || !lng.isFinite()) return@forEach
            if (lat == 0.0 && lng == 0.0) return@forEach
            points.put(JSONObject().apply {
                put("id", p.id)
                put("nom", p.nom)
                put("lat", lat)
                put("lng", lng)
                put("ongoing", list.any { it.isOngoing(now) })
                put("focus", focusIds == null || p.id in focusIds)   // <- ici
                if (userLocation != null) {
                    put("km", haversineKm(userLocation.lat, userLocation.lng, lat, lng))
                }
            })
        }
        JSONObject().apply {
            put("points", points)
            if (userLocation != null) {
                put("user", JSONObject().put("lat", userLocation.lat).put("lng", userLocation.lng))
            }
        }.toString()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript("fixSize()", null)
                        pageReady = true
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                        Log.d("GardesMap", "JS : ${m.message()} (ligne ${m.lineNumber()})")
                        return true
                    }
                }
                addJavascriptInterface(GardesMapBridge { latestClick(it) }, "Android")
                // la carte est dans une LazyColumn : on évite que le scroll vole le glissement
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                runCatching {
                    ctx.assets.open("map_gardes.html").bufferedReader().use { it.readText() }
                }.onSuccess { html ->
                    loadDataWithBaseURL("https://pharmacies-de-garde.local/", html, "text/html", "utf-8", null)
                }.onFailure {
                    Log.e("GardesMap", "map_gardes.html introuvable dans assets", it)
                }
                webView = this
            }
        },
        onRelease = {
            webView = null
            pageReady = false
            it.destroy()
        }
    )

    LaunchedEffect(json, pageReady, webView) {
        val wv = webView
        if (pageReady && wv != null) {
            wv.evaluateJavascript("fixSize(); setData($json)", null)        }
    }
}