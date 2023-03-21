package org.eclipse.paho.client.mqttv10.internal;

import org.eclipse.paho.client.mqtt.ILogger;
import org.eclipse.paho.client.mqtt.MqttInterceptor;

import java.util.List;
import java.util.Vector;

public class MqttInterceptorCallback implements Runnable {

    private final String TAG = "MqttInterceptorCallback";

    private static final int INBOUND_QUEUE_SIZE = 200;

    private final List<MqttInterceptor> mqttInterceptorList;

    private final Vector<MqttInterceptorMessage> messageQueue;

    private final Object lifecycle = new Object();

    private final Object workAvailable = new Object();

    private final Object spaceAvailable = new Object();

    private final ILogger logger;

    public boolean running = false;

    private Thread interceptorThread;

    MqttInterceptorCallback(List<MqttInterceptor> mqttInterceptorList, ILogger logger) {
        this.mqttInterceptorList = mqttInterceptorList;
        this.logger = logger;
        this.messageQueue = new Vector<>(INBOUND_QUEUE_SIZE);
    }

    public void start(String threadName) {
        synchronized (lifecycle) {
            if (!running) {
                messageQueue.clear();
                running = true;
                interceptorThread = new Thread(this, threadName);
                interceptorThread.start();
            }
        }
    }

    public void stop() {
        synchronized (lifecycle) {
            if (running) {
                running = false;
                if (interceptorThread != null && !Thread.currentThread().equals(interceptorThread)) {
                    try {
                        synchronized (workAvailable) {
                            // @TRACE 701=notify workAvailable and wait for run
                            // to finish
                            workAvailable.notifyAll();
                        }
                        // Wait for the thread to finish.
                        interceptorThread.join();
                    } catch (InterruptedException ex) {
                        // no ops
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                // If no work is currently available, then wait until there is some...
                try {
                    synchronized (workAvailable) {
                        if (running & messageQueue.isEmpty()) {
                            // @TRACE 704=wait for workAvailable
                            logger.d(TAG, "Callback Thread Waiting on workAvailable");
                            workAvailable.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    // no ops
                }

                MqttInterceptorMessage message = null;
                if (running) {
                    synchronized (messageQueue) {
                        if (!messageQueue.isEmpty()) {
                            message = (MqttInterceptorMessage) messageQueue.elementAt(0);
                            messageQueue.removeElementAt(0);
                        }
                    }
                    if (null != message) {
                        handleMessage(message);
                    }
                }

                synchronized (spaceAvailable) {
                    // Notify the spaceAvailable lock, to say that there's now
                    // some space on the queue...

                    // @TRACE 706=notify spaceAvailable
                    spaceAvailable.notifyAll();
                }
            } catch (Throwable ex) {
                logger.e(TAG, "exception occurred, shutting mqtt interceptor callback : ", ex);
                running = false;
            }
        }
    }


    public void mqttMessageIntercepted(byte[] mqttWireMessageBytes, boolean isSent) {
        MqttInterceptorMessage mqttInterceptorMessage = new MqttInterceptorMessage(mqttWireMessageBytes, isSent);
        synchronized (spaceAvailable) {
            while (messageQueue.size() >= INBOUND_QUEUE_SIZE) {
                try {
                    logger.d(TAG, "Waiting on call back Thread on space available");
                    spaceAvailable.wait();
                } catch (InterruptedException ex) {
                    // no ops
                }
            }
        }
        messageQueue.addElement(mqttInterceptorMessage);
        synchronized (workAvailable) {
            // @TRACE 710=new msg avail, notify workAvailable

            workAvailable.notifyAll();
        }
    }

    private void handleMessage(MqttInterceptorMessage message) {
        if (mqttInterceptorList != null) {
            for (MqttInterceptor mqttInterceptor : mqttInterceptorList) {
                try {
                    if (message.isSent) {
                        mqttInterceptor.onMqttWireMessageSent(message.mqttWireMessageBytes);
                    } else {
                        mqttInterceptor.onMqttWireMessageReceived(message.mqttWireMessageBytes);
                    }
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private static class MqttInterceptorMessage {
        private final byte[] mqttWireMessageBytes;
        private final boolean isSent;

        MqttInterceptorMessage(byte[] mqttWireMessageBytes, boolean isSent) {
            this.mqttWireMessageBytes = mqttWireMessageBytes;
            this.isSent = isSent;
        }

        public byte[] getMqttWireMessageBytes() {
            return mqttWireMessageBytes;
        }

        public boolean isSent() {
            return isSent;
        }
    }
}
