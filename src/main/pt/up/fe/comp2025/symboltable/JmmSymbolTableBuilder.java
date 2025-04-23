package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.TypeUtils.convertType;

public class JmmSymbolTableBuilder {

    private final List<Report> reports = new ArrayList<>();

    public List<Report> getReports() {
        return reports;
    }

    public JmmSymbolTable build(JmmNode root) {
        var imports = buildImports(root);
        var classDecl = root.getChildren(Kind.CLASS_DECL.getNodeName()).get(0);

        String className = classDecl.get("name");
        String superClass = classDecl.getOptional("superClass").orElse(null);

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
            var parts = importDecl.getChildren(Kind.IMPORT_PART.getNodeName())
                    .stream()
                    .flatMap(p -> Arrays.stream(p.get("name").split(",")))
                    .map(String::trim)
                    .collect(Collectors.toList());

            imports.add(String.join(".", parts));
        }

        return imports;
    }


    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();
        Set<String> fieldNames = new HashSet<>();

        for (JmmNode varDecl : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            var typeNode = varDecl.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            if (typeNode.isEmpty() || !varDecl.hasAttribute("name")) continue;

            if (typeNode.get().getKind().startsWith("VarArg")) {
                reports.add(Report.newError(Stage.SEMANTIC, varDecl.getLine(), varDecl.getColumn(),
                        "Vararg type not allowed for field '" + varDecl.get("name") + "'", null));
                continue;
            }

            String fieldName = varDecl.get("name");

            if (!fieldNames.add(fieldName)) {
                reports.add(Report.newError(Stage.SEMANTIC, varDecl.getLine(), varDecl.getColumn(),
                        "Duplicated field '" + fieldName + "'", null));
            }

            Type fieldType = convertType(typeNode.get());
            fields.add(new Symbol(fieldType, fieldName));
        }

        return fields;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        Set<String> methodNames = new HashSet<>();
        List<String> methods = new ArrayList<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");

            if (!methodNames.add(methodName)) {
                reports.add(Report.newError(
                        Stage.SEMANTIC,
                        method.getLine(),
                        method.getColumn(),
                        "Duplicated method '" + methodName + "'",
                        null
                ));
            }

            methods.add(methodName);
        }

        return methods;
    }


    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> returnTypes = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");
            JmmNode returnTypeNode = method.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst()
                    .orElse(null);

            Type returnType;
            if (returnTypeNode == null) {
                returnType = new Type("void", false);
            } else {
                if (returnTypeNode.getKind().equals("IntArrayType")) {
                    returnType = new Type("int", true);
                } else if (returnTypeNode.getKind().equals("BooleanArrayType")) {
                    returnType = new Type("boolean", true);
                } else {
                    returnType = convertType(returnTypeNode);
                }
            }
            returnTypes.put(methodName, returnType);
        }
        return returnTypes;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> paramsMap = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");
            List<Symbol> parameters = new ArrayList<>();
            Set<String> paramNames = new HashSet<>();
            boolean foundVararg = false;

            for (JmmNode param : method.getChildren(Kind.NORMAL_PARAM.getNodeName())) {
                Type paramType;
                boolean isVararg = param.getKind().equals("VarargParam");

                if (isVararg && foundVararg) {
                    reports.add(Report.newError(Stage.SEMANTIC, param.getLine(), param.getColumn(),
                            "Multiple varargs parameters in method '" + methodName + "'", null));
                }

                if (isVararg) {
                    paramType = new Type("int", true);
                    foundVararg = true;
                } else {
                    JmmNode typeNode = param.getChildren().get(0);
                    paramType = convertType(typeNode);
                }

                if (isVararg && !paramType.getName().equals("int")) {
                    reports.add(Report.newError(Stage.SEMANTIC, param.getLine(), param.getColumn(),
                            "Vararg parameter must be of type int in method '" + methodName + "'", null));
                }

                String paramName = param.get("name");

                if (!paramNames.add(paramName)) {
                    reports.add(Report.newError(Stage.SEMANTIC, param.getLine(), param.getColumn(),
                            "Duplicated parameter '" + paramName + "' in method '" + methodName + "'", null));
                }

                parameters.add(new Symbol(paramType, paramName));
            }

            if (foundVararg && parameters.size() > 1) {
                Symbol lastParam = parameters.get(parameters.size() - 1);
                if (!lastParam.getType().isArray()) {
                    reports.add(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter must be last in method '" + methodName + "'", null));
                }
            }

            paramsMap.put(methodName, parameters);
        }

        return paramsMap;
    }

    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> localsMap = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");
            List<Symbol> locals = new ArrayList<>();
            Set<String> localNames = new HashSet<>();

            for (JmmNode localVar : method.getChildren(Kind.VAR_DECL.getNodeName())) {
                var typeNode = localVar.getChildren().stream()
                        .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                        .findFirst();

                if (typeNode.isEmpty() || !localVar.hasAttribute("name")) {
                    continue;
                }

                if (typeNode.get().getKind().startsWith("VarArg")) {
                    reports.add(Report.newError(Stage.SEMANTIC, localVar.getLine(), localVar.getColumn(),
                            "Vararg type not allowed for local variable '" + localVar.get("name") + "'", null));
                    continue;
                }

                Type varType = convertType(typeNode.get());
                String varName = localVar.get("name");

                if (!localNames.add(varName)) {
                    reports.add(Report.newError(Stage.SEMANTIC, localVar.getLine(), localVar.getColumn(),
                            "Duplicated local variable '" + varName + "' in method '" + methodName + "'", null));
                }

                locals.add(new Symbol(varType, varName));
            }

            localsMap.put(methodName, locals);
        }

        return localsMap;
    }
}