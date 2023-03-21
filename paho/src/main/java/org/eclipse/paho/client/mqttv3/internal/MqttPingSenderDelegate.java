package org.eclipse.paho.client.mqttv3.internal;

import org.eclipse.paho.client.mqtt.ILogger;
import org.eclipse.paho.client.mqtt.MqttPingSender;

class MqttPingSenderDelegate {

    private ClientComms clientComms;

    private MqttPingSender mqttPingSender;

    public MqttPingSenderDelegate(ClientComms clientComms, MqttPingSender pingSender) {
        this.clientComms = clientComms;
        this.mqttPingSender = pingSender;
    }

    public void init(ILogger logger) {
        this.mqttPingSender.init(logger);
    }

    class PingInfoProvider {
        public long getKeepAliveSeconds() {
            return clientComms.getKeepAlive();
        }

        public String getClient() {
            return clientComms.getClient().getServerURI();
        }
    }
}
