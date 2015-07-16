package org.kantega.missinglink.findthemissinglink;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Report {
    private final Map<String, Set<String>> classesReferenced;
    private final Map<String, Set<String>> methodsReferenced;
    private final Set<String> classesVisited;
    private final Set<String> methodsVisited;
    private final List<String> ignorePackages;

    public Report(Set<String> classesVisited, Map<String, Set<String>> classesReferenced, Set<String> methodsVisited, Map<String, Set<String>> methodsReferenced, List<String> ignorePackages) {
        this.classesVisited = classesVisited;
        this.classesReferenced = classesReferenced;
        this.methodsVisited = methodsVisited;
        this.methodsReferenced = methodsReferenced;
        this.ignorePackages = ignorePackages;
    }

    public Set<String> getClassesVisited() {
        return classesVisited;
    }

    public Set<String> getClassesReferenced() {
        return classesReferenced.keySet();
    }

    public Set<String> getMethodsVisited() {
        return methodsVisited;
    }

    public Set<String> getMethodsReferenced() {
        return methodsReferenced.keySet();
    }

    public Set<String> getClassesMissing() {
        Set<String> missingClasses = new HashSet<>(classesReferenced.keySet());
        missingClasses.removeAll(classesVisited);
        return removeIgnoredPackages(missingClasses);
    }

    public Set<String> getMethodsMissing() {
        Set<String> missingMethods = new HashSet<>(methodsReferenced.keySet());
        missingMethods.removeAll(methodsVisited);
        return removeIgnoredPackages(missingMethods);
    }

    private Set<String> removeIgnoredPackages(Set<String> missingClasses) {
        return missingClasses.stream().filter(s -> {
            for (String ignorePackage : ignorePackages) {
                if (s.startsWith(ignorePackage)) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toSet());
    }
}
