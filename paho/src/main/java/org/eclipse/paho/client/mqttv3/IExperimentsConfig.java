package org.eclipse.paho.client.mqttv3;

public interface IExperimentsConfig {
    int getPingExperimentVariant();

    int inactivityTimeoutSecs();

    boolean shouldUseNewCommsCallback();
}
