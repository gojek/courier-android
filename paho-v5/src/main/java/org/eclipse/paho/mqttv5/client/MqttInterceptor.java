package org.eclipse.paho.mqttv5.client;

public interface MqttInterceptor {
    void onMqttWireMessageSent(byte[] mqttWireMessageBytes);

    void onMqttWireMessageReceived(byte[] mqttWireMessageBytes);
}
