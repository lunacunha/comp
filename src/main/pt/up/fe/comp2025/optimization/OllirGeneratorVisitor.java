package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
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
        /*
        addVisit(NORMAL_PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        */


        setDefaultVisit(this::defaultVisit);
    }

/*
    private String visitAssignStmt(JmmNode node, Void unused) {

        var rhs = exprVisitor.visit(node.getChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        var left = node.getChild(0);
        Type thisType = types.getExprType(left);
        String typeString = ollirTypes.toOllirType(thisType);
        var varCode = left.get("name") + typeString;


        code.append(varCode);
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        // TODO: Hardcoded for int type, needs to be expanded
        Type retType = TypeUtils.newIntType();


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


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = ollirTypes.toOllirType(node.getChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }
*/
    //TODO: Não é para visitar tudo no visitMethodDecl ?
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
        boolean returnFound = false;
        for (int i = 0; i < node.getNumChildren(); i++){
            if (node.getChild(i).getKind().equals("ReturnStatement")) {
                //TODO : E se não for AdditionExpr ou VarRef ?
                returnFound = true;
                printReturnStmt(node.getChild(i),code);
            }
            else if (node.getChild(i).getKind().equals("AssignStatement")){
                printAssignStmt(node.getChild(i),code);
            }
            else if (node.getChild(i).getKind().equals("IfStatement")){
                printIfStmt(node.getChild(i), code);
            }
            else if (node.getChild(i).getKind().equals("ArrayAssignStatement")){
                printArrayAssignStmt(node.getChild(i),code);
            }
            else {
                System.out.println(node.getChild(i));
            }
        }
        if (!returnFound) code.append("ret.V;\n");

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private void printIfStmt(JmmNode node, StringBuilder code) {
        String thenLabel = ollirTypes.nextLabel("then");
        String endifLabel = ollirTypes.nextLabel("endif");

        JmmNode condition = node.getChild(0); // VarRefExpr
        String condName = condition.get("name");

        code.append("if (").append(condName).append(".bool) goto ").append(thenLabel).append(";\n");

        // ELSE BLOCK
        JmmNode elseBlock = node.getChild(2); // second block is ELSE
        for (var stmt : elseBlock.getChildren()) {
            if (stmt.getKind().equals("ExprStatement")) {
                printExprStatement(stmt, code);
            }
        }
        code.append("goto ").append(endifLabel).append(";\n");

        // THEN BLOCK
        code.append(thenLabel).append(":\n");
        JmmNode thenBlock = node.getChild(1); // first block is THEN
        for (var stmt : thenBlock.getChildren()) {
            if (stmt.getKind().equals("ExprStatement")) {
                printExprStatement(stmt, code);
            }
        }

        // ENDIF
        code.append(endifLabel).append(":\n");
    }

    private void printExprStatement(JmmNode node, StringBuilder code) {
        JmmNode expr = node.getChild(0); // MethodCall

        if (expr.getKind().equals("MethodCall")) {
            String caller = expr.getChild(0).get("name"); // e.g., io
            String methodName = expr.get("name"); // e.g., print

            JmmNode argNode = expr.getChild(1);
            String arg;
            if (argNode.getKind().equals("IntegerLiteral")) {
                arg = argNode.get("value") + ".i32";
            } else {
                // fallback if it's a variable
                arg = argNode.get("name") + ".i32";
            }

            code.append("invokestatic(")
                    .append(caller)
                    .append(", \"")
                    .append(methodName)
                    .append("\", ")
                    .append(arg)
                    .append(").V;\n");
        }
    }

    private void printLessExpr(JmmNode node, StringBuilder code) {
        JmmNode exprNode = node.getChild(0);

        String boolType = ".bool";
        String intType = ".i32";
        String tmpVar = ollirTypes.nextTemp("tmp"); // generate a new temp

        JmmNode left = exprNode.getChild(0);
        JmmNode right = exprNode.getChild(1);

        String leftValue;
        if (left.getKind().equals("IntegerLiteral")) {
            leftValue = left.get("value");
        } else {
            leftValue = left.get("name");
        }

        String rightValue;
        if (right.getKind().equals("IntegerLiteral")) {
            rightValue = right.get("value");
        } else {
            rightValue = right.get("name");
        }

        code.append(tmpVar).append(boolType).append(" :=").append(boolType).append(" ")
                .append(leftValue).append(intType).append(" <.bool ")
                .append(rightValue).append(intType).append(";\n");

        String varName = node.get("name");
        code.append(varName).append(boolType).append(" :=").append(boolType).append(" ")
                .append(tmpVar).append(boolType).append(";\n");
    }

    private void printAssignStmt(JmmNode node, StringBuilder code){
        String lhsVar = node.get("name"); // The variable being assigned to

        switch(node.getChild(0).getKind()){
            case "IntegerLiteral":
                code.append(lhsVar + ".i32 :=.i32 " + node.getChild(0).get("value") + ".i32;\n");
                break;
            case "VarRefExpr":
                code.append(lhsVar + ".i32 :=.i32 " + node.getChild(0).get("name") + ".i32;\n");
                break;
            case "AndExpr":
                printAndExpr(node, code);
                break;
            case "LessThanExpr":
                printLessExpr(node,code);
                break;
            case "AdditionExpr":
                String sumResult = printAdditionExpr(node.getChild(0), code);
                code.append(lhsVar + ".i32 :=.i32 " + sumResult + ".i32;\n");
                break;
            case "ArrayAccess":
                String accessResult = printArrayAccess(node.getChild(0), code, node.getAncestor(METHOD_DECL).get().get("name"));
                code.append(lhsVar + ".i32 :=.i32 " + accessResult + ".i32;\n");
                break;
            default:
                System.out.println("Unhandled assignment for: " + node.getChild(0).getKind());
                break;
        }
    }

    private void printArrayAssignStmt(JmmNode node, StringBuilder code){
        String arrayName = node.get("name");

        JmmNode indexNode = node.getChild(0);
        JmmNode valueNode = node.getChild(1);

        String index = indexNode.getKind().equals("IntegerLiteral")
                ? indexNode.get("value") + ".i32"
                : indexNode.get("name") + ".i32";

        String value = valueNode.getKind().equals("IntegerLiteral")
                ? valueNode.get("value") + ".i32"
                : valueNode.get("name") + ".i32";

        code.append(arrayName).append("[")
                .append(index)
                .append("].i32 :=.i32 ")
                .append(value)
                .append(";\n");
    }


    private String handleAndExpr(JmmNode node, StringBuilder code) {
        String boolType = ".bool";
        String andTemp = ollirTypes.nextTemp("andTmp");
        String thenLabel = ollirTypes.nextLabel("then");
        String endifLabel = ollirTypes.nextLabel("endif");

        // Evaluate left child
        String leftValue;
        JmmNode left = node.getChild(0);
        if (left.getKind().equals("AndExpr")) {
            leftValue = handleAndExpr(left, code);
        } else {
            // Inline conversion for BooleanLiteral: "true"->"1" else "0"
            leftValue = left.getKind().equals("BooleanLiteral")
                    ? (left.get("value").equals("true") ? "1" : "0")
                    : left.get("name");
        }

        code.append("if (").append(leftValue).append(boolType).append(") goto ").append(thenLabel).append(";\n");

        code.append(andTemp).append(boolType).append(" :=").append(boolType).append(" 0.bool;\n");
        code.append("goto ").append(endifLabel).append(";\n");

        code.append(thenLabel).append(":\n");

        // Evaluate right child
        String rightValue;
        JmmNode right = node.getChild(1);
        if (right.getKind().equals("AndExpr")) {
            rightValue = handleAndExpr(right, code);
        } else {
            // Inline conversion for BooleanLiteral: "true"->"1" else "0"
            rightValue = right.getKind().equals("BooleanLiteral")
                    ? (right.get("value").equals("true") ? "1" : "0")
                    : right.get("name");
        }

        code.append(andTemp).append(boolType).append(" :=").append(boolType).append(" ")
                .append(rightValue).append(boolType).append(";\n");

        code.append(endifLabel).append(":\n");

        return andTemp;
    }

    private void printAndExpr(JmmNode node, StringBuilder code) {
        JmmNode exprNode = node.getChild(0); // The actual AndExpr inside the AssignStatement
        String finalTemp = handleAndExpr(exprNode, code);

        String varName = node.get("name"); // The variable being assigned to
        code.append(varName).append(".bool :=.bool ").append(finalTemp).append(".bool;\n");
    }

    private void printReturnStmt(JmmNode node, StringBuilder code){
            //TODO : E se não for AdditionExpr ou VarRef ?
            if (node.getChild(0).getKind().equals("VarRefExpr")){
                code.append("ret");
                code.append(printVarRef(node.getChild(0).get("name"),node.getParent().get("name")));
                code.append(";\n");
            }
            else if (node.getChild(0).getKind().equals("AdditionExpr")){
                String currentTemp = ollirTypes.nextTemp();
                code.append(currentTemp);
                code.append(ollirTypes.toOllirType(node.getParent().getChild(0)) + " :=");
                for (int j = 0;j < node.getChild(0).getChildren().size(); j++) {
                    JmmNode child = node.getChild(0).getChild(j);
                    if (child.getKind().equals("VarRefExpr")){
                        //TODO : E se não for VarRef ?
                        code.append(printVarRef(child.get("name"),node.getParent().get("name")) + " ");
                    }
                    if (j == node.getChild(0).getChildren().size() - 1) code.append(";\n");
                    else code.append("+");
                }
                //TODO : E se não for só o tipo de return ?
                code.append("ret" + ollirTypes.toOllirType(node.getChild(0)) + " " + currentTemp + ollirTypes.toOllirType(node.getChild(0)));
                code.append(";\n");
            }
    }

    private String printVarRef(String var, String method) {
        Type type = getRefType(var, method);
        String str = ollirTypes.toOllirType(type) + " " + var + ollirTypes.toOllirType(type);
        return str;
    }

    private String printAdditionExpr(JmmNode node, StringBuilder code) {
        String intType = ".i32";
        String tempVar = ollirTypes.nextTemp(); // For the final sum

        StringBuilder expr = new StringBuilder();

        for (int i = 0; i < node.getChildren().size(); i++) {
            JmmNode child = node.getChild(i);

            if (child.getKind().equals("VarRefExpr")) {
                expr.append(child.get("name")).append(intType);
            }
            else if (child.getKind().equals("IntegerLiteral")) {
                expr.append(child.get("value")).append(intType);
            }
            else if (child.getKind().equals("ArrayAccess")) {
                String tmp = printArrayAccess(child, code, node.getAncestor(METHOD_DECL).get().get("name"));
                expr.append(tmp).append(intType); // use the tmp returned
            }
            else {
                System.out.println("Warning: Unhandled kind inside AdditionExpr: " + child.getKind());
            }

            if (i != node.getChildren().size() - 1) {
                expr.append(" +.i32 ");
            }
        }

        code.append(tempVar).append(intType).append(" :=").append(intType).append(" ").append(expr.toString()).append(";\n");

        return tempVar; // Important: return the temp
    }


    private String printArrayAccess(JmmNode node, StringBuilder code, String method) {
        JmmNode arrayVar = node.getChild(0); // VarRefExpr
        JmmNode indexExpr = node.getChild(1); // IntegerLiteral or VarRefExpr

        String arrayName = arrayVar.get("name");

        // Find the type of the array
        Type arrayType = null;
        for (int i = 0; i < table.getLocalVariables(method).size() + table.getParameters(method).size(); i++) {
            if (table.getLocalVariables(method).get(i).getName().equals(arrayName)) {
                arrayType = table.getLocalVariables(method).get(i).getType();
                break;
            }
            else if (table.getParameters(method).get(i).getName().equals(arrayName)){
                arrayType = table.getParameters(method).get(i).getType();
                break;
            }
        }
        if (arrayType == null) {
            return null;
        }

        String elementTypeOllir = ".i32"; // Assuming arrays of i32 elements

        String indexValue;
        if (indexExpr.getKind().equals("IntegerLiteral")) {
            indexValue = indexExpr.get("value") + ".i32";
        } else {
            indexValue = indexExpr.get("name") + ".i32";
        }

        // Create a temp
        String tempVar = ollirTypes.nextTemp("tmp");

        // Generate the assignment
        code.append(tempVar).append(".i32 :=.i32 ")
                .append(arrayName).append(".array.i32[").append(indexValue).append("].i32;\n");

        return tempVar; // Return the temporary variable name so it can be used
    }

    private Type getRefType(String var, String method) {
        for (int i = 0; i < table.getLocalVariables(method).size(); i++) {
            if (table.getLocalVariables(method).get(i).getName().equals(var)) {
                return table.getLocalVariables(method).get(i).getType();
            }
        }
        return null;
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(NL);
        code.append(table.getClassName());

        code.append(L_BRACKET);
        code.append(NL);
        code.append(NL);

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

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
