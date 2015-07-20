package org.kantega.missinglink.findthemissinglink;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class CallTreeReport {
    public static List<CallNode> generateCallTree(Map<String, Set<String>> missing, Map<String, Set<String>> allReferences){
        return getCallNodes(missing.keySet(), allReferences);
    }

    private static List<CallNode> getCallNodes(Set<String> calls, Map<String, Set<String>> allReferences) {
        if(calls.isEmpty()){
            return Collections.emptyList();
        } else {
           return calls.stream()
                    .map(s -> {
                        Set<String> referencesToCall = allReferences.computeIfAbsent(s, s1 -> Collections.<String>emptySet());
                        List<CallNode> callNodes = getCallNodes(
                                referencesToCall.stream()
                                        .filter(Predicate.isEqual(s).negate()) // remove self references.
                                        .collect(Collectors.toSet()),
                                allReferences
                        );
                        return new CallNode(s, callNodes);
                    }).collect(Collectors.toList());
        }
    }

    public static List<String> getCallTreeAsList(List<CallNode> callNodes){
        List<String> calls = new LinkedList<>();
        for (CallNode callNode : callNodes) {
            calls.addAll(callNode.toStrings());
        }
        return calls;
    }


    public static class CallNode {
        private final String reference;
        private final List<CallNode> referencesBy;

        public CallNode(String reference, List<CallNode> referencesBy) {
            this.reference = reference;
            this.referencesBy = referencesBy;
        }

        public String getReference() {
            return reference;
        }

        public List<CallNode> getReferencesBy() {
            return referencesBy;
        }

        public List<String> toStrings(){
            List<String> referenceCalls = new LinkedList<>();
            if (referencesBy.isEmpty()) {
                referenceCalls.add(reference);
            } else {
                for (CallNode callNode : referencesBy) {
                    List<String> referenceCall = callNode.toStrings();
                    if(referenceCall.isEmpty()){
                        referenceCalls.add(reference + " <- " + callNode.reference);
                    } else {
                        for (String r : referenceCall) {
                            referenceCalls.add(reference + " <- " + callNode.reference + r);
                        }

                    }
                }
            }
            return referenceCalls;
        }
    }
}
