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


import org.eclipse.paho.client.mqttv10.ICommsCallback;
import org.eclipse.paho.client.mqttv10.IExperimentsConfig;
import org.eclipse.paho.client.mqtt.ILogger;
import org.eclipse.paho.client.mqttv10.IMqttActionListenerNew;
import org.eclipse.paho.client.mqtt.IPahoEvents;
import org.eclipse.paho.client.mqttv10.MqttClientPersistence;
import org.eclipse.paho.client.mqttv10.MqttDeliveryToken;
import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqttv10.MqttMessage;
import org.eclipse.paho.client.mqttv10.MqttPersistable;
import org.eclipse.paho.client.mqttv10.MqttPersistenceException;
import org.eclipse.paho.client.mqtt.MqttPingSender;
import org.eclipse.paho.client.mqttv10.MqttToken;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttAck;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPingResp;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPubAck;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPubComp;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPubRec;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPubRel;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttSubscribe;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttUnsubscribe;
import org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage;

import java.io.EOFException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 * The core of the client, which holds the state information for pending and in-flight messages.
 * 
 * Messages that have been accepted for delivery are moved between several objects while being delivered.
 * 
 * 1) When the client is not running messages are stored in a persistent store that implements the MqttClientPersistent Interface. The default is MqttDefaultFilePersistencew which
 * stores messages safely across failures and system restarts. If no persistence is specified there is a fall back to MemoryPersistence which will maintain the messages while the
 * Mqtt client is instantiated.
 * 
 * 2) When the client or specifically ClientState is instantiated the messages are read from the persistent store into: - outboundqos2 hashtable if a QoS 2 PUBLISH or PUBREL -
 * outboundqos1 hashtable if a QoS 1 PUBLISH (see restoreState)
 * 
 * 3) On Connect, copy messages from the outbound hashtables to the pendingMessages or pendingFlows vector in messageid order. - Initial message publish goes onto the
 * pendingmessages buffer. - PUBREL goes onto the pendingflows buffer (see restoreInflightMessages)
 * 
 * 4) Sender thread reads messages from the pendingflows and pendingmessages buffer one at a time. The message is removed from the pendingbuffer but remains on the outbound*
 * hashtable. The hashtable is the place where the full set of outstanding messages are stored in memory. (Persistence is only used at start up)
 * 
 * 5) Receiver thread - receives wire messages: - if QoS 1 then remove from persistence and outboundqos1 - if QoS 2 PUBREC send PUBREL. Updating the outboundqos2 entry with the
 * PUBREL and update persistence. - if QoS 2 PUBCOMP remove from persistence and outboundqos2
 * 
 * Notes: because of the multithreaded nature of the client it is vital that any changes to this class take concurrency into account. For instance as soon as a flow / message is
 * put on the wire it is possible for the receiving thread to receive the ack and to be processing the response before the sending side has finished processing. For instance a
 * connect may be sent, the conack received before the connect notify send has been processed!
 * 
 */
public class ClientState
{
	private static final String PERSISTENCE_SENT_PREFIX = "s-";

	private static final String PERSISTENCE_SENT_BUFFERED_PREFIX = "sb-";

	private static final String PERSISTENCE_CONFIRMED_PREFIX = "sc-";

	private static final String PERSISTENCE_RECEIVED_PREFIX = "r-";

	private static final int MIN_MSG_ID = 1; // Lowest possible MQTT message ID to use

	private static final int MAX_MSG_ID = 65535; // Highest possible MQTT message ID to use

	private int nextMsgId = MIN_MSG_ID - 1; // The next available message ID to use

	private Hashtable inUseMsgIds; // Used to store a set of in-use message IDs

	volatile private Vector pendingMessages;

	volatile private Vector pendingFlows;

	private CommsTokenStore tokenStore;

	private ClientComms clientComms = null;

	private ICommsCallback callback = null;

	private ILogger logger;

	private long keepAlive;

	private boolean cleanSession;

	private MqttClientPersistence persistence;

	private int maxInflight = 10;

	private int actualInFlight = 0;

	private int inFlightPubRels = 0;

	private Object queueLock = new Object();

	private Object quiesceLock = new Object();

	private boolean quiescing = false;

	private long fastReconnectCheckStartTime = 0;

	private long lastOutboundActivity = 0;

	private long lastInboundActivity = 0;

	private long lastPing = 0;

	private MqttWireMessage pingCommand;

	private Boolean pingOutstanding = Boolean.FALSE;

	private boolean connected = false;

	private Hashtable outboundQoS2 = null;

	private Hashtable outboundQoS1 = null;

	private Hashtable inboundQoS2 = null;

	private MqttPingSender pingSender = null;

	private final static String className = ClientState.class.getName();

	private long inactivityTimeout = DEFAULT_INACTIVITY_TIMEOUT;
	private final static long DEFAULT_INACTIVITY_TIMEOUT = 60 * 1000;

	private IPahoEvents pahoEvents;

	private final String TAG = "clientState";

	protected ClientState(
			MqttClientPersistence persistence,
			CommsTokenStore tokenStore,
			ICommsCallback callback,
			ClientComms clientComms,
			MqttPingSender pingSender,
			int maxInflightMsgs,
			ILogger logger,
			IExperimentsConfig experimentsConfig,
			IPahoEvents pahoEvents
	) throws MqttException
	{

		this.maxInflight = maxInflightMsgs;
		inUseMsgIds = new Hashtable();
		pendingMessages = new Vector(this.maxInflight);
		pendingFlows = new Vector();
		outboundQoS2 = new Hashtable();
		outboundQoS1 = new Hashtable();
		inboundQoS2 = new Hashtable();
		pingCommand = new MqttPingReq();
		inFlightPubRels = 0;
		actualInFlight = 0;

		this.persistence = persistence;
		this.callback = callback;
		this.tokenStore = tokenStore;
		this.clientComms = clientComms;
		this.pingSender = pingSender;
		this.logger = logger;
		this.pahoEvents = pahoEvents;

		if (experimentsConfig != null) {
			inactivityTimeout = experimentsConfig.inactivityTimeoutSecs() * 1000L;
		}

		restoreState();

		logger.d(TAG, "client id : " + clientComms.getClient().getClientId());

	}

