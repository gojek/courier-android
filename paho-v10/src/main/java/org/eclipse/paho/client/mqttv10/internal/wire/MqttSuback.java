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
import java.io.DataInputStream;
import java.io.IOException;

import org.eclipse.paho.client.mqtt.MqttException;

/**
 * An on-the-wire representation of an MQTT SUBACK.
 */
public class MqttSuback extends MqttAck
{
	private int[] grantedQos; // Not currently made available to anyone.

	public MqttSuback(byte info, byte[] data) throws IOException
	{
		super(MqttWireMessage.MESSAGE_TYPE_SUBACK);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);
		msgId = dis.readUnsignedShort();
		int index = 0;
		grantedQos = new int[data.length - 2];
		int qos = dis.read();
		while (qos != -1)
		{
			grantedQos[index] = qos;
			index++;
			qos = dis.read();
		}
		dis.close();
	}

	protected byte[] getVariableHeader() throws MqttException
	{
		// Not needed, as the client never encodes a SUBACK
		return new byte[0];
	}

	public String toString()
	{
		String rc = super.toString() + " granted Qos";
		for (int i = 0; i < grantedQos.length; ++i)
		{
			rc += " " + grantedQos[i];
		}
		return rc;
	}

	public int[] getGrantedQos() {
		return grantedQos;
	}
}
