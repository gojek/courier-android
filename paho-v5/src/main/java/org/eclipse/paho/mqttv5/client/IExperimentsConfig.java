package org.eclipse.paho.mqttv5.client;

public interface IExperimentsConfig {
    int inactivityTimeoutSecs();

    int connectPacketTimeoutSecs();

    Boolean useNewSSLFlow();
}