	protected void setKeepAliveSecs(long keepAliveSecs)
	{
		this.keepAlive = keepAliveSecs * 1000;
	}

	protected long getKeepAlive()
	{
		return this.keepAlive;
	}

	protected void setCleanSession(boolean cleanSession)
	{
		this.cleanSession = cleanSession;
	}

	private String getSendPersistenceKey(MqttWireMessage message)
	{
		return PERSISTENCE_SENT_PREFIX + message.getMessageId();
	}

	private String getSendConfirmPersistenceKey(MqttWireMessage message)
	{
		return PERSISTENCE_CONFIRMED_PREFIX + message.getMessageId();
	}

	private String getReceivedPersistenceKey(MqttWireMessage message)
	{
		return PERSISTENCE_RECEIVED_PREFIX + message.getMessageId();
	}

	protected void clearState() throws MqttException
	{
		final String methodName = "clearState";
		// @TRACE 603=clearState
		logger.d(TAG, "clearing state");
		persistence.clear();
		clientComms.clear();
		inUseMsgIds.clear();
		pendingMessages.clear();
		pendingFlows.clear();
		outboundQoS2.clear();
		outboundQoS1.clear();
		inboundQoS2.clear();
		tokenStore.clear();
	}

	private MqttWireMessage restoreMessage(String key, MqttPersistable persistable) throws MqttException
	{
		final String methodName = "restoreMessage";
		MqttWireMessage message = null;

		try
		{
			message = MqttWireMessage.createWireMessage(persistable, clientComms.getClient().getMqttVersion());
		}
		catch (MqttException ex)
		{
			// @TRACE 602=key={0} exception
			logger.e(TAG, "exception in restore message, cause : ", ex);
			if (ex.getCause() instanceof EOFException)
			{
				// Premature end-of-file means that the message is corrupted
				if (key != null)
				{
					persistence.remove(key);
				}
			}
			else
			{
				throw ex;
			}
		}
		// @TRACE 601=key={0} message={1}
		logger.d(TAG, "restoring message : " + message.toString() + " with key : " + key);
		return message;
	}

	/**
	 * Inserts a new message to the list, ensuring that list is ordered from lowest to highest in terms of the message id's.
	 * 
	 * @param list
	 *            the list to insert the message into
	 * @param newMsg
	 *            the message to insert into the list
	 */
	private void insertInOrder(Vector list, MqttWireMessage newMsg)
	{
		int newMsgId = newMsg.getMessageId();
		for (int i = 0; i < list.size(); i++)
		{
			MqttWireMessage otherMsg = (MqttWireMessage) list.elementAt(i);
			int otherMsgId = otherMsg.getMessageId();
			if (otherMsgId > newMsgId)
			{
				list.insertElementAt(newMsg, i);
				return;
			}
		}
		list.addElement(newMsg);
	}

	/**
	 * Produces a new list with the messages properly ordered according to their message id's.
	 * 
	 * @param list
	 *            the list containing the messages to produce a new reordered list for - this will not be modified or replaced, i.e., be read-only to this method
	 * @return a new reordered list
	 */
	private Vector reOrder(Vector list)
	{

		// here up the new list
		Vector newList = new Vector();

		if (list.size() == 0)
		{
			return newList; // nothing to reorder
		}

		int previousMsgId = 0;
		int largestGap = 0;
		int largestGapMsgIdPosInList = 0;
		for (int i = 0; i < list.size(); i++)
		{
			int currentMsgId = ((MqttWireMessage) list.elementAt(i)).getMessageId();
			if (currentMsgId - previousMsgId > largestGap)
			{
				largestGap = currentMsgId - previousMsgId;
				largestGapMsgIdPosInList = i;
			}
			previousMsgId = currentMsgId;
		}
		int lowestMsgId = ((MqttWireMessage) list.elementAt(0)).getMessageId();
		int highestMsgId = previousMsgId; // last in the sorted list

		// we need to check that the gap after highest msg id to the lowest msg id is not beaten
		if (MAX_MSG_ID - highestMsgId + lowestMsgId > largestGap)
		{
			largestGapMsgIdPosInList = 0;
		}

		// starting message has been located, let's start from this point on
		for (int i = largestGapMsgIdPosInList; i < list.size(); i++)
		{
			newList.addElement(list.elementAt(i));
		}

		// and any wrapping back to the beginning
		for (int i = 0; i < largestGapMsgIdPosInList; i++)
		{
			newList.addElement(list.elementAt(i));
		}

		return newList;
	}

