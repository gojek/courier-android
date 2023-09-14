package com.gojek.courier.app.data.network

import com.gojek.courier.QoS
import com.gojek.courier.annotation.Callback
import com.gojek.courier.annotation.Data
import com.gojek.courier.annotation.Path
import com.gojek.courier.annotation.Send
import com.gojek.courier.annotation.Subscribe
import com.gojek.courier.annotation.SubscribeMultiple
import com.gojek.courier.annotation.TopicMap
import com.gojek.courier.annotation.Unsubscribe
import com.gojek.courier.app.data.network.model.Message
import com.gojek.courier.callback.SendMessageCallback
import io.reactivex.Observable

interface CourierService {
    @Send(topic = "{topic}", qos = QoS.ONE)
    fun publish(@Path("topic") topic: String, @Data message: Message, @Callback callback: SendMessageCallback)

    @Subscribe(topic = "{topic}")
    fun subscribe(@Path("topic") topic: String): Observable<String>

    @Unsubscribe(topics = ["{topic}"])
    fun unsubscribe(@Path("topic") topic: String)

    @SubscribeMultiple
    fun subscribeAll(@TopicMap topicMap: Map<String, QoS>): Observable<String>
}