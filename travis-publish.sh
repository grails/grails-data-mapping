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

version=$(grep 'projectVersion =' build.gradle)
version=${version//[[:blank:]]/}
version="${version#*=}";
version=${version//\"/}

releaseType=$(grep 'releaseType =' build.gradle | egrep -v ^[[:blank:]]*\/\/ | egrep -v ^[[:blank:]]*isBuildSnapshot)
releaseType=${releaseType//[[:blank:]]/}
releaseType="${releaseType#*=}";
releaseType=${releaseType//\"/}

echo "Project Version: $version $releaseType"
if [[ ( $TRAVIS_BRANCH == 'master' || $TRAVIS_BRANCH == '3.x' ) && $TRAVIS_REPO_SLUG == "grails/grails-data-mapping" && $TRAVIS_PULL_REQUEST == 'false' 
    && $EXIT_STATUS -eq 0 && $releaseType == *-SNAPSHOT* 
    && -n "$ARTIFACTORY_PASSWORD" ]]; then
    echo "Publishing archives"
    ./gradlew -PartifactoryPublishUsername=travis-gdm upload || EXIT_STATUS=$?
fi

if [[ $releaseType != *-SNAPSHOT* ]]
then
    ./gradlew allDocs

    base_dir=$(pwd)

    echo "BASE DIR = $base_dir"

    git config --global user.name "$GIT_NAME"
    git config --global user.email "$GIT_EMAIL"
    git config --global credential.helper "store --file=~/.git-credentials"
    echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

    git clone https://${GH_TOKEN}@github.com/grails/grails-data-mapping.git -b gh-pages gh-pages --single-branch > /dev/null
    cd gh-pages
    echo "Making directory for Version: $version"
    mkdir -p "$version"
    cd "$version"    
    current_dir=$(pwd)
    git rm -rf .    
    mkdir -p "$current_dir"

    echo "Current Directory: $current_dir"
    cp -r "$base_dir/build/docs/." "$current_dir/"
    cd ..
    mkdir -p current
    cd current
    current_dir=$(pwd)
    git rm -rf .
    mkdir -p "$current_dir"
    
    echo "Current Directory: $current_dir"
    cp -r "$base_dir/build/docs/." "$current_dir/"
    cd ..
    cp -r "$base_dir/build/docs/." ./
    git add .
    git commit -a -m "Updating docs for Travis build: https://travis-ci.org/grails/grails-data-mapping/builds/$TRAVIS_BUILD_ID"
    git push origin HEAD
    cd ..
    rm -rf gh-pages
fi

exit $EXIT_STATUS
