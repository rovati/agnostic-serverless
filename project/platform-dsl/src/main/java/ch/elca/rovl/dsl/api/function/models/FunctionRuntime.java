package ch.elca.rovl.dsl.api.function.models;

public class FunctionRuntime {
    
    Language language;
    Version version;

    private FunctionRuntime(Language l, Version v) {
        this.language = l;
        this.version = v;
    }
    
    public static FunctionRuntime java(JavaVersion v) {
        return new FunctionRuntime(Language.JAVA, v);
    }

    public static FunctionRuntime node(NodeVersion v) {
        return new FunctionRuntime(Language.NODE, v);
    }

    public static FunctionRuntime python(PythonVersion v) {
        return new FunctionRuntime(Language.PYTHON, v);
    }

    public static FunctionRuntime dotnet(DotNetVersion v) {
        return new FunctionRuntime(Language.DOTNET, v);
    }

    public enum Language {
        JAVA,
        NODE,
        PYTHON,
        DOTNET
    }

    public enum JavaVersion implements Version {
        V_8,
        V_11,
        V_17
    }

    public enum NodeVersion implements Version {
        V_20,
    }

    public enum PythonVersion implements Version {
        //V_20,
    }

    public enum DotNetVersion implements Version {
        //V_20,
    }

    private interface Version {}

}
