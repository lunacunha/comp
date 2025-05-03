package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
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
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(RETURN_STATEMENT, this::visitReturn);
        addVisit(ASSIGN_STATEMENT, this::visitAssignStmt);
        addVisit(EXPR_STATEMENT, this::visitExprStatement);
        addVisit(IF_STATEMENT, this::visitIfStatement);
        addVisit(BLOCK_STATEMENT, this::visitBlockStatement);
        addVisit(WHILE_STATEMENT, this::visitWhileStatement);
        addVisit(ARRAY_ASSIGN_STATEMENT, this::visitArrayAssignStatement);


        setDefaultVisit(this::defaultVisit);
    }

    private String visitArrayAssignStatement(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        String arrayName = node.get("name");

        JmmNode indexNode = node.getChild(0);
        JmmNode valueNode = node.getChild(1);

        OllirExprResult index = exprVisitor.visit(indexNode);
        OllirExprResult value = exprVisitor.visit(valueNode);
        code.append(index.getComputation());
        code.append(value.getComputation());
        String type = ollirTypes.toOllirType(node.getChild(1));

        code.append(arrayName + "[" + index.getCode() + "]" + type + SPACE + ASSIGN + type + SPACE + value.getCode() + END_STMT);

        return code.toString();
    }

    private String visitWhileStatement(JmmNode node, Void unused) {
        StringBuilder ret = new StringBuilder();
        String whileLabel = ollirTypes.nextTemp("while");
        String endifLabel = ollirTypes.nextTemp("endif");

        OllirExprResult cond = exprVisitor.visit(node.getChild(0));
        String block = visit(node.getChild(1));

        ret.append(whileLabel + ":\n" + cond.getComputation() + "if (!.bool " + cond.getCode() + ") goto " + endifLabel + END_STMT);
        ret.append(block + "goto " + whileLabel + END_STMT+ endifLabel + ":\n");

        return ret.toString();
    }

    private String visitBlockStatement(JmmNode jmmNode, Void unused) {
        StringBuilder ret = new StringBuilder();

        for (JmmNode child : jmmNode.getChildren()) {
            ret.append(visit(child));
        }

        return ret.toString();
    }

    private String visitIfStatement(JmmNode node, Void unused) {
        StringBuilder ret = new StringBuilder();

        String thenLabel = ollirTypes.nextTemp("then");
        String ifLabel = ollirTypes.nextTemp("endif");
        OllirExprResult condition = exprVisitor.visit(node.getChild(0));

        ret.append(condition.getComputation());
        ret.append("if (").append(condition.getCode()).append(") goto ").append(thenLabel).append(";\n");

        ret.append(visit(node.getChild(2)));

        ret.append("goto ").append(ifLabel).append(";\n");

        ret.append(thenLabel).append(":\n");
        ret.append(visit(node.getChild(1)));

        ret.append(ifLabel).append(":\n");

        return ret.toString();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        boolean isField = exprVisitor.isField(node);
        String assigned = node.get("name");
        String type = ollirTypes.toOllirType(types.getExprType(node.getChild(0)));

        if (isField) {
            code.append("putfield(this, " + assigned + type + ", " + rhs.getCode() + ").V" + END_STMT);
        }   else {
            code.append(assigned + type);
            code.append(SPACE);

            code.append(ASSIGN);
            code.append(type);
            code.append(SPACE);

            code.append(rhs.getCode());

            code.append(END_STMT);
        }

        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {
        JmmNode retType = node.getAncestor(METHOD_DECL).get().getChild(0);


        StringBuilder code = new StringBuilder();


        var expr = node.getNumChildren() > 0 ? exprVisitor.visit(node.getChild(0)) : OllirExprResult.EMPTY;


        code.append(expr.getComputation());
        code.append("ret");
        code.append(ollirTypes.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitExprStatement(JmmNode node, Void unused) {
        return exprVisitor.visit(node.getChild(0)).getComputation();
    }

    private String visitMethodDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder(".method ");
        boolean isPublic = node.hasAttribute("pub") && node.get("pub").equals("public");
        boolean isStatic = node.hasAttribute("stat") && node.get("stat").equals("static");

        if (isPublic) {
            code.append("public ");
        }
        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        List<String> paramsList = new ArrayList<>();
        for (int i = 0; i < node.getNumChildren(); i++) {
            if (node.getChild(i).getKind().equals("NormalParam")) {
                paramsList.add(node.getChild(i).get("name") + ollirTypes.toOllirType(node.getChild(i).getChild(0)));
            }
        }
        String paramsCode = String.join(", ", paramsList);
        code.append("(" + paramsCode + ")");

        // type
        code.append(ollirTypes.toOllirType(node.getChild(0)));
        code.append(L_BRACKET);


        // rest of its children stmts
        var stmtsCode = node.getChildren().stream().filter(child -> !(child.getKind().equals("NormalParam") || child.getKind().equals("VarDecl") || child.getKind().equals("VarArgParam") || child.equals(node.getChild(0))))
                        .map(this::visit).collect(Collectors.joining("\n   ", "   ", ""));

        code.append(stmtsCode);
        if (node.hasAttribute("stat") && node.hasAttribute("pub") && node.get("name").equals("main") && node.getChild(0).getKind().equals("VoidType")){
            code.append("ret.V ;\n");
        }
        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitClass(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        if (node.hasAttribute("superClass"))
            code.append(" extends ").append(node.get("superClass")).append(" ");

        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

        // --- Add this block: process VarDecls ---
        for (var field : node.getChildren(VAR_DECL)) {
            String fieldName = field.get("name");
            String fieldType = ollirTypes.toOllirType(field.getChild(0)); // The type is child 0
            code.append(".field public ").append(fieldName).append(fieldType).append(";\n");
        }
        code.append(NL);
        // --- End of VarDecls ---

        code.append(buildConstructor());
        code.append(NL);

        for (var child : node.getChildren(METHOD_DECL)) {
            var result = visit(child);
            code.append(result);
        }

        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return """
                .construct %s().V {
                    invokespecial(this, "<init>").V;
                }
                """.formatted(table.getClassName());
    }

    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (JmmNode child : node.getChildren(IMPORT_DECL)) {
            code.append("import " + child.getChild(0).get("ID") + ";\n");
        }
        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    private String defaultVisit(JmmNode node, Void unused) {

        return "";
    }
}
