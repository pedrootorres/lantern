#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}

if [ $# -lt "1" ]
then
    die "$0: Received $# args... whether or not it's a release (true or false) required"
fi

VERSION=$(grep '<version>' pom.xml |head -1|sed 's,.*<version>\(.*\)</version>,\1,'|sed 's/-SNAPSHOT//'
RELEASE=$1

echo "RELEASE flag is $RELEASE"
./installerBuild.bash "-Dsun.arch.data.model=32 -Pwindows,-mac,-linux,release" $RELEASE || die "Could not build?"

install4jc -v --win-keystore-password=$INSTALL4J_WIN_PASS -m windows -r $VERSION ./install/lantern.install4j || die "Could not build installer"

name=lantern-$VERSION.exe
mv install/Lantern.exe $name || die "Could not move new installer -- failed to create somehow?"
./installMetaRefresh.bash win $name latest.exe $RELEASE || die "ERROR: Could not build meta-refresh redirect file"
