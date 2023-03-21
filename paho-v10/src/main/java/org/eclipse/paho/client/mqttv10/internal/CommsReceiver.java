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
package org.eclipse.paho.client.mqttv10.internal;


import org.eclipse.paho.client.mqtt.ILogger;
import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqtt.MqttInterceptor;
import org.eclipse.paho.client.mqttv10.MqttToken;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttInputStream;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

/**
 * Receives MQTT packets from the server.
 */
public class CommsReceiver implements Runnable
{
	private boolean running = false;

	private Object lifecycle = new Object();

	private ClientState clientState = null;

	private ClientComms clientComms = null;

	private MqttInputStream in;

	private CommsTokenStore tokenStore = null;

	private Thread recThread = null;

	private volatile boolean receiving;

	private Socket socket = null;

	private ILogger logger;

	private MqttInterceptorCallback mqttInterceptorCallback;

	private final static String className = CommsReceiver.class.getName();

	private final String TAG = "CommsReciever";

	public CommsReceiver(
			ClientComms clientComms,
			ClientState clientState,
			CommsTokenStore tokenStore,
			InputStream in,
			Socket socket,
			ILogger logger,
			MqttInterceptorCallback mqttInterceptorCallback
	)
	{
		this.socket = socket;
		this.in = new MqttInputStream(in, clientComms.getClient().getMqttVersion());
		this.clientComms = clientComms;
		this.clientState = clientState;
		this.tokenStore = tokenStore;
		this.logger = logger;
		this.mqttInterceptorCallback = mqttInterceptorCallback;
	}

	/**
	 * Starts up the Receiver's thread.
	 */
	public void start(String threadName)
	{
		final String methodName = "start";
		// @TRACE 855=starting
		synchronized (lifecycle)
		{
			if (running == false)
			{
				running = true;
				recThread = new Thread(this, threadName);
				recThread.start();
			}
		}
	}

	/**
	 * Stops the Receiver's thread. This call will block.
	 */
	public void stop()
	{
		final String methodName = "stop";
		synchronized (lifecycle)
		{
			// @TRACE 850=stopping
			logger.d(TAG, "Reciever stopping started");
			if (running)
			{
				running = false;
				receiving = false;
				if (recThread != null && !Thread.currentThread().equals(recThread))
				{
					String threadId = recThread.getName() + recThread.getId();
					long sTime = System.currentTimeMillis();
					try
					{
						// Wait for the thread to finish.
						recThread.join();
					}
					catch (InterruptedException ex)
					{
					}
					finally {
						logger.logMqttThreadEvent("receiver_stop", System.currentTimeMillis() - sTime, threadId);
					}
				}
			}
		}
		recThread = null;
		// @TRACE 851=stopped
		logger.d(TAG, "reciever stopping completed");
	}

