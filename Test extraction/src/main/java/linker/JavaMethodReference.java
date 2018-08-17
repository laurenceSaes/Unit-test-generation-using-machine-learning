package linker;

import java.io.Serializable;
import java.util.Objects;

public class JavaMethodReference implements Serializable {

    private String className;
    private String methodName;

    public JavaMethodReference(String className, String methodName) {
        this.className = className.replace("/", ".");
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaMethodReference)) return false;
        JavaMethodReference javaMethodReference = (JavaMethodReference) o;
        return Objects.equals(getClassName(), javaMethodReference.getClassName()) &&
                Objects.equals(getMethodName(), javaMethodReference.getMethodName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClassName(), getMethodName());
    }

    @Override
    public String toString() {
        return className + " - " + methodName;
    }
}
