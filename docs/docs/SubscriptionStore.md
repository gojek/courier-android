# Subscription Store

Courier library uses Subscription Store for maintaining the current subscriptions and pending unsubscribe requests.

Currently there are two implementations of SubscriptionStore provided by Courier library.

## PersistableSubscriptionStore

In this implementation, the current subscriptions are maintained in-memory and pending unsubscribe requests are maintained in shared preferences. When client reconnects, subscription packets are sent again and pending unsubscribe packets are also sent, if present.

## InMemorySubscriptionStore

In this implementation, the current subscriptions are maintained in-memory and no pending unsubscribe requests are maintained. When client reconnects, subscription packets are sent again.

## Usage

You can choose the subscription store implementation to be used using [ExperimentConfigs](ExperimentConfigs)