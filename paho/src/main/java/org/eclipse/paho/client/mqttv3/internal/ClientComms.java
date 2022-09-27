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

import org.eclipse.paho.client.mqttv3.BufferedMessage;
import org.eclipse.paho.client.mqttv3.ICommsCallback;
import org.eclipse.paho.client.mqttv3.IExperimentsConfig;
import org.eclipse.paho.client.mqttv3.ILogger;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IPahoEvents;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttInterceptor;
import org.eclipse.paho.client.mqttv3.MqttMessageInterceptor;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.NoOpsPahoEvents;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles client communications with the server. Sends and receives MQTT V3 messages.
 */
public class ClientComms {
    public static String VERSION = "${project.version}";

    public static String BUILD_LEVEL = "L${build.level}";

    private IMqttAsyncClient client;

    private int networkModuleIndex;

    private NetworkModule[] networkModules;

    CommsReceiver receiver;

    CommsSender sender;

    ICommsCallback callback;

    ClientState clientState;

    MqttInterceptorCallback mqttInterceptorCallback;

    MqttMessageInterceptorCallback messageInterceptorCallback;

    MqttConnectOptions conOptions;

    private MqttClientPersistence persistence;

    private MqttPingSender pingSender;

    private ILogger logger;

    CommsTokenStore tokenStore;

    boolean stoppingComms = false;

    final static byte CONNECTED = 0;

    final static byte CONNECTING = 1;

    final static byte DISCONNECTING = 2;

    final static byte DISCONNECTED = 3;

    final static byte CLOSED = 4;

    private byte conState = DISCONNECTED;

    Object conLock = new Object(); // Used to synchronize connection state

    private boolean closePending = false;

    private DisconnectedMessageBuffer disconnectedMessageBuffer;

    private IPahoEvents pahoEvents;

    private ExecutorService executorService = Executors.newSingleThreadExecutor(
            runnable -> new Thread(runnable, "disconnected-buffer-thread")
    );

    private final String TAG = "CLIENTCOMMS";

    /**
     * Creates a new ClientComms object, using the specified module to handle the network calls.
     */
    public ClientComms(
            IMqttAsyncClient client,
            MqttClientPersistence persistence,
            MqttPingSender pingSender,
            int maxInflightMsgs, ILogger logger,
            IExperimentsConfig experimentsConfig,
            List<MqttInterceptor> mqttInterceptorList,
            List<MqttMessageInterceptor> messageInterceptorList,
            IPahoEvents pahoEvents
    ) throws MqttException {
        this.conState = DISCONNECTED;
        this.client = client;
        this.persistence = persistence;
        this.pingSender = pingSender;
        this.logger = logger;
        this.pingSender.init(this, logger);
        this.pahoEvents = pahoEvents;

        this.tokenStore = new CommsTokenStore(getClient().getClientId());
        this.callback = new CommsCallback(this, logger);
        this.clientState = new ClientState(
                persistence,
                tokenStore,
                this.callback,
                this,
                pingSender,
                maxInflightMsgs,
                logger,
                experimentsConfig,
                pahoEvents
        );
        this.mqttInterceptorCallback = new MqttInterceptorCallback(mqttInterceptorList, logger);
        this.messageInterceptorCallback = new MqttMessageInterceptorCallback(messageInterceptorList);
        callback.setClientState(clientState);
    }

    /**
     * Sends a message to the server. Does not check if connected this validation must be done by invoking routines.
     *
     * @param message
     * @param token
     * @throws MqttException
     */
    void internalSend(MqttWireMessage message, MqttToken token) throws MqttException {
        internalSend(message, token, new NoOpsPahoEvents());
    }

