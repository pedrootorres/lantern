#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}

if [ $# -lt "1" ]
then
    die "$0: Received $# args... release (true or false) required"
fi

VERSION=$(grep '<version>' pom.xml |head -1|sed 's,.*<version>\(.*\)</version>,\1,'|sed 's/-SNAPSHOT//')
RELEASE=$1

echo "RELEASE flag is $RELEASE"
./installerBuild.bash "-Dsun.arch.data.model=64 -Pmac,-linux,-windows,release" $RELEASE || die "Could not build!!"

install4jc -v --mac-keystore-password=$INSTALL4J_MAC_PASS -m macos -r $VERSION ./install/lantern.install4j || die "Could not build installer?"

name=lantern-$VERSION.dmg
mv install/Lantern.dmg $name || die "Could not move new installer -- failed to create somehow?"
./installMetaRefresh.bash osx $name latest.dmg $RELEASE || die "ERROR: Could not build meta-refresh redirect file"

