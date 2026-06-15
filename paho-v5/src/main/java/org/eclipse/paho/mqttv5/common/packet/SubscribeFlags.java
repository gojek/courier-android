package org.eclipse.paho.mqttv5.common.packet;

/**
 * Courier customization carried on a SUBSCRIBE packet. Mirrors the MQTT v3
 * {@code SubscribeFlags} and is used to signal to the broker whether downstream
 * messages for a subscription should be persisted and/or retried.
 */
public class SubscribeFlags {

    private final boolean isPersistable;

    private final boolean isRetryable;

    public SubscribeFlags(boolean isPersistable, boolean isRetryable) {
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
