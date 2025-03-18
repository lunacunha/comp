package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

public class JmmSymbolTableBuilder {

    // In case we want to already check for some semantic errors during symbol table building.
    private List<Report> reports;

    public List<Report> getReports() {
        return reports;
    }

    private static Report newError(JmmNode node, String message) {
        return Report.newError(
                Stage.SEMANTIC,
                node.getLine(),
                node.getColumn(),
                message,
                null);
    }

    public JmmSymbolTable build(JmmNode root) {

        reports = new ArrayList<>();

        var imports = buildImports(root);

        // TODO: After your grammar supports more things inside the program (e.g., imports) you will have to change this
        var classDecl = root.getChildren(Kind.CLASS_DECL.getNodeName()).get(0);
        SpecsCheck.checkArgument(classDecl.getKind().equals(Kind.CLASS_DECL.getNodeName()),
                () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");
        String superClass = classDecl.hasAttribute("extends") ? classDecl.get("extends") : null;

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, superClass, imports, fields, methods, returnTypes, params, locals);
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var name = method.get("name");
            var returnType = parseType(method.getChildren("Type").get(0)); // Obtém o tipo correto
            map.put(name, returnType);
        }
        return map;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var name = method.get("name");
            var params = method.getChildren(Kind.PARAM.getNodeName()).stream()
                    .map(param -> new Symbol(parseType(param.getChildren("Type").get(0)), param.get("name")))
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
                    .map(varDecl -> new Symbol(parseType(varDecl.getChildren("Type").get(0)), varDecl.get("name")))
                    .toList();
            map.put(name, locals);
        }
        return map;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(Kind.METHOD_DECL.getNodeName()).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private List<String> buildImports(JmmNode root) {
        List<String> imports = new ArrayList<>();
        for (var importDecl : root.getChildren(Kind.IMPORT_DECL.getNodeName())) {
            imports.add(importDecl.get("name"));
        }
        return imports;
    }

    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();
        for (var field : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            fields.add(new Symbol(parseType(field.getChildren("Type").get(0)), field.get("name")));
        }
        return fields;
    }

    private Type parseType(JmmNode typeNode) {
        String typeName = typeNode.get("name");
        boolean isArray = typeNode.hasAttribute("array") && typeNode.get("array").equals("true");
        return new Type(typeName, isArray);
    }

}
