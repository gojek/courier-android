package org.eclipse.paho.client.mqttv10;

import org.eclipse.paho.client.mqttv10.internal.wire.MqttWireMessage;

public class NoOpLogger implements ILogger {
    @Override
    public void v(String tag, String msg) {

    }

    @Override
    public void v(String tag, String msg, Throwable tr) {

    }

    @Override
    public void d(String tag, String msg) {

    }

    @Override
    public void d(String tag, String msg, String sendLogMsg) {

    }

    @Override
    public void d(String tag, String msg, Throwable tr) {

    }

    @Override
    public void i(String tag, String msg) {

    }

    @Override
    public void i(String tag, String msg, String sendLogMsg) {

    }

    @Override
    public void i(String tag, String msg, Throwable tr) {

    }

    @Override
    public void w(String tag, String msg) {

    }

    @Override
    public void w(String tag, String msg, Throwable tr) {

    }

    @Override
    public void w(String tag, Throwable tr) {

    }

    @Override
    public void e(String tag, String msg) {

    }

    @Override
    public void e(String tag, String msg, Throwable tr) {

    }

    @Override
    public void wtf(String tag, String msg, Throwable tr) {

    }

    @Override
    public void wtf(String tag, String msg) {

    }

    @Override
    public void logEvent(String type, boolean isSuccessful, String endPoint, long timeTaken, Throwable throwable, int errorCode, long timestamp, long packetSize, String threadId, int uniqueMsgId) {

    }

    @Override
    public void logInitEvent(String eventType, long ts, String endPoint) {

    }

    @Override
    public void logMqttThreadEvent(String eventType, long timeTaken, String threadId) {

    }

    @Override
    public void logFastReconnectEvent(long fastReconnectCheckStartTime, long lastInboundActivity) {

    }

    @Override
    public void setAppKillTime(long time) {

    }

    @Override
    public void logMessageSentData(MqttWireMessage message) {

    }

    @Override
    public void logMessageReceivedData(MqttWireMessage message) {

    }
}
