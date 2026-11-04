@echo off
rem SPDX-License-Identifier: Apache-2.0
rem AI-generated (Claude Code, Anthropic). Untested on Windows.
rem
rem Starts the bundled AMI JViewer in StandAlone mode with the PacketFix agent.
rem JViewer asks for host, user and password itself. Arguments are passed through to it.
rem Env: JAVA (default: java on PATH, 21+), JAVA_OPTS, PACKETFIX_TRACE=1 to log sent packets.
setlocal
set "DIR=%~dp0"
if not defined JAVA set "JAVA=java"
set "J=%DIR%jviewer"
set "CP=%J%\natives.jar;%J%\JViewer.jar;%J%\JViewer-SOC.jar;%J%\JViewer-OEM.jar;%J%\JViewer-AVIStream.jar;%J%\JViewer-FolderRedir.jar"

rem -restService 1: use the BMC's REST API; see megarac-kvm.sh for why it is needed.
"%JAVA%" %JAVA_OPTS% -javaagent:"%DIR%agent\packetfix.jar" -cp "%CP%" com.ami.kvm.jviewer.JViewer -apptype StandAlone -restService 1 %*
