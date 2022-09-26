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
package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.ClientState;
import org.eclipse.paho.client.mqttv3.internal.ConnectActionListener;
import org.eclipse.paho.client.mqttv3.internal.DisconnectedMessageBuffer;
import org.eclipse.paho.client.mqttv3.internal.ExceptionHelper;
import org.eclipse.paho.client.mqttv3.internal.LocalNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.NetworkModule;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.SSLNetworkModuleV2;
import org.eclipse.paho.client.mqttv3.internal.TCPNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.security.SSLSocketFactoryFactory;
import org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModule;
import org.eclipse.paho.client.mqttv3.internal.websocket.WebSocketSecureNetworkModuleV2;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubscribe;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Lightweight client for talking to an MQTT server using non-blocking methods that allow an operation to run in the background.
 *
 * <p>
 * This class implements the non-blocking {@link IMqttAsyncClient} client interface allowing applications to initiate MQTT actions and then carry on working while the MQTT action
 * completes on a background thread. This implementation is compatible with all Java SE runtimes from 1.4.2 and up.
 * </p>
 * <p>
 * An application can connect to an MQTT server using:
 * <ul>
 * <li>A plain TCP socket
 * <li>A secure SSL/TLS socket
 * </ul>
 * </p>
 * <p>
 * To enable messages to be delivered even across network and client restarts messages need to be safely stored until the message has been delivered at the requested quality of
 * service. A pluggable persistence mechanism is provided to store the messages.
 * </p>
 * <p>
 * By default {@link MqttDefaultFilePersistence} is used to store messages to a file. If persistence is set to null then messages are stored in memory and hence can be lost if the
 * client, Java runtime or device shuts down.
 * </p>
 * <p>
 * If connecting with {@link MqttConnectOptions#setCleanSession(boolean)} set to true it is safe to use memory persistence as all state is cleared when a client disconnects. If
 * connecting with cleanSession set to false in order to provide reliable message delivery then a persistent message store such as the default one should be used.
 * </p>
 * <p>
 * The message store interface is pluggable. Different stores can be used by implementing the {@link MqttClientPersistence} interface and passing it to the clients constructor.
 * </p>
 *
 * @see IMqttAsyncClient
 */
public class MqttAsyncClient implements IMqttAsyncClient
{ // DestinationProvider {

	private static final String CLIENT_ID_PREFIX = "paho-";

	private static final long QUIESCE_TIMEOUT = 30000; // ms

	private static final long DISCONNECT_TIMEOUT = 10000; // ms

	private String clientId;

	private String mqttVersion;

	private String serverURI;

	protected ClientComms comms;

	private ILogger logger;

	private Hashtable topics;


	private MqttClientPersistence persistence;

	private IPahoEvents pahoEvents;

	private IExperimentsConfig experimentsConfig;

	final static String className = MqttAsyncClient.class.getName();

	final private String TAG = "MqttAsyncClient";

	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method on MqttConnectOptions.
	 *
	 * <p>
	 * The <code>serverURI</code> parameter is typically used with the the <code>clientId</code> parameter to form a key. The key is used to store and reference messages while they
	 * are being delivered. Hence the serverURI specified on the constructor must still be specified even if a list of servers is specified on an MqttConnectOptions object. The
	 * serverURI on the constructor must remain the same across restarts of the client for delivery of messages to be maintained from a given client to a given server or set of
	 * servers.
	 *
	 * <p>
	 * The address of the server to connect to is specified as a URI. Two types of connection are supported <code>tcp://</code> for a TCP connection and <code>ssl://</code> for a
	 * TCP connection secured by SSL/TLS. For example:
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters. It must be unique across all clients connecting to the same server. The
	 * clientId is used by the server to store data related to the client, hence it is important that the clientId remain the same when connecting to a server if durable
	 * subscriptions or reliable messaging are required.
	 * <p>
	 * A convenience method is provided to generate a random client id that should satisfy this criteria - {@link #generateClientId()}. As the client identifier is used by the
	 * server to identify a client when it reconnects, the client must use the same identifier between connections if durable subscriptions or reliable delivery of messages is
	 * required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the client will use in the following order:
	 * </p>
	 * <ul>
	 * <li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can use {@link MqttConnectOptions#setSocketFactory(SocketFactory)} to supply a factory with
	 * the appropriate SSL settings.</li>
	 * <li><strong>SSL Properties</strong> - applications can supply SSL settings as a simple Java Properties using {@link MqttConnectOptions#setSSLProperties(Properties)}.</li>
	 * <li><strong>Use JVM settings</strong> - There are a number of standard Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>
	 * In Java ME, the platform settings are used for SSL connections.
	 * </p>
	 *
	 * <p>
	 * An instance of the default persistence mechanism {@link MqttDefaultFilePersistence} is used by the client. To specify a different persistence mechanism or to turn off
	 * persistence, use the {@link #MqttAsyncClient(String, String, MqttClientPersistence)} constructor.
	 *
	 * @param serverURI
	 *            the address of the server to connect to, specified as a URI. Can be overridden using {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId
	 *            a client identifier that is unique on the server being connected to
	 * @throws IllegalArgumentException
	 *             if the URI does not start with "tcp://", "ssl://" or "local://".
	 * @throws IllegalArgumentException
	 *             if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(String serverURI, String clientId, String mqttVersion) throws MqttException
	{
		this(serverURI, clientId, mqttVersion, new MqttDefaultFilePersistence(), new TimerPingSender(), new NoOpLogger());
	}

	public MqttAsyncClient(String serverURI, String clientId, String mqttVersion, MqttClientPersistence persistence, MqttPingSender mqttPingSender, ILogger logger) throws MqttException
	{
		this(serverURI, clientId, mqttVersion, persistence, 100, mqttPingSender, logger, new NoOpsPahoEvents(), null);
	}

	public MqttAsyncClient(
			String serverURI,
			String clientId,
			String mqttVersion,
			MqttClientPersistence persistence,
			int maxInflightMsgs,
			MqttPingSender pingSender,
			ILogger logger,
			IPahoEvents pahoEvents,
			IExperimentsConfig experimentsConfig
	) throws MqttException
	{
		this(serverURI, clientId, mqttVersion, persistence, 100, pingSender, logger, new NoOpsPahoEvents(), null, null);
	}
	/**
	 * Create an MqttAsyncClient that is used to communicate with an MQTT server.
	 * <p>
	 * The address of a server can be specified on the constructor. Alternatively a list containing one or more servers can be specified using the
	 * {@link MqttConnectOptions#setServerURIs(String[]) setServerURIs} method on MqttConnectOptions.
	 *
	 * <p>
	 * The <code>serverURI</code> parameter is typically used with the the <code>clientId</code> parameter to form a key. The key is used to store and reference messages while they
	 * are being delivered. Hence the serverURI specified on the constructor must still be specified even if a list of servers is specified on an MqttConnectOptions object. The
	 * serverURI on the constructor must remain the same across restarts of the client for delivery of messages to be maintained from a given client to a given server or set of
	 * servers.
	 *
	 * <p>
	 * The address of the server to connect to is specified as a URI. Two types of connection are supported <code>tcp://</code> for a TCP connection and <code>ssl://</code> for a
	 * TCP connection secured by SSL/TLS. For example:
	 * <ul>
	 * <li><code>tcp://localhost:1883</code></li>
	 * <li><code>ssl://localhost:8883</code></li>
	 * </ul>
	 * If the port is not specified, it will default to 1883 for <code>tcp://</code>" URIs, and 8883 for <code>ssl://</code> URIs.
	 * </p>
	 *
	 * <p>
	 * A client identifier <code>clientId</code> must be specified and be less that 65535 characters. It must be unique across all clients connecting to the same server. The
	 * clientId is used by the server to store data related to the client, hence it is important that the clientId remain the same when connecting to a server if durable
	 * subscriptions or reliable messaging are required.
	 * <p>
	 * A convenience method is provided to generate a random client id that should satisfy this criteria - {@link #generateClientId()}. As the client identifier is used by the
	 * server to identify a client when it reconnects, the client must use the same identifier between connections if durable subscriptions or reliable delivery of messages is
	 * required.
	 * </p>
	 * <p>
	 * In Java SE, SSL can be configured in one of several ways, which the client will use in the following order:
	 * </p>
	 * <ul>
	 * <li><strong>Supplying an <code>SSLSocketFactory</code></strong> - applications can use {@link MqttConnectOptions#setSocketFactory(SocketFactory)} to supply a factory with
	 * the appropriate SSL settings.</li>
	 * <li><strong>SSL Properties</strong> - applications can supply SSL settings as a simple Java Properties using {@link MqttConnectOptions#setSSLProperties(Properties)}.</li>
	 * <li><strong>Use JVM settings</strong> - There are a number of standard Java system properties that can be used to configure key and trust stores.</li>
	 * </ul>
	 *
	 * <p>
	 * In Java ME, the platform settings are used for SSL connections.
	 * </p>
	 * <p>
	 * A persistence mechanism is used to enable reliable messaging. For messages sent at qualities of service (QoS) 1 or 2 to be reliably delivered, messages must be stored (on
	 * both the client and server) until the delivery of the message is complete. If messages are not safely stored when being delivered then a failure in the client or server can
	 * result in lost messages. A pluggable persistence mechanism is supported via the {@link MqttClientPersistence} interface. An implementer of this interface that safely stores
	 * messages must be specified in order for delivery of messages to be reliable. In addition {@link MqttConnectOptions#setCleanSession(boolean)} must be set to false. In the
	 * event that only QoS 0 messages are sent or received or cleanSession is set to true then a safe store is not needed.
	 * </p>
	 * <p>
	 * An implementation of file-based persistence is provided in class {@link MqttDefaultFilePersistence} which will work in all Java SE based systems. If no persistence is
	 * needed, the persistence parameter can be explicitly set to <code>null</code>.
	 * </p>
	 *
	 * @param serverURI
	 *            the address of the server to connect to, specified as a URI. Can be overridden using {@link MqttConnectOptions#setServerURIs(String[])}
	 * @param clientId
	 *            a client identifier that is unique on the server being connected to
	 * @param persistence
	 *            the persistence class to use to store in-flight message. If null then the default persistence mechanism is used
	 * @param mqttInterceptorList
	 * @throws IllegalArgumentException
	 *             if the URI does not start with "tcp://", "ssl://" or "local://"
	 * @throws IllegalArgumentException
	 *             if the clientId is null or is greater than 65535 characters in length
	 * @throws MqttException
	 *             if any other problem was encountered
	 */
	public MqttAsyncClient(
			String serverURI,
			String clientId,
			String mqttVersion,
			MqttClientPersistence persistence,
			int maxInflightMsgs,
			MqttPingSender pingSender,
			ILogger logger,
			IPahoEvents pahoEvents,
			IExperimentsConfig experimentsConfig,
			List<MqttInterceptor> mqttInterceptorList
	) throws MqttException
	{
		final String methodName = "MqttAsyncClient";

		if (clientId == null)
		{ // Support empty client Id, 3.1.1 standard
			throw new IllegalArgumentException("Null clientId");
		}
		// Count characters, surrogate pairs count as one character.
		int clientIdLength = 0;
		for (int i = 0; i < clientId.length() - 1; i++)
		{
			if (Character_isHighSurrogate(clientId.charAt(i)))
				i++;
			clientIdLength++;
		}
		if (clientIdLength > 65535)
		{
			throw new IllegalArgumentException("ClientId longer than 65535 characters");
		}

		MqttConnectOptions.validateURI(serverURI);

		this.serverURI = serverURI;
		this.clientId = clientId;
		this.mqttVersion = mqttVersion;

		this.persistence = persistence;
		this.experimentsConfig = experimentsConfig;
		if (this.persistence == null)
		{
			this.persistence = new MemoryPersistence();
		}

		// @TRACE 101=<init> ClientID={0} ServerURI={1} PersistenceType={2}

		this.persistence.open(clientId, serverURI);
		this.comms = new ClientComms(this, this.persistence, getMqttPingSender(pingSender), maxInflightMsgs, logger, experimentsConfig, mqttInterceptorList, pahoEvents);
		this.logger = logger;
		this.persistence.close();
		this.topics = new Hashtable();
		this.pahoEvents = pahoEvents;
	}

	private MqttPingSender getMqttPingSender(MqttPingSender mqttPingSender)
	{
		return mqttPingSender == null ? new TimerPingSender() : mqttPingSender;
	}

	/**
	 * @param ch
	 * @return returns 'true' if the character is a high-surrogate code unit
	 */
	protected static boolean Character_isHighSurrogate(char ch)
	{
		char MIN_HIGH_SURROGATE = '\uD800';
		char MAX_HIGH_SURROGATE = '\uDBFF';
		return (ch >= MIN_HIGH_SURROGATE) && (ch <= MAX_HIGH_SURROGATE);
	}

	/**
	 * Factory method to create an array of network modules, one for each of the supplied URIs
	 *
	 * @param address
	 *            the URI for the server.
	 * @return a network module appropriate to the specified address.
	 */

	// may need an array of these network modules

	protected NetworkModule[] createNetworkModules(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException
	{
		final String methodName = "createNetworkModules";
		// @TRACE 116=URI={0}

		NetworkModule[] networkModules = null;
		String[] serverURIs = options.getServerURIs();
		String[] array = null;
		if (serverURIs == null)
		{
			array = new String[] { address };
		}
		else if (serverURIs.length == 0)
		{
			array = new String[] { address };
		}
		else
		{
			array = serverURIs;
		}

		networkModules = new NetworkModule[array.length];
		for (int i = 0; i < array.length; i++)
		{
			networkModules[i] = createNetworkModule(array[i], options);
		}

		return networkModules;
	}

	/**
	 * Factory method to create the correct network module, based on the supplied address URI.
	 *
	 * @param address
	 *            the URI for the server.
	 * @param Connect
	 *            options
	 * @return a network module appropriate to the specified address.
	 */
	private NetworkModule createNetworkModule(String address, MqttConnectOptions options) throws MqttException, MqttSecurityException
	{
		final String methodName = "createNetworkModule";
		// @TRACE 115=URI={0}

		NetworkModule netModule;
		String shortAddress;
		String host;
		int port;
		SocketFactory factory = options.getSocketFactory();

		int serverURIType = MqttConnectOptions.validateURI(address);

		switch (serverURIType)
		{
			case MqttConnectOptions.URI_TYPE_TCP:
				shortAddress = address.substring(6);
				host = getHostName(shortAddress);
				port = getPort(shortAddress, 1883);
				if (factory == null)
				{
					factory = SocketFactory.getDefault();
				}
				else if (factory instanceof SSLSocketFactory)
				{
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
				}
				netModule = new TCPNetworkModule(factory, host, port, clientId, logger, pahoEvents);
				((TCPNetworkModule) netModule).setConnectTimeout(options.getConnectionTimeout());
				((TCPNetworkModule) netModule).setReadTimeout(options.getReadTimeout());
				break;
			case MqttConnectOptions.URI_TYPE_SSL:
				if(experimentsConfig.useNewSSLFlow()) {
					netModule = getSSLNetworkModuleV2(address, options);
					break;
				}
				shortAddress = address.substring(6);
				host = getHostName(shortAddress);
				port = getPort(shortAddress, 8883);
				SSLSocketFactoryFactory factoryFactory = null;

				factoryFactory = new SSLSocketFactoryFactory();
				Properties sslClientProps = options.getSSLProperties();
				if (null != sslClientProps) {
					factoryFactory.initialize(sslClientProps, null);
				}
				factory = factoryFactory.createSocketFactory(null);

				netModule = new SSLNetworkModule((SSLSocketFactory) factory, host, port, clientId, logger, pahoEvents);
				((SSLNetworkModule) netModule).setConnectTimeout(options.getConnectionTimeout());
				((SSLNetworkModule) netModule).setSSLhandshakeTimeout(options.getHandshakeTimeout());
				((SSLNetworkModule) netModule).setReadTimeout(options.getReadTimeout());
				// Ciphers suites need to be set, if they are available
				if (factoryFactory != null)
				{
					String[] enabledCiphers = factoryFactory.getEnabledCipherSuites(null);
					if (enabledCiphers != null)
					{
						((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
					}
				}
				break;
			case MqttConnectOptions.URI_TYPE_WS:
				shortAddress = address.substring(5);
				host = getHostName(shortAddress);
				port = getPort(shortAddress, 80);
				if (factory == null) {
					factory = SocketFactory.getDefault();
				}
				else if (factory instanceof SSLSocketFactory) {
					throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH);
				}
				netModule = new WebSocketNetworkModule(factory, address, host, port, clientId, logger, pahoEvents);
				((WebSocketNetworkModule)netModule).setConnectTimeout(options.getConnectionTimeout());
				((WebSocketNetworkModule)netModule).setReadTimeout(options.getReadTimeout());
				break;
			case MqttConnectOptions.URI_TYPE_WSS:
				if(experimentsConfig.useNewSSLFlow()) {
					netModule = getWSSNetworkModuleV2(address, options);
					break;
				}
				shortAddress = address.substring(6);
				host = getHostName(shortAddress);
				port = getPort(shortAddress, 443);
				SSLSocketFactoryFactory wSSFactoryFactory = null;

				wSSFactoryFactory = new SSLSocketFactoryFactory();
				sslClientProps = options.getSSLProperties();
				if (null != sslClientProps) {
					wSSFactoryFactory.initialize(sslClientProps, null);
				}
				factory = wSSFactoryFactory.createSocketFactory(null);

				netModule = new WebSocketSecureNetworkModule((SSLSocketFactory) factory, address, host, port, clientId, logger, pahoEvents);
				((WebSocketSecureNetworkModule)netModule).setConnectTimeout(options.getConnectionTimeout());
				((WebSocketSecureNetworkModule)netModule).setSSLhandshakeTimeout(options.getHandshakeTimeout());
				((WebSocketSecureNetworkModule)netModule).setReadTimeout(options.getReadTimeout());
				// Ciphers suites need to be set, if they are available
				if (wSSFactoryFactory != null) {
					String[] enabledCiphers = wSSFactoryFactory.getEnabledCipherSuites(null);
					if (enabledCiphers != null) {
						((SSLNetworkModule) netModule).setEnabledCiphers(enabledCiphers);
					}
				}
				break;
			case MqttConnectOptions.URI_TYPE_LOCAL:
				netModule = new LocalNetworkModule(address.substring(8));
				break;
			default:
				// This shouldn't happen, as long as validateURI() has been called.
				netModule = null;
		}
		return netModule;
	}

	private NetworkModule getSSLNetworkModuleV2(String address, MqttConnectOptions options)
			throws MqttException {
		String shortAddress = address.substring(6);
		String host = getHostName(shortAddress);
		int port = getPort(shortAddress, 443);

		// Create the network module...
		NetworkModule netModule = new SSLNetworkModuleV2(
				options.getSocketFactory(),
				options.getSslSocketFactory(),
				options.getX509TrustManager(),
				options.getConnectionSpec(),
				options.getAlpnProtocolList(),
				host,
				port,
				clientId,
				logger,
				pahoEvents
		);
		((SSLNetworkModuleV2) netModule).setConnectTimeout(options.getConnectionTimeout());
		((SSLNetworkModuleV2) netModule).setSSLhandshakeTimeout(options.getHandshakeTimeout());
		((SSLNetworkModuleV2) netModule).setReadTimeout(options.getReadTimeout());
		return netModule;
	}

	private NetworkModule getWSSNetworkModuleV2(String address, MqttConnectOptions options)
			throws MqttException {
		String shortAddress = address.substring(6);
		String host = getHostName(shortAddress);
		int port = getPort(shortAddress, 443);

		NetworkModule netModule = new WebSocketSecureNetworkModuleV2(
				options.getSocketFactory(),
				options.getSslSocketFactory(),
				options.getX509TrustManager(),
				options.getConnectionSpec(),
				options.getAlpnProtocolList(),
				address,
				host,
				port,
				clientId,
				logger,
				pahoEvents
		);
		((WebSocketSecureNetworkModuleV2) netModule).setConnectTimeout(options.getConnectionTimeout());
		((WebSocketSecureNetworkModuleV2) netModule).setSSLhandshakeTimeout(options.getHandshakeTimeout());
		((WebSocketSecureNetworkModuleV2) netModule).setReadTimeout(options.getReadTimeout());
		return netModule;
	}

	private int getPort(String uri, int defaultPort)
	{
		int port;
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1)
		{
			port = defaultPort;
		}
		else
		{
			port = Integer.valueOf(uri.substring(portIndex + 1)).intValue();
		}
		return port;
	}

	private String getHostName(String uri)
	{
		int schemeIndex = uri.lastIndexOf('/');
		int portIndex = uri.lastIndexOf(':');
		if (portIndex == -1)
		{
			portIndex = uri.length();
		}
		return uri.substring(schemeIndex + 1, portIndex);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException
	{
		return this.connect(new MqttConnectOptions(), userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect()
	 */
	public IMqttToken connect() throws MqttException, MqttSecurityException
	{
		return this.connect(null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(org.eclipse.paho.client.mqttv3.MqttConnectOptions)
	 */
	public IMqttToken connect(MqttConnectOptions options) throws MqttException, MqttSecurityException
	{
		return this.connect(options, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#connect(org.eclipse.paho.client.mqttv3.MqttConnectOptions, java.lang.Object,
	 * org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException
	{
		final String methodName = "connect";
		if (comms.isConnected())
		{
			throw ExceptionHelper.createMqttException(MqttException.REASON_CODE_CLIENT_CONNECTED);
		}
		if (comms.isConnecting())
		{
			throw new MqttException(MqttException.REASON_CODE_CONNECT_IN_PROGRESS);
		}
		if (comms.isDisconnecting())
		{
			throw new MqttException(MqttException.REASON_CODE_CLIENT_DISCONNECTING);
		}
		if (comms.isClosed())
		{
			throw new MqttException(MqttException.REASON_CODE_CLIENT_CLOSED);
		}

		// @TRACE 103=cleanSession={0} connectionTimeout={1} TimekeepAlive={2} userName={3} password={4} will={5} userContext={6} callback={7}

		comms.setNetworkModules(createNetworkModules(serverURI, options));

		// Insert our own callback to iterate through the URIs till the connect succeeds
		MqttToken userToken = new MqttToken(getClientId());
		ConnectActionListener connectActionListener = new ConnectActionListener(this, persistence, comms, options, userToken, userContext, callback, pahoEvents);
		userToken.setActionCallback(connectActionListener);
		userToken.setUserContext(this);

		comms.setNetworkModuleIndex(0);
		connectActionListener.connect();

		return userToken;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException
	{
		return this.disconnect(QUIESCE_TIMEOUT, userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect()
	 */
	public IMqttToken disconnect() throws MqttException
	{
		return this.disconnect(null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long)
	 */
	public IMqttToken disconnect(long quiesceTimeout) throws MqttException
	{
		return this.disconnect(quiesceTimeout, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnect(long, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException
	{
		final String methodName = "disconnect";
		// @TRACE 104=> quiesceTimeout={0} userContext={1} callback={2}
		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);

		MqttDisconnect disconnect = new MqttDisconnect();
		try
		{
			comms.disconnect(disconnect, quiesceTimeout, token);
		}
		catch (MqttException ex)
		{
			// @TRACE 105=< exception
			logger.e(TAG, "Exception in disconnect : " + ex);
			throw ex;
		}
		// @TRACE 108=<

		return token;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly()
	 */
	public void disconnectForcibly() throws MqttException
	{
		disconnectForcibly(QUIESCE_TIMEOUT, DISCONNECT_TIMEOUT);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly(long)
	 */
	public void disconnectForcibly(long disconnectTimeout) throws MqttException
	{
		disconnectForcibly(QUIESCE_TIMEOUT, disconnectTimeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#disconnectForcibly(long, long)
	 */
	public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException
	{
		comms.disconnectForcibly(quiesceTimeout, disconnectTimeout);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IMqttAsyncClient#isConnected()
	 */
	public boolean isConnected()
	{
		return comms.isConnected();
	}

	public boolean isConnecting()
	{
		return comms.isConnecting();
	}

	public boolean isDisconnecting()
	{
		return comms.isDisconnecting();
	}

	public boolean isDisconnected()
	{
		return comms.isDisconnected();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IMqttAsyncClient#getClientId()
	 */
	public String getClientId()
	{
		return clientId;
	}

	@Override
	public String getMqttVersion() {
		return mqttVersion;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IMqttAsyncClient#getServerURI()
	 */
	public String getServerURI()
	{
		return serverURI;
	}

	public void setServerURI(String serverURI) throws MqttException
	{
		// check if supplied url and the url we using are different
		// if they are different then we have to open a new persistence
		// so closing old persistence and initializing new persistence
		// we have to provide new persistence to clientcomms and clientstate classes as well
		// so made setPersistence function in them to set the new value of persistence

		if (null != this.serverURI && (!this.serverURI.equals(serverURI)))
		{
			this.serverURI = serverURI;
		}
	}

	/**
	 * Get a topic object which can be used to publish messages.
	 * <p>
	 * There are two alternative methods that should be used in preference to this one when publishing a message:
	 * <ul>
	 * <li>{@link MqttAsyncClient#publish(String, MqttMessage, MqttDeliveryToken)} to publish a message in a non-blocking manner or
	 * <li>{@link MqttClient#publishBlock(String, MqttMessage, MqttDeliveryToken)} to publish a message in a blocking manner
	 * </ul>
	 * </p>
	 * <p>
	 * When you build an application, the design of the topic tree should take into account the following principles of topic name syntax and semantics:
	 * </p>
	 *
	 * <ul>
	 * <li>A topic must be at least one character long.</li>
	 * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and <em>Accounts</em> are two different topics.</li>
	 * <li>Topic names can include the space character. For example, <em>Accounts
	 * 	payable</em> is a valid topic.</li>
	 * <li>A leading "/" creates a distinct topic. For example, <em>/finance</em> is different from <em>finance</em>. <em>/finance</em> matches "+/+" and "/+", but not "+".</li>
	 * <li>Do not include the null character (Unicode<samp class="codeph"> \x0000</samp>) in any topic.</li>
	 * </ul>
	 *
	 * <p>
	 * The following principles apply to the construction and content of a topic tree:
	 * </p>
	 *
	 * <ul>
	 * <li>The length is limited to 64k but within that there are no limits to the number of levels in a topic tree.</li>
	 * <li>There can be any number of root nodes; that is, there can be any number of topic trees.</li>
	 * </ul>
	 * </p>
	 *
	 * @param topic
	 *            the topic to use, for example "finance/stock/ibm".
	 * @return an MqttTopic object, which can be used to publish messages to the topic.
	 * @throws IllegalArgumentException
	 *             if the topic contains a '+' or '#' wildcard character.
	 */
	protected MqttTopic getTopic(String topic)
	{
		MqttTopic.validate(topic, false/* wildcards NOT allowed */);

		MqttTopic result = (MqttTopic) topics.get(topic);
		if (result == null)
		{
			result = new MqttTopic(topic, comms);
			topics.put(topic, result);
		}
		return result;
	}

	/*
	 * (non-Javadoc) Check and send a ping if needed. <p>By default, client sends PingReq to server to keep the connection to server. For some platforms which cannot use this
	 * mechanism, such as Android, developer needs to handle the ping request manually with this method. </p>
	 *
	 * @throws MqttException for other errors encountered while publishing the message.
	 */
	public IMqttToken checkPing(Object userContext, IMqttActionListener callback) throws MqttException
	{
		final String methodName = "ping";
		MqttToken token;
		// @TRACE 117=>
		logger.d(TAG, "checking for ping");

		token = comms.checkForActivity();
		// @TRACE 118=<

		return token;
	}

	public void checkActivity()
	{
		comms.checkActivity();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String, int, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException
	{
		return this.subscribe(new String[] { topicFilter }, new int[] { qos }, userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String, int)
	 */
	public IMqttToken subscribe(String topicFilter, int qos) throws MqttException
	{
		return this.subscribe(new String[] { topicFilter }, new int[] { qos }, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String[], int[])
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException
	{
		return this.subscribe(topicFilters, qos, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#subscribe(java.lang.String[], int[], java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException
	{
		final String methodName = "subscribe";

		if (topicFilters.length != qos.length)
		{
			throw new IllegalArgumentException();
		}

		String subs = "";
		for (int i = 0; i < topicFilters.length; i++)
		{
			if (i > 0)
			{
				subs += ", ";
			}
			subs += topicFilters[i] + ":" + qos[i];

			// Check if the topic filter is valid before subscribing
			MqttTopic.validate(topicFilters[i], true/* allow wildcards */);
		}

		// @TRACE 106=Subscribe topic={0} userContext={1} callback={2}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttSubscribe register = new MqttSubscribe(topicFilters, qos);

		comms.sendNoWait(register, token);
		// @TRACE 109=<

		return token;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String topicFilter, Object userContext, IMqttActionListener callback) throws MqttException
	{
		return unsubscribe(new String[] { topicFilter }, userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String)
	 */
	public IMqttToken unsubscribe(String topicFilter) throws MqttException
	{
		return unsubscribe(new String[] { topicFilter }, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String[])
	 */
	public IMqttToken unsubscribe(String[] topicFilters) throws MqttException
	{
		return unsubscribe(topicFilters, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#unsubscribe(java.lang.String[], java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback) throws MqttException
	{
		final String methodName = "unsubscribe";
		String subs = "";
		for (int i = 0; i < topicFilters.length; i++)
		{
			if (i > 0)
			{
				subs += ", ";
			}
			subs += topicFilters[i];

			// Check if the topic filter is valid before unsubscribing
			// Although we already checked when subscribing, but invalid
			// topic filter is meanless for unsubscribing, just prohibit it
			// to reduce unnecessary control packet send to broker.
			MqttTopic.validate(topicFilters[i], true/* allow wildcards */);
		}

		// @TRACE 107=Unsubscribe topic={0} userContext={1} callback={2}

		MqttToken token = new MqttToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.internalTok.setTopics(topicFilters);

		MqttUnsubscribe unregister = new MqttUnsubscribe(topicFilters);

		comms.sendNoWait(unregister, token);
		// @TRACE 110=<

		return token;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IMqttAsyncClient#setCallback(MqttCallback)
	 */
	public void setCallback(MqttCallback callback)
	{
		comms.setCallback(callback);
	}

	/**
	 * Returns a randomly generated client identifier based on the the fixed prefix (paho-) and the system time.
	 * <p>
	 * When cleanSession is set to false, an application must ensure it uses the same client identifier when it reconnects to the server to resume state and maintain assured
	 * message delivery.
	 * </p>
	 *
	 * @return a generated client identifier
	 * @see MqttConnectOptions#setCleanSession(boolean)
	 */
	public static String generateClientId()
	{
		// length of nanoTime = 15, so total length = 20 < 65535(defined in spec)
		return CLIENT_ID_PREFIX + System.nanoTime();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see IMqttAsyncClient#getPendingDeliveryTokens()
	 */
	public IMqttDeliveryToken[] getPendingDeliveryTokens()
	{
		return comms.getPendingDeliveryTokens();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean, java.lang.Object, org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext, IMqttActionListener callback) throws MqttException,
			MqttPersistenceException
	{
		MqttMessage message = new MqttMessage(payload);
		message.setQos(qos);
		message.setRetained(retained);
		return this.publish(topic, message, userContext, callback);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, byte[], int, boolean)
	 */
	public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained) throws MqttException, MqttPersistenceException
	{
		return this.publish(topic, payload, qos, retained, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException
	{
		return this.publish(topic, message, null, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#publish(java.lang.String, org.eclipse.paho.client.mqttv3.MqttMessage, java.lang.Object,
	 * org.eclipse.paho.client.mqttv3.IMqttActionListener)
	 */
	public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException, MqttPersistenceException
	{
		final String methodName = "publish";
		// @TRACE 111=< topic={0} message={1}userContext={1} callback={2}

		// Checks if a topic is valid when publishing a message.
		MqttTopic.validate(topic, false/* wildcards NOT allowed */);

		MqttDeliveryToken token = new MqttDeliveryToken(getClientId());
		token.setActionCallback(callback);
		token.setUserContext(userContext);
		token.setMessage(message);
		token.internalTok.setTopics(new String[] { topic });

		MqttPublish pubMsg = new MqttPublish(topic, message);
		comms.sendNoWait(pubMsg, token);

		// @TRACE 112=<

		return token;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.paho.client.mqttv3.IMqttAsyncClient#close()
	 */
	public void close() throws MqttException
	{
		final String methodName = "close";
		// @TRACE 113=<
		logger.d(TAG, "close started");
		comms.close();
		logger.d(TAG, "close completed");
		// @TRACE 114=>

	}

	public int getInflightMessages()
	{
		return comms.getClientState().getInflightMsgs();
	}

	public int getMaxflightMessages()
	{
		return comms.getClientState().getMaxInflightMsgs();
	}

	public void pingReq(IMqttActionListener listener)  throws MqttException {

		if(comms != null && comms.isConnected() && getClientId() != null) {
			MqttToken token = new MqttToken(getClientId());
			token.setActionCallback(listener);
			MqttPingReq pingMsg = new MqttPingReq();
			comms.sendNoWait(pingMsg, token);
		}
	}

	public long getFastReconnectCheckStartTime() {
		return comms.getClientState().getFastReconnectCheckStartTime();
	}

	public long getLastOutboundActivity() {
		synchronized (this) {
			if (comms != null && comms.getClientState() != null) {
				return comms.getClientState().getLastOutboundActivity();
			}
			return 0l;
		}
	}

	public long getLastInboundActivity() {
		synchronized (this) {
			try {
				ClientState clientState = comms != null ? comms.getClientState() : null;
				return clientState != null ? clientState.getLastInboundActivity() : 0l;
			} catch (NullPointerException e) {
				// can happen in multithreaded cases. client state can become null.
			}
			return 0l;
		}
	}

	/**
	 * Sets the DisconnectedBufferOptions for this client
	 *
	 * @param bufferOpts
	 *            the {@link DisconnectedBufferOptions}
	 */
	public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
		this.comms.setDisconnectedMessageBuffer(new DisconnectedMessageBuffer(bufferOpts));
	}

	/**
	 * Returns the number of messages in the Disconnected Message Buffer
	 *
	 * @return Count of messages in the buffer
	 */
	public int getBufferedMessageCount() {
		return this.comms.getBufferedMessageCount();
	}

	/**
	 * Returns a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be retrieved.
	 * @return the message located at the bufferIndex
	 */
	public MqttMessage getBufferedMessage(int bufferIndex) {
		return this.comms.getBufferedMessage(bufferIndex);
	}

	/**
	 * Deletes a message from the Disconnected Message Buffer
	 *
	 * @param bufferIndex
	 *            the index of the message to be deleted.
	 */
	public void deleteBufferedMessage(int bufferIndex) {
		this.comms.deleteBufferedMessage(bufferIndex);
	}

}
