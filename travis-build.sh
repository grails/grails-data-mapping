#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    ./gradlew  --resolve-dependencies --no-daemon check || EXIT_STATUS=$?
fi

./gradlew --stop
./travis-publish.sh || EXIT_STATUS=$?

exit $EXIT_STATUS



