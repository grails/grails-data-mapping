#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Tagged Release Skipping Tests for Publish"
    ./gradlew --stop
    ./travis-publish.sh || EXIT_STATUS=$?
else
    ./gradlew --no-daemon compileTestGroovy || EXIT_STATUS=$?
    ./gradlew --stop
    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --no-daemon  compileTestGroovy|| EXIT_STATUS=$?
        ./gradlew --stop
    fi
    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --no-daemon --refresh-dependencies check || EXIT_STATUS=$?
        if [[ $EXIT_STATUS -eq 0 && $TRAVIS_PULL_REQUEST == 'false' ]]; then
            if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == '6.1.x' ]]; then
                ./gradlew --stop
                ./travis-publish.sh || EXIT_STATUS=$?
            fi
        fi
    fi
fi


exit $EXIT_STATUS


