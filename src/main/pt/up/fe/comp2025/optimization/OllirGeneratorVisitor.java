package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";

    private final SymbolTable table;
    private final TypeUtils types;
    private final OptUtils ollirTypes;
    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        this.types = new TypeUtils(table);
        this.ollirTypes = new OptUtils(types);
        this.exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(NORMAL_PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);
        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(".class public ").append(table.getClassName()).append(";\n");

        if (table.getSuper() != null) {
            code.append(".super ").append(table.getSuper()).append(";\n");
        } else {
            code.append(".super Object;\n");
        }

        for (var field : table.getFields()) {
            code.append(".field ").append(field.getName())
                    .append(ollirTypes.toOllirType(field.getType()))
                    .append(";\n");
        }

        code.append("\n").append(buildConstructor()).append("\n");

        for (var child : node.getChildren(METHOD_DECL)) {
            code.append(visit(child));
        }

        return code.toString();
    }

    private String buildConstructor() {
        return String.format("""
            .construct %s().V {
                invokespecial(this, "<init>").V;
            }
        """, table.getClassName());
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");

        // Public / Static
        boolean isPublic = node.getOptional("isPublic").map(Boolean::parseBoolean).orElse(false);
        if (isPublic) code.append("public ");

        String methodName = node.get("name");
        boolean isStatic = methodName.equals("main");
        if (isStatic) code.append("static ");

        code.append(methodName);

        // Parameters
        List<Symbol> params = table.getParameters(methodName);
        String paramString = params.stream()
                .map(p -> p.getName() + ollirTypes.toOllirType(p.getType()))
                .collect(Collectors.joining(", "));
        code.append("(").append(paramString).append(")");

        // Return type
        Type returnType = table.getReturnType(methodName);
        String returnOllir = ollirTypes.toOllirType(returnType);
        code.append(returnOllir).append(" {\n");

        // Body
        String body = node.getChildren(STMT).stream()
                .map(this::visit)
                .collect(Collectors.joining("\n   ", "   ", ""));
        code.append(body);

        // Ensure there's a return
        boolean hasReturn = node.getChildren().stream()
                .anyMatch(child -> child.getKind().equals(RETURN_STMT.getNodeName()));

        if (!hasReturn) {
            code.append("\n   ret").append(returnOllir);

            if (!returnType.getName().equals("void")) {
                code.append(" ").append(getDefaultReturnValue(returnType));
            }

            code.append(";\n");
        }

        code.append("}\n\n");
        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {
        JmmNode typeNode = node.getChildren().stream()
                .filter(child -> child.getKind().endsWith("Type"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No type child in param node"));

        String id = node.get("name");
        return id + ollirTypes.toOllirType(typeNode);
    }

    private String visitReturn(JmmNode node, Void unused) {
        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;

        Type retType = node.getNumChildren() > 0
                ? types.getExprType(node.getChild(0))
                : new Type("void", false);

        StringBuilder code = new StringBuilder();

        code.append(expr.getComputation());

        code.append("ret").append(ollirTypes.toOllirType(retType));

        if (!retType.getName().equals("void")) {
            code.append(" ").append(expr.getCode());
        }

        code.append(";\n");

        return code.toString();
    }


    private String visitAssignStmt(JmmNode node, Void unused) {
        String varName = node.get("name");
        var rhs = exprVisitor.visit(node.getChild(0));
        Type lhsType = types.getVarType(varName, table.getMethods().get(0)); // Método atual (ajustar se possível)
        String typeStr = ollirTypes.toOllirType(lhsType);
        StringBuilder code = new StringBuilder();

        code.append(rhs.getComputation());
        code.append(varName).append(typeStr).append(SPACE)
                .append(ASSIGN).append(typeStr).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return code.toString();
    }

    private String defaultVisit(JmmNode node, Void unused) {
        for (var child : node.getChildren()) visit(child);
        return "";
    }

    private String getDefaultReturnValue(Type type) {
        if (type.isArray() || (!TypeUtils.isPrimitive(type.getName()) && !type.getName().equals("void"))) {
            return "null";
        }

        return switch (type.getName()) {
            case "int", "boolean" -> "0";
            default -> ""; // void já não entra aqui
        };
    }


}
