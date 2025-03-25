package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.TypeUtils.convertType;

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
        for (JmmNode importDecl : root.getChildren(Kind.IMPORT_DECL.getNodeName())) {
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

        for (JmmNode varDecl : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            if (!varDecl.hasAttribute("name")) continue;

            Optional<JmmNode> typeNode = varDecl.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            if (typeNode.isEmpty()) {
                reports.add(Report.newError(Stage.SEMANTIC, varDecl.getLine(), varDecl.getColumn(),
                        "Field declaration missing type", null));
                continue;
            }

            Type fieldType = convertType(typeNode.get());
            fields.add(new Symbol(fieldType, varDecl.get("name")));
        }

        return fields;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            methods.add(method.hasAttribute("name") ? method.get("name") : "main");
        }
        return methods;
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> returnTypes = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.hasAttribute("name") ? method.get("name") : "main";

            Optional<JmmNode> typeNode = method.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            Type returnType = typeNode.map(TypeUtils::convertType).orElse(new Type("void", false));
            returnTypes.put(methodName, returnType);
        }

        return returnTypes;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.hasAttribute("name") ? method.get("name") : "main";
            List<Symbol> paramList = new ArrayList<>();

            for (JmmNode param : method.getChildren(Kind.PARAM_DECL.getNodeName())) {
                Optional<JmmNode> typeNode = param.getChildren().stream()
                        .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                        .findFirst();

                if (typeNode.isEmpty()) {
                    reports.add(Report.newError(Stage.SEMANTIC, param.getLine(), param.getColumn(),
                            "Parameter declaration missing type", null));
                    continue;
                }

                Type paramType = convertType(typeNode.get());
                paramList.add(new Symbol(paramType, param.get("name")));
            }

            if (methodName.equals("main") && paramList.isEmpty()) {
                paramList.add(new Symbol(new Type("String", true), "args"));
            }

            map.put(methodName, paramList);
        }

        return map;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.hasAttribute("name") ? method.get("name") : "main";
            List<Symbol> locals = new ArrayList<>();

            for (JmmNode localVar : method.getChildren(Kind.VAR_DECL.getNodeName())) {
                if (!localVar.hasAttribute("name")) continue;

                Optional<JmmNode> typeNode = localVar.getChildren().stream()
                        .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                        .findFirst();

                if (typeNode.isEmpty()) {
                    reports.add(Report.newError(Stage.SEMANTIC, localVar.getLine(), localVar.getColumn(),
                            "Local variable declaration missing type", null));
                    continue;
                }

                Type varType = convertType(typeNode.get());
                locals.add(new Symbol(varType, localVar.get("name")));
            }

            map.put(methodName, locals);
        }

        return map;
    }

}
