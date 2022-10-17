package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqttv3.MqttMessageInterceptor;

import java.util.List;

public class MqttMessageInterceptorCallback {
    private final List<MqttMessageInterceptor> mqttInterceptorList;

    MqttMessageInterceptorCallback(List<MqttMessageInterceptor> mqttInterceptorList) {
        this.mqttInterceptorList = mqttInterceptorList;
    }

    public byte[] mqttMessageIntercepted(String topic, byte[] mqttWireMessageBytes, boolean isSent) {
        byte[] messageBytes = mqttWireMessageBytes;
        if (mqttInterceptorList != null) {
            for (MqttMessageInterceptor interceptor : mqttInterceptorList) {
                messageBytes = interceptor.intercept(topic, messageBytes, isSent);
            }
        }
        return messageBytes;
    }
}
