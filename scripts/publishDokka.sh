#!/bin/sh

echo "Publishing Dokka..."
./gradlew :paho:dokka :mqtt-client:dokka :courier:dokka :courier-core:dokka :courier-message-adapter-gson:dokka :courier-stream-adapter-rxjava2:dokka :courier-stream-adapter-coroutines:dokka :chuck-mqtt:dokka :chuck-mqtt-no-ops:dokka--daemon --parallel
status=$?
if [ "$status" = 0 ] ; then
    echo "Publishing Dokka found no problems."
    exit 0
else
    echo 1>&2 "Publishing Dokka violations! Fix them before pushing your code!"
    exit 1
fi

