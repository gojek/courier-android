package org.eclipse.paho.client.mqttv3;

public interface MqttMessageInterceptor {
    byte[] intercept(String topic, byte[] mqttWireMessageBytes, boolean isSent);
}
