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

import org.eclipse.paho.client.mqttv3.ILogger;
import org.eclipse.paho.client.mqttv3.IPahoEvents;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A network module for connecting over SSL.
 */
public class SSLNetworkModule extends TCPNetworkModule
{
	private String[] enabledCiphers;

	private int handshakeTimeoutSecs;

	final static String className = SSLNetworkModule.class.getName();

	private final String TAG = "SSLNETWORKMODULE";

	/**
	 * Constructs a new SSLNetworkModule using the specified host and port. The supplied SSLSocketFactory is used to supply the network socket.
	 */
	public SSLNetworkModule(SSLSocketFactory factory, String host, int port, String resourceContext, ILogger logger, IPahoEvents pahoEvents)
	{
		super(factory, host, port, resourceContext, logger, pahoEvents);
	}

	/**
	 * Returns the enabled cipher suites.
	 */
	public String[] getEnabledCiphers()
	{
		return enabledCiphers;
	}

	/**
	 * Sets the enabled cipher suites on the underlying network socket.
	 */
	public void setEnabledCiphers(String[] enabledCiphers)
	{
		final String methodName = "setEnabledCiphers";
		this.enabledCiphers = enabledCiphers;
		if ((socket != null) && (enabledCiphers != null))
		{

			((SSLSocket) socket).setEnabledCipherSuites(enabledCiphers);
		}
	}

	public void setSSLhandshakeTimeout(int timeout)
	{
		this.handshakeTimeoutSecs = timeout;
	}

	public void start() throws IOException, MqttException
	{
		super.start();
		long socketStartTime = System.nanoTime();
		try {
			pahoEvents.onSSLSocketAttempt(port, host, socket.getSoTimeout());

			socket = ((SSLSocketFactory) factory).createSocket(socket, host, port, true);

			long socketEndTime = System.nanoTime();
			pahoEvents.onSSLSocketSuccess(port, host, socket.getSoTimeout(), TimeUnit.NANOSECONDS.toMillis(socketEndTime-socketStartTime));

			setEnabledCiphers(enabledCiphers);
			int soTimeout = socket.getSoTimeout();
			// RTC 765: Set a timeout to avoid the SSL handshake being blocked indefinitely
			socket.setSoTimeout(this.handshakeTimeoutSecs * 1000);
			long handshakeStartTime = System.nanoTime();
			((SSLSocket) socket).startHandshake();
			long handshakeEndTime = System.nanoTime();

			pahoEvents.onSSLHandshakeSuccess(port, host, socket.getSoTimeout(), TimeUnit.NANOSECONDS.toMillis(handshakeEndTime-handshakeStartTime));

			// reset timeout to default value
			socket.setSoTimeout(soTimeout);
		} catch (IOException ex) {
			long socketEndTime = System.nanoTime();
			pahoEvents.onSSLSocketFailure(port, host, socket.getSoTimeout(), ex, TimeUnit.NANOSECONDS.toMillis(socketEndTime-socketStartTime));
			throw ex;
		}
	}

	public void stop() throws IOException
	{
		// In case of SSLSocket we should not try to shutdownOutput and shutdownInput it would result
		// in java.lang.UnsupportedOperationException. only SSLSocket.close() is enough to close
		// an SSLSocket.
		socket.close();
	}
}