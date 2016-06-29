#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    ./gradlew --refresh-dependencies --no-daemon check || EXIT_STATUS=$?
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop
    ./travis-publish.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS



