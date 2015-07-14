package org.kantega.findthemissinglink;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.TypePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

public class ClassFileVisitor {
    private static final Logger log = LoggerFactory.getLogger(ClassFileVisitor.class);

    private final Set<String> classesVisited = new HashSet<>();
    private final Set<String> classesReferenced = new HashSet<>();
    private final Set<String> methodsReferenced = new HashSet<>();
    private final Set<String> methodsVisited = new HashSet<>();
    private final Map<String, Set<String>> methodsByClass = new HashMap<>();
    private final Map<String, String> classParents = new HashMap<>();
    private final Map<String, List<String>> subclassesByParent = new HashMap<>();


    // Reference to primitives
    private final Set<String> ignoredClasses = new HashSet<>(
            asList(
                    "I", "V", "Z", "B", "C", "S", "D", "F", "J",
                    "[I", "[Z", "[B", "[C", "[S", "[D", "[F", "[J",
                    "[[I", "[[Z", "[[B", "[[C", "[[S", "[[D", "[[F", "[[J"
            ));

    public ClassFileVisitor() throws IOException, URISyntaxException {
        List<String> bootClasspath = getBootClasspath()
                .stream() // sunrsasign.jar is most likely listed on boot classpath, but does not exist.
                .filter(s -> !s.endsWith("sunrsasign.jar"))
                .collect(Collectors.toList());
        Report bootReport = generateReportForJar(bootClasspath);
        classesVisited.addAll(bootReport.getClassesVisited());
        methodsVisited.addAll(bootReport.getMethodsVisited());
    }

    public Report generateReportForJar(List<String> jarfiles) throws IOException, URISyntaxException {
        for (String jarfile : jarfiles) {
            if (jarfile.endsWith(".jar")) {
                URI uri = URI.create("jar:file:" + jarfile);
                try (FileSystem zipfs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
                    for (final Path path : zipfs.getRootDirectories()) {
                        Files.walkFileTree(path, Collections.<FileVisitOption>emptySet(), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (file.getFileName().toString().endsWith(".class")) {
                                    try (InputStream is = Files.newInputStream(file)) {
                                        ClassReader cr = new ClassReader(is);
                                        SignatureVisitor v = new SignatureVisitor();
                                        cr.accept(v, 0);
                                    }

                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    }

                } catch (Exception e){
                    // woops
                    log.warn("Woops", e);
                }
            }
        }
        handleInheritance("java/lang/Object");
        return new Report(classesVisited, classesReferenced, methodsVisited, methodsReferenced);
    }

    private void handleInheritance(String parent) {
        Set<String> superMethods = methodsByClass.get(parent);
        List<String> subclasses = subclassesByParent.get(parent);
        if (nonNull(subclasses)) {
            for (String subclass : subclasses) {
                if (nonNull(superMethods)) {
                    methodsVisited.addAll(superMethods
                            .stream()
                            .map(superMethod -> subclass + "." + superMethod)
                            .collect(Collectors.toList()));
                    Set<String> subclassMethods = methodsByClass.computeIfAbsent(subclass, s -> new HashSet<>());
                    if (subclassMethods != null) {
                        subclassMethods.addAll(superMethods);
                    }
                }
                handleInheritance(subclass);
            }
        }
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
            if (superName != null) {
                addReferencedClassIfNotIgnored(superName);
            }
            registerInheritance(className, superName);
            if(interfaces.length > 0){
                addReferencedClassesIfNotIgnored(interfaces);
                for (String implementedInterface : interfaces) {
                    registerInheritance(className, implementedInterface);
                }
            }
        }

        private void registerInheritance(String className, String superName) {
            classParents.put(className, superName);
            Collection<String> mapping = subclassesByParent.computeIfAbsent(superName, s -> new LinkedList<>());
            mapping.add(className);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            String method = name + desc;
            methodsVisited.add(className + "." + method);
            addClassMethodMapping(className, method);
            if (exceptions != null && exceptions.length > 0) {
                addReferencedClassesIfNotIgnored(exceptions);
            }
            return new MethodVisitor(Opcodes.ASM5) {

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitTypeAnnotation(typeRef, typePath, desc, visible);
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                    addReferencedClassIfNotIgnored(desc);
                    return super.visitParameterAnnotation(parameter, desc, visible);
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
                        methodsReferenced.add(normalizeClassName(owner) + "." + name + desc);
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }

                @Override
                public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                    addReferencedClassIfNotIgnored(desc);
                    super.visitLocalVariable(name, desc, signature, start, end, index);
                }

                @Override
                public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                    if (type != null) {
                        addReferencedClassIfNotIgnored(type);
                    }
                    super.visitTryCatchBlock(start, end, handler, type);
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
            };
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

    private void addClassMethodMapping(String className, String method) {
        Set<String> methodsForClass = methodsByClass.computeIfAbsent(className, s -> new HashSet<>());
        methodsForClass.add(method);
    }

    private boolean notIgnoredClass(String classname) {
        return !ignoredClasses.contains(classname)
                && !classname.startsWith("java/");
    }

    /*
      Extracts class name from references.
       Lorg/slf4j/Logger; -> org/slf4j/Logger
     */
    private String normalizeClassName(String classnameReference) {
        String classname = classnameReference;
        if(classname.length() > 1 && classname.startsWith("L")){
            classname = classname.substring(1, classnameReference.length() - 1);
        } else if(classname.length() > 2 && classname.startsWith("[L")){
            classname = classname.substring(2, classnameReference.length() - 1);
        } else if(classname.length() > 2 && classname.startsWith("[[L")){
            classname = classname.substring(3, classnameReference.length() - 1);
        }
        return classname;
    }

    // From animal-sniffer/java-boot-classpath-detector/src/main/java/org/codehaus/mojo/animal_sniffer/jbcpd/ShowClassPath.java
    public List<String> getBootClasspath(){
        String cp = System.getProperty("sun.boot.class.path");
        if (cp != null) {
            return Arrays.asList(cp.split(":"));
        }
        cp = System.getProperty("java.boot.class.path");
        if (cp != null) {
            return Arrays.asList(cp.split(":"));
        }
        Enumeration i = System.getProperties().propertyNames();
        String name = null;
        while (i.hasMoreElements()) {
            String temp = (String) i.nextElement();
            if (temp.contains(".boot.class.path")) {
                if (name == null) {
                    name = temp;
                } else {
                    System.err.println("Cannot auto-detect boot class path " + System.getProperty("java.version"));
                    System.exit(1);
                }
            }
        }
        return Arrays.asList(name.split(":"));
    }
}
