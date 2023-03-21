package org.eclipse.paho.client.mqtt.internal;

public interface IClientComms {
    public long getKeepAlive();

    public String getServerUri();

    public String getClientId();

    public void checkActivityWithCallback(PingActivityCallBack callBack);

    public void sendPingRequestWithCallback(PingActivityCallBack callBack);
}