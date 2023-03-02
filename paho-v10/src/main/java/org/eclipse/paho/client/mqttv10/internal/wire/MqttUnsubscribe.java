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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqtt.MqttException;

/**
 * An on-the-wire representation of an MQTT UNSUBSCRIBE message.
 */
public class MqttUnsubscribe extends org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage
{

	private String[] names;

	private int count;

	/**
	 * Constructs an MqttUnsubscribe
	 */
	public MqttUnsubscribe(String[] names)
	{
		super(org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
		this.names = names;
		this.count = names.length;
	}

	/**
	 * Constructor for an on the wire MQTT un-subscribe message
	 * 
	 * @param info
	 * @param data
	 * @throws IOException
	 */
	public MqttUnsubscribe(byte info, byte[] data) throws IOException
	{
		super(org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		msgId = dis.readUnsignedShort();

		count = 0;
		names = new String[10];
		boolean end = false;
		while (!end)
		{
			try
			{
				names[count] = decodeUTF8(dis);
				count ++;
			}
			catch (Exception e)
			{
				end = true;
			}
		}
		dis.close();
	}

	/**
	 * @return string representation of this un-subscribe packet
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(super.toString());
		sb.append(" names:[");
		for (int i = 0; i < count; i++)
		{
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append("\"" + names[i] + "\"");
		}
		sb.append("]");
		return sb.toString();
	}

	protected boolean[] getMessageInfo()
	{
		return byteToBitArray((byte) (2 | (duplicate ? 8 : 0)));
	}

	protected byte[] getVariableHeader() throws MqttException
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			dos.writeShort(msgId);
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex)
		{
			throw new MqttException(ex);
		}
	}

	public byte[] getPayload() throws MqttException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		for (int i = 0; i < names.length; i++)
		{
			encodeUTF8(dos, names[i]);
		}
		return baos.toByteArray();
	}

	public boolean isRetryable()
	{
		return true;
	}

	public String[] getNames() {
		return names;
	}

	public int getCount() {
		return count;
	}
}
