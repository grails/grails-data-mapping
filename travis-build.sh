#!/bin/bash
EXIT_STATUS=0

./gradlew --stop

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then

    echo "Tagged Release Skipping Tests for Publish"
    ./travis-publish.sh || EXIT_STATUS=$?
else
    ./gradlew --no-daemon compileTestGroovy || EXIT_STATUS=$?
    if [[ $EXIT_STATUS -eq 0 ]]; then
        ./gradlew --no-daemon --refresh-dependencies check || EXIT_STATUS=$?
        if [[ $EXIT_STATUS -eq 0 && $TRAVIS_PULL_REQUEST == 'false' ]]; then
            echo "Travis Branch $TRAVIS_BRANCH"
            if ([[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH == 'master' ]] && [[ "${TRAVIS_JDK_VERSION}" != "openjdk11" ]]); then
                ./travis-publish.sh || EXIT_STATUS=$?
            fi
        fi
    fi
fi

exit $EXIT_STATUS
