#!/bin/bash

# use travis_after_all.py for publishing only after all builds are successfull.
if [[ "$BUILD_LEADER" == "YES" ]]; then
    if [[ "$BUILD_AGGREGATE_STATUS" != "others_succeeded" ]]; then
      echo "Some builds failed, not publishing."
      exit 0
    fi
else
    # not build leader, exit
    exit 0
fi

echo "Publishing..."

EXIT_STATUS=0

if [[ ( $TRAVIS_BRANCH == 'master' || $TRAVIS_BRANCH == '3.x' ) && $TRAVIS_REPO_SLUG == "grails/grails-data-mapping" && $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

    echo "Publishing archives"

    if [[ -n $TRAVIS_TAG ]]; then
        ./gradlew bintrayUpload || EXIT_STATUS=$?
    else
        ./gradlew publish || EXIT_STATUS=$?
    fi

    ./gradlew allDocs || EXIT_STATUS=$?

	git clone https://${GH_TOKEN}@github.com/grails/grails-data-mapping.git -b gh-pages gh-pages --single-branch > /dev/null
	cd gh-pages

	# If this is the master branch then update the snapshot
	if [[ $TRAVIS_BRANCH == 'master' ]]; then
		mkdir -p snapshot
		cp -r ../build/docs/. ./snapshot/

		git add snapshot/*
	fi

    # If there is a tag present then this becomes the latest
    if [[ -n $TRAVIS_TAG ]]; then
        if [[ $TRAVIS_BRANCH == 'master' ]]; then
            git rm -rf latest/
            mkdir -p latest
            cp -r ../build/docs/. ./latest/
            git add latest/*
        fi

        version="$TRAVIS_TAG"
        version=${version:1}
        majorVersion=${version:0:4}
        majorVersion="${majorVersion}x"

        mkdir -p "$version"
        cp -r ../build/docs/. "./$version/"
        git add "$version/*"

        mkdir -p "$majorVersion"
        cp -r ../build/docs/. "./$majorVersion/"
        git add "$majorVersion/*"

    fi

    git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
    git push origin HEAD
    cd ..
    rm -rf gh-pages
fi

exit $EXIT_STATUS
