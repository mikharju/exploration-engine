#!/bin/bash

./gradlew :exploration-engine-ui-lanterna:installDist -q
BIN="./adapter-ui-lanterna/build/install/exploration-engine-ui-lanterna/bin/exploration-engine-ui-lanterna"
chmod +x "$BIN" 2>/dev/null || true
exec "$BIN" scenarios/default/default.json