	/**
	 * Restores the state information from persistence.
	 */
	protected void restoreState() throws MqttException
	{
		final String methodName = "restoreState";
		Enumeration messageKeys = persistence.keys();
		MqttPersistable persistable;
		String key;
		int highestMsgId = nextMsgId;
		Vector orphanedPubRels = new Vector();
		// @TRACE 600=>
		logger.d(TAG, "restoring state");

		while (messageKeys.hasMoreElements())
		{
			key = (String) messageKeys.nextElement();
			persistable = persistence.get(key);
			MqttWireMessage message = restoreMessage(key, persistable);
			if (message != null)
			{
				if (key.startsWith(PERSISTENCE_RECEIVED_PREFIX))
				{
					// @TRACE 604=inbound QoS 2 publish key={0} message={1}

					// The inbound messages that we have persisted will be QoS 2
					inboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
				}
				else if (key.startsWith(PERSISTENCE_SENT_PREFIX))
				{
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if (persistence.containsKey(getSendConfirmPersistenceKey(sendMessage)))
					{
						MqttPersistable persistedConfirm = persistence.get(getSendConfirmPersistenceKey(sendMessage));
						// QoS 2, and CONFIRM has already been sent...
						MqttPubRel confirmMessage = (MqttPubRel) restoreMessage(key, persistedConfirm);
						if (confirmMessage != null)
						{
							confirmMessage.setDuplicate(true);
							// @TRACE 605=outbound QoS 2 pubrel key={0} message={1}

							outboundQoS2.put(Integer.valueOf(confirmMessage.getMessageId()), confirmMessage);
						}
						else
						{
							// @TRACE 606=outbound QoS 2 completed key={0} message={1}
						}
					}
					else
					{
						// QoS 1 or 2, with no CONFIRM sent...
						// Put the SEND to the list of pending messages, ensuring message ID ordering...
						sendMessage.setDuplicate(true);
						if (sendMessage.getMessage().getQos() == 2)
						{
							// @TRACE 607=outbound QoS 2 publish key={0} message={1}

							outboundQoS2.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						}
						else
						{
							// @TRACE 608=outbound QoS 1 publish key={0} message={1}

							outboundQoS1.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
						}
					}
					MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					tok.internalTok.setClient(clientComms.getClient());
					inUseMsgIds.put(Integer.valueOf(sendMessage.getMessageId()), Integer.valueOf(sendMessage.getMessageId()));
				} else if(key.startsWith(PERSISTENCE_SENT_BUFFERED_PREFIX)) {
					// Buffered outgoing messages that have not yet been sent at all
					MqttPublish sendMessage = (MqttPublish) message;
					highestMsgId = Math.max(sendMessage.getMessageId(), highestMsgId);
					if(sendMessage.getMessage().getQos() == 2) {
						//@TRACE 607=outbound QoS 2 publish key={0} message={1}
						outboundQoS2.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
					} else if(sendMessage.getMessage().getQos() == 1) {
						//@TRACE 608=outbound QoS 1 publish key={0} message={1}
						outboundQoS1.put(Integer.valueOf(sendMessage.getMessageId()), sendMessage);
					}

					MqttDeliveryToken tok = tokenStore.restoreToken(sendMessage);
					tok.internalTok.setClient(clientComms.getClient());
					inUseMsgIds.put(Integer.valueOf(sendMessage.getMessageId()), Integer.valueOf(sendMessage.getMessageId()));


				} else if (key.startsWith(PERSISTENCE_CONFIRMED_PREFIX)) {
					MqttPubRel pubRelMessage = (MqttPubRel) message;
					if (!persistence.containsKey(getSendPersistenceKey(pubRelMessage)))
					{
						orphanedPubRels.addElement(key);
					}
				}
			}
		}

		messageKeys = orphanedPubRels.elements();
		while (messageKeys.hasMoreElements())
		{
			key = (String) messageKeys.nextElement();
			// @TRACE 609=removing orphaned pubrel key={0}

			persistence.remove(key);
		}

		nextMsgId = highestMsgId;
	}

	private void restoreInflightMessages()
	{

		logger.d(TAG, "restoring inflight messages started");
		final String methodName = "restoreInflightMessages";
		pendingMessages = new Vector(this.maxInflight);
		pendingFlows = new Vector();

		Enumeration keys = outboundQoS2.keys();
		while (keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			Object msg = outboundQoS2.get(key);
			if (msg instanceof MqttPublish)
			{
				// @TRACE 610=QoS 2 publish key={0}
				((MqttPublish) msg).setDuplicate(true);
				insertInOrder(pendingMessages, (MqttPublish) msg);
			}
			else if (msg instanceof MqttPubRel)
			{
				// @TRACE 611=QoS 2 pubrel key={0}

				insertInOrder(pendingFlows, (MqttPubRel) msg);
			}
		}
		keys = outboundQoS1.keys();
		while (keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			MqttPublish msg = (MqttPublish) outboundQoS1.get(key);
			// @TRACE 612=QoS 1 publish key={0}
			msg.setDuplicate(true);
			insertInOrder(pendingMessages, msg);
		}

		this.pendingFlows = reOrder(pendingFlows);
		this.pendingMessages = reOrder(pendingMessages);
		logger.d(TAG, "restoring inflight messages completed");
	}

