
package cz.uhk.KubeViewer

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

object InsecureSSLSocketFactory {
    fun sslSocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustManager()), SecureRandom())
        return context.socketFactory
    }

    fun trustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
}
