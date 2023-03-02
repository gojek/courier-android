package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqtt.MqttException;
import org.eclipse.paho.client.mqttv3.internal.ClientState;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;

public interface ICommsCallback {
    void setClientState(ClientState clientState);

    void stop();

    void asyncOperationComplete(MqttToken endToken);

    void connectionLost(MqttException reason);

    Thread getThread();

    void setCallback(MqttCallback mqttCallback);

    void start(String s);

    void fastReconnect();

    boolean isQuiesced();

    void messageArrived(MqttPublish send);

    void quiesce();
}
