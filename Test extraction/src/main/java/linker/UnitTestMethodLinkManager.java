package linker;

import parser.JavaSourceMapper;

import java.util.*;

public class UnitTestMethodLinkManager {

    private HashMap<String, List<MethodLink>> unitTestLinks = new HashMap<>();

    private JavaSourceMapper javaSourceMapper;

    public UnitTestMethodLinkManager(JavaSourceMapper javaSourceMapper) {
        this.javaSourceMapper = javaSourceMapper;
    }

    public void addLink(JavaMethodReference unitTest, TestMethodCall call) {
        String classUnderTest = call.getReference().getClassName();
        MethodLink newLink = new MethodLink(unitTest, call.getReference());

        if(unitTestLinks.containsKey(classUnderTest)) {
            List<MethodLink> methodLinks = unitTestLinks.get(classUnderTest);
            methodLinks.add(newLink);
        } else {
            unitTestLinks.put(classUnderTest, new ArrayList<>(Arrays.asList(newLink)));
        }
    }

    public void addLink(JavaMethodReference unitTest, Set<TestMethodCall> calls) {
        for(TestMethodCall call : calls) {
            this.addLink(unitTest, call);
        }
    }

    public List<MethodLink> getParserContainedClassWithMostLinks() {
        int largestSize = 0;
        List<MethodLink> largestList = null;

        for(Map.Entry<String,List<MethodLink>> list : unitTestLinks.entrySet()) {
            if(!this.javaSourceMapper.isClassContained(list.getKey()))
                continue;

            if(largestSize < list.getValue().size()) {
                largestSize = list.getValue().size();
                largestList = list.getValue();
            }
        }

        return largestList;
    }


    public class MethodLink{
        private JavaMethodReference unitTest;
        private JavaMethodReference method;

        public MethodLink(JavaMethodReference unitTest, JavaMethodReference method) {
            this.unitTest = unitTest;
            this.method = method;
        }

        public JavaMethodReference getUnitTest() {
            return unitTest;
        }

        public JavaMethodReference getMethod() {
            return method;
        }
    }
}
