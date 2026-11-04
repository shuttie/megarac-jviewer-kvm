#!/bin/sh
# Rebuilds agent/packetfix.jar: PacketFix.java + a shaded copy of ASM 9.8.
# Needs a JDK 21+ (javac, jar) on PATH, or JDK=/path/to/jdk. Downloads ASM once into agent/build/.
set -e
cd "$(dirname "$0")"
BIN=${JDK:+$JDK/bin/}
ASM=build/asm-9.8.jar
ASM_URL=https://repo1.maven.org/maven2/org/ow2/asm/asm/9.8/asm-9.8.jar
ASM_SHA256=876eab6a83daecad5ca67eb9fcabb063c97b5aeb8cf1fca7a989ecde17522051

mkdir -p build
if [ ! -f "$ASM" ]; then
    curl -fsSL "$ASM_URL" -o "$ASM.tmp"
    echo "$ASM_SHA256  $ASM.tmp" | sha256sum -c - >/dev/null
    mv "$ASM.tmp" "$ASM"
fi

rm -rf build/classes && mkdir -p build/classes/META-INF
"${BIN}javac" --release 21 -cp "$ASM" -d build/classes src/PacketFix.java
(cd build/classes && unzip -oq ../asm-9.8.jar 'org/*')
cp LICENSE-ASM.txt build/classes/META-INF/
printf 'Premain-Class: PacketFix\n' > build/manifest.txt
"${BIN}jar" --create --file packetfix.jar --manifest build/manifest.txt -C build/classes .
echo "built agent/packetfix.jar"
