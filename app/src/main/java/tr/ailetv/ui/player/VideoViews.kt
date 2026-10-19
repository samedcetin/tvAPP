package tr.ailetv.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import org.json.JSONObject

/** ExoPlayer output on a SurfaceView (hardware overlay, cheapest path on the Mali-G31), letterboxed to the video's aspect ratio. */
@Composable
fun VideoSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    var aspect by remember { mutableFloatStateOf(16f / 9f) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    isFocusable = false
                    player.setVideoSurfaceView(this)
                }
            },
            onRelease = { player.clearVideoSurfaceView(it) },
            modifier = Modifier.aspectRatio(aspect),
        )
    }
}

/**
 * Official YouTube IFrame player inside a WebView (policy: YouTube channels play embedded).
 * YouTube rules (RMF): the page identifies the app through its base URL, and nothing may be drawn over the
 * player — callers set [visible] = false before opening any overlay. While [visible] is false the player is
 * paused, also when playback first starts behind an overlay (the page asks on every playing state); resuming
 * jumps to the live edge only for a live video that sat paused long enough to fall behind.
 * Reports: [onReady] on the first playing state, [onBuffering]/[onResumed] for later stalls (engine watchdog).
 * Our own pause for [visible] = false is reported as [onResumed] and nothing is reported as buffering while
 * hidden, so no stall clock runs behind an overlay; a resume that does not play at once reports [onBuffering].
 * [onError] with YouTube's error code, 0 when the video ended, -1 when the IFrame API failed to load,
 * -2 when the WebView renderer died, -3 when no WebView could be created, -4 when the page was dropped while
 * the app was stopped and is still shown on return. Only the first error is reported, and nothing at all after
 * it: the engine always replaces a page that reported one.
 * The WebView never takes focus, so the remote keeps driving our UI (▲▼ zapping, OK for the list).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(
    videoId: String?,
    youtubeChannelId: String?,
    visible: Boolean,
    onReady: () -> Unit,
    onError: (Int) -> Unit,
    onBuffering: () -> Unit,
    onResumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready by rememberUpdatedState(onReady)
    val failed by rememberUpdatedState(onError)
    val buffering by rememberUpdatedState(onBuffering)
    val resumed by rememberUpdatedState(onResumed)
    val html = remember(videoId, youtubeChannelId) { youtubeHtml(videoId, youtubeChannelId) }
    val page = remember { WebState() }
    DropWhenStopped(page) { failed(ERROR_PAGE_DROPPED) }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            buildWebView(ctx, page, onUnavailable = { page.fail { failed(ERROR_WEBVIEW_UNAVAILABLE) } }) {
                isFocusable = false
                isFocusableInTouchMode = false
                setBackgroundColor(AndroidColor.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    // The embed page is all we show: the player never navigates the WebView away.
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        request.isForMainFrame

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        // Handled = the app survives; the engine moves on and onRelease destroys the dead view.
                        page.dead = true
                        page.fail { failed(ERROR_RENDERER_GONE) }
                        return true
                    }
                }
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onPlaying() { page.post { ready() } }

                    @JavascriptInterface
                    fun onBuffering() { page.post { buffering() } }

                    @JavascriptInterface
                    fun onResumed() { page.post { resumed() } }

                    @JavascriptInterface
                    fun onError(code: Int) { page.post { page.fail { failed(code) } } }

                    /** Asked by the page on every playing and buffering state (JavaBridge thread). */
                    @JavascriptInterface
                    fun isHidden(): Boolean = page.hidden
                }, "AileTv")
                // Base URL = app identity sent as Referer to YouTube (required for embedded players).
                loadDataWithBaseURL(YOUTUBE_BASE_URL, html, "text/html", "utf-8", null)
            }
        },
        update = update@{ view ->
            // Set first: the page may reach "playing" before the hide/resume script below runs.
            page.hidden = !visible
            val web = view as? WebView ?: return@update // no WebView: the placeholder is black anyway
            if (!page.dead) {
                val wasVisible = web.visibility == View.VISIBLE
                if (visible && !wasVisible) {
                    web.visibility = View.VISIBLE
                    // After an error or the end the engine replaces this page: do not restart its video meanwhile.
                    if (!page.failed) web.evaluateJavascript("window.aileResume && window.aileResume();", null)
                } else if (!visible && wasVisible) {
                    web.evaluateJavascript("window.aileHide && window.aileHide();", null)
                    web.visibility = View.INVISIBLE
                }
            }
        },
        onRelease = release@{ view ->
            page.close()
            val web = view as? WebView ?: return@release
            // A WebView whose renderer is gone may only be destroyed.
            if (!page.dead) {
                web.stopLoading()
                web.loadUrl("about:blank")
                web.removeJavascriptInterface("AileTv")
            }
            web.destroy()
        },
    )
}

