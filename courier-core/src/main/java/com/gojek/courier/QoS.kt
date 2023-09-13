package com.gojek.courier

enum class QoS(val value: Int, val type: Int) {
    // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718100
    ZERO(0, 0),

    // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718100
    ONE(1, 1),

    // http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718100
    TWO(2, 2),

    /** Like QoS1, Message delivery is acknowledged with PUBACK, but unlike QoS1 messages are
     neither persisted nor retried at send after one attempt.
     The message arrives at the receiver either once or not at all **/
    ONE_WITHOUT_PERSISTENCE_AND_NO_RETRY(0, 3),

    /** Like QoS1, Message delivery is acknowledged with PUBACK, but unlike QoS1 messages are
     not persisted. The messages are retried within active connection if delivery is not acknowledged.**/
    ONE_WITHOUT_PERSISTENCE_AND_RETRY(0, 4)
}
