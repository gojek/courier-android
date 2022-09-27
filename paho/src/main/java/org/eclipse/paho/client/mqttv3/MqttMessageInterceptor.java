package org.eclipse.paho.client.mqttv3;

public interface MqttMessageInterceptor {
    byte[] intercept(byte[] mqttWireMessageBytes, boolean isSent);
}
