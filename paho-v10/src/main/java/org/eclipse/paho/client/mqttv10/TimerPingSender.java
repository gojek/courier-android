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

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqtt.ILogger;
import org.eclipse.paho.client.mqtt.MqttPingSender;
import org.eclipse.paho.client.mqttv10.internal.ClientComms;


/**
 * Default ping sender implementation
 * 
 * <p>
 * This class implements the {@link IMqttPingSender} pinger interface allowing applications to send ping packet to server every keep alive interval.
 * </p>
 * 
 * @see MqttPingSender
 */
public class TimerPingSender implements MqttPingSender
{
	private ClientComms comms;

	private ILogger logger;

	private Timer timer;

	private final static String className = TimerPingSender.class.getName();

	private final String TAG = "TimerPingSender";

	public void init(ClientComms comms, ILogger logger)
	{
		if (comms == null)
		{
			throw new IllegalArgumentException("ClientComms cannot be null.");
		}
		this.comms = comms;
		this.logger = logger;
	}

	public void start()
	{
		final String methodName = "start";
		String clientid = comms.getClient().getClientId();

		// @Trace 659=start timer for client:{0}

		timer = new Timer("MQTT Ping: " + clientid);
		// Check ping after first keep alive interval.
		timer.schedule(new PingTask(), comms.getKeepAlive());
	}

	public void stop()
	{
		final String methodName = "stop";
		// @Trace 661=stop
		logger.d(TAG, "Stopping timer");
		if (timer != null)
		{
			timer.cancel();
		}
	}

	public void schedule(long delayInMilliseconds)
	{
		timer.schedule(new PingTask(), delayInMilliseconds);
	}

	class PingTask extends TimerTask
	{
		private static final String methodName = "PingTask.run";

		public void run()
		{
			// @Trace 660=Check schedule at {0}
			logger.d(TAG, "in ping timer task run function");
			comms.checkForActivity();
		}
	}
}
