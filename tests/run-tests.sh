#!/bin/sh
set -eu
: "${LANG:=C.UTF-8}"
: "${LC_ALL:=C.UTF-8}"
export LANG LC_ALL
cd "$(dirname "$0")/.."
sh build.sh
mkdir -p build/test-classes
javac --release 8 -encoding UTF-8 -cp "elevar-ged.jar:lib/*" -d build/test-classes tests/ged/*.java
java -cp "build/test-classes:elevar-ged.jar:lib/*" ged.BrasilApiIntegrationTest
java -cp "build/test-classes:elevar-ged.jar:lib/*" ged.AutoCompanyRegistrationTest
node --check assets/app.js

# Identidade visual e executavel oficial da v0.1.3.
test -s elevar-ged.jar
grep -q 'Elevar GED • v0.1.3' index.html
grep -q 'Elevar GED' assets/app.js
grep -q '"0.1.3"' src/ged/GedServer.java
echo "OK: Elevar GED v0.1.3 identity and packaging checks."
