#!/usr/bin/env bash

function die() {
  echo $*
  exit 1
}

if [ $# -ne "2" ]
then
    die "$0: Received $# args... osx-cert-password win-cert-passwork required"
fi

INSTALL4J_KEY_OSX=$1
INSTALL4J_KEY_WIN=$1 #???
./osxInstall.bash $INSTALL4J_KEY_OSX || die "Could not build OSX"
./winInstall.bash $INSTALL4J_KEY_WIN || die "Could not build windows"
./debInstall32Bit.bash || die "Could not build linux 32 bit"
./debInstall64Bit.bash || die "Could not build linux 64 bit"


