package org.eclipse.paho.client.mqtt;

public interface IPahoEvents {

    void onSocketConnectAttempt(int port, String host, long timeout);

    void onSocketConnectSuccess(long timeToConnect, int port, String host, long timeout);

    void onSocketConnectFailure(long timeToConnect, int port, String host, long timeout, Throwable throwable);

    void onConnectPacketSend();

    void onSSLSocketAttempt(int port, String host, long timeout);

    void onSSLSocketSuccess(int port, String host, long timeout, long timeTakenMillis);

    void onSSLSocketFailure(int port, String host, long timeout, Throwable throwable, long timeTakenMillis);

    void onSSLHandshakeSuccess(int port, String host, long timeout, long timeTakenMillis);

    void onOfflineMessageDiscarded(int messageId);

    void onInboundInactivity();
}
