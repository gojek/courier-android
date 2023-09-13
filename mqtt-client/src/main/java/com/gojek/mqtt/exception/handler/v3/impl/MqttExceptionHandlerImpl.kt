package com.gojek.mqtt.exception.handler.v3.impl

import com.gojek.courier.logging.ILogger
import com.gojek.mqtt.constants.SERVER_UNAVAILABLE_MAX_CONNECT_TIME
import com.gojek.mqtt.exception.handler.v3.MqttExceptionHandler
import com.gojek.mqtt.policies.connectretrytime.IConnectRetryTimePolicy
import com.gojek.mqtt.scheduler.IRunnableScheduler
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.Random
import javax.net.ssl.SSLHandshakeException
import org.eclipse.paho.client.mqttv3.MqttException

internal class MqttExceptionHandlerImpl(
    private val runnableScheduler: IRunnableScheduler,
    private val connectRetryTimePolicy: IConnectRetryTimePolicy,
    private val logger: ILogger,
    private val random: Random = Random()
) : MqttExceptionHandler {

    override fun handleException(mqttException: MqttException, reconnect: Boolean) {
        when (mqttException.reasonCode.toShort()) {
            MqttException.REASON_CODE_BROKER_UNAVAILABLE -> {
                logger.e(TAG, "Server Unavailable, try reconnecting later")
                val reconnectIn =
                    random.nextInt(SERVER_UNAVAILABLE_MAX_CONNECT_TIME) + 1
                // Converting minutes to seconds
                runnableScheduler.scheduleNextConnectionCheck(reconnectIn * 60L)
            }
            MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED -> {
                logger.e(TAG, "Client already disconnected.")
                if (reconnect) {
                    runnableScheduler.connectMqtt(
                        connectRetryTimePolicy.getConnRetryTimeSecs() * 1000L
                    )
                }
            }
            MqttException.REASON_CODE_CLIENT_DISCONNECTING -> if (reconnect) {
                // try reconnect after 1 sec, so that disconnect happens properly
                runnableScheduler.scheduleNextConnectionCheck(1)
            }
            MqttException.REASON_CODE_CLIENT_EXCEPTION -> {
                logger.e(TAG, "Client exception : entered REASON_CODE_CLIENT_EXCEPTION")
                if (mqttException.cause == null) {
                    handleOtherException()
                    runnableScheduler.scheduleNextConnectionCheck(
                        connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
                    )
                } else {
                    logger.e(TAG, "Exception : " + mqttException.cause!!.message)
                    if (mqttException.cause is UnknownHostException) {
                        handleDNSException()
                    } else if (mqttException.cause is SocketException) {
                        handleSocketException()
                    } else if (mqttException.cause is SocketTimeoutException) {
                        handleSocketTimeOutException()
                    } else if (mqttException.cause is UnresolvedAddressException) {
                        handleDNSException()
                    } else if (mqttException.cause is SSLHandshakeException) {
                        handleSSLHandshakeException()
                    } else {
                        handleOtherException()
                        runnableScheduler.scheduleNextConnectionCheck(
                            connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
                        )
                    }
                }
            }
            MqttException.REASON_CODE_CLIENT_NOT_CONNECTED -> {
                if (reconnect) {
                    runnableScheduler.connectMqtt()
                }
            }
            /*
             // Till this point disconnect has already happened.
                This could happen in PING or other TIMEOUT happen such as CONNECT, DISCONNECT
             */
            MqttException.REASON_CODE_CLIENT_TIMEOUT ->
                if (reconnect) {
                    runnableScheduler.connectMqtt()
                }
            MqttException.REASON_CODE_CONNECT_IN_PROGRESS -> {
                logger.e(TAG, "Client already in connecting state")
            }
            MqttException.REASON_CODE_CONNECTION_LOST -> {
                if (reconnect) {
                    /*
                    // since we can get this exception many times due to server exception
                        or during deployment so we dont retry frequently instead with backoff
                     */
                    runnableScheduler.scheduleNextConnectionCheck(
                        connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
                    )
                }
            }
            MqttException.REASON_CODE_MAX_INFLIGHT -> {
                logger.e(
                    TAG,
                    "There are already to many messages in publish. Exception : " +
                        mqttException.message
                )
            }
            MqttException.REASON_CODE_SERVER_CONNECT_ERROR -> {
                handleOtherException()
                runnableScheduler.scheduleNextConnectionCheck(
                    connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
                )
            }
            MqttException.REASON_CODE_CLIENT_CLOSED -> {
                // this will happen only when you close the conn, so dont do any thing
            }
            MqttException.REASON_CODE_CLIENT_CONNECTED -> {
                // The client is already connected.
            }
            MqttException.REASON_CODE_CLIENT_DISCONNECT_PROHIBITED -> {
                /* Thrown when an attempt to call MqttClient.disconnect()
                    has been made from within a method on MqttCallback.
                 */
            }
            MqttException.REASON_CODE_FAILED_AUTHENTICATION -> {
                runnableScheduler.scheduleAuthFailureRunnable(
                    connectRetryTimePolicy.getConnRetryTimeSecs(true) * 1000L
                )
            }
            MqttException.REASON_CODE_NOT_AUTHORIZED -> {
                runnableScheduler.scheduleAuthFailureRunnable(
                    connectRetryTimePolicy.getConnRetryTimeSecs(true) * 1000L
                )
            }
            MqttException.REASON_CODE_INVALID_CONNECT_OPTIONS -> {
                runnableScheduler.scheduleAuthFailureRunnable(
                    connectRetryTimePolicy.getConnRetryTimeSecs(true) * 1000L
                )
            }
            MqttException.REASON_CODE_INVALID_CLIENT_ID -> {
            }
            MqttException.REASON_CODE_INVALID_MESSAGE -> {
            }
            MqttException.REASON_CODE_INVALID_PROTOCOL_VERSION -> {
            }
            MqttException.REASON_CODE_NO_MESSAGE_IDS_AVAILABLE -> {
            }
            MqttException.REASON_CODE_SOCKET_FACTORY_MISMATCH -> {
            }
            MqttException.REASON_CODE_SSL_CONFIG_ERROR -> {
            }
            MqttException.REASON_CODE_TOKEN_INUSE -> {
            }
            MqttException.REASON_CODE_UNEXPECTED_ERROR -> {
                /* This could happen while reading or writing error on a socket,
                   hence disconnection happens
                 */
                handleOtherException()
                runnableScheduler.scheduleNextConnectionCheck(
                    connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
                )
            }
            else -> {
                handleOtherException()
                runnableScheduler.connectMqtt(
                    connectRetryTimePolicy.getConnRetryTimeSecs() * 1000L
                )
            }
        }
    }

    private fun handleOtherException() {
        logger.e(TAG, "Client exception : entered handleOtherException")
    }

    private fun handleDNSException() {
        logger.e(TAG, "DNS Failure , Connect using ips")
        runnableScheduler.scheduleNextConnectionCheck(
            connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
        )
    }

    private fun handleSocketException() {
        logger.e(TAG, "DNS Failure , Connect using ips")
        runnableScheduler.scheduleNextConnectionCheck(
            connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
        )
    }

    private fun handleSocketTimeOutException() {
        logger.e(TAG, "Client exception : entered handleSocketTimeOutException")
        runnableScheduler.scheduleNextConnectionCheck(
            reconnectTimeSecs = connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
        )
    }

    private fun handleSSLHandshakeException() {
        logger.e(TAG, "SSLHandshake Failure , Connect using ips")
        runnableScheduler.scheduleNextConnectionCheck(
            reconnectTimeSecs = connectRetryTimePolicy.getConnRetryTimeSecs().toLong()
        )
    }

    companion object {
        const val TAG = "MqttExceptionHandler"
    }
}
