"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[581],{5610:e=>{e.exports=JSON.parse('{"pluginId":"default","version":"current","label":"Next","banner":null,"badge":false,"className":"docs-version-current","isLast":true,"docsSidebars":{"tutorialSidebar":[{"type":"link","label":"Introduction","href":"/courier-android/docs/Introduction","docId":"Introduction"},{"type":"category","label":"Getting Started","items":[{"type":"link","label":"Installation","href":"/courier-android/docs/Installation","docId":"Installation"},{"type":"link","label":"Sample App","href":"/courier-android/docs/SampleApp","docId":"SampleApp"}],"collapsed":true,"collapsible":true},{"type":"category","label":"Guides","items":[{"type":"link","label":"Connection Setup","href":"/courier-android/docs/ConnectionSetup","docId":"ConnectionSetup"},{"type":"link","label":"MQTT Client Configuration","href":"/courier-android/docs/MqttConfiguration","docId":"MqttConfiguration"},{"type":"link","label":"Courier Service Interface","href":"/courier-android/docs/CourierService","docId":"CourierService"},{"type":"link","label":"Subscribe & Unsubscribe topics","href":"/courier-android/docs/SubscribeUnsubscribe","docId":"SubscribeUnsubscribe"},{"type":"link","label":"Send & Receive messages","href":"/courier-android/docs/SendReceiveMessage","docId":"SendReceiveMessage"},{"type":"link","label":"Message & Stream Adapters","href":"/courier-android/docs/MessageStreamAdapters","docId":"MessageStreamAdapters"},{"type":"link","label":"Experiment Configs","href":"/courier-android/docs/ExperimentConfigs","docId":"ExperimentConfigs"},{"type":"link","label":"Authenticator","href":"/courier-android/docs/Authenticator","docId":"Authenticator"},{"type":"link","label":"Non-standard Connection options","href":"/courier-android/docs/NonStandardOptions","docId":"NonStandardOptions"}],"collapsed":true,"collapsible":true},{"type":"category","label":"Features","items":[{"type":"link","label":"Adaptive KeepAlive","href":"/courier-android/docs/AdaptiveKeepAlive","docId":"AdaptiveKeepAlive"},{"type":"link","label":"MQTT Ping Sender","href":"/courier-android/docs/PingSender","docId":"PingSender"},{"type":"link","label":"Subscription Store","href":"/courier-android/docs/SubscriptionStore","docId":"SubscriptionStore"},{"type":"link","label":"MQTT Chuck","href":"/courier-android/docs/MqttChuck","docId":"MqttChuck"},{"type":"link","label":"Quality of Service","href":"/courier-android/docs/QoS","docId":"QoS"}],"collapsed":true,"collapsible":true},{"type":"link","label":"Contribution","href":"/courier-android/docs/CONTRIBUTION","docId":"CONTRIBUTION"},{"type":"link","label":"License","href":"/courier-android/docs/LICENSE","docId":"LICENSE"}]},"docs":{"AdaptiveKeepAlive":{"id":"AdaptiveKeepAlive","title":"Adaptive KeepAlive","description":"Adaptive keepalive is a feature in the Courier library which tries to find the most optimal keepalive interval for a client on a particular network. This helps us in optimising the number of ping requests sent over the network and keeping the connection alive.","sidebar":"tutorialSidebar"},"Authenticator":{"id":"Authenticator","title":"Authenticator","description":"When an MQTT client tries to make a connection with an MQTT broker, username and password are sent inside CONNECT packet, which the broker uses to authenticate the client. If username or password is incorrect, broker returns reason code 5.","sidebar":"tutorialSidebar"},"ConnectionSetup":{"id":"ConnectionSetup","title":"Connection Setup","description":"MqttClient","sidebar":"tutorialSidebar"},"CONTRIBUTION":{"id":"CONTRIBUTION","title":"How to Contribute","description":"Courier team would love to have your contributions.","sidebar":"tutorialSidebar"},"CourierService":{"id":"CourierService","title":"Courier Service Interface","description":"Courier provides the functionalities like Send, Receive, Subscribe, Unsubscribe through a service interface. This is similar to how we make HTTP calls using Retrofit.","sidebar":"tutorialSidebar"},"ExperimentConfigs":{"id":"ExperimentConfigs","title":"Experiment Configs","description":"These are the experimentation configs used in Courier library. These are volatile configs i.e., they can be modified/moved/removed in future.","sidebar":"tutorialSidebar"},"GettingStarted":{"id":"GettingStarted","title":"Getting Started","description":""},"Installation":{"id":"Installation","title":"Installation","description":"Supported SDK versions","sidebar":"tutorialSidebar"},"Introduction":{"id":"Introduction","title":"Introduction","description":"image banner","sidebar":"tutorialSidebar"},"LICENSE":{"id":"LICENSE","title":"LICENSE","description":"MIT License","sidebar":"tutorialSidebar"},"LICENSE.paho":{"id":"LICENSE.paho","title":"LICENSE.paho","description":"Eclipse Public License - v 2.0"},"MessageStreamAdapters":{"id":"MessageStreamAdapters","title":"Message & Stream Adapters","description":"Courier provides the functionality of passing your own custom or library-provided message & stream adapters.","sidebar":"tutorialSidebar"},"MqttChuck":{"id":"MqttChuck","title":"MQTT Chuck","description":"MQTT Chuck is used for inspecting all the outgoing or incoming MQTT packets for an underlying MQTT connection. MQTT Chuck is similar to HTTP Chuck, used for inspecting the HTTP calls on an android application.","sidebar":"tutorialSidebar"},"MqttConfiguration":{"id":"MqttConfiguration","title":"MQTT Client Configuration","description":"As we have seen earlier, MqttClient requires an instance of MqttV3Configuration. MqttV3Configuration allows you to configure the following properties of MqttClient:","sidebar":"tutorialSidebar"},"NonStandardOptions":{"id":"NonStandardOptions","title":"Non-standard Connection options","description":"UserProperties in MqttConnectionOptions","sidebar":"tutorialSidebar"},"PingSender":{"id":"PingSender","title":"MQTT Ping Sender","description":"When an MQTT connection between a client and the broker is idle for a long time, it may get torn down due to TCP binding timeout. In order to keep the connection alive, the client needs to send PINGREQ packets through the connection. If the connection is alive, the broker responds with a PINGRESP packet. If the client does not receive the PINGRESP packet within some fixed interval, it breaks the connection and reconnects. The interval at which these packets are sent is the Keepalive Interval.","sidebar":"tutorialSidebar"},"QoS":{"id":"QoS","title":"Quality of Service","description":"The Quality of Service (QoS) level is an agreement between the sender & the receiver of a message that defines the guarantee of delivery for a specific message. There are 3 QoS levels in MQTT:","sidebar":"tutorialSidebar"},"SampleApp":{"id":"SampleApp","title":"Sample App","description":"A sample application is added here which makes Courier connection with a HiveMQ public broker. It demonstrates multiple functionalities of Courier like Connect, Disconnect, Publish, Subscribe and Unsubscribe.","sidebar":"tutorialSidebar"},"SendReceiveMessage":{"id":"SendReceiveMessage","title":"Send & Receive messages","description":"Courier library provides the functionality of sending & receiving messages through both service interface and MqttClient.","sidebar":"tutorialSidebar"},"SubscribeUnsubscribe":{"id":"SubscribeUnsubscribe","title":"Subscribe & Unsubscribe topics","description":"Courier library provides the functionality of subscribing & unsubscribing topics through both service interface and MqttClient.","sidebar":"tutorialSidebar"},"SubscriptionStore":{"id":"SubscriptionStore","title":"Subscription Store","description":"Courier library uses Subscription Store for maintaining the current subscriptions and pending unsubscribe requests.","sidebar":"tutorialSidebar"}}}')}}]);