/**
 * Last-resort source: the broadcaster's official live page in a WebView. These pages build their player with
 * JS after load, so [onReady] waits until a `<video>` (page or same-origin iframe) really plays; until then the
 * page is probed every [WEB_PROBE_INTERVAL_MS] (unmute, play, fullscreen). The engine's own timeout covers
 * pages that never get there. After [onReady] the video keeps being watched every [WEB_WATCH_INTERVAL_MS],
 * following the page to another element (pre-roll ad → programme): [onBuffering] when it stalls or stops,
 * [onResumed] when it plays again (engine watchdog), [onError] when it ended or no video was left for
 * [WEB_GONE_TIMEOUT_MS]. [onError] also for a main-frame HTTP ≥ 400 or network error, a dead renderer, no
 * WebView, or a page dropped while the app was stopped; only the first error is reported, and nothing after it.
 * The probe runs only while the activity is started and the view is attached; it stops on release.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPagePlayer(
    url: String,
    onReady: () -> Unit,
    onError: () -> Unit,
    onBuffering: () -> Unit,
    onResumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready by rememberUpdatedState(onReady)
    val failed by rememberUpdatedState(onError)
    val buffering by rememberUpdatedState(onBuffering)
    val resumed by rememberUpdatedState(onResumed)
    val page = remember { WebState() }
    DropWhenStopped(page) { failed() }
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            buildWebView(ctx, page, onUnavailable = { page.fail { failed() } }) {
                isFocusable = false
                isFocusableInTouchMode = false
                setBackgroundColor(AndroidColor.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webChromeClient = WebChromeClient()
                val web = this
                // One probe answer (main thread). Before ready only "playing" counts; after it, only changes
                // are reported, so the engine is not called on every tick.
                fun onProbe(result: String?) {
                    if (!page.active || page.failed) return
                    if (!page.reported) {
                        if (result == WEB_PLAYING) {
                            page.reported = true
                            page.healthy = true
                            ready()
                        }
                        return
                    }
                    when (result) {
                        WEB_PLAYING -> {
                            page.goneSince = 0L
                            if (!page.healthy) {
                                page.healthy = true
                                resumed()
                            }
                        }
                        WEB_WAITING, WEB_ENDED, WEB_NONE -> {
                            if (page.healthy) {
                                page.healthy = false
                                buffering()
                            }
                            if (result == WEB_WAITING) {
                                page.goneSince = 0L
                            } else {
                                // Short gaps (an ad handing over to the programme) only arm the watchdog.
                                val now = SystemClock.elapsedRealtime()
                                if (page.goneSince == 0L) {
                                    page.goneSince = now
                                } else if (now - page.goneSince >= WEB_GONE_TIMEOUT_MS) {
                                    page.fail { failed() }
                                }
                            }
                        }
                        else -> Unit // no answer: the page navigated itself or the script threw
                    }
                }
                // Each tick is independent of the previous script's answer (a page that navigates itself drops
                // pending results). It stops for good once the page failed, was released, died or was stopped.
                val probe = object : Runnable {
                    override fun run() {
                        if (!page.active || page.failed) return
                        if (web.isAttachedToWindow) web.evaluateJavascript(WEB_PROBE_JS) { onProbe(it) }
                        page.main.postDelayed(this, if (page.reported) WEB_WATCH_INTERVAL_MS else WEB_PROBE_INTERVAL_MS)
                    }
                }
                val startProbe = {
                    if (!page.probing && page.active && !page.failed) {
                        page.probing = true
                        page.main.post(probe)
                    }
                }
                val fail = {
                    page.main.removeCallbacks(probe)
                    page.fail { failed() }
                }
                webViewClient = object : WebViewClient() {
                    // Probe from the first paint of the real page; onPageFinished can wait long for ads and trackers.
                    override fun onPageCommitVisible(view: WebView, url: String) = startProbe()

                    override fun onPageFinished(view: WebView, url: String) = startProbe()

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) fail()
                    }

                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame && response.statusCode >= 400) fail()
                    }

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        // Handled = the app survives; the engine moves on and onRelease destroys the dead view.
                        page.dead = true
                        fail()
                        return true
                    }
                }
                loadUrl(url)
            }
        },
        onRelease = release@{ view ->
            page.close()
            val web = view as? WebView ?: return@release
            // A WebView whose renderer is gone may only be destroyed.
            if (!page.dead) {
                web.stopLoading()
                web.loadUrl("about:blank")
            }
            web.destroy()
        },
    )
}

/**
 * Leaving the app (Google TV home, screen saver) stops the engine, but the player screen does not recompose
 * while the activity is stopped, so the page would live on behind the home screen. At ON_STOP it is dropped
 * (about:blank silences every video, cross-origin iframes included, and frees the renderer's memory); the
 * engine opens a new session when the app is back. A page still shown at ON_START reports [onLost], so a
 * dropped page can never stay on screen (an old session's report is ignored by the engine).
 */
