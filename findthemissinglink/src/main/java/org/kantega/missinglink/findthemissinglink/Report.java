package org.kantega.missinglink.findthemissinglink;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Report {
    private final Set<String> classesVisited;
    private final Set<String> classesReferenced;
    private final Set<String> methodsVisited;
    private final Set<String> methodsReferenced;
    private final List<String> ignorePackages;

    public Report(Set<String> classesVisited, Set<String> classesReferenced, Set<String> methodsVisited, Set<String> methodsReferenced, List<String> ignorePackages) {
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
        return classesReferenced;
    }

    public Set<String> getMethodsVisited() {
        return methodsVisited;
    }

    public Set<String> getMethodsReferenced() {
        return methodsReferenced;
    }

    public Set<String> getClassesMissing() {
        Set<String> missingClasses = new HashSet<>(classesReferenced);
        missingClasses.removeAll(classesVisited);
        return removeIgnoredPackages(missingClasses);
    }

    public Set<String> getMethodsMissing() {
        Set<String> missingMethods = new HashSet<>(methodsReferenced);
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
