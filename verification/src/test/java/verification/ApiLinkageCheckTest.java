package verification;

import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.objectweb.asm.*;
import static org.junit.Assert.*;

public class ApiLinkageCheckTest {
    private Set<String> call(int opcode, String owner, String name, String descriptor, boolean itf) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "fixture/Caller", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "call", "()V", null, null);
        method.visitCode();
        // Deliberately malformed linkage fixtures, never executed as bytecode.
        method.visitMethodInsn(opcode, owner, name, descriptor, itf);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(4, 1); method.visitEnd(); writer.visitEnd();
        return ApiLinkageCheck.inspectClasses(Map.of("fixture/Caller", writer.toByteArray()));
    }
    @Test public void resolvesInheritedMembers() {
        assertTrue(call(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "wait", "()V", false).isEmpty());
    }
    @Test public void rejectsMissingMembers() {
        assertTrue(call(Opcodes.INVOKEVIRTUAL, "java/lang/String", "removedMethod", "()V", false)
                .stream().anyMatch(s -> s.contains("missing java/lang/String.removedMethod")));
    }
    @Test public void rejectsStaticChanges() {
        assertTrue(call(Opcodes.INVOKESTATIC, "java/lang/String", "length", "()I", false)
                .stream().anyMatch(s -> s.contains("static mismatch")));
    }
    @Test public void rejectsClassInterfaceChanges() {
        assertTrue(call(Opcodes.INVOKEVIRTUAL, "java/util/List", "size", "()I", false)
                .stream().anyMatch(s -> s.contains("class/interface mismatch")));
    }
    @Test public void rejectsMissingDescriptorTypes() {
        assertTrue(call(Opcodes.INVOKEVIRTUAL, "java/lang/String", "accept", "(Lmissing/Type;)V", false)
                .stream().anyMatch(s -> s.contains("missing class missing/Type")));
    }
}
