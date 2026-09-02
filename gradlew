#!/bin/bash
GRADLE_BIN="/Users/qydxyx/.gradle/wrapper/dists/gradle-8.9-bin/90cnw93cvbtalezasaz0blq0a/gradle-8.9/bin/gradle"
if [ ! -f "$GRADLE_BIN" ]; then
    GRADLE_BIN=$(which gradle 2>/dev/null)
fi
exec "$GRADLE_BIN" "$@"
