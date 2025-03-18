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

        for (JmmNode varDecl : classDecl.getChildren(Kind.VAR_DECL.getNodeName())) {
            if (!varDecl.hasAttribute("name")) {
                continue;
            }

            Type field_type = convertType(varDecl.getChildren(Kind.TYPE.getNodeName()).get(0));
            String field_name = varDecl.get("name");
            fields.add(new Symbol(field_type, field_name));
        }

        return fields;
    }


    private List<String> buildMethods(JmmNode classDecl) {
        List<String> methods = new ArrayList<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (method.hasAttribute("name")) {
                var method_name = method.get("name");
                methods.add(method_name);;
            } else {
                methods.add("main");
            }
        }
        return methods;
    }


    private Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> return_types = new HashMap<>();

        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (!method.hasAttribute("name")) {
                continue;
            }

            String method_name = method.get("name");
            Type returnType;

            if (!method.getChildren(Kind.TYPE.getNodeName()).isEmpty()) {
                returnType = convertType(method.getChildren(Kind.TYPE.getNodeName()).get(0));
            } else {
                returnType = new Type("void", false);
            }

            return_types.put(method_name, returnType);

        }

        return return_types;
    }


    private Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();
        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            if (!method.hasAttribute("name")) {
                continue;
            }
            var method_name = method.get("name");
            List<Symbol> param_list = new ArrayList<>();

            for (var param : method.getChildren(Kind.PARAM_DECL.getNodeName())) {
                Type param_type = convertType(param.getChildren(Kind.TYPE.getNodeName()).get(0));
                String param_name = param.get("name");
                param_list.add(new Symbol(param_type, param_name));
            }

            map.put(method_name, param_list);
        }
        return map;
    }


    private Map<String, List<Symbol>> buildLocals(JmmNode classDecl, List<Symbol> fields) {
        Map<String, List<Symbol>> map = new HashMap<>();

        for (var method : classDecl.getChildren(Kind.METHOD_DECL.getNodeName())) {
            var method_name = method.get("name");
            List<Symbol> locals = new ArrayList<>();

            for (var local_var : method.getChildren(Kind.VAR_DECL.getNodeName())) {
                if (local_var.hasAttribute("name")) {
                    Type var_type = convertType(local_var.getChildren("Type").get(0));
                    String var_name = local_var.get("name");
                    locals.add(new Symbol(var_type, var_name));
                }
            }
            locals.addAll(fields);

            map.put(method_name, locals);
        }
        return map;
    }



}
