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
                //TODO : WE ALREADY SUPPORT MANY AND BUT NOT RECURSIVE ADDITION EXPR
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
            else if (node.getChild(i).getKind().equals("ExprStatement")){
                printExprStatement(node.getChild(i),code);
            }
        }
        if (!returnFound) code.append("ret.V;\n");

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private void printIfStmt(JmmNode node, StringBuilder code) {
        // Generate labels for the "then" and "endif" blocks
        String thenLabel = ollirTypes.nextTemp("then");
        String endifLabel = ollirTypes.nextTemp("endif");

        // Get the condition of the if statement
        JmmNode condition = node.getChild(0); // LessThanExpr or other conditions
        String condName = "";

        // If the condition is a "VarRefExpr", simply handle it
        if (condition.getKind().equals("VarRefExpr")) {
            condName = condition.get("name");
            code.append("if (").append(condName).append(".bool) goto ").append(thenLabel).append(";\n");
        }
        // If the condition is a "LessThanExpr", handle it
        else if (condition.getKind().equals("LessThanExpr")) {
            // Get the temporary variable from printLessExpr
            String tmpVar = printLessExpr(condition, code);  // This generates the comparison and stores it in tmp0

            // Use the temp variable for the if condition
            code.append("if (").append(tmpVar).append(".bool) goto ").append(thenLabel).append(";\n");
        }

        // Handle the else block (if any)
        JmmNode elseBlock = node.getChild(2); // second block is ELSE
        for (var stmt : elseBlock.getChildren()) {
            if (stmt.getKind().equals("ExprStatement")) {
                printExprStatement(stmt, code);
            }
        }
        code.append("goto ").append(endifLabel).append(";\n");

        // Handle the then block (first block)
        code.append(thenLabel).append(":\n");
        JmmNode thenBlock = node.getChild(1); // first block is THEN
        for (var stmt : thenBlock.getChildren()) {
            if (stmt.getKind().equals("ExprStatement")) {
                printExprStatement(stmt, code);
            }
        }

        // End the if statement
        code.append(endifLabel).append(":\n");
    }



    private void printExprStatement(JmmNode node, StringBuilder code) {
        JmmNode expr = node.getChild(0); // MethodCall

        if (expr.getKind().equals("MethodCall")) {
            String caller = expr.getChild(0).get("name"); // ioPlus
            String methodName = expr.get("name"); // printResult

            JmmNode argNode = expr.getChild(1);
            String arg;

            if (argNode.getKind().equals("IntegerLiteral")) {
                arg = argNode.get("value") + ".i32";
            }
            else if (argNode.getKind().equals("FieldAccess")) {
                arg = handleFieldAccess(argNode, code) + ".i32";
            }
            else if (argNode.getKind().equals("ArrayAccess")) {
                arg = handleArrayAccess(argNode, code) + ".i32";
            }
            else if (argNode.getKind().equals("VarRefExpr")) {
                arg = argNode.get("name") + ".i32";
            }
            else {
                arg = handleExpression(argNode, code) + ".i32";
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

    private String handleFieldAccess(JmmNode node, StringBuilder code) {
        JmmNode arrayNode = node.getChild(0); // VarRefExpr (a)
        String arrayName = arrayNode.get("name");

        String tmpVar = ollirTypes.nextTemp();
        code.append(tmpVar + ".i32 :=.i32 arraylength(" + arrayName + ".array.i32).i32;\n");

        return tmpVar + ".i32";
    }

    private String handleArrayAccess(JmmNode node, StringBuilder code) {
        JmmNode arrayNode = node.getChild(0); // VarRefExpr (a)
        JmmNode indexNode = node.getChild(1); // the expression inside []

        String arrayName = arrayNode.get("name");
        String index;

        if (indexNode.getKind().equals("IntegerLiteral")) {
            index = indexNode.get("value") + ".i32";
        }
        else if (indexNode.getKind().equals("VarRefExpr")) {
            index = indexNode.get("name") + ".i32";
        }
        else if (indexNode.getKind().equals("MethodCall")) {
            index = handleMethodCall(indexNode, code) + ".i32";
        }
        else if (indexNode.getKind().equals("ArrayAccess")) {
            index = handleArrayAccess(indexNode, code) + ".i32";
        }
        else if (indexNode.getKind().equals("FieldAccess")) {
            index = handleFieldAccess(indexNode, code) + ".i32";
        }
        else {
            index = handleExpression(indexNode, code) + ".i32";
        }

        String tmpVar = ollirTypes.nextTemp();
        code.append(tmpVar + ".i32 :=.i32 " + arrayName + ".array.i32[" + index + "].i32;\n");

        return tmpVar;
    }

    private String handleMethodCall(JmmNode node, StringBuilder code) {
        String obj = node.getChild(0).get("name"); // d
        String methodName = node.get("name"); // func

        JmmNode argNode = node.getChild(1);
        String arg;
        if (argNode.getKind().equals("IntegerLiteral")) {
            arg = argNode.get("value") + ".i32";
        } else if (argNode.getKind().equals("VarRefExpr")) {
            arg = argNode.get("name") + ".i32";
        } else {
            arg = handleExpression(argNode, code) + ".i32";
        }

        // 🧠 Now find the object's type dynamically
        String objectType = null;
        String method = node.getAncestor(METHOD_DECL).get().get("name");

        // First check locals
        for (var var : table.getLocalVariables(method)) {
            if (var.getName().equals(obj)) {
                objectType = ollirTypes.toOllirType(var.getType());
                break;
            }
        }

        // If not found, check parameters
        if (objectType == null) {
            for (var param : table.getParameters(method)) {
                if (param.getName().equals(obj)) {
                    objectType = ollirTypes.toOllirType(param.getType());
                    break;
                }
            }
        }

        // Final safety check
        if (objectType == null) {
            objectType = "UNKNOWN"; // So it doesn't crash (optional)
        }

        String tmpVar = ollirTypes.nextTemp();
        code.append(tmpVar + ".i32 :=.i32 invokevirtual(" + obj + objectType + ", \"" + methodName + "\", " + arg + ").i32;\n");

        return tmpVar;
    }

    private String handleExpression(JmmNode node, StringBuilder code) {
        if (node.getKind().equals("DivisionExpr")) {
            String left = handleExpression(node.getChild(0), code);
            String right = handleExpression(node.getChild(1), code);
            String tmpVar = ollirTypes.nextTemp();
            code.append(tmpVar + ".i32 :=.i32 " + left + ".i32 /.i32 " + right + ".i32;\n");
            return tmpVar;
        }
        else if (node.getKind().equals("AdditionExpr")) {
            String left = handleExpression(node.getChild(0), code);
            String right = handleExpression(node.getChild(1), code);
            String tmpVar = ollirTypes.nextTemp();
            code.append(tmpVar + ".i32 :=.i32 " + left + ".i32 +.i32 " + right + ".i32;\n");
            return tmpVar;
        }
        else if (node.getKind().equals("SubtractionExpr")) {
            String left = handleExpression(node.getChild(0), code);
            String right = handleExpression(node.getChild(1), code);
            String tmpVar = ollirTypes.nextTemp();
            code.append(tmpVar + ".i32 :=.i32 " + left + ".i32 -.i32 " + right + ".i32;\n");
            return tmpVar;
        }
        else if (node.getKind().equals("IntegerLiteral")) {
            return node.get("value");
        }
        else if (node.getKind().equals("VarRefExpr")) {
            return node.get("name");
        }
        else if (node.getKind().equals("FieldAccess")) {
            return handleFieldAccess(node, code).replace(".i32", "");
        }
        return "UNKNOWN";
    }

    private String printLessExpr(JmmNode node, StringBuilder code) {
        // Create a new temporary variable for the comparison result
        String boolType = ".bool";
        String intType = ".i32";
        String tmpVar = ollirTypes.nextTemp("tmp"); // Generate a new temporary variable

        JmmNode left = node.getChild(0);
        JmmNode right = node.getChild(1);

        String leftValue = (left.getKind().equals("IntegerLiteral")) ? left.get("value") : left.get("name");
        String rightValue = (right.getKind().equals("IntegerLiteral")) ? right.get("value") : right.get("name");

        // Generate the OLLIR code for the comparison
        code.append(tmpVar).append(boolType).append(" :=.bool ")
                .append(leftValue).append(intType).append(" <.bool ")
                .append(rightValue).append(intType).append(";\n");

        // Return the temporary variable containing the result
        return tmpVar;
    }


    private void printAssignStmt(JmmNode node, StringBuilder code) {
        String lhsVar = node.get("name"); // The variable being assigned to
        String rhsVar = node.getChild(0).getKind().equals("VarRefExpr") ? node.getChild(0).get("name") : null;

        boolean isField = exprVisitor.isField(node);

        if (isField) {
            if (rhsVar != null) {
                code.append("putfield(this, ").append(lhsVar).append(".i32, ").append(rhsVar).append(".i32).V;\n");
            } else {
                // Field assignment with an expression on the right-hand side
                String tmpRHS = printExpression(node.getChild(0), code);  // Compute rhs expression into a temp variable
                code.append("putfield(this, ").append(lhsVar).append(".i32, ").append(tmpRHS).append(".i32).V;\n");
            }
        } else {
            // Handle local variable assignment
            switch (node.getChild(0).getKind()) {
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
                    String tmpVar = printLessExpr(node.getChild(0), code);
                    code.append(lhsVar + ".bool :=.bool " + tmpVar + ".bool;\n");
                    break;
                case "AdditionExpr":
                    String sumResult = printAdditionExpr(node.getChild(0), code);
                    code.append(lhsVar + ".i32 :=.i32 " + sumResult + ".i32;\n");
                    break;
                case "ArrayAccess":
                    String accessResult = printArrayAccess(node.getChild(0), code, node.getAncestor(METHOD_DECL).get().get("name"));
                    code.append(lhsVar + ".i32 :=.i32 " + accessResult + ".i32;\n");
                    break;
                case "NewArray":
                    String type = ollirTypes.toOllirType(node.getChild(0).getChild(0));
                    String tmpArrayVar = printNewArray(node.getChild(0), code, type);
                    code.append(lhsVar + ".array" + type + " :=.array" + type + " " + tmpArrayVar + ".array" + type + ";\n");
                    break;
                case "NewObject":
                    String tmpObjectVar = printNewObject(node.getChild(0), code);
                    String className = node.getChild(0).get("name");
                    code.append(lhsVar + "." + className + " :=." + className + " " + tmpObjectVar + "." + className + ";\n");
                    break;
                default:
                    break;
            }
        }
    }

    // Helper method to print expressions and return the result as a temporary variable
    private String printExpression(JmmNode exprNode, StringBuilder code) {
        String tmpVar = ollirTypes.nextTemp("tmp");  // Generate a new temporary variable

        switch (exprNode.getKind()) {
            case "IntegerLiteral":
                code.append(tmpVar + ".i32 :=.i32 " + exprNode.get("value") + ".i32;\n");
                break;
            case "VarRefExpr":
                code.append(tmpVar + ".i32 :=.i32 " + exprNode.get("name") + ".i32;\n");
                break;
            case "AndExpr":
                printAndExpr(exprNode, code);
                break;
            case "LessThanExpr":
                String tmpLessThan = printLessExpr(exprNode, code);
                code.append(tmpVar + ".bool :=.bool " + tmpLessThan + ".bool;\n");
                break;
            case "AdditionExpr":
                String sumResult = printAdditionExpr(exprNode, code);
                code.append(tmpVar + ".i32 :=.i32 " + sumResult + ".i32;\n");
                break;
            case "ArrayAccess":
                String accessResult = printArrayAccess(exprNode, code, exprNode.getAncestor(METHOD_DECL).get().get("name"));
                code.append(tmpVar + ".i32 :=.i32 " + accessResult + ".i32;\n");
                break;
            case "NewArray":
                String type = ollirTypes.toOllirType(exprNode.getChild(0));
                String tmpArrayVar = printNewArray(exprNode, code, type);
                code.append(tmpVar + ".array" + type + " :=.array" + type + " " + tmpArrayVar + ".array" + type + ";\n");
                break;
            case "NewObject":
                String tmpObjectVar = printNewObject(exprNode, code);
                String className = exprNode.get("name");
                code.append(tmpVar + "." + className + " :=." + className + " " + tmpObjectVar + "." + className + ";\n");
                break;
            default:
                break;
        }
        return tmpVar;
    }


    private String printNewObject(JmmNode node, StringBuilder code) {
        String className = node.get("name"); // ComplexArrayAccess
        String tmpVar = ollirTypes.nextTemp(); // generate a fresh temporary variable name

        // Create the new object
        code.append(tmpVar + "." + className + " :=." + className + " new(" + className + ")." + className + ";\n");

        // Call the constructor
        code.append("invokespecial(" + tmpVar + "." + className + ", \"<init>\").V;\n");

        return tmpVar;
    }


    private String printNewArray(JmmNode node, StringBuilder code, String type) {
        String tmpVar = ollirTypes.nextTemp();
        String sizeValue = node.getChild(0).get("value"); // the integer inside new array (e.g., 5)

        code.append(tmpVar + ".array" + type + " :=.array" + type + " new(array, " + sizeValue +".i32).array"+type+";\n");

        return tmpVar; // return the temp variable holding the new array
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
        String thenLabel = ollirTypes.nextTemp("then");
        String endifLabel = ollirTypes.nextTemp("endif");

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
        if (node.getChild(0).getKind().equals("VarRefExpr")) {
            String varName = node.getChild(0).get("name");
            String methodName = node.getAncestor(METHOD_DECL).get().get("name");

            boolean isField = true;
            // check if it is a local variable or parameter (not a field)
            for (var local : table.getLocalVariables(methodName)) {
                if (local.getName().equals(varName)) {
                    isField = false;
                    break;
                }
            }
            for (var param : table.getParameters(methodName)) {
                if (param.getName().equals(varName)) {
                    isField = false;
                    break;
                }
            }

            if (isField) {
                // It's a field -> generate getfield first
                String fieldType = null;
                for (int i = 0; i < table.getFields().size();i++){
                    if (table.getFields().get(i).getName().equals(varName)) fieldType = ollirTypes.toOllirType(table.getFields().get(i).getType());
                }
                String tmpVar = ollirTypes.nextTemp();

                code.append(tmpVar).append(fieldType)
                        .append(" :=").append(fieldType)
                        .append(" getfield(this, ").append(varName).append(fieldType).append(")")
                        .append(fieldType).append(";\n");

                code.append("ret").append(fieldType).append(" ").append(tmpVar).append(fieldType).append(";\n");
            } else {
                // It’s a normal local var, just return it directly
                String varType = ollirTypes.toOllirType(node.getParent().getChild(0));
                code.append("ret").append(varType).append(" ").append(varName).append(varType).append(";\n");
            }
        }



        else if (node.getChild(0).getKind().equals("AdditionExpr")){
                String currentTemp = ollirTypes.nextTemp();
                code.append(currentTemp);
                code.append(ollirTypes.toOllirType(node.getParent().getChild(0)) + " :=").append(ollirTypes.toOllirType(node.getParent().getChild(0))).append(SPACE);
                for (int j = 0;j < node.getChild(0).getChildren().size(); j++) {
                    JmmNode child = node.getChild(0).getChild(j);
                    if (child.getKind().equals("VarRefExpr")){
                        //TODO : E se não for VarRef ?
                        code.append(exprVisitor.visit(child).getCode()).append(SPACE);
                    }
                    if (j == node.getChild(0).getChildren().size() - 1) code.append(";\n");
                    else code.append("+");
                }
                code.append("ret" + ollirTypes.toOllirType(node.getChild(0)) + " " + currentTemp + ollirTypes.toOllirType(node.getChild(0)));
                code.append(";\n");
            }
            else if (node.getChild(0).getKind().equals("IntegerLiteral")){
                code.append("ret.i32 " + node.getChild(0).get("value") + ".i32;\n");
            }
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

            if (i != node.getChildren().size() - 1) {
                expr.append(" +.i32 ");
            }
        }

        code.append(tempVar).append(intType).append(" :=").append(intType).append(" ").append(expr.toString()).append(";\n");

        return tempVar;
    }

    private String printArrayAccess(JmmNode node, StringBuilder code, String method) {
        JmmNode arrayVar = node.getChild(0); // VarRefExpr
        JmmNode indexExpr = node.getChild(1); // IntegerLiteral or VarRefExpr

        String arrayName = arrayVar.get("name");

        // Find the type of the array
        Type arrayType = null;
        for (int i = 0; i < table.getLocalVariables(method).size(); i++) {
            if (table.getLocalVariables(method).get(i).getName().equals(arrayName)) {
                arrayType = table.getLocalVariables(method).get(i).getType();
            }
        }
        for (int i = 0; i < table.getParameters(method).size(); i++) {
            if (table.getParameters(method).get(i).getName().equals(arrayName)) {
                arrayType = table.getParameters(method).get(i).getType();
            }
        }
        if (arrayType == null) {
            return null;
        }

        String elementTypeOllir = ".i32";

        String indexValue;
        if (indexExpr.getKind().equals("IntegerLiteral")) {
            indexValue = exprVisitor.visit(indexExpr).getCode();
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
