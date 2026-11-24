#!/usr/bin/env bash
# Builds both loaders. Jars end up in release/<version>/
cd "$(dirname "$0")" || exit 1
if bash ./gradlew buildRelease; then
    echo
    echo "Done! Jars are in the release folder."
else
    echo
    echo "BUILD FAILED. See the messages above."
    status=1
fi
if [ -t 0 ]; then read -r -p "Press Enter to close..." _; fi
exit "${status:-0}"
