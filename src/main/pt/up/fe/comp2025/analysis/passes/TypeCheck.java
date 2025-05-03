package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TypeCheck extends AnalysisVisitor {

    private SymbolTable symbolTable;
    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit("MethodDecl", this::visitMethod);
        addVisit("ReturnStatement", this::visitReturn);
        addVisit("AssignStatement", this::visitAssign);

        // Binary exprs
        addVisit("BinaryExpr", this::visitBinaryExpr);

        // Control flow
        addVisit("IfStatement", this::visitIf);
        addVisit("WhileStatement", this::visitWhile);

        // Arrays
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayInit", this::visitArrayLiteral);

        // Method calls
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("LocalMethodCall", this::visitMethodCall);

        addVisit(Kind.NEGATION_EXPR.getNodeName(), this::visitNegationExpr);

    }

    @Override
    public Void visit(JmmNode node, SymbolTable table) {
        this.symbolTable = table;
        this.typeUtils = new TypeUtils(table);
        return super.visit(node, table);
    }

    private Void visitMethod(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        List<Symbol> parameters = table.getParameters(currentMethod);

        boolean foundVararg = false;
        for (int i = 0; i < parameters.size(); i++) {
            Symbol param = parameters.get(i);
            Type paramType = param.getType();

            if (TypeUtils.isValidVararg(paramType)) {
                if (foundVararg) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Multiple varargs parameters in method '" + currentMethod + "'", null));
                }
                if (i != parameters.size() - 1) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter must be last in method '" + currentMethod + "'", null));
                }
                if (!paramType.getName().equals("int...")) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter in method '" + currentMethod + "' must be of type int...", null));
                }
                foundVararg = true;
            }
        }

        return null;
    }

    private boolean isVararg(Type type) {
        return type.isArray() && type.getName().equals("int...");
    }


    private Void visitReturn(JmmNode retNode, SymbolTable table) {
        if (currentMethod == null) {
            return null;
        }

        Type expected = table.getReturnType(currentMethod);
        Type actual = typeUtils.getExprType(retNode.getChild(0));

        if (!actual.getName().equals("unknown") && !typeUtils.isCompatible(expected, actual)) {
            addReport(Report.newError(Stage.SEMANTIC, retNode.getLine(), retNode.getColumn(),
                    "Return type mismatch: expected '" + expected + "', got '" + actual + "'", null));
        }
        return null;
    }


    private Void visitAssign(JmmNode assign, SymbolTable table) {
        String varName = assign.get("name");
        Type left = typeUtils.getVarType(varName, currentMethod);
        Type right = typeUtils.getExprType(assign.getChild(0));

        if (assign.getChild(0).getKind().equals("ArrayInit")) {
            if (!left.isArray() || !left.getName().equals("int")) {
                addReport(Report.newError(Stage.SEMANTIC, assign.getLine(), assign.getColumn(),
                        "Array initializer can only be assigned to int[] variables", null));
            }
        }


        if (!right.getName().equals("unknown") && !typeUtils.isCompatible(left, right)) {

            boolean leftImported = symbolTable.getImports().stream().anyMatch(i -> i.endsWith(left.getName()));
            boolean rightImported = symbolTable.getImports().stream().anyMatch(i -> i.endsWith(right.getName()));

            if (leftImported && rightImported) {
                return null;
            }

            if (right.getName().equals(symbolTable.getClassName())) {
                if (!typeUtils.canAssignThisTo(left)) {
                    addReport(Report.newError(Stage.SEMANTIC, assign.getLine(), assign.getColumn(),
                            "Cannot assign 'this' to variable '" + varName + "' of type '" + left + "'", null));
                }
            } else {
                addReport(Report.newError(Stage.SEMANTIC, assign.getLine(), assign.getColumn(),
                        "Cannot assign '" + right + "' to variable '" + varName + "' of type '" + left + "'", null));
            }
        }


        return null;
    }

    private Void visitBinaryExpr(JmmNode expr, SymbolTable table) {
        String op = expr.get("op");
        Type left = typeUtils.getExprType(expr.getChild(0));
        Type right = typeUtils.getExprType(expr.getChild(1));



        if (left.getName().equals("unknown") || right.getName().equals("unknown")) return null;

        switch (op) {
            case "+", "-", "*", "/" -> {
                if (!TypeUtils.isInt(left) || !TypeUtils.isInt(right) || left.isArray() || right.isArray()) {
                    addReport(Report.newError(Stage.SEMANTIC, expr.getLine(), expr.getColumn(),
                            "Arithmetic operation '" + op + "' requires int operands. Got: " + left + " and " + right, null));
                }
            }

            case "&&" -> {
                if (!TypeUtils.isBoolean(left) || !TypeUtils.isBoolean(right) || left.isArray() || right.isArray()) {
                    addReport(Report.newError(Stage.SEMANTIC, expr.getLine(), expr.getColumn(),
                            "Logical AND '&&' requires boolean (non-array) operands. Got: " + left + " and " + right, null));
                }
            }
            case "<" -> {
                if (!TypeUtils.isInt(left) || !TypeUtils.isInt(right) || left.isArray() || right.isArray()) {
                    addReport(Report.newError(Stage.SEMANTIC, expr.getLine(), expr.getColumn(),
                            "Comparison '<' requires non-array int operands. Got: " + left + " and " + right, null));
                }
            }
            case "==" -> {
                if (!typeUtils.isCompatible(left, right)) {
                    addReport(Report.newError(Stage.SEMANTIC, expr.getLine(), expr.getColumn(),
                            "Operator '==' used with incompatible types: " + left + " and " + right, null));
                }
            }
            default -> {
                addReport(Report.newError(Stage.SEMANTIC, expr.getLine(), expr.getColumn(),
                        "Unknown binary operator '" + op + "'", null));
            }
        }

        return null;
    }


    private Void visitIf(JmmNode ifStmt, SymbolTable table) {

        Type condition = typeUtils.getExprType(ifStmt.getChild(0));

        if (!condition.getName().equals("unknown") &&
                (!TypeUtils.isBoolean(condition) || condition.isArray())) {
            addReport(Report.newError(Stage.SEMANTIC, ifStmt.getLine(), ifStmt.getColumn(),
                    "Condition of if must be a non-array boolean, got: " + condition, null));
        }

        return null;
    }

    private Void visitWhile(JmmNode whileStmt, SymbolTable table) {

        Type condition = typeUtils.getExprType(whileStmt.getChild(0));

        if (!condition.getName().equals("unknown") &&
                (!TypeUtils.isBoolean(condition) || condition.isArray())) {

            addReport(Report.newError(Stage.SEMANTIC, whileStmt.getLine(), whileStmt.getColumn(),
                    "Condition of while must be a non-array boolean, got: " + condition, null));
        }

        return null;
    }


    private Void visitArrayAccess(JmmNode access, SymbolTable table) {
        Type arr = typeUtils.getExprType(access.getChild(0));
        Type index = typeUtils.getExprType(access.getChild(1));

        if (!arr.isArray()) {
            addReport(Report.newError(Stage.SEMANTIC, access.getLine(), access.getColumn(),
                    "Trying to access a non-array variable: " + arr, null));
        }
        if (!TypeUtils.isInt(index)) {
            addReport(Report.newError(Stage.SEMANTIC, access.getLine(), access.getColumn(),
                    "Array index must be of type int, got: " + index, null));
        }
        return null;
    }


    private Void visitArrayLiteral(JmmNode array, SymbolTable table) {
        List<JmmNode> elements = array.getChildren();
        if (elements.isEmpty()) {
            return null;
        }

        Type firstType = typeUtils.getExprType(elements.get(0));
        for (JmmNode elem : elements) {
            Type elemType = typeUtils.getExprType(elem);
            if (!elemType.getName().equals("unknown") && !elemType.equals(firstType)) {
                addReport(Report.newError(Stage.SEMANTIC, array.getLine(), array.getColumn(),
                        "All elements in array literal must be of same type. Found: " + firstType + " and " + elemType, null));
                break;
            }
        }
        return null;
    }


    private Void visitMethodCall(JmmNode call, SymbolTable table) {
        String methodName = call.hasAttribute("methodName") ? call.get("methodName") : call.get("name");

        boolean localMethod = symbolTable.getMethods().contains(methodName);
        boolean hasSuper = symbolTable.getSuper() != null && !symbolTable.getSuper().isEmpty();
        boolean hasImports = !symbolTable.getImports().isEmpty();

        if (!localMethod && !hasSuper && !hasImports) {
            addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                    "Method '" + methodName + "' is not declared in class '" +
                            symbolTable.getClassName() + "' or its superclass.", null));
            return null;
        }

        if (!localMethod) {
            return null;
        }

        List<Symbol> params = symbolTable.getParameters(methodName);
        // Collect argument nodes:
        List<JmmNode> args = call.getChildren().stream()
                .filter(child -> !Set.of("MethodName", "ThisExpr").contains(child.getKind()))
                .collect(Collectors.toList());
        if (localMethod){
            args = call.getChildren().stream().skip(1)
                    .filter(child -> !Set.of("MethodName", "ThisExpr").contains(child.getKind()))
                    .collect(Collectors.toList());
        }
        boolean hasVararg = typeUtils.hasVarargs(methodName);
        int fixedParams = hasVararg ? params.size() - 1 : params.size();

        if (!hasVararg && args.size() != params.size()) {
            addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                    "Incorrect number of arguments for method '" + methodName + "'. Expected " + params.size() + ", got " + args.size(), null));
        } else if (hasVararg && args.size() < fixedParams) {
            addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                    "Not enough arguments for method '" + methodName + "'. Expected at least " + fixedParams + ", got " + args.size(), null));
        }

        for (int i = 0; i < Math.min(args.size(), fixedParams); i++) {
            Type expected = params.get(i).getType();
            Type actual = typeUtils.getExprType(args.get(i));
            if (!typeUtils.isCompatible(expected, actual)) {
                addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                        "Type mismatch in argument " + (i + 1) + " of method '" + methodName +
                                "': expected " + expected + ", got " + actual, null));
            }
        }

        return null;
    }

    private Void visitNegationExpr(JmmNode node, SymbolTable table) {
        Type operand = typeUtils.getExprType(node.getChild(0));

        if (!TypeUtils.isBoolean(operand) || operand.isArray()) {
            addReport(Report.newError(Stage.SEMANTIC, node.getLine(), node.getColumn(),
                    "Unary '!' requires boolean operand. Got: " + operand, null));
        }

        return null;
    }

}
