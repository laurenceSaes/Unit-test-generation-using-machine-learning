package linker;

import java.util.Objects;

public class TestMethodCall {
    private JavaMethodReference reference;
    private String unitTestName;

    public TestMethodCall(String unitTestName, JavaMethodReference reference) {
        this.reference = reference;
        this.unitTestName = unitTestName;
    }

    public JavaMethodReference getReference() {
        return reference;
    }

    public String getUnitTestName() {
        return unitTestName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestMethodCall)) return false;
        TestMethodCall that = (TestMethodCall) o;
        return Objects.equals(getReference(), that.getReference()) &&
                Objects.equals(getUnitTestName(), that.getUnitTestName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReference(), getUnitTestName());
    }
}