	/**
	 * Submits a message for delivery. This method will block until there is room in the inFlightWindow for the message. The message is put into persistence before returning.
	 * 
	 * @param message
	 *            the message to send
	 * @param token
	 *            the token that can be used to track delivery of the message
	 * @throws MqttException
	 */
	public void send(MqttWireMessage message, MqttToken token) throws MqttException
	{
		final String methodName = "send";
		if (message.isMessageIdRequired() && (message.getMessageId() == 0))
		{
			message.setMessageId(getNextMessageId());
		}
		if (token != null)
		{
			try
			{
				token.internalTok.setMessageID(message.getMessageId());
			}
			catch (Exception e)
			{
			}
		}

		if (message instanceof MqttPublish)
		{
			synchronized (queueLock)
			{
				if (actualInFlight >= this.maxInflight)
				{
					// @TRACE 613= sending {0} msgs at max inflight window
					logger.e(TAG, "max in flight messages reached ");

					throw new MqttException(MqttException.REASON_CODE_MAX_INFLIGHT);
				}

				MqttMessage innerMessage = ((MqttPublish) message).getMessage();
				// @TRACE 628=pending publish key={0} qos={1} message={2}

				switch (innerMessage.getQos())
				{
				case 2:
					outboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				case 1:
					outboundQoS1.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendPersistenceKey(message), (MqttPublish) message);
					break;
				}
				tokenStore.saveToken(token, message);
				pendingMessages.addElement(message);
				queueLock.notifyAll();
			}
		}
		else
		{
			// @TRACE 615=pending send key={0} message {1}

			if (message instanceof MqttConnect)
			{
				synchronized (queueLock)
				{
					// Add the connect action at the head of the pending queue ensuring it jumps
					// ahead of any of other pending actions.
					tokenStore.saveToken(token, message);
					pendingFlows.insertElementAt(message, 0);
					queueLock.notifyAll();
				}
			}
			else
			{
				if (message instanceof MqttPingReq)
				{
					this.pingCommand = message;
				}
				else if (message instanceof MqttPubRel)
				{
					outboundQoS2.put(Integer.valueOf(message.getMessageId()), message);
					persistence.put(getSendConfirmPersistenceKey(message), (MqttPubRel) message);
				}
				else if (message instanceof MqttPubComp)
				{
					persistence.remove(getReceivedPersistenceKey(message));
				}

				synchronized (queueLock)
				{
					if (!(message instanceof MqttAck))
					{
						tokenStore.saveToken(token, message);
					}
					pendingFlows.addElement(message);
					queueLock.notifyAll();
				}
			}
		}
	}

	/**
	 * This removes the MqttSend message from the outbound queue and persistence.
	 * 
	 * @param message
	 * @throws MqttPersistenceException
	 */
	protected void undo(MqttPublish message) throws MqttPersistenceException
	{
		final String methodName = "undo";
		synchronized (queueLock)
		{
			// @TRACE 618=key={0} QoS={1}

			if (message.getMessage().getQos() == 1)
			{
				outboundQoS1.remove(Integer.valueOf(message.getMessageId()));
			}
			else
			{
				outboundQoS2.remove(Integer.valueOf(message.getMessageId()));
			}
			pendingMessages.removeElement(message);
			persistence.remove(getSendPersistenceKey(message));
			tokenStore.removeToken(message);
			checkQuiesceLock();
		}
	}

	/**
	 * Check and send a ping if needed and check for ping timeout. Need to send a ping if nothing has been sent or received in the last keepalive interval. It is important to check
	 * for both sent and received packets in order to catch the case where an app is solely sending QoS 0 messages or receiving QoS 0 messages. QoS 0 message are not good enough
	 * for checking a connection is alive as they are one way messages.
	 *
	 * If a ping has been sent but no data has been received in the last keepalive interval then the connection is deamed to be broken.
	 *
	 * @return token of ping command, null if no ping command has been sent.
	 */
	public MqttToken checkForActivity() throws MqttException
	{
		final String methodName = "checkForActivity";

		MqttToken token = null;
		long nextPingTime = getKeepAlive();

		if (connected && this.keepAlive > 0)
		{
			long time = System.currentTimeMillis();
			// Reduce schedule frequency since System.currentTimeMillis is no accurate, add a buffer
			// It is 1/10 in minimum keepalive unit.
			int delta = 100;
			int keepAliveMargin = 5000;

			synchronized (pingOutstanding)
			{

				if (!pingOutstanding.booleanValue())
				{
					long lastActivity = lastInboundActivity;

					// Is a ping required?
					if (time - lastActivity + keepAliveMargin >= this.keepAlive)
					{

						// @TRACE 620=ping needed. keepAlive={0} lastOutboundActivity={1} lastInboundActivity={2}
						logger.d(TAG, "inserting ping in pending flows , lastoutboundactivity time : " + lastOutboundActivity + " lastinboundactivitytime : " + lastInboundActivity);
						pingOutstanding = Boolean.TRUE;
						lastPing = time;
						token = new MqttToken(clientComms.getClient().getClientId());
						tokenStore.saveToken(token, pingCommand);
						pendingFlows.insertElementAt(pingCommand, 0);

						nextPingTime = getKeepAlive();

						// Wake sender thread since it may be in wait state (in ClientState.get())
						notifyQueueLock();
					}
					else
					{
						// @Trace 634=ping not needed yet. Schedule next ping.
						nextPingTime = getKeepAlive() - (time - lastActivity);
						logger.d(TAG, "ping not outstanding , nextping time : " + nextPingTime);
					}
				}
				else if ((time - lastPing >= keepAlive + delta) && (time - lastInboundActivity >= keepAlive + delta))
				{
					// any of the conditions is true means the client is active
					// lastInboundActivity will be updated once receiving is done.
					// Add a delta, since the timer and System.currentTimeMillis() is not accurate.
					// A ping is outstanding but no packet has been received in KA so connection is deemed broken
					// @TRACE 619=Timed out as no activity, keepAlive={0} lastOutboundActivity={1} lastInboundActivity={2} time={3} lastPing={4}
					logger.e(TAG, "timed out as no activity, already sent the ping but no response recieved,  lastoutboundactivity : " + lastOutboundActivity
							+ " fastReconnectCheckStartTime : " + fastReconnectCheckStartTime + " lastinboundactivity : " + lastInboundActivity);

					// A ping has already been sent. At this point, assume that the
					// broker has hung and the TCP layer hasn't noticed.
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
				}
			}
			// @TRACE 624=Schedule next ping at {0}
			logger.d(TAG, "scheduling next ping time : " + nextPingTime);
			pingSender.schedule(nextPingTime);
		}

		return token;
	}

	/**
	 * Check and send a ping and check for ping timeout.
	 *
	 * @return token of ping command, null if no ping command has been sent.
	 */
	public MqttToken sendForcePingRequest() throws MqttException
	{
		final String methodName = "checkForActivity";

		MqttToken token = null;

		if (connected && this.keepAlive > 0)
		{
			long time = System.currentTimeMillis();
			// Reduce schedule frequency since System.currentTimeMillis is no accurate, add a buffer
			// It is 1/10 in minimum keepalive unit.
			int delta = 100;

			synchronized (pingOutstanding)
			{

				if (!pingOutstanding.booleanValue())
				{
					logger.d(TAG, "inserting ping in pending flows , lastoutboundactivity time : " + lastOutboundActivity + " lastinboundactivitytime : " + lastInboundActivity);
					pingOutstanding = Boolean.TRUE;
					lastPing = time;
					token = new MqttToken(clientComms.getClient().getClientId());
					tokenStore.saveToken(token, pingCommand);
					pendingFlows.insertElementAt(pingCommand, 0);

					// Wake sender thread since it may be in wait state (in ClientState.get())
					notifyQueueLock();
				}
				else if ((time - lastPing >= keepAlive + delta) && (time - lastInboundActivity >= keepAlive + delta) && (time - lastOutboundActivity >= keepAlive + delta))
				{
					// any of the conditions is true means the client is active
					// lastInboundActivity will be updated once receiving is done.
					// Add a delta, since the timer and System.currentTimeMillis() is not accurate.
					// A ping is outstanding but no packet has been received in KA so connection is deemed broken
					// @TRACE 619=Timed out as no activity, keepAlive={0} lastOutboundActivity={1} lastInboundActivity={2} time={3} lastPing={4}
					logger.e(TAG, "timed out as no activity, already sent the ping but no response recieved,  lastoutboundactivity : " + lastOutboundActivity
							+ " fastReconnectCheckStartTime : " + fastReconnectCheckStartTime + " lastinboundactivity : " + lastInboundActivity);

					// A ping has already been sent. At this point, assume that the
					// broker has hung and the TCP layer hasn't noticed.
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
				}
			}
		}

		return token;
	}

	public void checkActivity() throws MqttException
	{
		synchronized (pingOutstanding)
		{	
			if (fastReconnectCheckStartTime > lastInboundActivity)
			{
				if(System.currentTimeMillis() - fastReconnectCheckStartTime >= inactivityTimeout)
					
				{
					logger.logFastReconnectEvent(fastReconnectCheckStartTime, lastInboundActivity);
					logger.e(TAG, "not recieved ack for 1 min so disconnecting");
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
				}
			}
		}
	}
	
	/**
	 * This returns the next piece of work, ie message, for the CommsSender to send over the network. Calls to this method block until either: - there is a message to be sent - the
	 * keepAlive interval is exceeded, which triggers a ping message to be returned - {@link #disconnected(MqttException, boolean)} is called
	 * 
	 * @return the next message to send, or null if the client is disconnected
	 */
	protected MqttWireMessage get() throws MqttException
	{
		final String methodName = "get";
		MqttWireMessage result = null;

		long sTime = System.currentTimeMillis();
		synchronized (queueLock)
		{
			while (result == null)
			{

				// If there is no work wait until there is work.
				// If the inflight window is full and no flows are pending wait until space is freed.
				// In both cases queueLock will be notified.
				if ((pendingMessages.isEmpty() && pendingFlows.isEmpty()) || (pendingFlows.isEmpty() && actualInFlight >= this.maxInflight))
				{
					try
					{
						// @TRACE 644=wait for new work or for space in the inflight window

						logger.d(TAG, "waiting for new work or for space in the inflight window ");

						queueLock.wait();

						// @TRACE 647=new work or ping arrived
						logger.d(TAG, "new work or ping arrived");
					}
					catch (InterruptedException e)
					{
					}
				}

				// Handle the case where not connected. This should only be the case if:
				// - in the process of disconnecting / shutting down
				// - in the process of connecting
				if (!connected && (pendingFlows.isEmpty() || !((MqttWireMessage) pendingFlows.elementAt(0) instanceof MqttConnect)))
				{
					// @TRACE 621=no outstanding flows and not connected
					logger.d(TAG, "no outstanding flows and not connected");
					String threadId = TAG + ":" + Thread.currentThread().getName() + ":" + Thread.currentThread().getId();
					logger.logMqttThreadEvent("get_empty_qos1_msg", (System.currentTimeMillis() - sTime), threadId);
					return null;
				}

				// Check if there is a need to send a ping to keep the session alive.
				// Note this check is done before processing messages. If not done first
				// an app that only publishes QoS 0 messages will prevent keepalive processing
				// from functioning.
				// checkForActivity(); //Use pinger, don't check here

				// Now process any queued flows or messages
				if (!pendingFlows.isEmpty())
				{
					// Process the first "flow" in the queue
					result = (MqttWireMessage) pendingFlows.remove(0);
					if (result instanceof MqttPubRel)
					{
						inFlightPubRels++;

						// @TRACE 617=+1 inflightpubrels={0}

					}

					checkQuiesceLock();
				}
				else if (!pendingMessages.isEmpty())
				{
					// If the inflight window is full then messages are not
					// processed until the inflight window has space.
					if (actualInFlight < this.maxInflight)
					{
						// The in flight window is not full so process the
						// first message in the queue
						result = (MqttWireMessage) pendingMessages.elementAt(0);
						pendingMessages.removeElementAt(0);
						actualInFlight++;

						// @TRACE 623=+1 actualInFlight={0}

					}
					else
					{
						// @TRACE 622=inflight window full
						logger.d(TAG, "inflight window full");
					}
				}
			}
		}
		String threadId = TAG + ":" + Thread.currentThread().getName() + ":" + Thread.currentThread().getId();
		logger.logMqttThreadEvent("get_qos1_msg", (System.currentTimeMillis() - sTime), threadId);
		return result;
	}

	public void setKeepAliveInterval(long interval)
	{
		this.keepAlive = interval;
	}

	/**
	 * Deduce how long to to wait until a ping is required.
	 * 
	 * In order to keep the connection alive the server must see activity within the keepalive interval. If the application is not sending / receiving any messages then the client
	 * will send a ping. This method works out the next time that a ping must be sent in order for the server to know the client is alive.
	 * 
	 * @return time before a ping needs to be sent to keep alive the connection
	 */
	long getTimeUntilPing()
	{
		long pingin = getKeepAlive();
		// If KA is zero which means just wait for work or
		// if a ping is outstanding return the KA value
		if (connected && (getKeepAlive() > 0) && !pingOutstanding.booleanValue())
		{

			long time = System.currentTimeMillis();
			long timeSinceOut = (time - lastOutboundActivity);
			long timeSinceIn = (time - lastInboundActivity);

			if (timeSinceOut > timeSinceIn)
			{
				pingin = (getKeepAlive() - timeSinceOut);
			}
			else
			{
				pingin = (getKeepAlive() - timeSinceIn);
			}

			// Unlikely to be negative or zero but in the case it is return a
			// small value > 0 to cause a ping to occur
			if (pingin <= 0)
			{
				pingin = 10;
			}
		}
		return (pingin);
	}

	/**
	 * Called by the CommsSender when a message has been sent
	 * 
	 * @param message
	 */
	protected void notifySent(MqttWireMessage message)
	{
		final String methodName = "notifySent";

		this.lastOutboundActivity = System.currentTimeMillis();
		// @TRACE 625=key={0}

		MqttToken token = tokenStore.getToken(message);
		token.internalTok.notifySent();
		if (message instanceof MqttPublish)
		{
			if (((MqttPublish) message).getMessage().getQos() == 0)
			{
				// once a QoS 0 message is sent we can clean up its records straight away as
				// we won't be hearing about it again
				token.internalTok.markComplete(null, null);
				callback.asyncOperationComplete(token);
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);
				checkQuiesceLock();
			}
			else
			// this is QOS 1 or 2
			{
				checkAndSetFastReconnectCheckStartTime();
				notifySentCallback(token);
			}
		}
		else if (message instanceof MqttPingReq || message instanceof MqttConnect ||
				message instanceof MqttSubscribe || message instanceof MqttUnsubscribe)
		{
			checkAndSetFastReconnectCheckStartTime();
		}
	}
	
	private void checkAndSetFastReconnectCheckStartTime()
	{
		if(fastReconnectCheckStartTime <= lastInboundActivity)
		{
			fastReconnectCheckStartTime = System.currentTimeMillis();
			logger.d(TAG, "fastReconnect Check StartTime : " + fastReconnectCheckStartTime);
		}
	}

	public void notifySentCallback(MqttToken token)
	{
		if (token != null)
		{
			IMqttActionListenerNew asyncCB = (IMqttActionListenerNew) token.getActionCallback();
			if (asyncCB != null)
			{
				asyncCB.notifyWrittenOnSocket(token);
			}
		}
	}

	private void decrementInFlight()
	{
		final String methodName = "decrementInFlight";
		synchronized (queueLock)
		{
			actualInFlight--;
			// @TRACE 646=-1 actualInFlight={0}

			if (!checkQuiesceLock())
			{
				queueLock.notifyAll();
			}
		}
	}

	protected boolean checkQuiesceLock()
	{
		final String methodName = "checkQuiesceLock";
		// if (quiescing && actualInFlight == 0 && pendingFlows.size() == 0 && inFlightPubRels == 0 && callback.isQuiesced()) {
		int tokC = tokenStore.count();
		if (quiescing && tokC == 0 && pendingFlows.size() == 0 && callback.isQuiesced())
		{
			// @TRACE 626=quiescing={0} actualInFlight={1} pendingFlows={2} inFlightPubRels={3} callbackQuiesce={4} tokens={5}

			synchronized (quiesceLock)
			{
				quiesceLock.notifyAll();
			}
			return true;
		}
		return false;
	}

	/**
	 * Called by the CommsReceiver when an ack has arrived.
	 * 
	 * @param message
	 * @throws MqttException
	 */
	protected void notifyReceivedAck(MqttAck ack) throws MqttException
	{
		final String methodName = "notifyReceivedAck";
		onInboundActivity();

		// @TRACE 627=received key={0} message={1}

		MqttToken token = tokenStore.getToken(ack);
		MqttException mex = null;

		if (token == null) {
			logger.d(TAG, "Token = null for MqttAck" + ack);
		}
		else if (ack instanceof MqttPubRec)
		{
			// Complete the QoS 2 flow. Unlike all other
			// flows, QoS is a 2 phase flow. The second phase sends a
			// PUBREL - the operation is not complete until a PUBCOMP
			// is received
			MqttPubRel rel = new MqttPubRel((MqttPubRec) ack);
			this.send(rel, token);
		}
		else if (ack instanceof MqttPubAck || ack instanceof MqttPubComp)
		{
			// QoS 1 & 2 notify users of result before removing from
			// persistence
			notifyResult(ack, token, mex);
			// Do not remove publish / delivery token at this stage
			// do this when the persistence is removed later
		}
		else if (ack instanceof MqttPingResp)
		{
			synchronized (pingOutstanding)
			{
				pingOutstanding = Boolean.FALSE;
				notifyResult(ack, token, mex);
				tokenStore.removeToken(ack);
			}
		}
		else if (ack instanceof MqttConnack)
		{
			int rc = ((MqttConnack) ack).getReturnCode();
			if (rc == 0)
			{
				synchronized (queueLock)
				{
					if (cleanSession)
					{
						clearState();
						// Add the connect token back in so that users can be
						// notified when connect completes.
						tokenStore.saveToken(token, ack);
					}
					inFlightPubRels = 0;
					actualInFlight = 0;
					restoreInflightMessages();
					connected();
				}
			}
			else
			{
				mex = ExceptionHelper.createMqttException(rc);
				throw mex;
			}

			clientComms.connectComplete((MqttConnack) ack, mex);
			notifyResult(ack, token, mex);
			tokenStore.removeToken(ack);

			// Notify the sender thread that there maybe work for it to do now
			synchronized (queueLock)
			{
				queueLock.notifyAll();
			}
		}
		else
		{
			// Sub ack or unsuback
			notifyResult(ack, token, mex);
			releaseMessageId(ack.getMessageId());
			tokenStore.removeToken(ack);
		}

		checkQuiesceLock();
	}

	/**
	 * Called by the CommsReceiver when a message has been received. Handles inbound messages and other flows such as PUBREL.
	 * 
	 * @param message
	 * @throws MqttException
	 */
	protected void notifyReceivedMsg(MqttWireMessage message) throws MqttException
	{
		final String methodName = "notifyReceivedMsg";
		onInboundActivity();

		// @TRACE 651=received key={0} message={1}

		if (!quiescing)
		{
			if (message instanceof MqttPublish)
			{
				MqttPublish send = (MqttPublish) message;
				switch (send.getMessage().getQos())
				{
				case 0:
				case 1:
					if (callback != null)
					{
						String logMessage = "Calling call back Message arrived for message ";
						logger.d(TAG, logMessage + ":" + send.getMessage().toString());
						callback.messageArrived(send);
					}
					break;
				case 2:
					persistence.put(getReceivedPersistenceKey(message), (MqttPublish) message);
					inboundQoS2.put(Integer.valueOf(send.getMessageId()), send);
					this.send(new MqttPubRec(send), null);
				}
			}
			else if (message instanceof MqttPubRel)
			{
				MqttPublish sendMsg = (MqttPublish) inboundQoS2.get(Integer.valueOf(message.getMessageId()));
				if (sendMsg != null)
				{
					if (callback != null)
					{
						callback.messageArrived(sendMsg);
					}
				}
				else
				{
					// Original publish has already been delivered.
					MqttPubComp pubComp = new MqttPubComp(message.getMessageId());
					this.send(pubComp, null);
				}
			}
		}
	}

	/**
	 * Called when waiters and callbacks have processed the message. For messages where delivery is complete the message can be removed from persistence and counters adjusted
	 * accordingly. Also tidy up by removing token from store...
	 * 
	 * @param message
	 * @throws MqttException
	 */
	protected void notifyComplete(MqttToken token) throws MqttException
	{
		final String methodName = "notifyComplete";

		MqttWireMessage message = token.internalTok.getWireMessage();

		if (message != null && message instanceof MqttAck)
		{
			// @TRACE 629=received key={0} token={1} message={2}

			MqttAck ack = (MqttAck) message;

			if (ack instanceof MqttPubAck)
			{
				// QoS 1 - user notified now remove from persistence...
				persistence.remove(getSendPersistenceKey(message));
				persistence.remove(getSendBufferedPersistenceKey(message));
				outboundQoS1.remove(Integer.valueOf(ack.getMessageId()));
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);
				// @TRACE 650=removed Qos 1 publish. key={0}

			}
			else if (ack instanceof MqttPubComp)
			{
				// QoS 2 - user notified now remove from persistence...
				persistence.remove(getSendPersistenceKey(message));
				persistence.remove(getSendConfirmPersistenceKey(message));
				persistence.remove(getSendBufferedPersistenceKey(message));
				outboundQoS2.remove(Integer.valueOf(ack.getMessageId()));

				inFlightPubRels--;
				decrementInFlight();
				releaseMessageId(message.getMessageId());
				tokenStore.removeToken(message);

				// @TRACE 645=removed QoS 2 publish/pubrel. key={0}, -1 inFlightPubRels={1}

			}

			checkQuiesceLock();
		}
	}

	protected void notifyResult(MqttWireMessage ack, MqttToken token, MqttException ex)
	{
		try
		{

			final String methodName = "notifyResult";
			// unblock any threads waiting on the token
			token.internalTok.markComplete(ack, ex);

			// Let the user know an async operation has completed and then remove the token
			if (ack != null && ack instanceof MqttAck && !(ack instanceof MqttPubRec))
			{
				// @TRACE 648=key{0}, msg={1}, excep={2}

				callback.asyncOperationComplete(token);
			}
			// There are cases where there is no ack as the operation failed before
			// an ack was received
			if (ack == null)
			{
				// @TRACE 649=key={0},excep={1}

				callback.asyncOperationComplete(token);
			}
		}
		catch (Exception e)
		{
			logger.e(TAG, "Exception occured", e);
		}
	}

	/**
	 * Called when the client has successfully connected to the broker
	 */
	public void connected()
	{
		final String methodName = "connected";
		// @TRACE 631=connected
		logger.d(TAG, "client has successfully connected");
		this.connected = true;

		pingSender.start(); // Start ping thread when client connected to server.
	}

	/**
	 * Called during shutdown to work out if there are any tokens still to be notified and waiters to be unblocked. Notifying and unblocking takes place after most shutdown
	 * processing has completed. The tokenstore is tidied up so it only contains outstanding delivery tokens which are valid after reconnect (if clean session is false)
	 * 
	 * @param reason
	 *            The root cause of the disconnection, or null if it is a clean disconnect
	 */
	public Vector resolveOldTokens(MqttException reason)
	{
		final String methodName = "resolveOldTokens";
		// @TRACE 632=reason {0}

		// If any outstanding let the user know the reason why it is still
		// outstanding by putting the reason shutdown is occurring into the
		// token.
		MqttException shutReason = reason;
		if (reason == null)
		{
			shutReason = new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
		}

		// Set the token up so it is ready to be notified after disconnect
		// processing has completed. Do not
		// remove the token from the store if it is a delivery token, it is
		// valid after a reconnect.
		Vector outT = tokenStore.getOutstandingTokens();
		Enumeration outTE = outT.elements();
		while (outTE.hasMoreElements())
		{
			MqttToken tok = (MqttToken) outTE.nextElement();
			synchronized (tok)
			{
				if (!tok.isComplete() && !tok.internalTok.isCompletePending() && tok.getException() == null)
				{
					tok.internalTok.setException(shutReason);
				}
			}
			if (!(tok instanceof MqttDeliveryToken))
			{
				// If not a delivery token it is not valid on
				// restart so remove
				tokenStore.removeToken(tok.internalTok.getKey());
			}
		}
		return outT;
	}

	/**
	 * Called when the client has been disconnected from the broker.
	 * 
	 * @param reason
	 *            The root cause of the disconnection, or null if it is a clean disconnect
	 */
	public void disconnected(MqttException reason)
	{
		final String methodName = "disconnected";
		// @TRACE 633=disconnected

		this.connected = false;

		try
		{
			if (cleanSession)
			{
				clearState();
			}

			pendingMessages.clear();
			pendingFlows.clear();
			synchronized (pingOutstanding)
			{
				// Reset pingOutstanding to allow reconnects to assume no previous ping.
				pingOutstanding = Boolean.FALSE;
			}
		}
		catch (MqttException e)
		{
			// Ignore as we have disconnected at this point
		}
	}

	/**
	 * Releases a message ID back into the pool of available message IDs. If the supplied message ID is not in use, then nothing will happen.
	 * 
	 * @param msgId
	 *            A message ID that can be freed up for re-use.
	 */
	private synchronized void releaseMessageId(int msgId)
	{
		inUseMsgIds.remove(Integer.valueOf(msgId));
	}

	/**
	 * Get the next MQTT message ID that is not already in use, and marks it as now being in use.
	 * 
	 * @return the next MQTT message ID to use
	 */
	private synchronized int getNextMessageId() throws MqttException
	{
		int startingMessageId = nextMsgId;
		// Allow two complete passes of the message ID range. This gives
		// any asynchronous releases a chance to occur
		int loopCount = 0;
		do
		{
			nextMsgId++;
			if (nextMsgId > MAX_MSG_ID)
			{
				nextMsgId = MIN_MSG_ID;
			}
			if (nextMsgId == startingMessageId)
			{
				loopCount++;
				if (loopCount == 2)
				{
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE);
				}
			}
		}
		while (inUseMsgIds.containsKey(Integer.valueOf(nextMsgId)));
		Integer id = Integer.valueOf(nextMsgId);
		inUseMsgIds.put(id, id);
		return nextMsgId;
	}

	/**
	 * Quiesce the client state, preventing any new messages getting sent, and preventing the callback on any newly received messages. After the timeout expires, delete any pending
	 * messages except for outbound ACKs, and wait for those ACKs to complete.
	 */
	public void quiesce(long timeout)
	{
		final String methodName = "quiesce";
		// If the timeout is greater than zero t
		if (timeout > 0)
		{
			// @TRACE 637=timeout={0}
			logger.d(TAG, "quiesce started, timeout : " + timeout);
			synchronized (queueLock)
			{
				this.quiescing = true;
			}
			// We don't want to handle any new inbound messages
			callback.quiesce();
			notifyQueueLock();

			synchronized (quiesceLock)
			{
				try
				{
					// If token count is not zero there is outbound work to process and
					// if pending flows is not zero there is outstanding work to complete and
					// if call back is not quiseced there it needs to complete.
					int tokc = tokenStore.count();
					if (tokc > 0 || pendingFlows.size() > 0 || !callback.isQuiesced())
					{
						// @TRACE 639=wait for outstanding: actualInFlight={0} pendingFlows={1} inFlightPubRels={2} tokens={3}

						// wait for outstanding in flight messages to complete and
						// any pending flows to complete
						quiesceLock.wait(timeout);
					}
				}
				catch (InterruptedException ex)
				{
					// Don't care, as we're shutting down anyway
				}
			}

			// Quiesce time up or inflight messages delivered. Ensure pending delivery
			// vectors are cleared ready for disconnect to be sent as the final flow.
			synchronized (queueLock)
			{
				pendingMessages.clear();
				pendingFlows.clear();
				quiescing = false;
				actualInFlight = 0;
			}
			// @TRACE 640=finished
			logger.d(TAG, "quiesce completed");
		}
	}

	public void notifyQueueLock()
	{
		final String methodName = "notifyQueueLock";
		synchronized (queueLock)
		{
			// @TRACE 638=notifying queueLock holders
			queueLock.notifyAll();
		}
	}

	protected void deliveryComplete(MqttPublish message) throws MqttPersistenceException
	{
		final String methodName = "deliveryComplete";

		// @TRACE 641=remove publish from persistence. key={0}

		persistence.remove(getReceivedPersistenceKey(message));
		inboundQoS2.remove(Integer.valueOf(message.getMessageId()));
	}

	/**
	 * Tidy up - ensure that tokens are released as they are maintained over a disconnect / connect cycle.
	 */
	protected void close()
	{
		inUseMsgIds.clear();
		pendingMessages.clear();
		pendingFlows.clear();
		outboundQoS2.clear();
		outboundQoS1.clear();
		inboundQoS2.clear();
		tokenStore.clear();
		inUseMsgIds = null;
		pendingMessages = null;
		pendingFlows = null;
		outboundQoS2 = null;
		outboundQoS1 = null;
		inboundQoS2 = null;
		tokenStore = null;
		callback = null;
		clientComms = null;
		persistence = null;
		pingCommand = null;
		fastReconnectCheckStartTime = 0;
		lastOutboundActivity = 0;
		lastInboundActivity = 0;
		lastPing = 0;
	}

	public Properties getDebug()
	{
		Properties props = new Properties();
		props.put("In use msgids", inUseMsgIds);
		props.put("pendingMessages", pendingMessages);
		props.put("pendingFlows", pendingFlows);
		props.put("maxInflight", Integer.valueOf(maxInflight));
		props.put("nextMsgID", Integer.valueOf(nextMsgId));
		props.put("actualInFlight", Integer.valueOf(actualInFlight));
		props.put("inFlightPubRels", Integer.valueOf(inFlightPubRels));
		props.put("quiescing", new Boolean(quiescing));
		props.put("pingoutstanding", pingOutstanding);
		props.put("lastOutboundActivity", Long.valueOf(lastOutboundActivity));
		props.put("lastInboundActivity", Long.valueOf(lastInboundActivity));
		props.put("outboundQoS2", outboundQoS2);
		props.put("outboundQoS1", outboundQoS1);
		props.put("inboundQoS2", inboundQoS2);
		props.put("tokens", tokenStore);
		return props;
	}

	public int getInflightMsgs()
	{
		return actualInFlight;
	}

	public int getMaxInflightMsgs()
	{
		return maxInflight;
	}

	public void setPersistence(MqttClientPersistence persistence)
	{
		this.persistence = persistence;
	}

	public long getFastReconnectCheckStartTime() {
		return fastReconnectCheckStartTime;
	}

	public long getLastOutboundActivity() {
		return lastOutboundActivity;
	}

	public long getLastInboundActivity() {
		return lastInboundActivity;
	}

	/**
	 * Persists a buffered message to the persistence layer
	 *
	 * @param message The {@link MqttWireMessage} to persist
	 * @throws MqttException if an exception occurs when no message ids available.
	 */
	public void persistBufferedMessage(MqttWireMessage message) throws MqttException {
		final String methodName = "persistBufferedMessage";
		String key;

		// Because the client will have disconnected, we will want to re-open persistence
		try {
			message.setMessageId(getNextMessageId());
			key = getSendBufferedPersistenceKey(message);
			try {
				persistence.put(key, (MqttPublish) message);
			} catch (MqttPersistenceException mpe){
				//@TRACE 515=Could not Persist, attempting to Re-Open Persistence Store
				persistence.open(this.clientComms.getClient().getClientId(), this.clientComms.getClient().getServerURI());
				persistence.put(key, (MqttPublish) message);
			}
			//@TRACE 513=Persisted Buffered Message key={0}
		} catch (MqttException ex){
			//@TRACE 514=Failed to persist buffered message key={0}
			throw ex;
		}
	}

	/**
	 * @param message The {@link MqttWireMessage} to un-persist
	 */
	public void unPersistBufferedMessage(MqttWireMessage message){
		final String methodName = "unPersistBufferedMessage";
		try{
			//@TRACE 517=Un-Persisting Buffered message key={0}
			persistence.remove(getSendBufferedPersistenceKey(message));
		} catch (MqttPersistenceException mpe){
			//@TRACE 518=Failed to Un-Persist Buffered message key={0}
		}
	}

	private String getSendBufferedPersistenceKey(MqttWireMessage message){
		return PERSISTENCE_SENT_BUFFERED_PREFIX + message.getMessageId();
	}

	private void onInboundActivity() {
		this.lastInboundActivity = System.currentTimeMillis();
		logger.d(TAG, "Last in bound activity changed : " + lastInboundActivity);
		this.pahoEvents.onInboundInactivity();
	}
}
