package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqtt.IPahoEvents;

public class NoOpsPahoEvents implements IPahoEvents {

    @Override
    public void onSocketConnectAttempt(int port, String host, long timeout) {

    }

    @Override
    public void onSocketConnectSuccess(long timeToConnect, int port, String host, long timeout) {

    }

    @Override
    public void onSocketConnectFailure(long timeToConnect, int port, String host, long timeout, Throwable throwable) {

    }

    @Override
    public void onConnectPacketSend() {

    }

    @Override
    public void onSSLSocketAttempt(int port, String host, long timeout) {

    }

    @Override
    public void onSSLSocketSuccess(int port, String host, long timeout, long timeTakenMillis) {

    }

    @Override
    public void onSSLSocketFailure(int port, String host, long timeout, Throwable throwable, long timeTakenMillis) {

    }

    @Override
    public void onSSLHandshakeSuccess(int port, String host, long timeout, long timeTakenMillis) {

    }

    @Override
    public void onOfflineMessageDiscarded(int messageId) {

    }

    @Override
    public void onInboundInactivity() {

    }
}
