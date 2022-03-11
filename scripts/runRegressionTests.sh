#!/bin/sh

echo "Running Regression Tests..."
./gradlew clean :paho:testDebugUnitTest :mqtt-client:testDebugUnitTest :courier:testDebugUnitTest :courier-core:testDebugUnitTest :courier-message-adapter-gson:testDebugUnitTest :courier-stream-adapter-rxjava2:testDebugUnitTest :courier-stream-adapter-coroutines:testDebugUnitTest :chuck-mqtt:testDebugUnitTest :chuck-mqtt-no-ops:testDebugUnitTest :app-state-manager:testDebugUnitTest --daemon --parallel
status=$?
if [ "$status" = 0 ] ; then
    echo "Regression Tests found no problems."
    exit 0
else
    echo 1>&2 "Regression Tests violations! Fix them before pushing your code!"
    exit 1
fi

