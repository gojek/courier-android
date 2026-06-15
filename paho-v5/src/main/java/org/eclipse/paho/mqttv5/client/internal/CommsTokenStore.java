/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corp.
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
 */
package org.eclipse.paho.mqttv5.client.internal;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.paho.mqttv5.client.MqttToken;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.packet.MqttPublish;
import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;


/**
 * Provides a "token" based system for storing and tracking actions across 
 * multiple threads. 
 * When a message is sent, a token is associated with the message
 * and saved using the {@link CommsTokenStore#saveToken(MqttToken, MqttWireMessage)} method. Anyone interested
 * in tacking the state can call one of the wait methods on the token or using 
 * the asynchronous listener callback method on the operation. 
 * The {@link CommsReceiver} class, on another thread, reads responses back from 
 * the network. It uses the response to find the relevant token, which it can then 
 * notify. 
 * 
 * Note:
 *   Ping, connect and disconnect do not have a unique message id as
 *   only one outstanding request of each type is allowed to be outstanding
 */
public class CommsTokenStore {
	private static final String CLASS_NAME = CommsTokenStore.class.getName();

	// Maps message-specific data (usually message IDs) to tokens
	private final Hashtable<String, MqttToken> tokens;
	private String logContext;
	private MqttException closedResponse = null;

	public CommsTokenStore(String logContext) {
		this.tokens = new Hashtable<String, MqttToken>();
		this.logContext = logContext;
	}

	/**
	 * Based on the message type that has just been received return the associated
	 * token from the token store or null if one does not exist.
	 * @param message whose token is to be returned 
	 * @return token for the requested message
	 */
	public MqttToken getToken(MqttWireMessage message) {
		String key = message.getKey(); 
		return (MqttToken)tokens.get(key);
	}

	public MqttToken getToken(String key) {
		return (MqttToken)tokens.get(key);
	}

	
	public MqttToken removeToken(MqttWireMessage message) {
		if (message != null) {
			return removeToken(message.getKey());
		}
		return null;
	}
	
	public MqttToken removeToken(String key) {
		if ( null != key ){
		    return (MqttToken) tokens.remove(key);
		}
		
		return null;
	}
		
	/**
	 * Restores a token after a client restart.  This method could be called
	 * for a SEND of CONFIRM, but either way, the original SEND is what's 
	 * needed to re-build the token.
	 * @param message The {@link MqttPublish} message to restore
	 * @return {@link MqttToken}
	 */
	protected MqttToken restoreToken(MqttPublish message) {
		MqttToken token;
		synchronized(tokens) {
			String key = Integer.valueOf(message.getMessageId()).toString();
			if (this.tokens.containsKey(key)) {
				token = this.tokens.get(key);
			} else {
				token = new MqttToken(logContext);
                                token.internalTok.setDeliveryToken(true);
				token.internalTok.setKey(key);
				this.tokens.put(key, token);
			}
		}
		return token;
	}
	
	// For outbound messages store the token in the token store 
	// For pubrel use the existing publish token 
	protected void saveToken(MqttToken token, MqttWireMessage message) throws MqttException {
		synchronized(tokens) {
			if (closedResponse == null) {
				String key = message.getKey();
				saveToken(token,key);
			} else {
				throw closedResponse;
			}
		}
	}
	
	protected void saveToken(MqttToken token, String key) {
		synchronized(tokens) {
			token.internalTok.setKey(key);
			this.tokens.put(key, token);
		}
	}

	protected void quiesce(MqttException quiesceResponse) {
		synchronized(tokens) {
			closedResponse = quiesceResponse;
		}
	}
	
	public void open() {
		synchronized(tokens) {
			closedResponse = null;
		}
	}

	public MqttToken[] getOutstandingDelTokens() {
		synchronized(tokens) {
			Vector<MqttToken> list = new Vector<MqttToken>();
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null 
					&& token.internalTok.isDeliveryToken() == true
					&& !token.internalTok.isNotified()) {
					
					list.addElement(token);
				}
			}
	
			MqttToken[] result = new MqttToken[list.size()];
			return (MqttToken[]) list.toArray(result);
		}
	}
	
	public Vector<MqttToken> getOutstandingTokens() {
		synchronized(tokens) {
			Vector<MqttToken> list = new Vector<MqttToken>();
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
				if (token != null) {
						list.addElement(token);
				}
			}
			return list;
		}
	}

	/**
	 * Empties the token store without notifying any of the tokens.
	 */
	public void clear() {
		synchronized(tokens) {
			tokens.clear();
		}
	}
	
	public int count() {
		synchronized(tokens) {
			return tokens.size();
		}
	}
	public String toString() {
		String lineSep = System.getProperty("line.separator","\n");
		StringBuffer toks = new StringBuffer();
		synchronized(tokens) {
			Enumeration<MqttToken> enumeration = tokens.elements();
			MqttToken token;
			while(enumeration.hasMoreElements()) {
				token = (MqttToken)enumeration.nextElement();
					toks.append("{"+token.internalTok+"}"+lineSep);
			}
			return toks.toString();
		}
	}
}
