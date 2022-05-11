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
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * An on-the-wire representation of an MQTT CONNECT message.
 */
public class MqttConnect extends MqttWireMessage
{

	/** 38 - User Defined Pair (UTF-8 key value). */
	public static final byte USER_DEFINED_PAIR_IDENTIFIER = 0x26;

	public static String KEY = "Con";

	private String protocolName;

	private int protocolLevel;

	private String clientId;

	private boolean cleanSession;

	private MqttMessage willMessage;

	private String userName;

	private char[] password;

	private int keepAliveInterval;

	private String willDestination;

	private List<UserProperty> userProperties;

	/**
	 * Constructor for an on the wire MQTT connect message
	 * 
	 * @param info
	 * @param data
	 * @throws IOException
	 * @throws MqttException
	 */
	public MqttConnect(byte info, byte[] data) throws IOException, MqttException
	{
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais);

		protocolName = decodeUTF8(dis);
		protocolLevel = dis.readByte();
		byte connect_flags = dis.readByte();

		cleanSession = ((connect_flags >> 1) & 1) > 0;
		boolean willMessagePresent = ((connect_flags >> 2) & 1) > 0;
		int willMessageQos =  (connect_flags >> 3) & 3;
		boolean willMessageRetained = ((connect_flags >> 5) & 1) > 0;
		boolean userNamePresent = ((connect_flags >> 7) & 1) > 0;
		boolean passwordPresent = ((connect_flags >> 6) & 1) > 0;

		keepAliveInterval = dis.readUnsignedShort();
		clientId = decodeUTF8(dis);

		if (willMessagePresent)
		{
			willMessage = new MqttMessage();
			willDestination = decodeUTF8(dis);
			int payloadLength = dis.readShort();
			byte payload[] = new byte[payloadLength];
			dis.read(payload, 0, payloadLength);
			willMessage.setPayload(payload);
			willMessage.setQos(willMessageQos);
			willMessage.setRetained(willMessageRetained);
		}

		if(userNamePresent) {
			userName = decodeUTF8(dis);
			if(passwordPresent) {
				password = decodeUTF8(dis).toCharArray();
			}
		}

		dis.close();
	}

	public MqttConnect(
			String clientId,
			boolean cleanSession,
			int keepAliveInterval,
			String userName,
			char[] password,
			MqttMessage willMessage,
			String willDestination,
			String protocolName,
			int protocolLevel,
			List<UserProperty> userProperties
	) {
		super(MqttWireMessage.MESSAGE_TYPE_CONNECT);
		this.protocolName = protocolName;
		this.protocolLevel = protocolLevel;
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAliveInterval = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.willMessage = willMessage;
		this.willDestination = willDestination;
		this.userProperties = userProperties;
	}

	public String toString()
	{
		String rc = super.toString();
		rc += " clientId " + clientId + " keepAliveInterval " + keepAliveInterval;
		return rc;
	}

	protected byte getMessageInfo()
	{
		return (byte) 0;
	}

	public boolean isCleanSession()
	{
		return cleanSession;
	}

	protected byte[] getVariableHeader() throws MqttException
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			encodeUTF8(dos, protocolName);
			dos.write(protocolLevel);

			byte connectFlags = 0;

			if (cleanSession)
			{
				connectFlags |= 0x02;
			}

			if (willMessage != null)
			{
				connectFlags |= 0x04;
				connectFlags |= (willMessage.getQos() << 3);
				if (willMessage.isRetained())
				{
					connectFlags |= 0x20;
				}
			}

			if (userName != null)
			{
				connectFlags |= 0x80;
				if (password != null)
				{
					connectFlags |= 0x40;
				}
			}
			dos.write(connectFlags);
			dos.writeShort(keepAliveInterval);
			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ioe)
		{
			throw new MqttException(ioe);
		}
	}

	public byte[] getPayload() throws MqttException
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			encodeUTF8(dos, clientId);

			if (willMessage != null)
			{
				encodeUTF8(dos, willDestination);
				dos.writeShort(willMessage.getPayload().length);
				dos.write(willMessage.getPayload());
			}

			if (userName != null)
			{
				encodeUTF8(dos, userName);
				if (password != null)
				{
					encodeUTF8(dos, new String(password));
				}
			}

			if (userProperties != null && !userProperties.isEmpty()) {
				for (UserProperty property : userProperties) {
					dos.writeByte(USER_DEFINED_PAIR_IDENTIFIER);
					encodeUTF8(dos, property.getKey());
					encodeUTF8(dos, property.getValue());
				}
			}

			dos.flush();
			return baos.toByteArray();
		}
		catch (IOException ex)
		{
			throw new MqttException(ex);
		}
	}

	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired()
	{
		return false;
	}

	public String getKey()
	{
		return new String(KEY);
	}

	public String getClientId() {
		return clientId;
	}

	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public String getUserName() {
		return userName;
	}

	public char[] getPassword() {
		return password;
	}

	public MqttMessage getWillMessage() {
		return willMessage;
	}

	public String getWillDestination() {
		return willDestination;
	}
}
