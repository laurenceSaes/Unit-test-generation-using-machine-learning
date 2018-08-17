package linker;

import java.util.*;

public class TestMethodCallPerClassManager {

    private Map<String, Set<TestMethodCall>> testMethodCallList = new HashMap<>();

    public void addCall(String unitTestName, JavaMethodReference reference) {
        TestMethodCall newCall = new TestMethodCall(unitTestName, reference);
        if(testMethodCallList.containsKey(reference.getClassName())) {
            Set<TestMethodCall> methodCalls = testMethodCallList.get(reference.getClassName());
            methodCalls.add(newCall);
        } else {
            testMethodCallList.put(reference.getClassName(), new HashSet<>(Arrays.asList(newCall)));
        }
    }

    public Set<String> getClasses() {
        return this.testMethodCallList.keySet();
    }

    public TestMethodCallPerClassManager filterOnUnitTestNames(String unitTestName) {
        TestMethodCallPerClassManager filteredReference = new TestMethodCallPerClassManager();

        for(String className : getClasses()) {
            Set<TestMethodCall> testMethodCalls = this.testMethodCallList.get(className);
            for(TestMethodCall testMethodCall : testMethodCalls) {
                if(testMethodCall.getUnitTestName().equals(unitTestName)) {
                    filteredReference.addCall(className, testMethodCall.getReference());
                }
            }
        }

        return filteredReference;
    }

    public TestMethodCallPerClassManager filterCallsToClass(String className) {
        TestMethodCallPerClassManager filteredReferences = new TestMethodCallPerClassManager();
        for(TestMethodCall testMethodCall : this.getCallsToClass(className)) {
            filteredReferences.addCall(testMethodCall.getUnitTestName(), testMethodCall.getReference());
        }

        return filteredReferences;
    }

    public Set<TestMethodCall> getCallsToClass(String className) {
        return this.testMethodCallList.get(className);
    }

    public Set<JavaMethodReference> getAllReferences() {
        Set<JavaMethodReference> calls = new HashSet();
        for(Set<TestMethodCall> callList : this.testMethodCallList.values()) {
            for(TestMethodCall call : callList) {
                calls.add(call.getReference());
            }
        }
        return calls;
    }
}
