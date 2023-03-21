package org.eclipse.paho.client.mqtt.internal;

public interface PingActivityCallBack {
    void onPingMqttTokenNull();

    void onSuccess();

    void onFailure(Throwable throwable);
}
