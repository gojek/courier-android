#!/bin/sh

echo "Publishing Libraries to Maven Local..."
echo "is ci value is : $PIS_CI"
echo "present directory : $pwd"
./gradlew :paho:assemble -PIS_CI=:$1 :courier-core:assemble -PIS_CI=:$1 :courier-core-android:assemble -PIS_CI=:$1 --parallel --daemon && ./gradlew :paho:publishToMavenLocal -PIS_CI=:$1 :courier-core:publishToMavenLocal -PIS_CI=:$1 :courier-core-android:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon
./gradlew :mqtt-pingsender:assemble -PIS_CI=:$1 && ./gradlew :mqtt-pingsender:publishToMavenLocal -PIS_CI=:$1
./gradlew :workmanager-pingsender:assemble -PIS_CI=:$1 :alarm-pingsender:assemble -PIS_CI=:$1 :timer-pingsender:assemble -PIS_CI=:$1 --parallel --daemon && ./gradlew :workmanager-pingsender:publishToMavenLocal -PIS_CI=:$1 :alarm-pingsender:publishToMavenLocal -PIS_CI=:$1 :timer-pingsender:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon
./gradlew :network-tracker:assembleRelease -PIS_CI=:$1 :adaptive-keep-alive:assemble -PIS_CI=:$1 :courier-message-adapter-gson:assemble -PIS_CI=:$1 :courier-message-adapter-protobuf:assemble -PIS_CI=:$1 :courier-message-adapter-moshi:assemble -PIS_CI=:$1 :courier-stream-adapter-rxjava:assemble -PIS_CI=:$1 :courier-stream-adapter-rxjava2:assemble -PIS_CI=:$1 :courier-stream-adapter-coroutines:assemble -PIS_CI=:$1 :courier:assembleRelease -PIS_CI=:$1 :app-state-manager:assembleRelease -PIS_CI=:$1 --parallel --daemon && ./gradlew :network-tracker:publishToMavenLocal -PIS_CI=:$1 :adaptive-keep-alive:publishToMavenLocal -PIS_CI=:$1 :courier-message-adapter-gson:publishToMavenLocal -PIS_CI=:$1 :courier-message-adapter-moshi:publishToMavenLocal -PIS_CI=:$1 :courier-message-adapter-protobuf:publishToMavenLocal -PIS_CI=:$1 :courier-stream-adapter-rxjava:publishToMavenLocal -PIS_CI=:$1 :courier-stream-adapter-rxjava2:publishToMavenLocal -PIS_CI=:$1 :courier-stream-adapter-coroutines:publishToMavenLocal -PIS_CI=:$1 :courier:publishToMavenLocal -PIS_CI=:$1 :app-state-manager:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon
./gradlew :mqtt-client:assembleRelease -PIS_CI=:$1 --parallel --daemon && ./gradlew :mqtt-client:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon
./gradlew :courier:assembleRelease -PIS_CI=:$1 :courier-auth-http:assembleRelease -PIS_CI=:$1 --parallel --daemon && ./gradlew :courier:publishToMavenLocal -PIS_CI=:$1 :courier-auth-http:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon
./gradlew :chuck-mqtt:assembleRelease -PIS_CI=:$1 :chuck-mqtt-no-ops:assembleRelease --parallel --daemon && ./gradlew :chuck-mqtt:publishToMavenLocal -PIS_CI=:$1 :chuck-mqtt-no-ops:publishToMavenLocal -PIS_CI=:$1 --parallel --daemon

status=$?
if [ "$status" = 0 ] ; then
    echo "Publishing Libraries found no problems."
    exit 0
else
    echo 1>&2 "Publishing Libraries violations! Fix them before pushing your code!"
    exit 1
fi

