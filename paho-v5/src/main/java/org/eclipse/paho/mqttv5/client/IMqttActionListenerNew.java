package org.eclipse.paho.mqttv5.client;

/**
 * Courier extension of {@link MqttActionListener} that adds a callback fired once
 * the associated message has been written on the socket. This mirrors the
 * MQTT v3 {@code IMqttActionListenerNew} extension.
 */
public interface IMqttActionListenerNew extends MqttActionListener {
	void notifyWrittenOnSocket(IMqttToken asyncActionToken);
}
