package com.yjw.bigtool.agent;

import java.util.function.Function;

public class Tool {

    private String name;

    private String description;

    private Function<Object[], Object> function;

    private Class<?>[] parameterTypes;

    public Tool(String name, String description, Function<Object[], Object> function, Class<?>[] parameterTypes) {
        this.name = name;
        this.description = description;
        this.function = function;
        this.parameterTypes = parameterTypes;
    }

    public Object execute(Object... args) {
        return function.apply(args);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    @Override
    public String toString() {
        return "Tool{name='" + name + "', description='" + description + "'}";
    }


}
