# Authenticator

When an MQTT client tries to make a connection with an MQTT broker, username and password are sent inside CONNECT packet, which the broker uses to authenticate the client. If username or password is incorrect, broker returns reason code `5`.

Courier library uses the Authenticator to refresh the connect options, which contains the username and password, in order to reconnect with the broker successfully.

You can pass your own implementation of [Authenticator][1] interface or uses the library provided [HttpAuthenticator][2]

## Http Authenticator

Courier library provides an implementation of Authenticator, which allows you to fetch the latest connect options by making an HTTP call.

### Usage

Add this dependency for using Http Authenticator

~~~ kotlin
dependencies {
    implementation "com.gojek.courier:courier-auth-http:x.y.z"
}
~~~

An instance of HttpAuthenticator can be created using the factory class.

~~~ kotlin
httpAuthenticator = HttpAuthenticatorFactory.create(
            retrofit = retrofit,
            apiUrl = TOKEN_AUTH_API,
            responseHandler = responseHandler,
            eventHandler = eventHandler,
            authRetryPolicy = authRetryPolicy
        )
~~~

[1]: https://github.com/gojek/courier-android/blob/main/mqtt-client/src/main/java/com/gojek/mqtt/auth/Authenticator.kt
[2]: https://github.com/gojek/courier-android/blob/main/courier-auth-http/src/main/java/com/gojek/courier/authhttp/HttpAuthenticator.kt