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
        var locals = buildLocals(classDecl, fields);

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
            if (!varDecl.hasAttribute("name")) {
                continue;
            }

            Optional<JmmNode> typeNode = varDecl.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            if (typeNode.isEmpty()) {
                reports.add(Report.newError(Stage.SEMANTIC, varDecl.getLine(), varDecl.getColumn(),
                        "Field declaration missing type", null));
                continue;
            }

            Type field_type = convertType(typeNode.get());
            String field_name = varDecl.get("name");
            if (varDecl.getKind().equals("VarArgInt") || varDecl.getKind().equals("VarArgBool")) {
                reports.add(Report.newError(Stage.SEMANTIC, varDecl.getLine(), varDecl.getColumn(),
                        "Fields cannot be varargs", null));
                continue;
            }

            fields.add(new Symbol(field_type, field_name));
        }

        return fields;
    }


    private List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (method.hasAttribute("name")) {
                var method_name = method.hasAttribute("name") ? method.get("name") : "main";
                methods.add(method_name);;
            } else {
                methods.add("main");
            }
        }
        return methods;
    }


    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> returnTypes = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.hasAttribute("name") ? method.get("name") : "main";
            Type returnType;

            // Procura o nó filho que representa o tipo de retorno (Kind.TYPE)
            Optional<JmmNode> typeNode = method.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            if (typeNode.isEmpty()) {
                // Assume void para métodos sem tipo explícito (como main)
                returnType = new Type("void", false);
            } else {
                returnType = convertType(typeNode.get());

                if (typeNode.get().getKind().equals("VarArgInt") || typeNode.get().getKind().equals("VarArgBool")) {
                    reports.add(Report.newError(Stage.SEMANTIC, typeNode.get().getLine(), typeNode.get().getColumn(),
                            "Method return type cannot be vararg", null));
                }
            }

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
                String paramName = param.get("name");
                paramList.add(new Symbol(paramType, paramName));
            }

            if (methodName.equals("main") && paramList.isEmpty()) {
                paramList.add(new Symbol(new Type("String", true), "args"));
            }

            // Regras de vararg
            boolean foundVararg = false;
            for (int i = 0; i < paramList.size(); i++) {
                Symbol param = paramList.get(i);
                if (param.getType().isArray() && param.getType().getName().equals("int")) {
                    if (foundVararg) {
                        reports.add(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                                "Only one vararg parameter allowed", null));
                        break;
                    }
                    if (i != paramList.size() - 1) {
                        reports.add(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                                "Vararg parameter must be the last in the parameter list", null));
                        break;
                    }
                    foundVararg = true;
                }
            }

            map.put(methodName, paramList);
        }

        return map;
    }



    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl, List<Symbol> fields) {
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
                String varName = localVar.get("name");

                if (typeNode.get().getKind().equals("VarArgInt") || typeNode.get().getKind().equals("VarArgBool")) {
                    reports.add(Report.newError(Stage.SEMANTIC, localVar.getLine(), localVar.getColumn(),
                            "Local variables cannot be vararg", null));
                    continue;
                }

                locals.add(new Symbol(varType, varName));
            }

            map.put(methodName, locals);
        }

        return map;
    }




}