    /**
     * Sends a message to the server. Does not check if connected this validation must be done by invoking routines.
     *
     * @param message
     * @param token
     * @throws MqttException
     */
    void internalSend(MqttWireMessage message, MqttToken token, IPahoEvents pahoEvents) throws MqttException {
        final String methodName = "internalSend";
        // @TRACE 200=internalSend key={0} message={1} token={2}

        if (token.getClient() == null) {
            // Associate the client with the token - also marks it as in use.
            token.internalTok.setClient(getClient());
        } else {
            // Token is already in use - cannot reuse
            // @TRACE 213=fail: token in use: key={0} message={1} token={2}
            logger.e(TAG, "Token is already in use - cannot reuse");

            throw new MqttException(MqttException.REASON_CODE_TOKEN_INUSE);
        }

        try {
            // Persist if needed and send the message
            this.clientState.send(message, token);
            if (message.getType() == MqttWireMessage.MESSAGE_TYPE_CONNECT) {
                pahoEvents.onConnectPacketSend();
            }
        } catch (MqttException e) {
            if (message instanceof MqttPublish) {
                this.clientState.undo((MqttPublish) message);
            }
            throw e;
        }
    }

	/**
	 * Sends a message to the broker if in connected state, but only waits for the message to be stored, before returning.
	 */
	public void sendNoWait(MqttWireMessage message, MqttToken token) throws MqttException {
		if (isConnected() ||
				(!isConnected() && message instanceof MqttConnect) ||
				(isDisconnecting() && message instanceof MqttDisconnect)) {
			if(disconnectedMessageBuffer != null && disconnectedMessageBuffer.getMessageCount() != 0){
				//@TRACE 507=Client Connected, Offline Buffer available, but not empty. Adding message to buffer. message={0}
                //This is done to maintain ordering of messages
				if (message instanceof MqttPublish && ((MqttPublish) message).getMessage().getQos() > 0) {
					if (disconnectedMessageBuffer.isPersistBuffer()) {
						this.clientState.persistBufferedMessage(message);
					}
					disconnectedMessageBuffer.putMessage(message, token);
				}
			} else {
				this.internalSend(message, token);
			}
		} else if(disconnectedMessageBuffer != null) {
			//@TRACE 508=Offline Buffer available. Adding message to buffer. message={0}
			if (message instanceof MqttPublish && ((MqttPublish) message).getMessage().getQos() > 0) {
				if (disconnectedMessageBuffer.isPersistBuffer()) {
					this.clientState.persistBufferedMessage(message);
				}
				disconnectedMessageBuffer.putMessage(message, token);
			}
		} else {
			//@TRACE 208=failed: not connected
			logger.e(TAG, "send failed , not connected");
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
		}
	}

    /**
     * Close and tidy up.
     * <p>
     * Call each main class and let it tidy up e.g. releasing the token store which normally survives a disconnect.
     *
     * @throws MqttException if not disconnected
     */
    public void close() throws MqttException {
        final String methodName = "close";
        synchronized (conLock) {
            if (!isClosed()) {
                // Must be disconnected before close can take place
                if (!isDisconnected()) {
                    // @TRACE 224=failed: not disconnected
                    logger.e(TAG, "close failed not disconnected");

                    if (isConnecting()) {
                        throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
                    } else if (isConnected()) {
                        throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
                    } else if (isDisconnecting()) {
                        closePending = true;
                        return;
                    }
                }

                conState = CLOSED;

                // ShutdownConnection has already cleaned most things
                clientState.close();
                executorService.shutdown();
                disconnectedMessageBuffer = null;
                clientState = null;
                callback = null;
                mqttInterceptorCallback = null;
                messageInterceptorCallback = null;
                persistence = null;
                sender = null;
                pingSender = null;
                receiver = null;
                networkModules = null;
                conOptions = null;
                tokenStore = null;
                logger.d(TAG, "close completed");
            }
        }
    }

