#!/bin/sh
BASE_DIR=$(dirname `readlink -f "$0"`)

if [ ! -f ~/.android/debug.keystore ] ; then
  echo "Android debug.keystore not found '~/.android/debug.keystore'"
  exit 1
fi

# Development signature hash
SIGHASH=$(keytool \
    -exportcert \
    -alias androiddebugkey \
    -keystore ~/.android/debug.keystore \
    -storepass android \
  | openssl sha1 -binary | openssl base64)

# Url encoded development signature hash
ENCODED=$(echo $SIGHASH \
 | python -c \
"import sys
try:
   from urllib import quote
except ImportError:
  from urllib.parse import quote
print(quote(sys.stdin.read()[0:-1], \"\"))")

# Project package name
PACKAGE=$(grep 'package="[^"]\+"' ${BASE_DIR}/app/src/main/AndroidManifest.xml \
  | sed 's/.*package="\([^"]\+\)".*/\1/g')

echo ""
echo "Debug signature hash: ${SIGHASH}"
echo "Project package: ${PACKAGE}"
echo "Redirect uri: msauth://${PACKAGE}/${ENCODED}"
echo ""