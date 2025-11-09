package com.itu.ETU003222;

public class Mapping {
    public String className;
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String methodName;
    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Mapping(String c, String m) {
        className = c;
        methodName = m;
    }
}
