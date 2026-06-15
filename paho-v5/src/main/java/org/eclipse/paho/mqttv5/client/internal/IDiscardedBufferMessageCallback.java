package org.eclipse.paho.mqttv5.client.internal;


import org.eclipse.paho.mqttv5.common.packet.MqttWireMessage;

public interface IDiscardedBufferMessageCallback {
    void messageDiscarded(MqttWireMessage message);
}