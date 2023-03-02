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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

import org.eclipse.paho.client.mqtt.MqttException;

/**
 * Special comms class that allows an MQTT client to use a non TCP / optimised mechanism to talk to an MQTT server when running in the same JRE instance as the MQTT server.
 * 
 * This class checks for the existence of the optimised comms adatper class i.e. the one that provides the optimised communication mechanism. If not available the request to
 * connect using the optimised mechanism is rejected.
 * 
 * The only known server that implements this is the microbroker:- an MQTT server that ships with a number of IBM products.
 */
public class LocalNetworkModule implements NetworkModule
{
	private Class LocalListener;

	private String brokerName;

	private Object localAdapter;

	public LocalNetworkModule(String brokerName)
	{
		this.brokerName = brokerName;
	}

	public void start() throws IOException, MqttException
	{
		if (!ExceptionHelper.isClassAvailable("com.ibm.mqttdirect.modules.local.bindings.LocalListener"))
		{
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
		}
		try
		{
			LocalListener = Class.forName("com.ibm.mqttdirect.modules.local.bindings.LocalListener");
			Method connect_m = LocalListener.getMethod("connect", new Class[] { String.class });
			localAdapter = connect_m.invoke(null, new Object[] { brokerName });
		}
		catch (Exception e)
		{
		}
		if (localAdapter == null)
		{
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR);
		}
	}

	public InputStream getInputStream() throws IOException
	{
		InputStream stream = null;
		try
		{
			Method m = LocalListener.getMethod("getClientInputStream", new Class[] {});
			stream = (InputStream) m.invoke(this.localAdapter, new Object[] {});
		}
		catch (Exception e)
		{
		}
		return stream;
	}

	public OutputStream getOutputStream() throws IOException
	{
		OutputStream stream = null;
		try
		{
			Method m = LocalListener.getMethod("getClientOutputStream", new Class[] {});
			stream = (OutputStream) m.invoke(this.localAdapter, new Object[] {});
		}
		catch (Exception e)
		{
		}
		return stream;
	}

	public void stop() throws IOException
	{
		if (localAdapter != null)
		{
			try
			{
				Method m = LocalListener.getMethod("close", new Class[] {});
				m.invoke(this.localAdapter, new Object[] {});
			}
			catch (Exception e)
			{
			}
		}
	}

	@Override
	public Socket getSocket()
	{
		// TODO Auto-generated method stub
		return null;
	}
}