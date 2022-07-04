#!/bin/sh

echo "Publishing Libraries to Maven Local..."
./gradlew :paho:assemble :courier-core:assemble :courier-core-android:assemble --parallel --daemon && ./gradlew :paho:publishToMavenLocal -PIS_LOCAL=true :courier-core:publishToMavenLocal -PIS_LOCAL=true :courier-core-android:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :mqtt-pingsender:assemble && ./gradlew :mqtt-pingsender:publishToMavenLocal -PIS_LOCAL=true
./gradlew :workmanager-pingsender:assemble :workmanager-2.6.0-pingsender:assemble :alarm-pingsender:assemble :timer-pingsender:assemble --parallel --daemon && ./gradlew :workmanager-pingsender:publishToMavenLocal -PIS_LOCAL=true :workmanager-2.6.0-pingsender:publishToMavenLocal -PIS_LOCAL=true :alarm-pingsender:publishToMavenLocal -PIS_LOCAL=true :timer-pingsender:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :network-tracker:assemble :adaptive-keep-alive:assemble :courier-message-adapter-gson:assemble :courier-message-adapter-protobuf:assemble :courier-message-adapter-moshi:assemble :courier-stream-adapter-rxjava:assemble :courier-stream-adapter-rxjava2:assemble :courier-stream-adapter-coroutines:assemble :courier:assemble :app-state-manager:assemble --parallel --daemon && ./gradlew :network-tracker:publishToMavenLocal -PIS_LOCAL=true :adaptive-keep-alive:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-gson:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-moshi:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-protobuf:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-rxjava:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-rxjava2:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-coroutines:publishToMavenLocal -PIS_LOCAL=true :courier:publishToMavenLocal -PIS_LOCAL=true :app-state-manager:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :mqtt-client:assemble --parallel --daemon && ./gradlew :mqtt-client:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :courier:assemble :courier-auth-http:assemble --parallel --daemon && ./gradlew :courier:publishToMavenLocal -PIS_LOCAL=true :courier-auth-http:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :chuck-mqtt:assemble :chuck-mqtt-no-ops:assembleRelease --parallel --daemon && ./gradlew :chuck-mqtt:publishToMavenLocal -PIS_LOCAL=true :chuck-mqtt-no-ops:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon

status=$?
if [ "$status" = 0 ] ; then
    echo "Publishing Libraries found no problems."
    exit 0
else
    echo 1>&2 "Publishing Libraries violations! Fix them before pushing your code!"
    exit 1
fi

