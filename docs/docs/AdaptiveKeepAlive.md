# Adaptive KeepAlive

Adaptive keepalive is a feature in the Courier library which tries to find the most optimal keepalive interval for a client on a particular network. This helps us in optimising the number of ping requests sent over the network and keeping the connection alive.

You can read about Adaptive KeepAlive in detail [here][1].

## Usage

To enable adaptive keepalive for your Courier connection, you just need to pass `AdaptiveKeepAliveConfig` inside [ExperimentConfigs](ExperimentConfigs).

This will create a new connection having the same connect options as the original connection. Only the client id for this new connection is changed by appending the `:adaptive` suffix to the original client id.

### AdaptiveKeepAliveConfig

AdaptiveKeepAliveConfig has the following configs:

- **lowerBoundMinutes** : Lower bound of the window in which optimal keepalive interval has to be searched.

- **upperBoundMinutes** : Upper bound of the window in which optimal keepalive interval has to be searched.

- **stepMinutes** : Step size with which keep alive interval is incremented while searching for the optimal keepalive interval. 

- **optimalKeepAliveResetLimit** : Once optimal keepalive interval is found, it will be reset if it keeps failing beyond **optimalKeepAliveResetLimit**.

- **pingSender** : Implementation of ping sender used for sending ping requests over the new MQTT connection used for finding the optimal keepalive interval.

[1]: https://medium.com/gojekengineering/adaptive-heartbeats-for-our-information-superhighway-26459bf85d62
