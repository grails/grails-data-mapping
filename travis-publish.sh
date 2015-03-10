#!/bin/bash

# use travis_after_all.py for publishing only after all builds are successfull.
#if [[ "$BUILD_LEADER" == "YES" ]]; then
#  if [[ "$BUILD_AGGREGATE_STATUS" != "others_succeeded" ]]; then
#    echo "Some builds failed, not publishing."
#    exit 0
#  fi
#else
#  # not build leader, exit
#  exit 0
#fi

echo "Publishing..."

EXIT_STATUS=0

if [[ $TRAVIS_REPO_SLUG == "grails/grails-data-mapping" && $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

  echo "Publishing archives"

  gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY
  if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" uploadArchives publish || EXIT_STATUS=$?
  else
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" uploadArchives publish || EXIT_STATUS=$?
  fi

  ./gradlew allDocs || EXIT_STATUS=$?

  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global credential.helper "store --file=~/.git-credentials"
  echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

  git clone https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git -b gh-pages gh-pages --single-branch > /dev/null
  cd gh-pages

  # If this is the master branch then update the snapshot
  if [[ $TRAVIS_BRANCH == 'master' ]]; then
    mkdir -p snapshot
    cp -r ../build/docs/. ./snapshot/

    git add snapshot/*
  fi

  # If there is a tag present then this becomes the latest
  if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    version="$TRAVIS_TAG"
    version=${version:1}
    milestone=${version:5}
    if [[ -n $milestone ]]; then
      mkdir -p latest
      cp -r ../build/docs/. ./latest/
      git add latest/*
    fi

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
