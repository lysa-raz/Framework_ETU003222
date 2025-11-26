package com.itu.ETU003222;

import java.lang.reflect.Method;

import com.itu.ETU003222.model.Mapping;
public class Invoker {
    
    public static Object invoke(Mapping mapping) throws Exception {
        Class<?> clazz = Class.forName(mapping.getClassName());
        
        Object instance = clazz.getDeclaredConstructor().newInstance();
        
        Method method = clazz.getDeclaredMethod(mapping.getMethodName());
        
        return method.invoke(instance);
    }
}