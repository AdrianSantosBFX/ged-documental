#!/bin/sh
set -eu
cd "$(dirname "$0")"
: "${LANG:=C.UTF-8}"
export LANG
exec java -Dfile.encoding=UTF-8 -Dged.base="$PWD" -jar ged-server.jar "$@"
