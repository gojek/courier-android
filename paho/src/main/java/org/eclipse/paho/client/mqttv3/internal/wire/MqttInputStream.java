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
package org.eclipse.paho.client.mqttv3.internal.wire;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttInterceptor;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.MqttInterceptorCallback;
import org.eclipse.paho.client.mqttv3.internal.MqttMessageInterceptorCallback;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An <code>MqttInputStream</code> lets applications read instances of <code>MqttWireMessage</code>.
 */
public class MqttInputStream extends InputStream
{
	private static final String className = MqttInputStream.class.getName();

	private final String TAG = "MQTTINPUTSTREAM";

	private DataInputStream in;

	private String mqttVersion;

	public MqttInputStream(InputStream in, String mqttVersion)
	{
		this.in = new DataInputStream(in);
		this.mqttVersion = mqttVersion;
	}

	public int read() throws IOException
	{
		return in.read();
	}

	public int available() throws IOException
	{
		return in.available();
	}

	public void close() throws IOException
	{
		in.close();
	}

	/**
	 * Reads an <code>MqttWireMessage</code> from the stream.
	 * @param mqttInterceptorCallback
	 */
	public MqttWireMessage readMqttWireMessage(MqttInterceptorCallback mqttInterceptorCallback, MqttMessageInterceptorCallback messageInterceptorCallback) throws IOException, MqttException
	{
		final String methodName = "readMqttWireMessage";
		ByteArrayOutputStream bais = new ByteArrayOutputStream();
		byte first = in.readByte();
		byte type = (byte) ((first >>> 4) & 0x0F);
		if ((type < MqttWireMessage.MESSAGE_TYPE_CONNECT) || (type > MqttWireMessage.MESSAGE_TYPE_DISCONNECT))
		{
			// Invalid MQTT message type...
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_INVALID_MESSAGE);
		}
		long remLen = MqttWireMessage.readMBI(in).getValue();
		bais.write(first);
		// bit silly, we decode it then encode it
		bais.write(MqttWireMessage.encodeMBI(remLen));
		byte[] packet = new byte[(int) (bais.size() + remLen)];
		in.readFully(packet, bais.size(), packet.length - bais.size());
		byte[] header = bais.toByteArray();
		System.arraycopy(header, 0, packet, 0, header.length);
		MqttWireMessage message = MqttWireMessage.createWireMessage(packet, mqttVersion);
		byte[] updatedPacket = packet;
		if (message instanceof MqttPublish) {
			updatedPacket = messageInterceptorCallback.mqttMessageIntercepted(
					((MqttPublish) message).getTopicName(),
					packet,
					false
			);
		}
		intercept(mqttInterceptorCallback, updatedPacket);
		// @TRACE 501= received {0}
		return message;
	}

	private void intercept(MqttInterceptorCallback mqttInterceptorCallback, byte[] mqttWireMessageBytes) {
		mqttInterceptorCallback.mqttMessageIntercepted(mqttWireMessageBytes, false);
	}
}