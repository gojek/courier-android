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

import org.eclipse.paho.client.mqttv10.internal.ClientComms;

/**
 * Represents an object used to send ping packet to MQTT broker every keep alive interval.
 */
public interface MqttPingSender
{

	/**
	 * Initial method. Pass interal state of current client in.
	 * 
	 * @param The
	 *            core of the client, which holds the state information for pending and in-flight messages.
	 */
	public void init(ClientComms comms, ILogger logger);

	/**
	 * Start ping sender. It will be called after connection is success.
	 */
	public void start();

	/**
	 * Stop ping sender. It is called if there is any errors or connection shutdowns.
	 */
	public void stop();

	/**
	 * Schedule next ping in certain delay.
	 * 
	 * @param delay
	 *            in milliseconds.
	 */
	public void schedule(long delayInMilliseconds);

}
