#!/bin/sh

SDK_TOOLS_PATH=~/adt-bundle-linux-x86_64-20130729/sdk/tools/

if [ -z "$3" ]; then
  echo "Syntax: $0 <keystore> <unsigned.apk> <keyalias>"
  echo "The APK is expected to be unsigned. In Eclipse, do RightClick->Android Tools->Export Unsigned"
  exit 1
fi

check_file() {
  if [ ! -f "$2" ]; then
    echo "$1 file '$2' not found or not a regular file."
    exit 1
  fi
}

echo "Using Keystore: $1"
check_file "Keystore" "$1"

echo "Signing/aligining APK: $2"
check_file "APK" "$2"

echo "Using alias: $3"

run_cmd() {
  echo "RUN> $1"
  $1
  if [ $? -ne 0 ]; then
    exit 1
  fi
}

run_cmd "jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore $1 $2 $3"
run_cmd "jarsigner -verify -verbose -certs $2"
run_cmd "$SDK_TOOLS_PATH/zipalign -v 4 $2 ${2}.done"
