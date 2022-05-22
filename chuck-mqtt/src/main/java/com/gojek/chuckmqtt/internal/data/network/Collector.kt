package com.gojek.chuckmqtt.internal.data.network

import com.gojek.chuckmqtt.internal.data.network.model.CollectorModel
import io.reactivex.subjects.ReplaySubject

internal class Collector {

    private val subject: ReplaySubject<CollectorModel> = ReplaySubject.create<CollectorModel>()

    internal fun onMessageReceived(mqttWireMessageBytes: ByteArray) {
        subject.onNext(
            CollectorModel(
                isSent = false,
                messageBytes = mqttWireMessageBytes
            )
        )
    }

    internal fun onMessageSent(mqttWireMessageBytes: ByteArray) {
        subject.onNext(
            CollectorModel(
                isSent = true,
                messageBytes = mqttWireMessageBytes
            )
        )
    }

    internal fun observe() = subject.hide()
}
