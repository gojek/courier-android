package org.eclipse.paho.client.mqttv3.internal.wire;

public class SubscribeFlags {

    private final boolean isPersistable;

    private final boolean isRetryable;

    public SubscribeFlags(boolean isPersistable, boolean isRetryable){
        this.isPersistable = isPersistable;
        this.isRetryable = isRetryable;
    }

    public boolean isPersistableFlagEnabled() {
        return isPersistable;
    }

    public boolean isRetryableFlagEnabled() {
        return isRetryable;
    }
}
