package org.kantega.missinglink.findthemissinglink;

import java.util.HashMap;
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

    public Map<String, Set<String>> getClassesMissing() {
        Map<String, Set<String>> missingClasses = new HashMap<>(classesReferenced);
        Set<String> classes = missingClasses.keySet();
        classes.removeAll(classesVisited);

        Set<String> ignoredPackages = getIgnoredPackages(classes);
        classes.removeAll(ignoredPackages);

        return missingClasses;
    }

    public Map<String, Set<String>> getMethodsMissing() {
        Map<String, Set<String>> missingMethods = new HashMap<>(methodsReferenced);
        Set<String> methods = missingMethods.keySet();
        methods.removeAll(methodsVisited);

        Set<String> ignoredPackages = getIgnoredPackages(methods);
        methods.removeAll(ignoredPackages);

        return missingMethods;
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
}
