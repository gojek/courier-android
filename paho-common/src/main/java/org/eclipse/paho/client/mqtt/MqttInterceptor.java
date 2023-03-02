package org.eclipse.paho.client.mqtt;

public interface MqttInterceptor {
    void onMqttWireMessageSent(byte[] mqttWireMessageBytes);

    void onMqttWireMessageReceived(byte[] mqttWireMessageBytes);
}
