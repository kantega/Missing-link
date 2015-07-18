package org.kantega.missinglink.findthemissinglink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Report {
    private final Map<String, Set<String>> classesReferenced;
    private final Map<String, Set<String>> methodsReferenced;
    private final Set<String> annotationsReferenced;
    private final Set<String> classesVisited;
    private final Set<String> methodsVisited;
    private final List<String> ignorePackages;
    private final List<String> ignoreReferencesInPackages;
    private final boolean ignoreAnnotationReferences;

    public Report(Set<String> classesVisited, Map<String, Set<String>> classesReferenced, Set<String> methodsVisited, Map<String, Set<String>> methodsReferenced, Set<String> annotationsReferenced, List<String> ignorePackages, List<String> ignoreReferencesInPackages, boolean ignoreAnnotationReferences) {
        this.classesVisited = classesVisited;
        this.classesReferenced = classesReferenced;
        this.methodsVisited = methodsVisited;
        this.methodsReferenced = methodsReferenced;
        this.annotationsReferenced = annotationsReferenced;
        this.ignorePackages = ignorePackages;
        this.ignoreReferencesInPackages = ignoreReferencesInPackages;
        this.ignoreAnnotationReferences = ignoreAnnotationReferences;
    }

    /**
     * @return FQN of all classes visited when scanning the classpath.
     */
    public Set<String> getClassesVisited() {
        return classesVisited;
    }

    /**
     * @return FQN of all classes referenced by visited classes and where they where referenced.
     */
    public Map<String, Set<String>> getClassesReferenced() {
        return classesReferenced;
    }

    /**
     * @return FQN of all methods visited when scanning the classpath.
     */
    public Set<String> getMethodsVisited() {
        return methodsVisited;
    }

    /**
     * @return FQN of all methods referenced in methods of visited classes and where they where referenced.
     */
    public Map<String, Set<String>> getMethodsReferenced() {
        return methodsReferenced;
    }

    /**
     * @return Classes that are referenced but have not been visited, and the classes
     * where they was referenced.
     * Classes whose package starts with an entry in {@code ignoredPackages} are removed.
     * Missing classes referenced in an entry in {@code ignoreReferencesInPackages} are removed.
     */
    public Map<String, Set<String>> getClassesMissing() {
        Map<String, Set<String>> missingClasses = new HashMap<>(classesReferenced);
        Set<String> classes = missingClasses.keySet();
        classes.removeAll(classesVisited);
        if(ignoreAnnotationReferences) {
            classes.removeAll(annotationsReferenced);
        }

        Set<String> ignoredPackages = getIgnoredPackages(classes);
        classes.removeAll(ignoredPackages);

        if(!ignoreReferencesInPackages.isEmpty()){
            missingClasses = removeMissingReferencesInIgnoredPackages(missingClasses);
        }

        return missingClasses;
    }

    /**
     * @return Methods that are referenced but have not been visited, and the methods
     * where they was referenced.
     * Methods whose package starts with an entry in {@code ignoredPackages} are removed.
     * Missing methods referenced in an entry in {@code ignoreReferencesInPackages} are removed.
     */
    public Map<String, Set<String>> getMethodsMissing() {
        Map<String, Set<String>> missingMethods = new HashMap<>(methodsReferenced);
        Set<String> methods = missingMethods.keySet();
        methods.removeAll(methodsVisited);

        Set<String> ignoredPackages = getIgnoredPackages(methods);
        methods.removeAll(ignoredPackages);

        if(!ignoreReferencesInPackages.isEmpty()){
            missingMethods = removeMissingReferencesInIgnoredPackages(missingMethods);

        }
        return missingMethods;
    }


    private Map<String, Set<String>> removeMissingReferencesInIgnoredPackages(Map<String, Set<String>> missingClasses) {
        return missingClasses
                .entrySet()
                .stream()
                .map(e -> new Reference(e.getKey(), getNonIgnoredReferenced(e.getValue())))
                .filter(Reference::isNotEmpty)
                .collect(Collectors.toMap(Reference::getReference, Reference::getReferencedFrom));
    }

    private Set<String> getIgnoredPackages(Set<String> missingClasses) {
        return missingClasses.stream().filter(s -> {
            for (String ignorePackage : ignorePackages) {
                if (s.startsWith(ignorePackage)) {
                    return true;
                }
            }
            return false;
        }).collect(Collectors.toSet());
    }

    private Set<String> getNonIgnoredReferenced(Set<String> references) {
        return references.stream().filter(s -> {
            for (String ignorePackage : ignoreReferencesInPackages) {
                if (s.startsWith(ignorePackage)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toSet());

    }

    private static class Reference {
        public final String reference;
        public final Set<String> referencedFrom;

        private Reference(String reference, Set<String> referencedFrom) {
            this.reference = reference;
            this.referencedFrom = referencedFrom;
        }

        public String getReference() {
            return reference;
        }

        public Set<String> getReferencedFrom() {
            return referencedFrom;
        }

        public boolean isNotEmpty(){
            return !referencedFrom.isEmpty();
        }
    }
}
