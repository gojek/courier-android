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
package org.eclipse.paho.client.mqttv10.internal.wire;

import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqtt.MqttInterceptor;
import org.eclipse.paho.client.mqttv10.internal.MqttInterceptorCallback;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An <code>MqttOutputStream</code> lets applications write instances of <code>MqttWireMessage</code>.
 */
public class MqttOutputStream extends OutputStream
{
	private static final String className = MqttOutputStream.class.getName();

	private final String TAG = "MQTTOUTPUTSTREAM";

	private BufferedOutputStream out;

	public MqttOutputStream(OutputStream out)
	{
		this.out = new BufferedOutputStream(out);
	}

	public void close() throws IOException
	{
		out.close();
	}

	public void flush() throws IOException
	{
		out.flush();
	}

	public void write(byte[] b) throws IOException
	{
		out.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException
	{
		out.write(b, off, len);
	}

	public void write(int b) throws IOException
	{
		out.write(b);
	}

	/**
	 * Writes an <code>MqttWireMessage</code> to the stream.
	 */
	public void write(MqttInterceptorCallback mqttInterceptorCallback, MqttWireMessage message) throws IOException, MqttException
	{
		final String methodName = "write";
		byte[] bytes = message.getHeader();
		byte[] pl = message.getPayload();
		// out.write(message.getHeader());
		// out.write(message.getPayload());
		out.write(bytes, 0, bytes.length);
		out.write(pl, 0, pl.length);

		intercept(mqttInterceptorCallback, message);

		// @TRACE 500= sent {0}
	}

	private void intercept(MqttInterceptorCallback mqttInterceptorCallback, MqttWireMessage mqttWireMessage) throws MqttException {

		byte[] bytes = mqttWireMessage.getHeader();
		byte[] pl = mqttWireMessage.getPayload();
		byte[] packet = new byte[(int) (bytes.length + pl.length)];
		System.arraycopy(bytes, 0, packet, 0, bytes.length);
		System.arraycopy(pl, 0, packet, bytes.length, pl.length);

		mqttInterceptorCallback.mqttMessageIntercepted(packet, true);
	}
}
