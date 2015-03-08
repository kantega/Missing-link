package org.kantega.findthemissinglink;

import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

public class ClassFileVisitor {
    private final Set<String> classesVisited = new HashSet<>();
    private final Set<String> classesReferenced = new HashSet<>();
    private final Set<String> methodsReferenced = new HashSet<>();
    private final Set<String> methodsVisited = new HashSet<>();

    // Reference to primitives
    private final Set<String> ignoredClasses = new HashSet<>(asList("I", "V", "Z", "B", "C", "S", "D", "F", "J",
            "[I", "V", "[Z", "[B", "[C", "[S", "[D", "[F", "[J"));

    public static void main(String[] args) throws IOException {
        Report report = new ClassFileVisitor().generateReportForJar("/home/marv/.m2/repository/org/kantega/openaksess/openaksess-core/7.8.18/openaksess-core-7.8.18.jar");
        report.getClassesReferenced();
    }

    public Report generateReportForJar(String jarfile) throws IOException {
        URI uri = URI.create("jar:file:" + jarfile);
        try (FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
            for (final Path path : zipfs.getRootDirectories()) {
                Files.walkFileTree(path, Collections.<FileVisitOption>emptySet(), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if(file.getFileName().toString().endsWith(".class")){
                            try(InputStream is = Files.newInputStream(file)){
                                ClassReader cr = new ClassReader( is );
                                SignatureVisitor v = new SignatureVisitor();
                                cr.accept( v, 0 );
                            }

                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

        }
        return new Report(classesVisited, classesReferenced, methodsVisited, methodsReferenced);
    }


    private class SignatureVisitor extends ClassVisitor {
        private String className;

        public SignatureVisitor() {
            super(Opcodes.ASM5);
        }

        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            className = name;
            classesVisited.add(name);
            addReferencedClassIfNotIgnored(superName);
            if(interfaces.length > 0){
                addReferencedClassesIfNotIgnored(interfaces);
            }
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            methodsVisited.add(className + "." + name + desc);
            if (exceptions != null && exceptions.length > 0) {
                addReferencedClassesIfNotIgnored(exceptions);
            }
            return new MethodVisitor(Opcodes.ASM5) {
                @Override
                public void visitParameter(String name, int access) {
                    super.visitParameter(name, access);
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return super.visitAnnotationDefault();
                }

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitParameterAnnotation(parameter, desc, visible);
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    super.visitAttribute(attr);
                }

                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addReferencedClassIfNotIgnored(type);
                    super.visitTypeInsn(opcode, type);
                }

                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                    addReferencedClassIfNotIgnored(desc);
                    super.visitFieldInsn(opcode, owner, name, desc);
                }


                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (notIgnoredClass(owner)) {
                        methodsReferenced.add(owner + "." + name + desc);
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }

                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
                    super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                }

                @Override
                public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
                    return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
                }

                @Override
                public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                    addReferencedClassIfNotIgnored(desc);
                    super.visitLocalVariable(name, desc, signature, start, end, index);
                }

                @Override
                public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    return super.visitTryCatchAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                    if (type != null) {
                        addReferencedClassIfNotIgnored(type);
                    }
                    super.visitTryCatchBlock(start, end, handler, type);
                }

                @Override
                public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    return super.visitInsnAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public void visitMultiANewArrayInsn(String desc, int dims) {
                    super.visitMultiANewArrayInsn(desc, dims);
                }
            };
        }

        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            addReferencedClassIfNotIgnored(desc);
            return new FieldVisitor(Opcodes.ASM5) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    super.visitAttribute(attr);
                }
            };
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
            super.visitOuterClass(owner, name, desc);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return new AnnotationVisitor(Opcodes.ASM5) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                }

                @Override
                public void visitEnum(String name, String desc, String value) {
                    super.visitEnum(name, desc, value);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String desc) {
                    return super.visitAnnotation(name, desc);
                }
            };
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
            return new AnnotationVisitor(Opcodes.ASM5) {
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                }

                @Override
                public void visitEnum(String name, String desc, String value) {
                    super.visitEnum(name, desc, value);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String desc) {
                    return super.visitAnnotation(name, desc);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return super.visitArray(name);
                }

            };
        }

        @Override
        public void visitAttribute(Attribute attr) {
            super.visitAttribute(attr);
        }

        private void addReferencedClassIfNotIgnored(String classnameReference) {
            String classname = normalizeClassName(classnameReference);
            if(notIgnoredClass(classname)){
                classesReferenced.add(classname);
            }
        }

        private void addReferencedClassesIfNotIgnored(String... classname) {
            for (String s : classname) {
                addReferencedClassIfNotIgnored(s);
            }
        }
    }

    private boolean notIgnoredClass(String classname) {
        return !ignoredClasses.contains(classname) && !classname.startsWith("java/");
    }

    private String normalizeClassName(String classnameReference) {
        String classname = classnameReference;
        if(classname.length() > 1 && classname.startsWith("L")){
            classname = classname.substring(1);
        } else if(classname.length() > 2 && classname.startsWith("[L")){
            classname = classname.substring(2);
        }
        return classname;
    }
}
