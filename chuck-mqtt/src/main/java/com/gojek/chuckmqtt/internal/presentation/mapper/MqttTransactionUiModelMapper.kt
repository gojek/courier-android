package com.gojek.chuckmqtt.internal.presentation.mapper

import com.gojek.chuckmqtt.internal.base.Mapper
import com.gojek.chuckmqtt.internal.domain.model.MqttTransactionDomainModel
import com.gojek.chuckmqtt.internal.presentation.model.MqttTransactionUiModel
import com.gojek.chuckmqtt.internal.utils.formatBody
import com.gojek.chuckmqtt.internal.utils.formatByteCount
import java.text.DateFormat
import kotlin.text.Charsets.UTF_8
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnack
import org.eclipse.paho.client.mqttv3.internal.wire.MqttConnect
import org.eclipse.paho.client.mqttv3.internal.wire.MqttDisconnect
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingReq
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingResp
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubComp
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRec
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubRel
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSuback
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubAck
import org.eclipse.paho.client.mqttv3.internal.wire.MqttUnsubscribe
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage

internal class MqttTransactionUiModelMapper : Mapper<MqttTransactionDomainModel, MqttTransactionUiModel> {
    override fun map(input: MqttTransactionDomainModel): MqttTransactionUiModel {
        return with(input) {
            MqttTransactionUiModel(
                id = id,
                packetName = getPacketName(mqttWireMessage),
                packetPreview = getPacketContents(mqttWireMessage),
                isSent = isSent,
                transmissionTime = getTransmissionTime(transmissionTime),
                bytesString = getBytesString(sizeInBytes),
                packetInfo = getPacketDetail(mqttWireMessage),
                packetBody = getPacketBody(mqttWireMessage),
                shareText = getShareText(mqttWireMessage)
            )
        }
    }

    private fun getPacketName(mqttWireMessage: MqttWireMessage): String {
        return mqttWireMessage.packetName()
    }