@Composable
private fun DropWhenStopped(page: WebState, onLost: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { page.stop() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { if (page.stopped) page.fail(onLost) }
}

/**
 * The page's WebView, or a black placeholder when none can be created: the first WebView of the process loads
 * the WebView provider, which throws while Play is replacing Android System WebView or when it is missing.
 * [onUnavailable] then runs after the composition has been applied, so the engine moves on to the next source.
 */
private fun buildWebView(ctx: Context, page: WebState, onUnavailable: () -> Unit, setup: WebView.() -> Unit): View {
    var created: WebView? = null
    return try {
        WebView(ctx).also { created = it }.apply(setup).also { page.view = it }
    } catch (e: RuntimeException) { // AndroidRuntimeException: provider missing or being updated
        Log.w(TAG, "WebView unavailable: ${e.message}")
        created?.let { runCatching { it.destroy() } }
        page.post(onUnavailable)
        View(ctx).apply {
            isFocusable = false
            setBackgroundColor(AndroidColor.BLACK)
        }
    }
}

/** Bookkeeping shared by one WebView's factory, update and release. Main thread only, except [hidden]. */
private class WebState {
    val main = Handler(Looper.getMainLooper())

    /** The WebView, once built (null for the placeholder). */
    var view: WebView? = null

    /** YouTube: the caller hides the player. Read by the page through the JS bridge (JavaBridge thread). */
    @Volatile var hidden = true

    /** The renderer crashed or was killed: the WebView may only be destroyed. */
    var dead = false

    /** The activity stopped: the page was dropped and nothing more is asked of it or reported from it. */
    var stopped = false
        private set

    /** Web page: probe loop started / onReady sent / the last watch found the video playing. */
    var probing = false
    var reported = false
    var healthy = false

    /** Web page: since when the video has been ended or missing (0 = it is there). */
    var goneSince = 0L

    var failed = false
        private set

    private var closed = false

    /** False once released, dead or stopped: nothing more is asked of the page. */
    val active: Boolean get() = !closed && !dead && !stopped

    /** Runs [block] on the main thread unless the view has been released, stopped or has failed by then. */
    fun post(block: () -> Unit) {
        main.post { if (!closed && !stopped && !failed) block() }
    }

    /** Reports the first failure only (an error, the ended state and a dead renderer can follow each other). */
    fun fail(report: () -> Unit) {
        if (!failed && !closed) {
            failed = true
            report()
        }
    }

    fun stop() {
        if (closed || stopped) return
        stopped = true
        main.removeCallbacksAndMessages(null)
        val web = view
        if (web != null && !dead) {
            web.stopLoading()
            web.loadUrl("about:blank")
        }
    }

    fun close() {
        closed = true
        view = null
        main.removeCallbacksAndMessages(null)
    }
}

private const val TAG = "VideoViews"

private const val YOUTUBE_BASE_URL = "https://tr.ailetv"

/** [YouTubeEmbed] error codes of our own (YouTube's are positive, 0 = ended, -1 = IFrame API not loaded). */
private const val ERROR_RENDERER_GONE = -2
private const val ERROR_WEBVIEW_UNAVAILABLE = -3
private const val ERROR_PAGE_DROPPED = -4

/**
 * A live video hidden for longer than this resumes at the live edge. Shorter pauses (a toast, the banner,
 * the pause when playback starts behind the opening layer) continue from the buffer instead of flushing it.
 */
private const val LIVE_RESEEK_AFTER_MS = 15_000L

/** Web page: probe rate until the video plays, then the (cheaper) watch rate. */
private const val WEB_PROBE_INTERVAL_MS = 500L
private const val WEB_WATCH_INTERVAL_MS = 2_000L

/** Web page: a video ended or missing for this long is an error (the engine reloads the page). */
private const val WEB_GONE_TIMEOUT_MS = 10_000L

/** [WEB_PROBE_JS] answers, as evaluateJavascript returns them (JSON strings). */
private const val WEB_PLAYING = "\"playing\""
private const val WEB_WAITING = "\"waiting\""
private const val WEB_ENDED = "\"ended\""
private const val WEB_NONE = "\"none\""

private fun youtubeHtml(videoId: String?, channelId: String?): String {
    val target = if (videoId != null) {
        "videoId: ${JSONObject.quote(videoId)},"
    } else {
        "videoId: 'live_stream',"
    }
    val channelVar = if (videoId == null && channelId != null) "channel: ${JSONObject.quote(channelId)}," else ""
    return """
<!doctype html>
<html><head>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{position:absolute;top:0;left:0;width:100%;height:100%}</style>
</head><body>
<div id="p"></div>
<script>
  var player = null, reported = false, paused = false, pausedAt = 0;
  var tag = document.createElement('script');
  tag.src = 'https://www.youtube.com/iframe_api';
  tag.onerror = function(){ AileTv.onError(-1); };
  document.head.appendChild(tag);
  function hidden() { try { return AileTv.isHidden(); } catch(x) { return false; } }
  function pauseNow() {
    try { player.pauseVideo(); if (!paused) { paused = true; pausedAt = Date.now(); } } catch(x){}
    // Paused by us, not stalled: stop the engine's stall clock (no playing state can come while we hold it).
    try { AileTv.onResumed(); } catch(x){}
  }
  function onYouTubeIframeAPIReady() {
    player = new YT.Player('p', {
      width: '100%', height: '100%',
      $target
      playerVars: { $channelVar autoplay: 1, controls: 0, disablekb: 1, fs: 0, rel: 0, iv_load_policy: 3,
                    playsinline: 1, cc_load_policy: 0, hl: 'tr', origin: '$YOUTUBE_BASE_URL' },
      events: {
        onReady: function(e){ try { e.target.unMute(); e.target.setVolume(100); } catch(x){} e.target.playVideo(); },
        onStateChange: function(e){
          if (e.data === 1) {
            if (!reported) { reported = true; AileTv.onPlaying(); } else AileTv.onResumed();
            // Hidden means paused, also when playback starts while an overlay already hides the player.
            if (hidden()) pauseNow();
          } else if (e.data === 3) {
            // Buffering while we hold the player paused is not a stall.
            if (!paused && !hidden()) AileTv.onBuffering();
          } else if (e.data === 0) AileTv.onError(0);
        },
        onError: function(e){ AileTv.onError(e.data); }
      }
    });
  }
  window.aileHide = function(){ if (player) pauseNow(); };
  window.aileResume = function(){
    try {
      if (!player) return;
      if (paused) {
        paused = false;
        var vd = player.getVideoData && player.getVideoData();
        if (vd && vd.isLive && Date.now() - pausedAt > $LIVE_RESEEK_AFTER_MS) player.seekTo(player.getDuration(), true);
      }
      player.playVideo();
      // Not playing yet: the stall clock covers the resume too (the next playing state stops it), also for a
      // player that never leaves its state and so sends no event. Not if an overlay came back meanwhile.
      if (player.getPlayerState() !== 1 && !hidden()) AileTv.onBuffering();
    } catch(x){}
  };
</script>
</body></html>
""".trimIndent()
}

/**
 * Finds the page's player and returns its state: "playing" (plays and its time moved since the last tick),
 * "waiting" (paused, stalled or frozen), "ended", or "none" (no video in the page or a same-origin iframe).
 * Keeps the video it already took over unless that one ended, left the page, or is paused while another plays
 * (pre-roll ad → programme); otherwise takes the largest playing video that is not small next to the page's
 * largest one, else the largest one not ended.
 * The chosen video (and its iframe) is stretched over the page, unmuted and started; a video it leaves gets
 * its own style back. Layout is only read when it has to choose.
 */
private const val WEB_PROBE_JS = """
(function(){
  var w = window, cur = w.__aileV, v = cur, owner = w.__aileO;
  function playing(e) { return !e.paused && !e.ended && e.readyState >= 2; }
  function each(fn) {
    fn(document, null);
    var fs = document.getElementsByTagName('iframe');
    for (var j = 0; j < fs.length; j++) {
      try { var d = fs[j].contentDocument; if (d) fn(d, fs[j]); } catch(e) {}
    }
  }
  // Buffering is not a reason to look elsewhere; being paused while another video plays is.
  var keep = !!cur && cur.isConnected && !cur.ended &&
      (!owner || (owner.isConnected && owner.contentDocument === cur.ownerDocument));
  if (keep && cur.paused) {
    each(function(doc){
      var vs = doc.getElementsByTagName('video');
      for (var i = 0; i < vs.length; i++) if (vs[i] !== cur && playing(vs[i])) keep = false;
    });
  }
  if (!keep) {
    var list = [], max = 0, rank = -1, area = -1;
    each(function(doc, frame){
      var vs = doc.getElementsByTagName('video');
      for (var i = 0; i < vs.length; i++) {
        // The video we stretched counts with its size in the page, not with the whole screen.
        var e = vs[i], s = e.__aileA;
        if (s === undefined) { var r = e.getBoundingClientRect(); s = r.width * r.height; }
        list.push([e, frame, s]);
        if (!e.ended && s > max) max = s;
      }
    });
    // A playing video wins, unless it is small next to the page's player (a muted banner or preview).
    v = null; owner = null;
    for (var n = 0; n < list.length; n++) {
      var e = list[n][0], a = list[n][2];
      var k = e.ended ? 0 : (playing(e) && a * 4 >= max) ? 2 : 1;
      if (k > rank || (k === rank && a > area)) { rank = k; area = a; v = e; owner = list[n][1]; }
    }
  }
  function cover(el) {
    var s = el.style;
    if (s.position === 'fixed' && s.left === '0px' && s.top === '0px' && s.width === '100vw' &&
        s.height === '100vh' && s.zIndex === '2147483647') return;
    if (el.__aileCss === undefined) {
      var r = el.getBoundingClientRect();
      el.__aileA = r.width * r.height; el.__aileCss = s.cssText;
    }
    s.position = 'fixed'; s.left = '0'; s.top = '0'; s.width = '100vw'; s.height = '100vh';
    s.zIndex = '2147483647'; s.background = '#000'; s.border = '0';
  }
  function uncover(el) {
    if (el && el.__aileCss !== undefined) {
      el.style.cssText = el.__aileCss; el.__aileCss = undefined; el.__aileA = undefined;
    }
  }
  try {
    if (v !== cur) { uncover(cur); w.__aileV = v; }
    if (owner !== w.__aileO) { uncover(w.__aileO); w.__aileO = owner; }
  } catch(e) {}
  if (!v) return 'none';
  try { if (owner) cover(owner); cover(v); } catch(e) {}
  try {
    if (v.muted) v.muted = false;
    if (v.volume < 1) v.volume = 1;
    if (v.paused && !v.ended) { var p = v.play(); if (p && p.catch) p.catch(function(){}); }
  } catch(e) {}
  if (v.ended) return 'ended';
  var t = v.currentTime, moved = v.__aileT === undefined || t !== v.__aileT;
  v.__aileT = t;
  return !v.paused && v.readyState >= 3 && moved ? 'playing' : 'waiting';
})();
"""
