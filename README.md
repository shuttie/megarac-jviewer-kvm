# megarac-jviewer-kvm

Runs the AMI MegaRAC JViewer remote console without a browser or Java Web Start, with a
small Java agent that fixes JViewer showing a black screen on newer MegaRAC SP-X firmware.

## Quick start

```sh
git clone <this repo>
cd megarac-jviewer-kvm
./megarac-kvm.sh
```

JViewer opens its connection dialog and asks for the BMC address, user and password.
On Windows, run `megarac-kvm.cmd`.

![JViewer connection dialog](docs/login.png)

After **Launch**, the console shows the host screen:

![JViewer showing a Linux console login](docs/run.png)

## Requirements

- Java 21 or newer. Set `JAVA=/path/to/java` if the `java` on your PATH is older.
- A BMC running AMI MegaRAC SP-X with its web UI reachable on HTTPS (port 443).

## What it fixes

| Symptom | Cause | Fix |
|---|---|---|
| `jviewer.jnlp` does nothing | Java Web Start was removed in Java 11 | JViewer runs in StandAlone mode from the bundled jars |
| "unable to access native libraries" | JViewer looks for its natives at a path the BMC's jars don't use | `jviewer/natives.jar` puts them there |
| "reconnect in progress" forever | JViewer probes `/rpc/*.asp` endpoints the firmware no longer has and dials port 7578, which is closed in single-port mode | the runner passes `-restService 1` |
| Connects, black screen, 0 fps | JViewer never sends its session token (below) | `agent/packetfix.jar` |

## How the agent works

`agent/packetfix.jar` is a Java agent that patches two JViewer methods in memory when they
load. The vendor jars on disk are not changed.

- **`JViewerApp.getMacAddress`** now returns `00-00-00-00-00-00`. JViewer needs the client MAC
  for its session packet. When the lookup fails it logs "Failed to get mac Address" and skips
  sending the session token, so the BMC never starts the video stream. The BMC's own HTML5
  viewer sends the same placeholder.
- **`Client.sendMessage`** rewrites the header of the session packet (IVTP type 18) before it
  goes out. JViewer sets its size to 446, header included, and its status to 0. The HTML5
  viewer, which the firmware accepts, sends 438 (body only) and status 1.

`PACKETFIX_TRACE=1 ./megarac-kvm.sh` logs the header of every packet JViewer sends.

## Platforms

| Platform | Status |
|---|---|
| Linux x86-64 | tested |
| Linux i386, Windows x86-64 and i386 | natives bundled, untested |
| macOS x86-64 | natives bundled, untested |
| macOS on Apple Silicon | the bundled natives are x86-64 only, so you would need an x86-64 Java under Rosetta. Untested |

## Tested hardware

GIGABYTE MZ32-AR0 rev 1.0, BMC firmware 12.61.39 (built 2025-07-02), ASPEED SoC, BIOS R44.
The JViewer jars come from that BMC. [`jviewer/SOURCE.md`](jviewer/SOURCE.md) lists each file
and its SHA-256.

Other boards with the same MegaRAC SP-X generation (other GIGABYTE, ASRock Rack and Tyan
servers) may work with these jars, or with the jars from their own BMC copied into
`jviewer/`. Neither has been tested.

## Rebuilding the agent

```sh
agent/build.sh            # or JDK=/path/to/jdk agent/build.sh
```

It downloads ASM 9.8 from Maven Central (pinned SHA-256), compiles `agent/src/PacketFix.java`
for Java 21, and writes `agent/packetfix.jar` with ASM shaded in.

## License

The runner scripts and the agent are Apache-2.0, see [`LICENSE`](LICENSE).

The files in `jviewer/` are copyright American Megatrends and are not covered by that license.
ASM, shaded into `packetfix.jar`, is BSD-3-Clause, see
[`agent/LICENSE-ASM.txt`](agent/LICENSE-ASM.txt).

The runner scripts and this README were generated with Claude Code (Anthropic).
