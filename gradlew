#!/bin/sh

set -e

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
for dir in "$GRADLE_USER_HOME"/wrapper/dists/gradle-9.7.0-all/*; do
    if [ -x "$dir/gradle-9.7.0/bin/gradle" ]; then
        exec "$dir/gradle-9.7.0/bin/gradle" "$@"
    fi
done

echo "gradle-9.7.0 not found in $GRADLE_USER_HOME/wrapper/dists" >&2
exit 1