	/**
	 * Run loop to receive messages from the server.
	 */
	public void run()
	{
		final String methodName = "run";
		MqttToken token = null;
		final String threadId = recThread.getName() + recThread.getId();

		while (running && (in != null))
		{
			long sTime = System.currentTimeMillis();
			try
			{
				// @TRACE 852=network read message
				receiving = true;
				MqttWireMessage message = in.readMqttWireMessage(mqttInterceptorCallback);
				receiving = false;
				if (message instanceof MqttPublish)
				{
					String logMsg = "socket read completed for message";
					logger.d(TAG, logMsg + " : " + ((MqttPublish) message).getMessage().toString());
					int length = ((MqttPublish) message).getHeaderLength() + ((MqttPublish) message).getPayloadLength();
					logger.d(TAG, "bytes read on socket : " + length);
					if(((MqttPublish) message).getMessage().getQos() == 1) {
						logger.logEvent("offline_msg_rcv_event", true, this.clientComms.getClient().getServerURI()
								, (System.currentTimeMillis() - sTime), null, 0, System.currentTimeMillis(), length, threadId, message.getMessageId());
					}
				}
				else if (message instanceof MqttAck)
				{
					logger.d(TAG, "socket read completed for ack : " + ((MqttAck) message).toString());
					long key = ((MqttAck) message).getMessageId();
				}
				else
				{
					logger.d(TAG, "socket read completed");
				}
				logSocketProperties();

				if (message instanceof MqttAck)
				{
					if(tokenStore != null) {
						token = tokenStore.getToken(message);
					}

					if (token != null)
					{
						logger.logEvent("mqtt_ack_rcv_event", true, this.clientComms.getClient().getServerURI()
								, (System.currentTimeMillis() - sTime),null, 0, System.currentTimeMillis(), 0, threadId, message.getMessageId());
						synchronized (token)
						{
							// Ensure the notify processing is done under a lock on the token
							// This ensures that the send processing can complete before the
							// receive processing starts! ( request and ack and ack processing
							// can occur before request processing is complete if not!
							clientState.notifyReceivedAck((MqttAck) message);
						}
					}
					else
					{
						MqttException ex = new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
						logger.logEvent("mqtt_ack_rcv_event", false, this.clientComms.getClient().getServerURI()
								, (System.currentTimeMillis() - sTime), ex, MqttException.REASON_CODE_UNEXPECTED_ERROR, System.currentTimeMillis(), 0, threadId, message.getMessageId());
						// It its an ack and there is no token then something is not right.
						// An ack should always have a token assoicated with it.
						throw ex;
					}
				}
				else
				{
					// A new message has arrived
					clientState.notifyReceivedMsg(message);
				}

				logger.logMessageReceivedData(message);
			}
			catch (MqttException ex)
			{
				// @TRACE 856=Stopping, MQttException
				logger.e(TAG, "exception occured , cause : " , ex);
				running = false;
				logger.logEvent("offline_msg_rcv_event", false, this.clientComms.getClient().getServerURI()
						, (System.currentTimeMillis() - sTime), ex, ex.getReasonCode(), System.currentTimeMillis(), 0, threadId, -1);
				// Token maybe null but that is handled in shutdown
				clientComms.shutdownConnection(token, ex);
			}
			catch (IOException ioe)
			{
				// @TRACE 853=Stopping due to IOException
				logger.e(TAG, "IO excetion occured , cause : ", ioe);
				logger.logEvent("offline_msg_rcv_event", false, this.clientComms.getClient().getServerURI()
						, (System.currentTimeMillis() - sTime), ioe, MqttException.REASON_CODE_CONNECTION_LOST, System.currentTimeMillis(), 0, threadId, -1);

				running = false;
				// An EOFException could be raised if the broker processes the
				// DISCONNECT and ends the socket before we complete. As such,
				// only shutdown the connection if we're not already shutting down.
				if (!clientComms.isDisconnecting())
				{
					clientComms.shutdownConnection(token, new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ioe));
				} // else {
			}
			finally
			{
				receiving = false;
			}
		}

		// @TRACE 854=<
	}

	public boolean isRunning()
	{
		return running;
	}

	/**
	 * Returns the receiving state.
	 * 
	 * @return true if the receiver is receiving data, false otherwise.
	 */
	public boolean isReceiving()
	{
		return receiving;
	}

	private void logSocketProperties()
	{
		try
		{
			if (socket.getChannel() != null)
			{
				logger.d(TAG, "is socket channel blocking : " + socket.getChannel().isBlocking());
				logger.d(TAG, "is socket channel connected : " + socket.getChannel().isConnected());
				logger.d(TAG, "is socket channel connection pending : " + socket.getChannel().isConnectionPending());
				logger.d(TAG, "is socket channel open : " + socket.getChannel().isOpen());
				logger.d(TAG, "is socket channel connected : " + socket.getChannel().isRegistered());
				logger.d(TAG, "socket channel validOps: " + socket.getChannel().validOps());
			}
			logger.d(TAG, "is socket closed : " + socket.isClosed() + "  is socket connected : " + socket.isConnected() + "  is socket input shutdown : " + socket.isInputShutdown() + "  is socket output shutdown : " + socket.isOutputShutdown());
			//Logger.d(TAG, "is socket keep alive on: " + socket.getKeepAlive());
			//Logger.d(TAG, "is socket tcp no delay on: " + socket.getTcpNoDelay());
			//Logger.d(TAG, "is socket OOBline enabled : " + socket.getOOBInline());
			//Logger.d(TAG, "is socket bound : " + socket.isBound());
			//Logger.d(TAG, "is socket closed : " + socket.isClosed());
			//Logger.d(TAG, "is socket connected : " + socket.isConnected());
			//Logger.d(TAG, "is socket input shutdown : " + socket.isInputShutdown());
			//Logger.d(TAG, "is socket output shutdown : " + socket.isOutputShutdown());
			//Logger.d(TAG, "socket receive buffer size : " + socket.getReceiveBufferSize());
			//Logger.d(TAG, "socket send buffer size : " + socket.getSendBufferSize());
			//Logger.d(TAG, "socket linger timeout : " + socket.getSoLinger());
			//Logger.d(TAG, "socket timeout : " + socket.getSoTimeout());
			//Logger.d(TAG, "socket traffic class : " + socket.getTrafficClass());
		}
		catch (Exception ex)
		{
			logger.e(TAG, "exception during taking logs", ex);
		}
	}
}