    /**
     * Sends a connect message and waits for an ACK or NACK. Connecting is a special case which will also start up the network connection, receive thread, and keep alive thread.
     */
    public void connect(MqttConnectOptions options, MqttToken token, IPahoEvents pahoEvents) throws MqttException {
        final String methodName = "connect";
        synchronized (conLock) {
            if (isDisconnected() && !closePending) {
                // @TRACE 214=state=CONNECTING

                conState = CONNECTING;

                this.conOptions = options;

                MqttConnect connect = new MqttConnect(client.getClientId(), options.isCleanSession(), options.getKeepAliveIntervalServer(), options.getUserName(), options.getPassword(),
                        options.getWillMessage(), options.getWillDestination(), options.getProtocolName(), options.getProtocolLevel(), options.getUserPropertyList());

                this.clientState.setKeepAliveSecs(options.getKeepAliveInterval());
                this.clientState.setCleanSession(options.isCleanSession());

                tokenStore.open();
                ConnectBG conbg = new ConnectBG(this, token, connect, pahoEvents);
                conbg.start();
            } else {
                // @TRACE 207=connect failed: not disconnected {0}
                logger.e(TAG, "connect failed : not disconnected");
                if (isClosed() || closePending) {
                    throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
                } else if (isConnecting()) {
                    throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
                } else if (isDisconnecting()) {
                    throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
                } else {
                    throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
                }
            }
        }
    }

    public void connectComplete(MqttConnack cack, MqttException mex) throws MqttException {
        final String methodName = "connectComplete";
        int rc = cack.getReturnCode();
        synchronized (conLock) {
            if (rc == 0) {
                // We've successfully connected
                // @TRACE 215=state=CONNECTED
                logger.d(TAG, "client successfully connected");
                conState = CONNECTED;
                return;
            }
        }

        // @TRACE 204=connect failed: rc={0}
        logger.e(TAG, "connected failed , rc is not zero", mex);
        throw mex;
    }

    /**
     * Shuts down the connection to the server. This may have been invoked as a result of a user calling disconnect or an abnormal disconnection. The method may be invoked multiple
     * times in parallel as each thread when it receives an error uses this method to ensure that shutdown completes successfully.
     */
    public void shutdownConnection(MqttToken token, MqttException reason) {
        final String methodName = "shutdownConnection";
        boolean wasConnected;
        MqttToken endToken = null; // Token to notify after disconnect completes

        // This method could concurrently be invoked from many places only allow it
        // to run once.
        synchronized (conLock) {
            if (stoppingComms || closePending) {
                return;
            }
            stoppingComms = true;

            // @TRACE 216=state=DISCONNECTING
            logger.d(TAG, "setting constate to DISCONNECTING");

            wasConnected = (isConnected() || isDisconnecting());
            conState = DISCONNECTING;
        }

        // Update the token with the reason for shutdown if it
        // is not already complete.
        if (token != null && !token.isComplete()) {
            token.internalTok.setException(reason);
        }

        // Stop the thread that is used to call the user back
        // when actions complete
        if (callback != null) {
            callback.stop();
        }

        // Stop the network module, send and receive now not possible
        try {
            if (networkModules != null) {
                NetworkModule networkModule = networkModules[networkModuleIndex];
                if (networkModule != null) {
                    networkModule.stop();
                }
            }
        } catch (Exception ioe) {
            logger.e(TAG, "exception while trying to stop network module", ioe);
        }

        // Stop the thread that handles inbound work from the network
        if (receiver != null) {
            receiver.stop();
        }

        // Stop any new tokens being saved by app and throwing an exception if they do
        if (tokenStore != null) {
            tokenStore.quiesce(new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING));
        }

        // Notify any outstanding tokens with the exception of
        // con or discon which may be returned and will be notified at
        // the end
        endToken = handleOldTokens(token, reason);

        try {
            // Clean session handling and tidy up
            if (clientState != null) {
                clientState.disconnected(reason);
            }
        } catch (Exception ex) {
            logger.e(TAG, "exception while trying to disconnect clientState", ex);
        }

        if (sender != null) {
            sender.stop();
        }

