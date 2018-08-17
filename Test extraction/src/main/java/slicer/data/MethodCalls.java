package slicer.data;

import linker.JavaMethodReference;

import java.util.List;

public class MethodCalls {
    private String methodClass;
    private String methodName;
    private List<JavaMethodReference> calls;

    public MethodCalls(String methodClass, String methodName, List<JavaMethodReference> calls) {
        this.methodClass = methodClass;
        this.methodName = methodName;
        this.calls = calls;
    }

    public String getMethodClass() {
        return methodClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<JavaMethodReference> getCalls() {
        return calls;
    }
}