    private fun getPacketContents(mqttWireMessage: MqttWireMessage): String {
        return try {
            mqttWireMessage.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun getTransmissionTime(transmissionTime: Long): String {
        return DateFormat.getTimeInstance().format(transmissionTime)
    }

    private fun getBytesString(sizeInBytes: Long): String {
        return formatByteCount(sizeInBytes, true)
    }

    private fun getPacketDetail(mqttWireMessage: MqttWireMessage): String {
        val sb = StringBuilder("")
        when (mqttWireMessage) {
            is MqttConnect -> {
                sb.append("<b> ClientId : </b> ${mqttWireMessage.clientId} <br />")
                sb.append("<b> CleanSession : </b> ${mqttWireMessage.isCleanSession} <br />")
                sb.append("<b> KeepAliveInterval : </b> ${mqttWireMessage.keepAliveInterval} <br />")
                sb.append("<b> UserName : </b> ${mqttWireMessage.userName} <br />")
                sb.append("<b> Password : </b> ${String(mqttWireMessage.password)} <br />")
                mqttWireMessage.willMessage?.let {
                    sb.append("<b> WillMessage : </b> ${getWillMessageDetail(it)} <br />")
                }
                mqttWireMessage.willDestination?.let {
                    sb.append("<b> WillDestination : </b> ${mqttWireMessage.willDestination} <br />")
                }
            }
            is MqttConnack -> {
                sb.append("<b> Reason Code : </b> ${mqttWireMessage.returnCode} <br />")
            }
            is MqttPublish -> {
                sb.append("<b> Qos : </b> ${mqttWireMessage.message.qos} <br />")
                if(mqttWireMessage.message.qos > 0) {
                    sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
                }
                sb.append("<b> Retained : </b> ${mqttWireMessage.message.isRetained} <br />")
                sb.append("<b> Duplicate : </b> ${mqttWireMessage.message.isDuplicate} <br />")
            }
            is MqttPubAck -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
            }
            is MqttPubRec -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
            }
            is MqttPubRel -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
            }
            is MqttPubComp -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
            }
            is MqttSubscribe -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
                sb.append("<b> Topics : </b>")
                for(i in 0 until mqttWireMessage.count) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.names[i] + "\"")
                }
                sb.append("<br />")
                sb.append("<b> Qos : </b>")
                for(i in 0 until mqttWireMessage.count) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.qos[i] + "\"")
                }
                sb.append("<br />")
            }
            is MqttSuback -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
                sb.append("<b> Granted Qos : </b>")
                for(i in mqttWireMessage.grantedQos.indices) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.grantedQos[i] + "\"")
                }
                sb.append("<br />")
            }
            is MqttUnsubscribe -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
                sb.append("<b> Topics : </b>")
                for(i in 0 until mqttWireMessage.count) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.names[i] + "\"")
                }
                sb.append("<br />")
            }
            is MqttUnsubAck -> {
                sb.append("<b> MsgId : </b> ${mqttWireMessage.messageId} <br />")
            }
            is MqttPingReq -> {
                sb.append("")
            }
            is MqttPingResp -> {
                sb.append("")
            }
            is MqttDisconnect -> {
                sb.append("")
            }
        }
        return sb.toString()
    }

    private fun getPacketBody(mqttWireMessage: MqttWireMessage): String {
        return when(mqttWireMessage) {
            is MqttPublish -> {
                formatBody(String(mqttWireMessage.message.payload, UTF_8))
            } else -> ""
        }
    }

    private fun getWillMessageDetail(mqttMessage: MqttMessage): String {
        val sb = StringBuilder()
        sb.append("<b> Qos : </b> ${mqttMessage.qos} <br />")
        sb.append("<b> Retained : </b> ${mqttMessage.isRetained} <br />")
        sb.append("<b> payload : </b><br /> ${String(mqttMessage.payload,  UTF_8)} <br />")
        return sb.toString()
    }

    private fun getShareText(mqttWireMessage: MqttWireMessage): String {
        val sb = StringBuilder("")
        when (mqttWireMessage) {
            is MqttConnect -> {
                sb.append("CONNECT \n")
                sb.append("ClientId : ${mqttWireMessage.clientId} \n")
                sb.append("CleanSession : ${mqttWireMessage.isCleanSession} \n")
                sb.append("KeepAliveInterval : ${mqttWireMessage.keepAliveInterval} \n")
                sb.append("UserName : ${mqttWireMessage.userName} \n")
                sb.append("Password : ${String(mqttWireMessage.password)} \n")
                mqttWireMessage.willMessage?.let {
                    sb.append("WillMessage : \n")
                    sb.append("Qos : ${it.qos} \n")
                    sb.append("Retained : ${it.isRetained} \n")
                    sb.append("Payload : \n")
                    sb.append(formatBody(String(it.payload, UTF_8)))
                    sb.append("\n")
                }
                mqttWireMessage.willDestination?.let {
                    sb.append("WillDestination : ${mqttWireMessage.willDestination} \n")
                }
            }
            is MqttConnack -> {
                sb.append("CONNACK \n")
                sb.append("Reason Code : ${mqttWireMessage.returnCode} \n")
            }
            is MqttPublish -> {
                sb.append("PUBLISH \n")
                sb.append("Qos : ${mqttWireMessage.message.qos} \n")
                if(mqttWireMessage.message.qos > 0) {
                    sb.append("MsgId : ${mqttWireMessage.messageId} \n")
                }
                sb.append("Retained : ${mqttWireMessage.message.isRetained} \n")
                sb.append("Duplicate : ${mqttWireMessage.message.isDuplicate} \n")
                sb.append("Payload : \n")
                sb.append(formatBody(String(mqttWireMessage.message.payload, UTF_8)))
                sb.append("\n")
            }
            is MqttPubAck -> {
                sb.append("PUBACK \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
            }
            is MqttPubRec -> {
                sb.append("PUBREC \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
            }
            is MqttPubRel -> {
                sb.append("PUBREL \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
            }
            is MqttPubComp -> {
                sb.append("PUBCOMP \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
            }
            is MqttSubscribe -> {
                sb.append("SUBSCRIBE \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} ")
                sb.append("Subscribe Topics : ")
                for(i in 0 until mqttWireMessage.count) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.names[i] + "\"")
                }
                sb.append("\n")
                sb.append("Qos : ")
                for(i in 0 until mqttWireMessage.count) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.qos[i] + "\"")
                }
                sb.append("\n")
            }
            is MqttSuback -> {
                sb.append("SUBACK \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
                sb.append("Granted Qos : ")
                for(i in mqttWireMessage.grantedQos.indices) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.grantedQos[i] + "\"")
                }
                sb.append("\n")
            }
            is MqttUnsubscribe -> {
                sb.append("UNSUBSCRIBE \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
                sb.append("Unsubscribe Topics : ")
                for(i in mqttWireMessage.names.indices) {
                    if (i > 0) {
                        sb.append(", ")
                    }
                    sb.append("\"" + mqttWireMessage.names[i] + "\"")
                }
                sb.append("\n")
            }
            is MqttUnsubAck -> {
                sb.append("UNSUBACK \n")
                sb.append("MsgId : ${mqttWireMessage.messageId} \n")
            }
            is MqttPingReq -> {
                sb.append("PINGREQ")
            }
            is MqttPingResp -> {
                sb.append("PINGRESP")
            }
            is MqttDisconnect -> {
                sb.append("DISCONNECT")
            }
        }
        return sb.toString()
    }
}