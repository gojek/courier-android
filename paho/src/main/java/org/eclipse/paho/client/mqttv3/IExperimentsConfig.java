package org.eclipse.paho.client.mqttv3;

public interface IExperimentsConfig {
    int inactivityTimeoutSecs();

    int connectPacketTimeoutSecs();

    Boolean useNewSSLFlow();
}
