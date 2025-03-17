package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.*;
import java.util.stream.Collectors;

public class JmmSymbolTable extends AJmmSymbolTable {

    private final List<String> imports;
    private final String className;
    private final String superClassName;
    private final List<String> methods;
    private final Map<String, Type> returnTypes;
    private final Map<String, List<Symbol>> params;
    private final List<Symbol> fields;
    private final Map<String, List<Symbol>> locals;


    public JmmSymbolTable(List<String> imports, String className,
                          String superClassName,
                          List<String> methods,
                          Map<String, Type> returnTypes,
                          Map<String, List<Symbol>> params,
                          List<Symbol> fields,
                          Map<String, List<Symbol>> locals) {
        this.imports = imports;
        this.className = className;
        this.superClassName = superClassName;
        this.methods = methods;
        this.returnTypes = returnTypes;
        this.params = params;
        this.fields = fields;
        this.locals = locals;
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }


    @Override
    public List<String> getMethods() {
        return methods;
    }


    @Override
    public Type getReturnType(String methodSignature) {
        // TODO: Simple implementation that needs to be expanded
        return returnTypes.getOrDefault(methodSignature, TypeUtils.newIntType());
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return params.getOrDefault(methodSignature, Collections.emptyList());
    }

    @Override
    public List<Symbol> getLocalVariables(String methodSignature) {
        return locals.getOrDefault(methodSignature, Collections.emptyList());
    }

    @Override
    public String toString() {
        return print();
    }

}
