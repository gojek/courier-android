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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqttv10.MqttPersistable;
import org.eclipse.paho.client.mqttv10.MqttToken;
import org.eclipse.paho.client.mqttv10.internal.ExceptionHelper;

/**
 * An on-the-wire representation of an MQTT message.
 */
public abstract class MqttWireMessage
{
	protected static final String STRING_ENCODING = "UTF-8";

	public static final byte MESSAGE_TYPE_CONNECT = 1;

	public static final byte MESSAGE_TYPE_CONNACK = 2;

	public static final byte MESSAGE_TYPE_PUBLISH = 3;

	public static final byte MESSAGE_TYPE_PUBACK = 4;

	public static final byte MESSAGE_TYPE_PUBREC = 5;

	public static final byte MESSAGE_TYPE_PUBREL = 6;

	public static final byte MESSAGE_TYPE_PUBCOMP = 7;

	public static final byte MESSAGE_TYPE_SUBSCRIBE = 8;

	public static final byte MESSAGE_TYPE_SUBACK = 9;

	public static final byte MESSAGE_TYPE_UNSUBSCRIBE = 10;

	public static final byte MESSAGE_TYPE_UNSUBACK = 11;

	public static final byte MESSAGE_TYPE_PINGREQ = 12;

	public static final byte MESSAGE_TYPE_PINGRESP = 13;

	public static final byte MESSAGE_TYPE_DISCONNECT = 14;

	String packet_names[] = { "reserved", "CONNECT", "CONNACK", "PUBLISH", "PUBACK", "PUBREC", "PUBREL", "PUBCOMP", "SUBSCRIBE", "SUBACK", "UNSUBSCRIBE", "UNSUBACK", "PINGREQ",
			"PINGRESP", "DISCONNECT" };

	/** The type of the message (e.g. CONNECT, PUBLISH, PUBACK) */
	private byte type;

	/** The MQTT message ID */
	protected int msgId;

	protected boolean duplicate = false;

	private byte[] encodedHeader = null;

	/**
	 * The token associated with the message. It needs to be stored here,
	 * because QoS 0 messages do not have an ID, and tokens for these messages
	 * can thus not be stored in the Token Store.
	 */
	private MqttToken token;

	public MqttWireMessage(byte type)
	{
		this.type = type;
		// Use zero as the default message ID. Can't use -1, as that is serialized
		// as 65535, which would be a valid ID.
		this.msgId = 0;
	}

	/**
	 * Sub-classes should override this to encode the message info. Only the least-significant four bits will be used.
	 * @return
	 */
	abstract protected boolean[] getMessageInfo();

	/**
	 * Sub-classes should override this method to supply the payload bytes.
	 */
	public byte[] getPayload() throws MqttException
	{
		return new byte[0];
	}

	/**
	 * Returns the type of the message.
	 */
	public byte getType()
	{
		return type;
	}

	/**
	 * Returns the MQTT message ID.
	 */
	public int getMessageId()
	{
		return msgId;
	}

	/**
	 * Sets the MQTT message ID.
	 */
	public void setMessageId(int msgId)
	{
		this.msgId = msgId;
	}

	/**
	 * Returns a key associated with the message. For most message types this will be unique. For connect, disconnect and ping only one message of this type is allowed so a fixed
	 * key will be returned
	 * 
	 * @return key a key associated with the message
	 */
	public String getKey()
	{
		return new Integer(getMessageId()).toString();
	}

	public byte[] getHeader() throws MqttException
	{
		if (encodedHeader == null)
		{
			try
			{
				int first = ((getType() & 0x0f) << 4);//^ (getMessageInfo() & 0x0f);
				boolean[] messageInfoArray = getMessageInfo();
				for(int i = 4; i >= 1; i --) {
					first = first | (messageInfoArray[i] ? 2^(i -1) : 0);
				}

				byte[] varHeader = getVariableHeader();
				int remLen = varHeader.length + getPayload().length;

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeByte(first);

				if(messageInfoArray.length == 9) {
					dos.writeBoolean(messageInfoArray[0]);
				}
				dos.write(encodeMBI(remLen));
				dos.write(varHeader);
				dos.flush();
				encodedHeader = baos.toByteArray();
			}
			catch (IOException ioe)
			{
				throw new MqttException(ioe);
			}
		}
		return encodedHeader;
	}

	protected abstract byte[] getVariableHeader() throws MqttException;

	/**
	 * Returns whether or not this message needs to include a message ID.
	 */
	public boolean isMessageIdRequired()
	{
		return true;
	}

	public static MqttWireMessage createWireMessage(MqttPersistable data, String mqttVersion) throws MqttException
	{
		byte[] payload = data.getPayloadBytes();
		// The persistable interface allows a message to be restored entirely in the header array
		// Need to treat these two arrays as a single array of bytes and use the decoding
		// logic to identify the true header/payload split
		if (payload == null)
		{
			payload = new byte[0];
		}
		org.eclipse.paho.client.mqttv10.internal.wire.MultiByteArrayInputStream mbais = new org.eclipse.paho.client.mqttv10.internal.wire.MultiByteArrayInputStream(data.getHeaderBytes(), data.getHeaderOffset(), data.getHeaderLength(), payload, data.getPayloadOffset(),
				data.getPayloadLength());
		return createWireMessage(mbais, mqttVersion);
	}

