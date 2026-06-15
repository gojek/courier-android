/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    https://www.eclipse.org/legal/epl-2.0
 * and the Eclipse Distribution License is available at 
 *   https://www.eclipse.org/org/documents/edl-v10.php
 *
 * Contributors:
 *    Dave Locke - initial API and implementation and/or initial documentation
 *    Ian Craggs - per subscription message handlers (bug 466579)
 *    Ian Craggs - ack control (bug 472172)
 *    James Sutton - Automatic Reconnect & Offline Buffering
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttAuth;
import org.eclipse.paho.mqttv5.common.packet.MqttDisconnect;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.packet.MqttPubAck;
import org.eclipse.paho.mqttv5.common.packet.MqttPubComp;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttReturnCode;
import org.eclipse.paho.mqttv5.common.packet.UserProperty;
import org.eclipse.paho.mqttv5.common.util.MqttTopicValidator;

/**
 * Bridge between Receiver and the external API. This class gets called by
 * Receiver, and then converts the comms-centric MQTT message objects into ones
 * understood by the external API.
 */
public class CommsCallback implements Runnable {
	private static final String CLASS_NAME = CommsCallback.class.getName();
	private final String TAG = "CommsCallback";

	private static final int INBOUND_QUEUE_SIZE = 10;
	private MqttCallback mqttCallback;
	private MqttCallback reconnectInternalCallback;
	private HashMap<Integer, IMqttMessageListener> callbackMap; // Map of message handler callbacks to internal IDs
	private HashMap<String, Integer> callbackTopicMap; // Map of Topic Strings to internal callback Ids
	private HashMap<Integer, Integer> subscriptionIdMap; // Map of Subscription Ids to callback Ids
	private AtomicInteger messageHandlerId = new AtomicInteger(0);
	private ClientComms clientComms;
	private ArrayList<MqttPublish> messageQueue;
	private ArrayList<MqttToken> completeQueue;

	private enum State {STOPPED, RUNNING, QUIESCING}

	private State current_state = State.STOPPED;
	private State target_state = State.STOPPED;	
	private final Object lifecycle = new Object();
	private Thread callbackThread;
	private String threadName;
	private Future<?> callbackFuture;
	
	private final Object workAvailable = new Object();
	private final Object spaceAvailable = new Object();
	private ClientState clientState;
	private boolean manualAcks = false;

	// Courier customizations
	private org.eclipse.paho.mqttv5.client.ILogger logger = new org.eclipse.paho.mqttv5.client.NoOpLogger();


	CommsCallback(ClientComms clientComms) {
		this.clientComms = clientComms;
		this.messageQueue = new ArrayList<>(INBOUND_QUEUE_SIZE);
		this.completeQueue = new ArrayList<>(INBOUND_QUEUE_SIZE);
		this.callbackMap = new HashMap<>();
		this.callbackTopicMap = new HashMap<>();
		this.subscriptionIdMap = new HashMap<>();
	}

	CommsCallback(ClientComms clientComms, org.eclipse.paho.mqttv5.client.ILogger logger) {
		this(clientComms);
		if (logger != null) {
			this.logger = logger;
		}
	}

	public void setClientState(ClientState clientState) {
		this.clientState = clientState;
	}

	/**
	 * Starts up the Callback thread.
	 * 
	 * @param threadName
	 *            The name of the thread
	 * @param executorService
	 *            the {@link ExecutorService}
	 */
	public void start(String threadName) {
		this.threadName = threadName;
		synchronized (lifecycle) {
			if (current_state == State.STOPPED) {
				// Preparatory work before starting the background thread.
				// For safety ensure any old events are cleared.
				synchronized (workAvailable) {
					messageQueue.clear();
					completeQueue.clear();
				}
				target_state = State.RUNNING;
				if (executorService == null) {
					new Thread(this).start();
				} else {
					callbackFuture = executorService.submit(this);
				}
			}
		}
		while (!isRunning()) {
			try { Thread.sleep(100); } catch (Exception e) { }
		}			
	}

