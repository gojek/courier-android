/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 */
package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.ConnectionSpec;
import org.eclipse.paho.client.mqttv3.ILogger;
import org.eclipse.paho.client.mqttv3.IPahoEvents;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.Protocol;
import org.eclipse.paho.client.mqttv3.internal.platform.Platform;
import org.eclipse.paho.client.mqttv3.internal.tls.CertificateChainCleaner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A network module for connecting over SSL.
 */
public class SSLNetworkModuleV2 extends TCPNetworkModule {
	private SocketFactory socketFactory;
	private SSLSocketFactory sslSocketFactory;
	private X509TrustManager x509TrustManager;
    private ConnectionSpec connectionSpec;
	private List<Protocol> alpnProtocolList;

    private int handshakeTimeoutSecs;

    final static String className = SSLNetworkModuleV2.class.getName();

    private final String TAG = "SSLNETWORKMODULE";

    /**
     * Constructs a new SSLNetworkModule using the specified host and port. The supplied
     * SSLSocketFactory is used to supply the network socket.
     */

	public SSLNetworkModuleV2(
			SocketFactory socketFactory,
			SSLSocketFactory sslSocketFactory,
			X509TrustManager x509TrustManager,
			ConnectionSpec connectionSpec,
			List<Protocol> alpnProtocolList,
			String host,
			int port,
			String resourceContext,
			ILogger logger,
			IPahoEvents pahoEvents
	) {
		super(socketFactory, host, port, resourceContext, logger, pahoEvents);
		this.socketFactory = socketFactory;
		this.sslSocketFactory = sslSocketFactory;
		this.x509TrustManager = x509TrustManager;
		this.connectionSpec = connectionSpec;
		this.alpnProtocolList = alpnProtocolList;
	}

    public void setSSLhandshakeTimeout(int timeout) {
        this.handshakeTimeoutSecs = timeout;
    }

    public void start() throws IOException, MqttException {
        super.start();
        long socketStartTime = System.nanoTime();
        try {
            pahoEvents.onSSLSocketAttempt(port, host, socket.getSoTimeout());

            socket = sslSocketFactory.createSocket(socket, host, port, true);

            connectionSpec.apply((SSLSocket) socket, false);
			if (connectionSpec.supportsTlsExtensions()) {
				Platform.get().configureTlsExtensions((SSLSocket) socket, host, alpnProtocolList);
			}
            long socketEndTime = System.nanoTime();
            pahoEvents.onSSLSocketSuccess(port, host, socket.getSoTimeout(),
                    TimeUnit.NANOSECONDS.toMillis(socketEndTime - socketStartTime));

            int soTimeout = socket.getSoTimeout();
            // RTC 765: Set a timeout to avoid the SSL handshake being blocked indefinitely
            socket.setSoTimeout(this.handshakeTimeoutSecs * 1000);
            long handshakeStartTime = System.nanoTime();
            ((SSLSocket) socket).startHandshake();
            long handshakeEndTime = System.nanoTime();

            pahoEvents.onSSLHandshakeSuccess(port, host, socket.getSoTimeout(),
                    TimeUnit.NANOSECONDS.toMillis(handshakeEndTime - handshakeStartTime));

            // reset timeout to default value
            socket.setSoTimeout(soTimeout);
        } catch (IOException ex) {
            long socketEndTime = System.nanoTime();
            pahoEvents.onSSLSocketFailure(port, host, socket.getSoTimeout(), ex,
                    TimeUnit.NANOSECONDS.toMillis(socketEndTime - socketStartTime));
            throw ex;
        }
    }

    public void stop() throws IOException {
        // In case of SSLSocket we should not try to shutdownOutput and shutdownInput it would result
        // in java.lang.UnsupportedOperationException. only SSLSocket.close() is enough to close
        // an SSLSocket.
        socket.close();
    }
}