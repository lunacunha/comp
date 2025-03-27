package pt.up.fe.comp2025.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
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
            System.out.println("IMPORT: Processing importDecl node: " + importDecl);
            List<JmmNode> importParts = importDecl.getChildren(Kind.IMPORT_PART.getNodeName());
            if (importParts.isEmpty()) {
                System.out.println("IMPORT: No importPart found for node: " + importDecl);
                continue;
            }
            JmmNode importPart = importParts.get(0);
            System.out.println("IMPORT: Found importPart: " + importPart);

            String namesStr = (String) importPart.get("name");
            if (namesStr == null) {
                System.out.println("IMPORT: No 'name' attribute found in importPart: " + importPart);
                continue;
            }
            System.out.println("IMPORT: Original name attribute: " + namesStr);

            if (namesStr.length() >= 2) {
                namesStr = namesStr.substring(1, namesStr.length() - 1);
            }
            System.out.println("IMPORT: After removing brackets: " + namesStr);

            String completeImport = namesStr.replaceAll(",\\s*", ".");
            System.out.println("IMPORT: Completed import: " + completeImport);

            imports.add(completeImport);
        }

        return imports;
    }



    private List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        for (JmmNode varDecl : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            var typeNode = varDecl.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst();

            if (typeNode.isEmpty() || !varDecl.hasAttribute("name")) continue;

            Type fieldType = convertType(typeNode.get());
            fields.add(new Symbol(fieldType, varDecl.get("name")));
        }

        return fields;
    }

    private List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(Kind.METHOD_DECL.getNodeName()).stream()
                .map(method -> method.getOptional("name").orElse("main"))
                .collect(Collectors.toList());
    }

    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> returnTypes = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");
            Type returnType = method.getChildren().stream()
                    .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                    .findFirst()
                    .map(TypeUtils::convertType)
                    .orElse(new Type("void", false));

            returnTypes.put(methodName, returnType);
        }

        return returnTypes;
    }

    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> paramsMap = new HashMap<>();

        for (JmmNode method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            String methodName = method.getOptional("name").orElse("main");
            List<Symbol> parameters = new ArrayList<>();

            for (JmmNode param : method.getChildren(Kind.NORMAL_PARAM.getNodeName())) {
                Type paramType;

                if (param.getKind().equals("VarargParam")) { // int... → int[]
                    paramType = new Type("int", true);
                } else {
                    JmmNode typeNode = param.getChildren().get(0);
                    paramType = convertType(typeNode);
                    System.out.println("PARAM TYPE IS " + paramType);

                }

                parameters.add(new Symbol(paramType, param.get("name")));
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

            for (JmmNode localVar : method.getChildren(Kind.VAR_DECL.getNodeName())) {
                var typeNode = localVar.getChildren().stream()
                        .filter(child -> child.getKind().endsWith("Type") || child.getKind().startsWith("VarArg"))
                        .findFirst();

                if (typeNode.isEmpty() || !localVar.hasAttribute("name")) continue;

                Type varType = convertType(typeNode.get());
                locals.add(new Symbol(varType, localVar.get("name")));
            }

            localsMap.put(methodName, locals);
        }

        return localsMap;
    }
}