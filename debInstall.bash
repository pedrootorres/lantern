#!/usr/bin/env bash

#Do not run this directly.  Instead, use debInstall32Bit.bash or 
#debInstall64Bit.bash

function die() {
  echo $*
  exit 1
}

if [ $# -ne "3" ]
then
    die "$0: Received $# args...whether or not this is a release, architecture, and build ID required"
fi

VERSION=$(grep '<version>' pom.xml |head -1|sed 's,.*<version>\(.*\)</version>,\1,'|sed 's/-SNAPSHOT//')
RELEASE=$1
ARCH=$2
BUILD_ID=$3

./installerBuild.bash "-Dbuildos=linux -Dsun.arch.data.model=$ARCH -Plinux,-mac,-windows,release" $RELEASE || die "Could not build!!"

#install4jc -m linuxDeb -r $VERSION ./install/lantern.install4j || die "Could not build Linux installer?"

install4jc -b $BUILD_ID -r $VERSION ./install/lantern.install4j || die "Could not build Linux installer?"

name=lantern-$VERSION-$ARCH-bit.deb
mv install/lantern*$ARCH*.deb $name || die "Could not find built installer?"

./installMetaRefresh.bash linux $name latest-$ARCH.deb $RELEASE

#cp $name ~/Desktop/virtual-machine-files/