	public static MqttWireMessage createWireMessage(byte[] bytes, String mqttVersion) throws MqttException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		return createWireMessage(bais, mqttVersion);
	}

	private static MqttWireMessage createWireMessage(InputStream inputStream, String mqttVersion) throws MqttException
	{
		try
		{
			org.eclipse.paho.client.mqttv10.internal.wire.CountingInputStream counter = new org.eclipse.paho.client.mqttv10.internal.wire.CountingInputStream(inputStream);
			DataInputStream in = new DataInputStream(counter);
			int first = in.readUnsignedByte();
			byte type = (byte) (first >> 4);
			byte info = (byte) (first &= 0x0f);
//			if(type == MqttWireMessage.MESSAGE_TYPE_PUBLISH) {
//				in.readBoolean(); //ignore retryable bit since client doesn't take any action on this
//			}

			long remLen = readMBI(in).getValue();
			long totalToRead = counter.getCounter() + remLen;

			MqttWireMessage result;
			long remainder = totalToRead - counter.getCounter();
			byte[] data = new byte[0];
			// The remaining bytes must be the payload...
			if (remainder > 0)
			{
				data = new byte[(int) remainder];
				in.readFully(data, 0, data.length);
			}

			if (type == MqttWireMessage.MESSAGE_TYPE_CONNECT)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttConnect(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PUBLISH)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPublish(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PUBACK)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPubAck(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PUBCOMP)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPubComp(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_CONNACK)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttConnack(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PINGREQ)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPingReq(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PINGRESP)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPingResp(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttSubscribe(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_SUBACK)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttSuback(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_UNSUBSCRIBE)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttUnsubscribe(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_UNSUBACK)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttUnsubAck(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PUBREL)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPubRel(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_PUBREC)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttPubRec(info, data);
			}
			else if (type == MqttWireMessage.MESSAGE_TYPE_DISCONNECT)
			{
				result = new org.eclipse.paho.client.mqttv10.internal.wire.MqttDisconnect(info, data);
			}
			else
			{
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
			}
			return result;
		}
		catch (IOException io)
		{
			throw new MqttException(io);
		}
	}

	protected static byte[] encodeMBI(long number)
	{
		int numBytes = 0;
		long no = number;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		// Encode the remaining length fields in the four bytes
		do
		{
			byte digit = (byte) (no % 128);
			no = no / 128;
			if (no > 0)
			{
				digit |= 0x80;
			}
			bos.write(digit);
			numBytes++;
		}
		while ((no > 0) && (numBytes < 4));

		return bos.toByteArray();
	}

	/**
	 * Decodes an MQTT Multi-Byte Integer from the given stream.
	 */
	protected static org.eclipse.paho.client.mqttv10.internal.wire.MultiByteInteger readMBI(DataInputStream in) throws IOException
	{
		byte digit;
		long msgLength = 0;
		int multiplier = 1;
		int count = 0;

		do
		{
			digit = in.readByte();
			count++;
			msgLength += ((digit & 0x7F) * multiplier);
			multiplier *= 128;
		}
		while ((digit & 0x80) != 0);

		return new org.eclipse.paho.client.mqttv10.internal.wire.MultiByteInteger(msgLength, count);
	}

	protected byte[] encodeMessageId() throws MqttException
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

	public boolean isRetryable()
	{
		return false;
	}

	public void setDuplicate(boolean duplicate)
	{
		this.duplicate = duplicate;
	}

	/**
	 * Encodes a String given into UTF-8, before writing this to the DataOutputStream the length of the encoded string is encoded into two bytes and then written to the
	 * DataOutputStream. @link{DataOutputStream#writeUFT(String)} should be no longer used. @link{DataOutputStream#writeUFT(String)} does not correctly encode UTF-16 surrogate
	 * characters.
	 * 
	 * @param dos
	 *            The stream to write the encoded UTF-8 String to.
	 * @param stringToEncode
	 *            The String to be encoded
	 * @throws MqttException
	 *             Thrown when an error occurs with either the encoding or writing the data to the stream
	 */
	protected void encodeUTF8(DataOutputStream dos, String stringToEncode) throws MqttException
	{
		try
		{

			byte[] encodedString = stringToEncode.getBytes("UTF-8");
			byte byte1 = (byte) ((encodedString.length >>> 8) & 0xFF);
			byte byte2 = (byte) ((encodedString.length >>> 0) & 0xFF);

			dos.write(byte1);
			dos.write(byte2);
			dos.write(encodedString);
		}
		catch (UnsupportedEncodingException ex)
		{
			throw new MqttException(ex);
		}
		catch (IOException ex)
		{
			throw new MqttException(ex);
		}
	}

	/**
	 * Decodes a UTF-8 string from the DataInputStream provided. @link(DataInoutStream#readUTF()) should be no longer used, because @link(DataInoutStream#readUTF()) does not decode
	 * UTF-16 surrogate characters correctly.
	 * 
	 * @param input
	 *            The input stream from which to read the encoded string
	 * @return a decoded String from the DataInputStream
	 * @throws MqttException
	 *             thrown when an error occurs with either reading from the stream or decoding the encoded string.
	 */
	protected String decodeUTF8(DataInputStream input) throws MqttException
	{
		int encodedLength;
		try
		{
			encodedLength = input.readUnsignedShort();

			byte[] encodedString = new byte[encodedLength];
			input.readFully(encodedString);

			return new String(encodedString, "UTF-8");
		}
		catch (IOException ex)
		{
			throw new MqttException(ex);
		}
	}

	/**
	 * Get the token associated with the message.
	 *
	 * @return The token associated with the message.
	 */
	public MqttToken getToken() {
		return token;
	}

	/**
	 * Set the token associated with the message.
	 *
	 * @param token the token associated with the message.
	 */
	public void setToken(MqttToken token) {
		this.token = token;
	}

	public String toString()
	{
		return packet_names[type];
	}

	public String packetName() {
		return packet_names[type];
	}

	public boolean[] byteToBitArray(byte b) {
		boolean[] buff = new boolean[8];
		int index = 0;
		for (int i = 7; i >= 0; i--) {
			buff[index++] = ((b >>> i) & 1) == 1;
		}
		return buff;
	}
}
