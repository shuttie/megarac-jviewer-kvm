# Bundled JViewer files

These are AMI's files, unmodified, downloaded from a BMC's `https://<bmc>/Java/release/`.
They are copyright American Megatrends and are not covered by this repo's Apache-2.0 license.

## Source

| | |
|---|---|
| Board | GIGABYTE MZ32-AR0 rev 1.0 |
| BMC firmware | AMI MegaRAC SP-X 12.61.39, built 2025-07-02 |
| BMC SoC | ASPEED (JViewer SOC module `AST`) |
| Host BIOS | R44 |
| Jar build | Azul JDK 1.8.0_172, entries dated 2025-07-02, signed (`JVIEWER.SF`/`.DSA`) |

JViewer has no version string of its own, so the firmware version and the hashes below
identify it.

## Files

| File | Contents | SHA-256 |
|---|---|---|
| `JViewer.jar` | viewer application | `b60222139689bbf57b92ee2ee5f38620bd6a29136f881bd30e572d173cd38552` |
| `JViewer-SOC.jar` | ASPEED video decoder | `f3551f47435a6fd06c15b98feb81a4a71c875c748ec28fd5febde4a09cb4f9d2` |
| `JViewer-OEM.jar` | OEM strings and resources | `a00b10eefec0bf2043d0201348ae749f7d614980418ddbe55794f82e3a93813a` |
| `JViewer-AVIStream.jar` | video recording | `be0e54273b40c7bfd2f056146ef20b4e95a77d679a733dd6c83abba9e29ae40c` |
| `JViewer-FolderRedir.jar` | folder redirection | `02634bcb0ec485b1661114ba23c4ed55f946a25459eb23cafe704274d49dd736` |
| `Linux_x86_64.jar` | natives, Linux x86-64 `.so` | `970a1ce5718a0ee4a861c75b9242d5aa882bd5af5463abe0f307dbff11c9927c` |
| `Linux_x86_32.jar` | natives, Linux i386 `.so` | `bf1eb7c119ab64bf68ff04dc0174ec1fe323e46749f54a91fbd49186081de3af` |
| `Win64.jar` | natives, Windows x86-64 `.dll` | `06b095b544d3eef99eb7eb6ac0106b58c38080a5f0fc23c516167560af0dff49` |
| `Win32.jar` | natives, Windows i386 `.dll` | `883b6fdbdfebacb450f8183690614451fc4766aa410768d8b1409063fe59ebef` |
| `Mac64.jar` | natives, macOS x86-64 `.jnilib` | `4b0fa86ccd121440862e74dae3d34ddfae3ede456869590dff8e9d8d916da20d` |

The firmware has no `Mac32.jar`.

## natives.jar

Not from the BMC. It holds the same native libraries as the five platform jars above, moved to
`com/ami/kvm/jviewer/lib/{linux64,linux32,win64,win32,mac64}/`. JViewer loads its natives as
resources relative to its own class from those paths, and picks the directory by `os.name` and
`os.arch`. The BMC's platform jars put them at the jar root, which is why a plain launch fails
with "unable to access native libraries".
