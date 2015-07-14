package org.kantega.missinglink.findthemissinglink;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Report {
    private final Set<String> classesVisited;
    private final Set<String> classesReferenced;
    private final Set<String> methodsVisited;
    private final Set<String> methodsReferenced;

    public Report(Set<String> classesVisited, Set<String> classesReferenced, Set<String> methodsVisited, Set<String> methodsReferenced) {
        this.classesVisited = classesVisited;
        this.classesReferenced = classesReferenced;
        this.methodsVisited = methodsVisited;
        this.methodsReferenced = methodsReferenced;
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

    public Collection<String> getClassesMissing() {
        Set<String> missingClasses = new HashSet<>(classesReferenced);
        missingClasses.removeAll(classesVisited);
        return missingClasses;
    }

    public Set<String> getMethodsMissing() {
        Set<String> missingMethods = new HashSet<>(methodsReferenced);
        missingMethods.removeAll(methodsVisited);
        return missingMethods;
    }
}
