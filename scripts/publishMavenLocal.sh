#!/bin/sh

echo "Publishing Libraries to Maven Local..."
echo "is IS_LOCAL value is : true"
echo "present directory : $PWD"
./gradlew :paho:assemble -PIS_LOCAL=true :courier-core:assemble -PIS_LOCAL=true :courier-core-android:assemble -PIS_LOCAL=true --parallel --daemon && ./gradlew :paho:publishToMavenLocal -PIS_LOCAL=true :courier-core:publishToMavenLocal -PIS_LOCAL=true :courier-core-android:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :mqtt-pingsender:assemble -PIS_LOCAL=true && ./gradlew :mqtt-pingsender:publishToMavenLocal -PIS_LOCAL=true
./gradlew :workmanager-pingsender:assemble -PIS_LOCAL=true :workmanager-2.6.0-pingsender:assemble -PIS_LOCAL=true :alarm-pingsender:assemble -PIS_LOCAL=true :timer-pingsender:assemble -PIS_LOCAL=true --parallel --daemon && ./gradlew :workmanager-pingsender:publishToMavenLocal -PIS_LOCAL=true :workmanager-2.6.0-pingsender:publishToMavenLocal -PIS_LOCAL=true :alarm-pingsender:publishToMavenLocal -PIS_LOCAL=true :timer-pingsender:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :network-tracker:assembleRelease -PIS_LOCAL=true :adaptive-keep-alive:assemble -PIS_LOCAL=true :courier-message-adapter-gson:assemble -PIS_LOCAL=true :courier-message-adapter-protobuf:assemble -PIS_LOCAL=true :courier-message-adapter-moshi:assemble -PIS_LOCAL=true :courier-stream-adapter-rxjava:assemble -PIS_LOCAL=true :courier-stream-adapter-rxjava2:assemble -PIS_LOCAL=true :courier-stream-adapter-coroutines:assemble -PIS_LOCAL=true :courier:assembleRelease -PIS_LOCAL=true :app-state-manager:assembleRelease -PIS_LOCAL=true --parallel --daemon && ./gradlew :network-tracker:publishToMavenLocal -PIS_LOCAL=true :adaptive-keep-alive:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-gson:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-moshi:publishToMavenLocal -PIS_LOCAL=true :courier-message-adapter-protobuf:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-rxjava:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-rxjava2:publishToMavenLocal -PIS_LOCAL=true :courier-stream-adapter-coroutines:publishToMavenLocal -PIS_LOCAL=true :courier:publishToMavenLocal -PIS_LOCAL=true :app-state-manager:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :mqtt-client:assembleRelease -PIS_LOCAL=true --parallel --daemon && ./gradlew :mqtt-client:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :courier:assembleRelease -PIS_LOCAL=true :courier-auth-http:assembleRelease -PIS_LOCAL=true --parallel --daemon && ./gradlew :courier:publishToMavenLocal -PIS_LOCAL=true :courier-auth-http:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon
./gradlew :chuck-mqtt:assembleRelease -PIS_LOCAL=true :chuck-mqtt-no-ops:assembleRelease --parallel --daemon && ./gradlew :chuck-mqtt:publishToMavenLocal -PIS_LOCAL=true :chuck-mqtt-no-ops:publishToMavenLocal -PIS_LOCAL=true --parallel --daemon

status=$?
if [ "$status" = 0 ] ; then
    echo "Publishing Libraries found no problems."
    exit 0
else
    echo 1>&2 "Publishing Libraries violations! Fix them before pushing your code!"
    exit 1
fi

