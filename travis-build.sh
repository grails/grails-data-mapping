#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
else
    ./gradlew compileGroovy || EXIT_STATUS=$?
    ./gradlew --stop
    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew compileTestGroovy|| EXIT_STATUS=$?
        ./gradlew --stop
    fi
    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --refresh-dependencies check || EXIT_STATUS=$?
    fi
fi

if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew --stop
    ./travis-publish.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS



