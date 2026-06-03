#!/bin/bash

./gradlew :exploration-engine-ui-libgdx:installDist -q
BIN="./adapter-ui-libgdx/build/install/exploration-engine-ui-libgdx/bin/exploration-engine-ui-libgdx"
chmod +x "$BIN" 2>/dev/null || true
exec "$BIN" scenarios/default/default.json
