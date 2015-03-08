package org.kantega.findthemissinglink;

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
}
