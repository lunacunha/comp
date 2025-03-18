package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.Objects;

public class JmmSymbolTableBuilder {

    private List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    public JmmSymbolTable build(JmmNode root) {
        reports = new ArrayList<>();

        var imports = buildImports(root);
        var classDecl = root.getChildren(Kind.CLASS_DECL.getNodeName()).get(0);

        SpecsCheck.checkArgument(classDecl.getKind().equals(Kind.CLASS_DECL.getNodeName()),
                () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        String superClass = classDecl.hasAttribute("superClass") ? classDecl.get("superClass") : null;

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superClass, imports, fields, methods, returnTypes, params, locals);
    }

    private List<String> buildImports(JmmNode root) {
        List<String> imports = new ArrayList<>();
        for (var importDecl : root.getChildren(Kind.IMPORT_DECL.getNodeName())) {
            List<JmmNode> parts = importDecl.getChildren("ImportPart");
            if (!parts.isEmpty()) {
                String fullImport = parts.stream()
                        .map(node -> node.get("name"))
                        .collect(Collectors.joining("."));
                imports.add(fullImport);
            } else if (importDecl.hasAttribute("name")) {
                imports.add(importDecl.get("name"));
            }
        }
        return imports;
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();
        for (var field : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            if (!field.hasAttribute("name")) continue;
            var fields_name = field.get("name");
            var children = field.getChildren();
            Type fields_type = TypeUtils.convertType(children.get(0));
            fields.add(new Symbol(fields_type, fields_name));
        }
        return fields;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (method.hasAttribute("name")) {
                methods.add(method.get("name"));
            } else {
                methods.add("main");
            }
        }
        return methods;
    }


    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> returnTypes = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var returnType = method.getChildren("Type").isEmpty() ? new Type("void", false) : parseType(method.getChildren("Type").get(0));
            returnTypes.put(method.get("name"), returnType);
        }
        return returnTypes;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (!method.hasAttribute("name")) continue;
            var name = method.get("name");
            var params = method.getChildren(Kind.PARAM.getNodeName()).stream()
                    .map(param -> {
                        if (!param.getChildren(Kind.TYPE.getNodeName()).isEmpty() && param.hasAttribute("name")) {
                            return new Symbol(parseType(param.getChildren(Kind.TYPE.getNodeName()).get(0)), param.get("name"));
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            map.put(name, params);
        }
        return map;
    }


    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var name = method.get("name");
            var locals = method.getChildren(Kind.VAR_DECL.getNodeName()).stream()
                    .filter(varDecl -> varDecl.hasAttribute("name"))
                    .map(varDecl -> new Symbol(parseType(varDecl.getChildren("Type").get(0)), varDecl.get("name")))
                    .toList();
            map.put(name, locals);
        }
        return map;
    }

    private Type parseType(JmmNode typeNode) {
        switch (typeNode.getKind()) {
            case "IntType":
                return new Type("int", false);
            case "BooleanType":
                return new Type("boolean", false);
            case "IntArrayType":
                return new Type("int", true);
            default:
                return new Type(typeNode.hasAttribute("name") ? typeNode.get("name") : "unknown", typeNode.hasAttribute("array"));
        }
    }
}
