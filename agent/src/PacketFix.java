// SPDX-License-Identifier: Apache-2.0
//
// Java agent that makes AMI MegaRAC JViewer stream video from single-port, REST-based
// MegaRAC SP-X firmware (seen on GIGABYTE MZ32-AR0, BMC 12.61.39).
//
// Without it JViewer connects, the BMC accepts the session, and the screen stays black at 0 fps.
// Two patches, applied in memory at class load time (the vendor jars stay untouched):
//
// 1. JViewerApp.getMacAddress() -> always "00-00-00-00-00-00".
//    OnsendWebsessionToken() needs the client MAC for the session packet. If the lookup throws,
//    JViewer logs "Failed to get mac Address" and silently skips sending the session token, so
//    the BMC waits forever. The BMC's own H5 viewer sends this placeholder too.
//
// 2. Client.sendMessage() -> rewrite the IVTP header of session packets before they go out.
//    JViewer sends VALIDATE_VIDEO_SESSION (type 18) with pktSize 446 (header included) and
//    status 0. The H5 viewer, which this firmware accepts, sends 438 (body only) and status 1.
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PacketFix {
    private static final String CLIENT = "com/ami/kvm/jviewer/communication/Client";
    private static final String APP = "com/ami/kvm/jviewer/gui/JViewerApp";
    private static final String NO_MAC = "00-00-00-00-00-00";

    // IVTP: 8-byte little-endian header {u16 type, u32 pktSize, u16 status} + body
    private static final int HDR = 8;
    private static final int VALIDATE_VIDEO_SESSION = 18;
    private static final int CONNECTION_COMPLETE = 58;
    private static final boolean TRACE = System.getenv("PACKETFIX_TRACE") != null;

    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> cls,
                                    ProtectionDomain pd, byte[] bytes) {
                try {
                    // patch 2: every outgoing IVTP packet passes through Client.sendMessage(buf, len)
                    if (CLIENT.equals(name)) return patch(bytes, "sendMessage", "([BI)I", false);
                    // patch 1: the MAC lookup whose failure aborts the session handshake
                    if (APP.equals(name)) return patch(bytes, "getMacAddress",
                            "(I[Ljava/lang/String;)Ljava/lang/String;", true);
                    return null;
                } catch (Throwable t) {
                    System.err.println("PacketFix: failed to patch " + name + ": " + t);
                    return null;
                }
            }
        });
    }

    // replaceBody: the method just returns NO_MAC. Otherwise: prepend a call to fix(buf, len).
    private static byte[] patch(byte[] bytes, String method, String desc, boolean replaceBody) {
        ClassReader reader = new ClassReader(bytes);
        // only straight-line code is added at method entry, so max stack is all that needs recomputing
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        boolean[] found = {false};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                             String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (!name.equals(method) || !descriptor.equals(desc)) return mv;
                found[0] = true;
                if (replaceBody) {
                    mv.visitCode();
                    mv.visitLdcInsn(NO_MAC);
                    mv.visitInsn(Opcodes.ARETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                    return null; // drop the original body
                }
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override
                    public void visitCode() {
                        super.visitCode();
                        super.visitVarInsn(Opcodes.ALOAD, 1); // byte[] buf
                        super.visitVarInsn(Opcodes.ILOAD, 2); // int len
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, "PacketFix", "fix", "([BI)V", false);
                    }
                };
            }
        }, 0);
        if (!found[0]) {
            System.err.println("PacketFix: " + reader.getClassName() + "." + method + " not found, not patched");
            return null;
        }
        System.err.println("PacketFix: patched " + reader.getClassName() + "." + method);
        return writer.toByteArray();
    }

    // Called with every outgoing IVTP packet; edits the header in place. PACKETFIX_TRACE=1 logs each one.
    public static void fix(byte[] buf, int len) {
        if (buf == null || len < HDR || buf.length < HDR) return;
        int type = (buf[0] & 0xff) | (buf[1] & 0xff) << 8;
        int size = (buf[2] & 0xff) | (buf[3] & 0xff) << 8 | (buf[4] & 0xff) << 16 | (buf[5] & 0xff) << 24;
        if (TRACE) System.err.println("PacketFix: send type " + type + " pktSize " + size + " len " + len
                + " status " + ((buf[6] & 0xff) | (buf[7] & 0xff) << 8));
        if (type == CONNECTION_COMPLETE && size == 0 && len == HDR) {
            // header-only reconnect notice sent before the token; the H5 viewer uses status 1
            buf[6] = 1;
            buf[7] = 0;
            return;
        }
        if (type != VALIDATE_VIDEO_SESSION && type != CONNECTION_COMPLETE) return;
        if (size != len) return; // already body-only: leave it alone
        // pktSize must count the body only, otherwise the BMC waits for 8 bytes that never come
        int body = len - HDR;
        buf[2] = (byte) body;
        buf[3] = (byte) (body >>> 8);
        buf[4] = (byte) (body >>> 16);
        buf[5] = (byte) (body >>> 24);
        buf[6] = 1;
        buf[7] = 0;
        System.err.println("PacketFix: type " + type + " pktSize " + size + " -> " + body + ", status -> 1");
    }
}
