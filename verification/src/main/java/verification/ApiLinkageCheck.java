package verification;

import org.objectweb.asm.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Resolves the RELEASE JAR's bytecode against one API classpath without starting Minecraft.
 * Deliberately does not claim to test reflective calls or server behavior. */
public final class ApiLinkageCheck {
    private record Member(String name, String descriptor) {}
    private static final class Info {
        String parent;
        String[] interfaces;
        int access;
        final Map<Member,Integer> methods = new HashMap<>(), fields = new HashMap<>();
    }
    private final Map<String,byte[]> plugin = new HashMap<>();
    private final Map<String,Info> cache = new HashMap<>();
    private final Set<String> failures = new TreeSet<>();
    private int references;

    // Package-private entrypoint for fault-injection tests; production checks use the final JAR.
    static Set<String> inspectClasses(Map<String, byte[]> classes) {
        ApiLinkageCheck check = new ApiLinkageCheck();
        check.plugin.putAll(classes);
        classes.forEach(check::inspect);
        return Set.copyOf(check.failures);
    }

    public static void main(String[] args) throws Exception {
        ApiLinkageCheck check = new ApiLinkageCheck();
        try (JarFile jar = new JarFile(args[0])) {
            String metadata = new String(jar.getInputStream(jar.getJarEntry("plugin.yml")).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            if (!metadata.contains("api-version: '1.21'")) throw new IllegalStateException("Unexpected minimum API version");
            for (JarEntry entry : Collections.list(jar.entries())) if (entry.getName().endsWith(".class")) {
                byte[] bytes=jar.getInputStream(entry).readAllBytes();
                if (((bytes[6]&255)<<8 | (bytes[7]&255)) > 65) throw new IllegalStateException("Not Java 21 bytecode: " + entry);
                if (!entry.getName().startsWith("com/example/speedrunnerswap/")) throw new IllegalStateException("Unexpected shaded dependency: " + entry);
                check.plugin.put(entry.getName().replace(".class",""),bytes);
            }
        }
        if (check.plugin.size()<30) throw new IllegalStateException("Incomplete plugin JAR");
        check.plugin.forEach(check::inspect);
        if (!check.failures.isEmpty()) throw new IllegalStateException(args[1]+" linkage failures:\n"+String.join("\n",check.failures));
        System.out.println("PASS " + args[1] + ": " + check.plugin.size() + " plugin classes; " + check.references + " references; Java 21 bytecode; API floor 1.21");
    }

    private Info info(String name) {
        if (name.startsWith("[")) return info("java/lang/Object");
        if (cache.containsKey(name)) return cache.get(name);
        byte[] bytes=plugin.get(name);
        if (bytes==null) try (InputStream in=Thread.currentThread().getContextClassLoader().getResourceAsStream(name+".class")) {
            if (in!=null) bytes=in.readAllBytes();
        } catch(IOException e) { throw new UncheckedIOException(e); }
        if (bytes==null) { cache.put(name,null); return null; }
        Info result=new Info();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public void visit(int version,int access,String n,String signature,String parent,String[] interfaces) {
                result.access=access; result.parent=parent; result.interfaces=interfaces;
            }
            @Override public MethodVisitor visitMethod(int access,String n,String d,String s,String[] exceptions) {
                result.methods.put(new Member(n,d),access); return null;
            }
            @Override public FieldVisitor visitField(int access,String n,String d,String s,Object value) {
                result.fields.put(new Member(n,d),access); return null;
            }
        },ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        cache.put(name,result); return result;
    }
    private Integer resolve(String owner,Member member,boolean method,Set<String> seen) {
        if (owner==null || !seen.add(owner)) return null;
        Info i=info(owner); if(i==null)return null;
        Integer access=(method?i.methods:i.fields).get(member);
        if(access!=null)return access;
        if(member.name.equals("<init>"))return null;
        access=resolve(i.parent,member,method,seen);
        if(access!=null)return access;
        for(String parent:i.interfaces) { access=resolve(parent,member,method,seen); if(access!=null)return access; }
        return null;
    }
    private void type(String source,Type t) {
        if(t.getSort()==Type.ARRAY)type(source,t.getElementType());
        else if(t.getSort()==Type.METHOD) { type(source,t.getReturnType()); for(Type a:t.getArgumentTypes())type(source,a); }
        else if(t.getSort()==Type.OBJECT && info(t.getInternalName())==null) failures.add(source+": missing class "+t.getInternalName());
    }
    private void reference(String source,int opcode,String owner,String name,String descriptor,boolean method,boolean itf) {
        references++;
        type(source,Type.getType(descriptor));
        // Array clone has a JVM-provided public implementation.
        if(owner.startsWith("[") && name.equals("clone"))return;
        Info i=info(owner);
        if(i==null) { failures.add(source+": missing owner "+owner); return; }
        Integer access=resolve(owner,new Member(name,descriptor),method,new HashSet<>());
        if(access==null) { failures.add(source+": missing "+owner+"."+name+descriptor); return; }
        boolean expectedStatic=opcode==Opcodes.INVOKESTATIC||opcode==Opcodes.GETSTATIC||opcode==Opcodes.PUTSTATIC;
        if(expectedStatic!=((access&Opcodes.ACC_STATIC)!=0))failures.add(source+": static mismatch "+owner+"."+name);
        if(method && itf!=((i.access&Opcodes.ACC_INTERFACE)!=0))failures.add(source+": class/interface mismatch "+owner+"."+name);
    }
    private void constant(String source,Object value) {
        if(value instanceof Type t)type(source,t);
        if(value instanceof Handle h) {
            int opcode=switch(h.getTag()) {
                case Opcodes.H_GETFIELD -> Opcodes.GETFIELD; case Opcodes.H_GETSTATIC -> Opcodes.GETSTATIC;
                case Opcodes.H_PUTFIELD -> Opcodes.PUTFIELD; case Opcodes.H_PUTSTATIC -> Opcodes.PUTSTATIC;
                case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
                case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
                case Opcodes.H_INVOKESPECIAL,Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
                default -> Opcodes.INVOKEVIRTUAL;
            };
            reference(source,opcode,h.getOwner(),h.getName(),h.getDesc(),h.getTag()>Opcodes.H_PUTSTATIC,h.isInterface());
        }
        if(value instanceof ConstantDynamic c) {
            type(source,Type.getType(c.getDescriptor()));constant(source,c.getBootstrapMethod());
            for(int n=0;n<c.getBootstrapMethodArgumentCount();n++)constant(source,c.getBootstrapMethodArgument(n));
        }
    }
    private void inspect(String source,byte[] bytes) {
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public void visit(int v,int access,String name,String signature,String parent,String[] interfaces) {
                if(parent!=null)type(source,Type.getObjectType(parent));
                for(String i:interfaces)type(source,Type.getObjectType(i));
            }
            @Override public FieldVisitor visitField(int a,String n,String d,String s,Object value) { type(source,Type.getType(d));return null; }
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] exceptions) {
                type(source,Type.getMethodType(d));
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int op,String o,String n,String d,boolean itf) {reference(source,op,o,n,d,true,itf);}
                    @Override public void visitFieldInsn(int op,String o,String n,String d) {reference(source,op,o,n,d,false,false);}
                    @Override public void visitTypeInsn(int op,String t) {type(source,t.startsWith("[")?Type.getType(t):Type.getObjectType(t));}
                    @Override public void visitLdcInsn(Object value) {constant(source,value);}
                    @Override public void visitInvokeDynamicInsn(String n,String d,Handle b,Object... args) {
                        type(source,Type.getMethodType(d));constant(source,b);for(Object arg:args)constant(source,arg);
                    }
                    @Override public void visitMultiANewArrayInsn(String d,int dims) {type(source,Type.getType(d));}
                    @Override public void visitTryCatchBlock(Label s,Label e,Label h,String t) {if(t!=null)type(source,Type.getObjectType(t));}
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
    }
}
