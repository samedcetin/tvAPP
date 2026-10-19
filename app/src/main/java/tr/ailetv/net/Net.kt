package tr.ailetv.net

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object UserAgents {
    /** What the box looks like to CDNs. Several Turkish CDNs reject unknown agents, so we look like Chrome on Android TV. */
    const val TV = "Mozilla/5.0 (Linux; Android 11; MiTV-AFKR0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
    /** Official pages / APIs that serve the desktop player its token. */
    const val DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
}

/**
 * Home routers in our tests occasionally answered ENOTFOUND for daioncdn/ercdn hosts under load.
 * Try the system resolver twice, then DNS-over-HTTPS (Google, then Cloudflare).
 */
class ResilientDns(bootstrap: OkHttpClient) : Dns {
    private val google = DnsOverHttps.Builder()
        .client(bootstrap)
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(InetAddress.getByName("8.8.8.8"), InetAddress.getByName("8.8.4.4"))
        .build()
    private val cloudflare = DnsOverHttps.Builder()
        .client(bootstrap)
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("1.0.0.1"))
        .build()

    override fun lookup(hostname: String): List<InetAddress> {
        var last: UnknownHostException? = null
        repeat(2) { attempt ->
            try {
                return Dns.SYSTEM.lookup(hostname)
            } catch (e: UnknownHostException) {
                last = e
                if (attempt == 0) Thread.sleep(250)
            }
        }
        for (doh in listOf(google, cloudflare)) {
            try {
                val result = doh.lookup(hostname)
                if (result.isNotEmpty()) return result
            } catch (e: UnknownHostException) {
                last = e
            }
        }
        throw last ?: UnknownHostException(hostname)
    }
}

object Net {
    fun createClient(): OkHttpClient {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
        return bootstrap.newBuilder()
            .dns(ResilientDns(bootstrap))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
