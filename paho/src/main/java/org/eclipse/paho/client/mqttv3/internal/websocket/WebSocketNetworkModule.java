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
 *    James Sutton - Bug 459142 - WebSocket support for the Java client.
 */
package org.eclipse.paho.client.mqttv3.internal.websocket;

import org.eclipse.paho.client.mqttv3.ILogger;
import org.eclipse.paho.client.mqtt.IPahoEvents;
import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;

import javax.net.SocketFactory;

public class WebSocketNetworkModule extends TCPNetworkModule {
	
	private static final String CLASS_NAME = WebSocketNetworkModule.class.getName();

	private String uri;
	private String host;
	private int port;
	private ILogger logger;
	private PipedInputStream pipedInputStream;
	private WebSocketReceiver webSocketReceiver;
	ByteBuffer recievedPayload;

	/**
	 * Overrides the flush method.
	 * This allows us to encode the MQTT payload into a WebSocket
	 *  Frame before passing it through to the real socket.
	 */
	private ByteArrayOutputStream outputStream = new ExtendedByteArrayOutputStream(this);
	
	public WebSocketNetworkModule(SocketFactory factory, String uri, String host, int port, String resourceContext, ILogger logger, IPahoEvents pahoEvents){
		super(factory, host, port, resourceContext, logger, pahoEvents);
		this.uri = uri;
		this.host = host;
		this.port = port;
		this.logger = logger;
		this.pipedInputStream = new PipedInputStream();

	}
	
	public void start() throws IOException, MqttException {
		super.start();
		WebSocketHandshake handshake = new WebSocketHandshake(getSocketInputStream(), getSocketOutputStream(), uri, host, port);
		handshake.execute();
		this.webSocketReceiver = new WebSocketReceiver(getSocketInputStream(), pipedInputStream);
		webSocketReceiver.start("webSocketReceiver");
	}
	
	OutputStream getSocketOutputStream() throws IOException {
		return super.getOutputStream();
	}
	
	InputStream getSocketInputStream() throws IOException {
		return super.getInputStream();
	}
	
	public InputStream getInputStream() throws IOException {
		return pipedInputStream;
	}
	
	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}
	
	/**
	 * Stops the module, by closing the TCP socket.
	 */
	public void stop() throws IOException {
		// Creating Close Frame
		WebSocketFrame frame = new WebSocketFrame((byte)0x08, true, "1000".getBytes());
		byte[] rawFrame = frame.encodeFrame();
		getSocketOutputStream().write(rawFrame);
		getSocketOutputStream().flush();

		if(webSocketReceiver != null){
			webSocketReceiver.stop();
		}
		super.stop();
	}
	
	public String getServerURI() {
		return "ws://" + host + ":" + port;
	}
	
}
