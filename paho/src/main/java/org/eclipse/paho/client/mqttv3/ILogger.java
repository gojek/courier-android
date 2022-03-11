package org.eclipse.paho.client.mqttv3;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

public interface ILogger {

    void v(String tag, String msg);

    void v(String tag, String msg, Throwable tr);

    void d(String tag, final String msg);

    void d(String tag, final String msg, final String sendLogMsg);

    void d(String tag, String msg, Throwable tr);

    void i(String tag, String msg);

    void i(String tag, String msg, String sendLogMsg);

    void i(String tag, String msg, Throwable tr);

    void w(String tag, String msg);

    void w(String tag, String msg, Throwable tr);

    void w(String tag, Throwable tr);

    void e(String tag, String msg);

    void e(String tag, String msg, Throwable tr);

    void wtf(String tag, String msg, Throwable tr);

    void wtf(String tag, String msg);

    void logEvent(String type, boolean isSuccessful, String endPoint, long timeTaken, Throwable throwable, int errorCode, long timestamp, long packetSize, String threadId, int uniqueMsgId);

    void logInitEvent(String eventType, long ts, String endPoint);

    void logMqttThreadEvent(String eventType, long timeTaken, String threadId);

    void logFastReconnectEvent(long fastReconnectCheckStartTime, long lastInboundActivity);

    void setAppKillTime(long time);

    void logMessageSentData(MqttWireMessage message);

    void logMessageReceivedData(MqttWireMessage message);

}
