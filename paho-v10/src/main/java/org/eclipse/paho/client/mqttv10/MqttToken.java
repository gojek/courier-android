/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.paho.client.mqttv10;

import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqttv10.internal.Token;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage;

/**
 * Provides a mechanism for tracking the completion of an asynchronous action.
 * <p>
 * A token that implements the ImqttToken interface is returned from all non-blocking method with the exception of publish.
 * </p>
 * 
 * @see IMqttToken
 */

public class MqttToken implements IMqttToken, IToken
{
	/**
	 * A reference to the the class that provides most of the implementation of the MqttToken. MQTT application programs must not use the internal class.
	 */
	public Token internalTok = null;

	public MqttToken()
	{
	}

	public MqttToken(String logContext)
	{
		internalTok = new Token(logContext);
	}

	public MqttException getException()
	{
		return internalTok.getException();
	}

	public boolean isComplete()
	{
		return internalTok.isComplete();
	}

	public void setActionCallback(IMqttActionListener listener)
	{
		internalTok.setActionCallback(listener);

	}

	public IMqttActionListener getActionCallback()
	{
		return internalTok.getActionCallback();
	}

	public void waitForCompletion() throws MqttException
	{
		internalTok.waitForCompletion(-1);
	}

	public void waitForCompletion(long timeout) throws MqttException
	{
		internalTok.waitForCompletion(timeout);
	}

	public IMqttAsyncClient getClient()
	{
		return internalTok.getClient();
	}

	public String[] getTopics()
	{
		return internalTok.getTopics();
	}

	public Object getUserContext()
	{
		return internalTok.getUserContext();
	}

	public void setUserContext(Object userContext)
	{
		internalTok.setUserContext(userContext);
	}

	public int getMessageId()
	{
		return internalTok.getMessageID();
	}

	public MqttWireMessage getResponse() {
		return internalTok.getResponse();
	}
}
