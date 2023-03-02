package org.eclipse.paho.client.mqttv10;

public interface IMqttActionListenerNew extends IMqttActionListener
{
	public void notifyWrittenOnSocket(IMqttToken asyncActionToken);
}