	/**
	 * Stops the callback thread. This call will block until stop has completed.
	 */
	public void stop() {
		synchronized (lifecycle) {
			if (callbackFuture != null) {
				callbackFuture.cancel(true);
			}
		}
		if (isRunning()) {
			logger.d(TAG, "callback stopping");
			synchronized (lifecycle) {
				target_state = State.STOPPED;
			}
			if (!Thread.currentThread().equals(callbackThread)) {
				synchronized (workAvailable) {
					logger.d(TAG, "notify workAvailable and wait for run to finish");
					workAvailable.notifyAll();
				}
				// Wait for the thread to finish.
				while (isRunning()) {
					try { Thread.sleep(100); } catch (Exception e) { }
					clientState.notifyQueueLock();
				}
			}
			callbackThread = null;
			logger.d(TAG, "callback stopped");
		}
	}

	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}

	public void setReconnectCallback(MqttCallback callback) {
		this.reconnectInternalCallback = callback;
	}

	public void setManualAcks(boolean manualAcks) {
		this.manualAcks = manualAcks;
	}

	public void run() {
		callbackThread = Thread.currentThread();
		callbackThread.setName(threadName);
		
		synchronized (lifecycle) {
			current_state = State.RUNNING;
		}

		while (isRunning()) {
			try {
				// If no work is currently available, then wait until there is some...
				try {
					synchronized (workAvailable) {
						if (isRunning() && messageQueue.isEmpty()
								&& completeQueue.isEmpty()) {
							logger.d(TAG, "wait for workAvailable");
							workAvailable.wait();
						}
					}
				} catch (InterruptedException e) {
				}

				if (isRunning()) {
					// Check for deliveryComplete callbacks...
					MqttToken token = null;
					synchronized (workAvailable) {
						if (!completeQueue.isEmpty()) {
							// First call the delivery arrived callback if needed
							token = completeQueue.get(0);
							completeQueue.remove(0);
						}
					}
					if (null != token) {
						handleActionComplete(token);
					}

					// Check for messageArrived callbacks...
					MqttPublish message = null;
					synchronized (workAvailable) {
						if (!messageQueue.isEmpty()) {
							// Note, there is a window on connect where a publish
							// could arrive before we've
							// finished the connect logic.
							message = messageQueue.get(0);
							messageQueue.remove(0);
						}
					}
					if (null != message) {
						handleMessage(message);
					}
				}

				if (isQuiescing()) {
					clientState.checkQuiesceLock();
				}

			} catch (Throwable ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				logger.e(TAG, "callback threw exception", ex);

				clientComms.shutdownConnection(null, new MqttException(ex), null);
			} finally {

			    synchronized (spaceAvailable) {
                    // Notify the spaceAvailable lock, to say that there's now
                    // some space on the queue...

					logger.d(TAG, "notify spaceAvailable");
					spaceAvailable.notifyAll();
				}
			}
		}
		synchronized (lifecycle) {
			current_state = State.STOPPED;
		}
		callbackThread = null;
	}

	private void handleActionComplete(MqttToken token) throws MqttException {
		synchronized (token) {
			logger.d(TAG, "callback and notify for key=" + token.internalTok.getKey());
			if (token.isComplete()) {
				// Finish by doing any post processing such as delete
				// from persistent store but only do so if the action
				// is complete
				clientState.notifyComplete(token);
			}

			// Unblock any waiters and if pending complete now set completed
			token.internalTok.notifyComplete();

			if (!token.internalTok.isNotified()) {
				// If a callback is registered and delivery has finished
				// call delivery complete callback.
				if (mqttCallback != null && token.internalTok.isDeliveryToken() == true && token.isComplete()) {
					try {
						mqttCallback.deliveryComplete(token);
					} catch (Throwable ex) {
						// Just log the fact that an exception was thrown
						logger.d(TAG, "Ignoring Exception thrown from deliveryComplete " + ex);
					}
				}
				// Now call async action completion callbacks
				fireActionEvent(token);
			}

			// Set notified so we don't tell the user again about this action.
			if (token.isComplete()) {
				if (token.internalTok.isDeliveryToken() == true || token.getActionCallback() instanceof MqttActionListener) {
					token.internalTok.setNotified(true);
				}
			}

		}
	}

	/**
	 * This method is called when the connection to the server is lost. If there is
	 * no cause then it was a clean disconnect. The connectionLost callback will be
	 * invoked if registered and run on the thread that requested shutdown e.g.
	 * receiver or sender thread. If the request was a user initiated disconnect
	 * then the disconnect token will be notified.
	 * 
	 * @param cause
	 *            the reason behind the loss of connection.
	 * @param message
	 *            The {@link MqttDisconnect} packet sent by the server
	 */
	public void connectionLost(MqttException cause, MqttDisconnect message) {
		// If there was a problem and a client callback has been set inform
		// the connection lost listener of the problem.
		try {
			if (mqttCallback != null && message != null) {

				logger.d(TAG, "Server initiated disconnect, connection closed. Disconnect=" + message.toString());
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(message.getReturnCode(),
						message.getProperties().getReasonString(),
						(ArrayList<UserProperty>) message.getProperties().getUserProperties(),
						message.getProperties().getServerReference());
				mqttCallback.disconnected(disconnectResponse);
			} else if (mqttCallback != null && cause != null) {
				logger.d(TAG, "call connectionLost " + cause);
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(cause);
				mqttCallback.disconnected(disconnectResponse);
			}
			if (reconnectInternalCallback != null && cause != null) {
				MqttDisconnectResponse disconnectResponse = new MqttDisconnectResponse(cause);

				reconnectInternalCallback.disconnected(disconnectResponse);
			}
		} catch (Throwable t) {
			// Just log the fact that an exception was thrown
			logger.d(TAG, "Ignoring Exception thrown from connectionLost " + t);
		}
	}

	/**
	 * Courier customization: notify the registered callback that a fast-reconnect
	 * has been triggered because the connection was deemed inactive.
	 */
	public void fastReconnect() {
		if (mqttCallback != null) {
			mqttCallback.fastReconnect();
		}
	}

	/**
	 * An action has completed - if a completion listener has been set on the token
	 * then invoke it with the outcome of the action.
	 * 
	 * @param token
	 *            The {@link MqttToken} that has completed
	 */
	public void fireActionEvent(MqttToken token) {
		if (token != null) {
			MqttActionListener asyncCB = token.getActionCallback();
			if (asyncCB != null) {
				if (token.getException() == null) {
					logger.d(TAG, "call onSuccess key=" + token.internalTok.getKey());
					asyncCB.onSuccess(token);
				} else {
					logger.d(TAG, "call onFailure key=" + token.internalTok.getKey());
					asyncCB.onFailure(token, token.getException());
				}
			}
		}
	}

	/**
	 * This method is called when a message arrives on a topic. Messages are only
	 * added to the queue for inbound messages if the client is not quiescing.
	 * 
	 * @param sendMessage
	 *            the MQTT SEND message.
	 */
	public void messageArrived(MqttPublish sendMessage) {
		if (mqttCallback != null || callbackMap.size() > 0) {
			// If we already have enough messages queued up in memory, wait
			// until some more queue space becomes available. This helps
			// the client protect itself from getting flooded by messages
			// from the server.
			synchronized (spaceAvailable) {
				while (isRunning() && !isQuiescing() && messageQueue.size() >= INBOUND_QUEUE_SIZE) {
					try {
						logger.d(TAG, "wait for spaceAvailable");
						spaceAvailable.wait(200);
					} catch (InterruptedException ex) {
					}
				}
			}
			if (!isQuiescing()) {
				// Notify the CommsCallback thread that there's work to do...
				synchronized (workAvailable) {
					messageQueue.add(sendMessage);
					logger.d(TAG, "new msg avail, notify workAvailable");
					workAvailable.notifyAll();
				}
			}
		}
	}

	/**
	 * This method is called when an Auth Message is received.
	 * 
	 * @param authMessage
	 *            The {@link MqttAuth} message.
	 */
	public void authMessageReceived(MqttAuth authMessage) {
		if (mqttCallback != null) {
			try {
				mqttCallback.authPacketArrived(authMessage.getReturnCode(), authMessage.getProperties());
			} catch (Throwable ex) {
				// Just log the fact that an exception was thrown
				logger.d(TAG, "Ignoring Exception thrown from authPacketArrived " + ex);
			}
		}
	}

	/**
	 * This method is called when a non-critical MQTT error has occurred in the
	 * client that the application should choose how to deal with.
	 * 
	 * @param exception
	 *            The exception that was thrown containing the cause for
	 *            disconnection.
	 */
	public void mqttErrorOccurred(MqttException exception) {
		logger.w(TAG, "mqtt error occurred: " + exception.getMessage());
		if (mqttCallback != null) {
			try {
				mqttCallback.mqttErrorOccurred(exception);
			} catch (Exception ex) {
				// Just log the fact that an exception was thrown
				logger.d(TAG, "Ignoring Exception thrown from mqttErrorOccurred: " + ex);
			}
		}
	}

	/**
	 * Let the call back thread quiesce. Prevent new inbound messages being added to
	 * the process queue and let existing work quiesce. (until the thread is told to
	 * shutdown).
	 */
	public void quiesce() {
		synchronized (lifecycle) {
			if (current_state == State.RUNNING)
			current_state = State.QUIESCING;
		}
		synchronized (spaceAvailable) {
			logger.d(TAG, "quiesce notify spaceAvailable");
			// Unblock anything waiting for space...
			spaceAvailable.notifyAll();
		}
	}

	boolean areQueuesEmpty() {
		synchronized (workAvailable) {
			return completeQueue.isEmpty() && messageQueue.isEmpty();
		}
	}

	public boolean isQuiesced() {
		return (isQuiescing() && areQueuesEmpty());
	}

	private void handleMessage(MqttPublish publishMessage) throws Exception {
		// If quisecing process any pending messages.
		String destName = publishMessage.getTopicName();

		logger.d(TAG, "call messageArrived key=" + publishMessage.getMessageId() + " topic=" + destName);
		boolean sendAck = deliverMessage(destName, publishMessage.getMessageId(), publishMessage.getMessage());

		// Courier: only acknowledge the message if the application handler signalled
		// that it should be acked (e.g. it was successfully persisted).
		if (!this.manualAcks && publishMessage.getMessage().getQos() == 1 && sendAck) {
			this.clientComms.internalSend(new MqttPubAck(MqttReturnCode.RETURN_CODE_SUCCESS,
					publishMessage.getMessageId(), new MqttProperties()),
					new MqttToken(clientComms.getClient().getClientId()));
		}
	}

	public void messageArrivedComplete(int messageId, int qos) throws MqttException {
		if (qos == 1) {
			this.clientComms.internalSend(
					new MqttPubAck(MqttReturnCode.RETURN_CODE_SUCCESS, messageId, new MqttProperties()),
					new MqttToken(clientComms.getClient().getClientId()));
		} else if (qos == 2) {
			this.clientComms.deliveryComplete(messageId);
			MqttPubComp pubComp = new MqttPubComp(MqttReturnCode.RETURN_CODE_SUCCESS, messageId, new MqttProperties());
			logger.i(TAG, "Creating MqttPubComp due to manual ACK: " + pubComp.toString());

			this.clientComms.internalSend(pubComp, new MqttToken(clientComms.getClient().getClientId()));
		}
	}

	public void asyncOperationComplete(MqttToken token) {
		if (isRunning()) {
			// invoke callbacks on callback thread
			synchronized (workAvailable) {
				completeQueue.add(token);
				logger.d(TAG, "new workAvailable. key=" + token.internalTok.getKey());
				workAvailable.notifyAll();
			}
		} else {
			// invoke async callback on invokers thread
			try {
				handleActionComplete(token);
			} catch (MqttException ex) {
				// Users code could throw an Error or Exception e.g. in the case
				// of class NoClassDefFoundError
				logger.e(TAG, "callback threw exception", ex);

				// Shutdown likely already in progress but no harm to confirm
				clientComms.shutdownConnection(null, new MqttException(ex), null);
			}

		}
	}

	/**
	 * Returns the thread used by this callback.
	 * 
	 * @return The {@link Thread}
	 */
	protected Thread getThread() {
		return callbackThread;
	}

	public void setMessageListener(Integer subscriptionId, String topicFilter, IMqttMessageListener messageListener) {
		int internalId = messageHandlerId.incrementAndGet();
		this.callbackMap.put(internalId, messageListener);
		this.callbackTopicMap.put(topicFilter, internalId);

		if (subscriptionId != null) {
			this.subscriptionIdMap.put(subscriptionId, internalId);
		}
	}

	/**
	 * Removes a Message Listener by Topic. If the Topic is null or incorrect, this
	 * function will return without making any changes. It will also attempt to find
	 * any subscription IDs linked to the same message listener and will remove them
	 * too.
	 * 
	 * @param topicFilter
	 *            the topic filter that identifies the Message listener to remove.
	 */
	public void removeMessageListener(String topicFilter) {
		Integer callbackId = this.callbackTopicMap.get(topicFilter);
		this.callbackMap.remove(callbackId);
		this.callbackTopicMap.remove(topicFilter);

		// Reverse lookup the subscription ID if it exists to remove that as well
		for (Map.Entry<Integer, Integer> entry : this.subscriptionIdMap.entrySet()) {
			if (entry.getValue().equals(callbackId)) {
				this.subscriptionIdMap.remove(entry.getKey());
			}
		}
	}

	/**
	 * Removes a Message Listener by subscription ID. If the Subscription Identifier
	 * is null or incorrect, this function will return without making any changes.
	 * It will also attempt to find any Topic Strings linked to the same message
	 * listener and will remove them too.
	 * 
	 * @param subscriptionId
	 *            the subscription ID that identifies the Message listener to
	 *            remove.
	 */
	public void removeMessageListener(Integer subscriptionId) {
		Integer callbackId = this.subscriptionIdMap.get(subscriptionId);
		this.subscriptionIdMap.remove(callbackId);
		this.callbackMap.remove(callbackId);

		// Reverse lookup the topic if it exists to remove that as well
		for (Map.Entry<String, Integer> entry : this.callbackTopicMap.entrySet()) {
			if (entry.getValue().equals(callbackId)) {
				this.callbackTopicMap.remove(entry.getKey());
			}
		}
	}

	public void removeMessageListeners() {
		this.callbackMap.clear();
		this.subscriptionIdMap.clear();
		this.callbackTopicMap.clear();
	}

	/**
	 * Delivers a message to the registered handlers.
	 *
	 * @return Courier customization: {@code true} if the message should be
	 *         acknowledged to the server. Per-subscription listeners always ack;
	 *         the default handler controls the ack via its boolean return value.
	 */
	protected boolean deliverMessage(String topicName, int messageId, MqttMessage aMessage) throws Exception {
		boolean delivered = false;
		boolean sendAck = true;

		if (aMessage.getProperties().getSubscriptionIdentifiers().isEmpty()) {
			// No Subscription IDs, use topic filter matching
			for (Map.Entry<String, Integer> entry : this.callbackTopicMap.entrySet()) {
				if (MqttTopicValidator.isMatched(entry.getKey(), topicName)) {
					aMessage.setId(messageId);
					this.callbackMap.get(entry.getValue()).messageArrived(topicName, aMessage);
					delivered = true;
				}
			}

		} else {
			// We have Subscription IDs
			for (Integer subId : aMessage.getProperties().getSubscriptionIdentifiers()) {
				if (this.subscriptionIdMap.containsKey(subId)) {
					Integer callbackId = this.subscriptionIdMap.get(subId);
					aMessage.setId(messageId);
					this.callbackMap.get(callbackId).messageArrived(topicName, aMessage);
					delivered = true;
				}
			}
		}

		/*
		 * if the message hasn't been delivered to a per subscription handler, give it
		 * to the default handler
		 */
		if (mqttCallback != null && !delivered) {
			aMessage.setId(messageId);
			try {
				sendAck = mqttCallback.messageArrived(topicName, aMessage);
			} catch (Exception ex) {
				// Just log the fact that an exception was thrown
				logger.d(TAG, "Ignoring Exception thrown from messageArrived: " + ex);
			}
			delivered = true;
		}

		return sendAck;
	}

	public boolean doesSubscriptionIdentifierExist(int subscriptionIdentifier) {
		return (this.subscriptionIdMap.containsKey(subscriptionIdentifier));
	}

	public boolean isRunning() {
		boolean result;
		synchronized (lifecycle) {
			result = ((current_state == State.RUNNING || current_state == State.QUIESCING)
					&& target_state == State.RUNNING);
		}
		return result;
	}
	
	public boolean isQuiescing() {
		boolean result;
		synchronized (lifecycle) {
			result = (current_state == State.QUIESCING);
		}
		return result;
	}
	
}
