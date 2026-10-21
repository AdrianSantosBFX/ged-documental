#!/bin/sh
set -eu
cd "$(dirname "$0")"
rm -rf build/classes
mkdir -p build/classes
javac --release 8 -encoding UTF-8 -cp "lib/*" -d build/classes src/ged/*.java
printf 'Main-Class: ged.GedServer\nClass-Path: lib/pdfbox-1.8.16.jar lib/fontbox-1.8.16.jar lib/commons-logging-1.3.0.jar\n\n' > build/MANIFEST.MF
jar cfm ged-server.jar build/MANIFEST.MF -C build/classes .
echo "ged-server.jar criado."
