#!/bin/bash


version=$(grep 'projectVersion =' build.gradle)
version=${version//[[:blank:]]/}
version="${version#*=}";
version=${version//\"/}

releaseType=$(grep 'releaseType =' build.gradle | egrep -v ^[[:blank:]]*\/\/ | egrep -v ^[[:blank:]]*isBuildSnapshot)
releaseType=${releaseType//[[:blank:]]/}
releaseType="${releaseType#*=}";
releaseType=${releaseType//\"/}


echo "$releaseType"

echo "$version"