        if (pingSender != null) {
            pingSender.stop();
        }

        if(mqttInterceptorCallback != null) {
            mqttInterceptorCallback.stop();
        }

        try {
            if (disconnectedMessageBuffer == null && persistence != null) {
                persistence.close();
            }
        } catch (Exception ex) {
            logger.e(TAG, "exception while trying to close persistence db", ex);
        }
        // All disconnect logic has been completed allowing the
        // client to be marked as disconnected.
        synchronized (conLock) {
            // @TRACE 217=state=DISCONNECTED
            logger.e(TAG, "setting constate to : DISCONNECTED");

            conState = DISCONNECTED;
            stoppingComms = false;
        }

        // Internal disconnect processing has completed. If there
        // is a disconnect token or a connect in error notify
        // it now. This is done at the end to allow a new connect
        // to be processed and now throw a currently disconnecting error.
        // any outstanding tokens and unblock any waiters
        if (endToken != null & callback != null) {
            callback.asyncOperationComplete(endToken);
        }

        // While disconnecting, close may have been requested - try it now
        synchronized (conLock) {
            if (wasConnected && callback != null) {
                // Let the user know client has disconnected either normally or abnormally
                callback.connectionLost(reason);
            }

            if (closePending) {
                try {
                    close();
                } catch (Exception e) { // ignore any errors as closing
                }
            }
        }
    }

    // Tidy up. There may be tokens outstanding as the client was
    // not disconnected/quiseced cleanly! Work out what tokens still
    // need to be notified and waiters unblocked. Store the
    // disconnect or connect token to notify after disconnect is
    // complete.
    private MqttToken handleOldTokens(MqttToken token, MqttException reason) {
        final String methodName = "handleOldTokens";
        // @TRACE 222=>

        MqttToken tokToNotifyLater = null;
        try {
            // First the token that was related to the disconnect / shutdown may
            // not be in the token table - temporarily add it if not
            if (token != null) {
                if (tokenStore.getToken(token.internalTok.getKey()) == null) {
                    tokenStore.saveToken(token, token.internalTok.getKey());
                }
            }

            Vector toksToNot = clientState.resolveOldTokens(reason);
            Enumeration toksToNotE = toksToNot.elements();
            while (toksToNotE.hasMoreElements()) {
                MqttToken tok = (MqttToken) toksToNotE.nextElement();

                if (tok.internalTok.getKey().equals(MqttDisconnect.KEY) || tok.internalTok.getKey().equals(MqttConnect.KEY)) {
                    // Its con or discon so remember and notify @ end of disc routine
                    tokToNotifyLater = tok;
                } else {
                    // notify waiters and callbacks of outstanding tokens
                    // that a problem has occurred and disconnect is in
                    // progress
                    callback.asyncOperationComplete(tok);
                }
            }
        } catch (Exception ex) {
            // Ignore as we are shutting down
        }
        return tokToNotifyLater;
    }

    public void disconnect(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token) throws MqttException {
        final String methodName = "disconnect";
        synchronized (conLock) {
            if (isClosed()) {
                // @TRACE 223=failed: in closed state
                logger.e(TAG, "disconnect failed, cause : " + "in closed state");
                throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
            } else if (isDisconnected()) {
                // @TRACE 211=failed: already disconnected
                logger.e(TAG, "disconnect failed, cause : " + "already disconnected");
                throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED);
            } else if (isDisconnecting()) {
                // @TRACE 219=failed: already disconnecting
                logger.e(TAG, "disconnect failed, cause : " + "already disconnecting");
                throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
            } else if (Thread.currentThread() == callback.getThread()) {
                // @TRACE 210=failed: called on callback thread
                logger.e(TAG, "disconnect failed, cause : " + "called on callback thread");
                // Not allowed to call disconnect() from the callback, as it will deadlock.
                throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED);
            }

            // @TRACE 218=state=DISCONNECTING
            logger.d(TAG, "setting constate state : DISCONNECTING");
            conState = DISCONNECTING;
            DisconnectBG discbg = new DisconnectBG(disconnect, quiesceTimeout, token);
            discbg.start();
        }
    }

    /**
     * Disconnect the connection and reset all the states.
     */
    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
        // Allow current inbound and outbound work to complete
        clientState.quiesce(quiesceTimeout);
        MqttToken token = new MqttToken(client.getClientId());
        try {
            // Send disconnect packet
            internalSend(new MqttDisconnect(), token);

            // Wait util the disconnect packet sent with timeout
            token.waitForCompletion(disconnectTimeout);
        } catch (Exception ex) {
            // ignore, probably means we failed to send the disconnect packet.
        } finally {
            token.internalTok.markComplete(null, null);
            shutdownConnection(token, null);
        }
    }

    public boolean isConnected() {
        synchronized (conLock) {
            return conState == CONNECTED;
        }
    }

    public boolean isConnecting() {
        synchronized (conLock) {
            return conState == CONNECTING;
        }
    }

    public boolean isDisconnected() {
        synchronized (conLock) {
            return conState == DISCONNECTED;
        }
    }

    public boolean isDisconnecting() {
        synchronized (conLock) {
            return conState == DISCONNECTING;
        }
    }

    public boolean isClosed() {
        synchronized (conLock) {
            return conState == CLOSED;
        }
    }

    public void setCallback(MqttCallback mqttCallback) {
        this.callback.setCallback(mqttCallback);
    }

    protected MqttTopic getTopic(String topic) {
        return new MqttTopic(topic, this);
    }

    public void setNetworkModuleIndex(int index) {
        this.networkModuleIndex = index;
    }

    public int getNetworkModuleIndex() {
        return networkModuleIndex;
    }

    public NetworkModule[] getNetworkModules() {
        return networkModules;
    }

    public void setNetworkModules(NetworkModule[] networkModules) {
        this.networkModules = networkModules;
    }

    public MqttDeliveryToken[] getPendingDeliveryTokens() {
        return tokenStore.getOutstandingDelTokens();
    }

    protected void deliveryComplete(MqttPublish msg) throws MqttPersistenceException {
        this.clientState.deliveryComplete(msg);
    }

    public IMqttAsyncClient getClient() {
        return client;
    }

    public long getKeepAlive() {
        return this.clientState.getKeepAlive();
    }

    public ClientState getClientState() {
        return clientState;
    }

    public MqttConnectOptions getConOptions() {
        return conOptions;
    }

    public Properties getDebug() {
        Properties props = new Properties();
        props.put("conState", new Integer(conState));
        props.put("serverURI", getClient().getServerURI());
        props.put("callback", callback);
        props.put("stoppingComms", new Boolean(stoppingComms));
        return props;
    }

    public void clear() {
        if (disconnectedMessageBuffer != null) {
            disconnectedMessageBuffer.clear();
        }
    }

    // Kick off the connect processing in the background so that it does not block. For instance
    // the socket could take time to create.
    private class ConnectBG implements Runnable {
        ClientComms clientComms = null;

        Thread cBg = null;

        MqttToken conToken;

        MqttConnect conPacket;

        IPahoEvents pahoEvents;

        ConnectBG(ClientComms cc, MqttToken cToken, MqttConnect cPacket, IPahoEvents pahoEvents) {
            clientComms = cc;
            conToken = cToken;
            conPacket = cPacket;
            cBg = new Thread(this, "MQTT Con: " + getClient().getClientId());
            this.pahoEvents = pahoEvents;
        }

        void start() {
            cBg.start();
        }

        public void run() {
            final String methodName = "connectBG:run";
            MqttException mqttEx = null;
            long time = System.currentTimeMillis();
            String eventType = "socket_conn_event";
            // @TRACE 220=>

            try {
                // Reset an exception on existing delivery tokens.
                // This will have been set if disconnect occured before delivery was
                // fully processed.
                MqttDeliveryToken[] toks = tokenStore.getOutstandingDelTokens();
                for (int i = 0; i < toks.length; i++) {
                    toks[i].internalTok.setException(null);
                }

                // Save the connect token in tokenStore as failure can occur before send
                tokenStore.saveToken(conToken, conPacket);

                // Connect to the server at the network level e.g. TCP socket and then
                // start the background processing threads before sending the connect
                // packet.
                NetworkModule networkModule = networkModules[networkModuleIndex];
                networkModule.start();
                logger.logEvent(eventType, true, client.getServerURI(), (System.currentTimeMillis() - time), null, 0, 0, 0, "", 0);
                time = System.currentTimeMillis();
                receiver = new CommsReceiver(clientComms, clientState, tokenStore, networkModule.getInputStream(), networkModule.getSocket(), logger, mqttInterceptorCallback, messageInterceptorCallback);
                receiver.start("MQTT Rec: " + getClient().getClientId());
                sender = new CommsSender(clientComms, clientState, tokenStore, networkModule.getOutputStream(), networkModule.getSocket(), logger, mqttInterceptorCallback, messageInterceptorCallback);
                sender.start("MQTT Snd: " + getClient().getClientId());
                callback.start("MQTT Call: " + getClient().getClientId());
                mqttInterceptorCallback.start("MQTT Int Call: " + getClient().getClientId());
                internalSend(conPacket, conToken, pahoEvents);
                eventType = "conn_pkt_event";
                logger.logEvent(eventType, true, client.getServerURI(), (System.currentTimeMillis() - time), null, 0, 0, 0, "", 0);
            } catch (MqttException ex) {
                // @TRACE 212=connect failed: unexpected exception
                logger.e(TAG, "connect failed : unxpected exception , cause : ", ex);
                mqttEx = ex;
            } catch (Exception ex) {
                // @TRACE 209=connect failed: unexpected exception
                logger.e(TAG, "connect failed : unxpected exception , cause : ", ex);
                mqttEx = ExceptionHelper.createMqttException(ex);
            }

            if (mqttEx != null) {
                logger.logEvent(eventType, false, client.getServerURI(), (System.currentTimeMillis() - time), mqttEx, mqttEx.getReasonCode(), 0, 0, "", 0);
                shutdownConnection(conToken, mqttEx);
            }
        }
    }

    // Kick off the disconnect processing in the background so that it does not block. For instance
    // the quiesce
    private class DisconnectBG implements Runnable {
        Thread dBg = null;

        MqttDisconnect disconnect;

        long quiesceTimeout;

        MqttToken token;

        DisconnectBG(MqttDisconnect disconnect, long quiesceTimeout, MqttToken token) {
            this.disconnect = disconnect;
            this.quiesceTimeout = quiesceTimeout;
            this.token = token;
        }

        void start() {
            dBg = new Thread(this, "MQTT Disc: " + getClient().getClientId());
            dBg.start();
        }

        public void run() {
            final String methodName = "disconnectBG:run";
            // @TRACE 221=>

            // Allow current inbound and outbound work to complete
            clientState.quiesce(quiesceTimeout);
            try {
                internalSend(disconnect, token);
                token.internalTok.waitUntilSent();
            } catch (MqttException ex) {
            } finally {
                token.internalTok.markComplete(null, null);
                shutdownConnection(token, null);
            }
        }
    }

    /*
     * Check and send a ping if needed and check for ping timeout. Need to send a ping if nothing has been sent or received in the last keepalive interval.
     */
    public MqttToken checkForActivity() {
        MqttToken token = null;
        try {
            token = clientState.checkForActivity();
        } catch (MqttException e) {
            handleRunException(e);
        } catch (Exception e) {
            handleRunException(e);
        }
        return token;
    }

    /*
     * Check and send a ping and check for ping timeout.
     */
    public MqttToken sendPingRequest() {
        MqttToken token = null;
        try {
            token = clientState.sendForcePingRequest();
        } catch (MqttException e) {
            handleRunException(e);
        } catch (Exception e) {
            handleRunException(e);
        }
        return token;
    }

    /**
     * This fucntion will check if there is an ack or incoming message in out - in window
     * If no msg or ack then we disconnect and reconnect.
     */
    public void checkActivity() {
        try {
            clientState.checkActivity();
        } catch (MqttException e) {
            callback.fastReconnect();
            handleRunException(e);
        } catch (Exception e) {
            handleRunException(e);
        }
    }

    private void handleRunException(Exception ex) {
        final String methodName = "handleRunException";
        // @TRACE 804=exception
        logger.e(TAG, "excpetion occured , shutting down connection : ", ex);
        MqttException mex;
        if (!(ex instanceof MqttException)) {
            mex = new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, ex);
        } else {
            mex = (MqttException) ex;
        }

        shutdownConnection(null, mex);
    }

    public void setPersistence(MqttClientPersistence persistence) {
        this.persistence = persistence;
        clientState.setPersistence(persistence);

    }

	public void setDisconnectedMessageBuffer(DisconnectedMessageBuffer disconnectedMessageBuffer) {
		this.disconnectedMessageBuffer = disconnectedMessageBuffer;
	}

	public int getBufferedMessageCount(){
		return this.disconnectedMessageBuffer.getMessageCount();
	}

	public MqttMessage getBufferedMessage(int bufferIndex){
		MqttPublish send = (MqttPublish) this.disconnectedMessageBuffer.getMessage(bufferIndex).getMessage();
		return send.getMessage();
	}

	public void deleteBufferedMessage(int bufferIndex){
		this.disconnectedMessageBuffer.deleteMessage(bufferIndex);
	}

	/**
	 * When the client connects, we want to send all messages from the
	 * buffer first before allowing the user to send any messages
	 */
	public void notifyConnect() {
		final String methodName = "notifyConnect";
		if(disconnectedMessageBuffer != null) {
            //@TRACE 509=Client Connected, Offline Buffer Available. Sending Buffered Messages.

            disconnectedMessageBuffer.setPublishCallback(new ReconnectDisconnectedBufferCallback(methodName));
            disconnectedMessageBuffer.setMessageDiscardedCallBack(new MessageDiscardedCallback());
            executorService.execute(disconnectedMessageBuffer);
        }
	}


	class MessageDiscardedCallback implements IDiscardedBufferMessageCallback {

		@Override
		public void messageDiscarded(MqttWireMessage message) {
			if(disconnectedMessageBuffer.isPersistBuffer()) {
				clientState.unPersistBufferedMessage(message);
			}
            pahoEvents.onOfflineMessageDiscarded(message.getMessageId());
		}
	}


	class ReconnectDisconnectedBufferCallback implements IDisconnectedBufferCallback{

		final String methodName;

		ReconnectDisconnectedBufferCallback(String methodName) {
			this.methodName = methodName;
		}

		public void publishBufferedMessage(BufferedMessage bufferedMessage) throws MqttException {
			if (isConnected()) {
				// First pass at making sure that we don't flood the in-flight messages
				while(clientState.getInflightMsgs() >= (clientState.getMaxInflightMsgs()-3)){
					// We need to Yield to the other threads to allow the in flight messages to clear
					Thread.yield();

				}
				//@TRACE 510=Publishing Buffered message message={0}
				internalSend(bufferedMessage.getMessage(), bufferedMessage.getToken());

				// Delete from persistence if in there
				clientState.unPersistBufferedMessage(bufferedMessage.getMessage());
			} else {
				//@TRACE 208=failed: not connected
				throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
			}
		}
	}

	public int getActualInFlight() {
		return this.clientState.getInflightMsgs();
	}
}
