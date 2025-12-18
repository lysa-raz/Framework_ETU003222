package com.itu.ETU003222.model;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

public class Mapping {
    private String className;
    private String methodName;
    private String urlPattern;
    private Pattern regex;
    private String httpMethod = "GET";
    private boolean isRestApi = false; // NOUVEAU
    
    public Mapping() {
    }
    
    public Mapping(String className, String methodName, String urlPattern) {
        this.className = className;
        this.methodName = methodName;
        this.urlPattern = urlPattern;
        this.regex = compilePattern(urlPattern);
    }
    
    private Pattern compilePattern(String urlPattern) {
        String regexPattern = urlPattern.replaceAll("\\{[^/]+\\}", "([^/]+)");
        return Pattern.compile("^" + regexPattern + "$");
    }
    
    public boolean matches(String url) {
        if (regex == null) return false;
        return regex.matcher(url).matches();
    }
    
    public Map<String, String> extractParams(String url) {
        Map<String, String> params = new HashMap<>();
        if (regex == null) return params;
        
        Matcher patternMatcher = regex.matcher(url);
        
        if (patternMatcher.matches()) {
            java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile("\\{([^/]+)\\}");
            Matcher paramNameMatcher = paramPattern.matcher(urlPattern);
            
            int groupIndex = 1;
            while (paramNameMatcher.find()) {
                String paramName = paramNameMatcher.group(1);
                String paramValue = patternMatcher.group(groupIndex++);
                params.put(paramName, paramValue);
            }
        }
        
        return params;
    }
    
    // Getters et setters
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public String getUrlPattern() {
        return urlPattern;
    }
    
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
        this.regex = compilePattern(urlPattern);
    }
    
    public Pattern getRegex() {
        return regex;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    // NOUVEAU
    public boolean isRestApi() {
        return isRestApi;
    }
    
    public void setRestApi(boolean isRestApi) {
        this.isRestApi = isRestApi;
    }
}