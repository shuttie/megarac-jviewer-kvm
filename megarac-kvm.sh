#!/bin/sh
# SPDX-License-Identifier: Apache-2.0
# AI-generated (Claude Code, Anthropic).
#
# Starts the bundled AMI JViewer in StandAlone mode with the PacketFix agent.
# JViewer asks for host, user and password itself. Arguments are passed through to it,
# e.g. ./megarac-kvm.sh -hostname 10.0.0.5 -u admin
# Env: JAVA (default: java on PATH, 21+), JAVA_OPTS, PACKETFIX_TRACE=1 to log sent packets.
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
JAVA=${JAVA:-java}

ver=$("$JAVA" -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -n 1)
major=${ver%%.*}
[ "$major" = 1 ] && major=$(echo "$ver" | cut -d. -f2)
if [ -z "$major" ] || [ "$major" -lt 21 ]; then
    echo "megarac-kvm: needs Java 21 or newer, '$JAVA' is ${ver:-not found}. Set JAVA=/path/to/java." >&2
    exit 1
fi

J=$DIR/jviewer
CP=$J/natives.jar:$J/JViewer.jar:$J/JViewer-SOC.jar:$J/JViewer-OEM.jar:$J/JViewer-AVIStream.jar:$J/JViewer-FolderRedir.jar

# -restService 1: query the BMC's REST API (/api/...). Without it JViewer uses legacy /rpc/*.asp
# endpoints that this firmware no longer has, misses that KVM runs over the web port
# (single-port mode) and loops on "reconnect in progress".
exec "$JAVA" $JAVA_OPTS -javaagent:"$DIR/agent/packetfix.jar" -cp "$CP" \
    com.ami.kvm.jviewer.JViewer -apptype StandAlone -restService 1 "$@"
