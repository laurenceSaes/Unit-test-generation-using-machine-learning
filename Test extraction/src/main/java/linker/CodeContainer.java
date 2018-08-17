package linker;

import parser.JavaSourceMapper;

public class CodeContainer {
    private String plainCode;
    private String SBTCode;

    public CodeContainer(JavaMethodReference codeReference, JavaSourceMapper javaSourceMapper) {
        this.plainCode = javaSourceMapper.getMethodCode(codeReference);
        this.SBTCode = javaSourceMapper.getMethodCodeSBT(codeReference);
    }

    public CodeContainer(String methodClass, String methodName, JavaSourceMapper javaSourceMapper) {
        JavaMethodReference codeReference = new JavaMethodReference(methodClass, methodName);
        this.plainCode = javaSourceMapper.getMethodCode(codeReference);
        this.SBTCode = javaSourceMapper.getMethodCodeSBT(codeReference);
    }

    public String getPlainCode() {
        return plainCode;
    }

    public String getSBTCode() {
        return SBTCode;
    }
}