/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.paho.client.mqttv3.internal.platform

import android.annotation.SuppressLint
import android.os.Build
import android.security.NetworkSecurityPolicy
import android.util.CloseGuard
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import org.eclipse.paho.client.mqttv3.Protocol
import org.eclipse.paho.client.mqttv3.SuppressSignatureCheck
import org.eclipse.paho.client.mqttv3.internal.platform.android.Android10SocketAdapter
import org.eclipse.paho.client.mqttv3.internal.platform.android.AndroidCertificateChainCleaner
import org.eclipse.paho.client.mqttv3.internal.platform.android.AndroidSocketAdapter
import org.eclipse.paho.client.mqttv3.internal.platform.android.BouncyCastleSocketAdapter
import org.eclipse.paho.client.mqttv3.internal.platform.android.ConscryptSocketAdapter
import org.eclipse.paho.client.mqttv3.internal.platform.android.DeferredSocketAdapter
import org.eclipse.paho.client.mqttv3.internal.tls.CertificateChainCleaner

/** Android 29+. */
@SuppressSignatureCheck
class Android10Platform : Platform() {
    private val socketAdapters = listOfNotNull(
        Android10SocketAdapter.buildIfSupported(),
        DeferredSocketAdapter(AndroidSocketAdapter.playProviderFactory),
        // Delay and Defer any initialisation of Conscrypt and BouncyCastle
        DeferredSocketAdapter(ConscryptSocketAdapter.factory),
        DeferredSocketAdapter(BouncyCastleSocketAdapter.factory)
    ).filter { it.isSupported() }

    override fun trustManager(sslSocketFactory: SSLSocketFactory): X509TrustManager? =
        socketAdapters.find { it.matchesSocketFactory(sslSocketFactory) }
            ?.trustManager(sslSocketFactory)

    override fun configureTlsExtensions(sslSocket: SSLSocket, hostname: String?, protocols: List<Protocol>) {
        // No TLS extensions if the socket class is custom.
        socketAdapters.find { it.matchesSocket(sslSocket) }
            ?.configureTlsExtensions(sslSocket, hostname, protocols)
    }

    override fun getSelectedProtocol(sslSocket: SSLSocket): String? =
        // No TLS extensions if the socket class is custom.
        socketAdapters.find { it.matchesSocket(sslSocket) }?.getSelectedProtocol(sslSocket)

    override fun getStackTraceForCloseable(closer: String): Any? {
        return if (Build.VERSION.SDK_INT >= 30) {
            CloseGuard().apply { open(closer) }
        } else {
            super.getStackTraceForCloseable(closer)
        }
    }

    override fun logCloseableLeak(message: String, stackTrace: Any?) {
        if (Build.VERSION.SDK_INT >= 30) {
            (stackTrace as CloseGuard).warnIfOpen()
        } else {
            // Unable to report via CloseGuard. As a last-ditch effort, send it to the logger.
            super.logCloseableLeak(message, stackTrace)
        }
    }

    @SuppressLint("NewApi")
    override fun isCleartextTrafficPermitted(hostname: String): Boolean =
        NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(hostname)

    override fun buildCertificateChainCleaner(trustManager: X509TrustManager): CertificateChainCleaner =
        AndroidCertificateChainCleaner.buildIfSupported(trustManager) ?: super.buildCertificateChainCleaner(trustManager)

    companion object {
        val isSupported: Boolean = isAndroid && Build.VERSION.SDK_INT >= 29

        fun buildIfSupported(): Platform? = if (isSupported) Android10Platform() else null
    }
}
