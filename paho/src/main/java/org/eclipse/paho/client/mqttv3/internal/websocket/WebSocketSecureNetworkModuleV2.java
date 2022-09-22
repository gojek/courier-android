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

import org.eclipse.paho.client.mqttv3.ConnectionSpec;
import org.eclipse.paho.client.mqttv3.ILogger;
import org.eclipse.paho.client.mqttv3.IPahoEvents;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.Protocol;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleV2;
import org.eclipse.paho.client.mqttv3.internal.tls.CertificateChainCleaner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class WebSocketSecureNetworkModuleV2 extends SSLNetworkModuleV2 {

    private static final String CLASS_NAME = WebSocketSecureNetworkModuleV2.class.getName();

    private PipedInputStream pipedInputStream;
    private WebSocketReceiver webSocketReceiver;
    private String uri;
    private String host;
    private int port;
    private ILogger logger;
    ByteBuffer recievedPayload;

    /**
     * Overrides the flush method.
     * This allows us to encode the MQTT payload into a WebSocket
     * Frame before passing it through to the real socket.
     */
    private ByteArrayOutputStream outputStream = new ExtendedByteArrayOutputStream(this);

    public WebSocketSecureNetworkModuleV2(
            SocketFactory socketFactory,
            SSLSocketFactory sslSocketFactory,
            X509TrustManager x509TrustManager,
            ConnectionSpec connectionSpec,
            List<Protocol> alpnProtocolList,
            String uri,
            String host,
            int port,
            String clientId,
            ILogger logger,
            IPahoEvents pahoEvents
    ) {
        super(
                socketFactory,
                sslSocketFactory,
                x509TrustManager,
                connectionSpec,
                alpnProtocolList,
                host,
                port,
                clientId,
                logger,
                pahoEvents
        );
        this.uri = uri;
        this.host = host;
        this.port = port;
        this.logger = logger;
        this.pipedInputStream = new PipedInputStream();
    }

    public void start() throws IOException, MqttException {
        super.start();
        WebSocketHandshake handshake = new WebSocketHandshake(super.getInputStream(),
                super.getOutputStream(), uri, host, port);
        handshake.execute();
        this.webSocketReceiver = new WebSocketReceiver(getSocketInputStream(), pipedInputStream);
        webSocketReceiver.start("WssSocketReceiver");

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

    public void stop() throws IOException {
        // Creating Close Frame
        WebSocketFrame frame = new WebSocketFrame((byte) 0x08, true, "1000".getBytes());
        byte[] rawFrame = frame.encodeFrame();
        getSocketOutputStream().write(rawFrame);
        getSocketOutputStream().flush();

        if (webSocketReceiver != null) {
            webSocketReceiver.stop();
        }
        super.stop();
    }

    public String getServerURI() {
        return "wss://" + host + ":" + port;
    }


}
