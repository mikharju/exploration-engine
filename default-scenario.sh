#!/bin/bash

# ./build/install/exploration-engine/bin/exploration-engine --ui LANTERNA scenarios/default/default.json
./gradlew :exploration-engine-ui-lanterna:installDist
./adapter-ui-lanterna/build/install/exploration-engine-ui-lanterna/bin/exploration-engine-ui-lanterna scenarios/default/default.json