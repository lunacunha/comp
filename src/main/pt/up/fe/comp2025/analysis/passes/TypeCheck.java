package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TypeCheck extends AnalysisVisitor {

    private SymbolTable symbolTable;
    private String currentMethod;
    private TypeUtils typeUtils;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL.getNodeName(), this::visitMethod);
        addVisit(Kind.RETURN_STMT.getNodeName(), this::visitReturn);
        addVisit(Kind.ASSIGN_STMT.getNodeName(), this::visitAssign);
        addVisit(Kind.BINARY_EXPR.getNodeName(), this::visitBinaryExpr);
        addVisit(Kind.IF_STMT.getNodeName(), this::visitIf);
        addVisit(Kind.WHILE_STMT.getNodeName(), this::visitWhile);
        addVisit(Kind.ARRAY_ACCESS_EXPR.getNodeName(), this::visitArrayAccess);
        addVisit(Kind.ARRAY_LITERAL_EXPR.getNodeName(), this::visitArrayLiteral);
        addVisit(Kind.METHOD_CALL_EXPR.getNodeName(), this::visitMethodCall);
        addVisit(Kind.LOCAL_METHOD_CALL_EXPR.getNodeName(), this::visitMethodCall);
    }

    @Override
    public Void visit(JmmNode node, SymbolTable table) {
        this.symbolTable = table;
        this.typeUtils = new TypeUtils(table);
        return super.visit(node, table);
    }

    private Void visitMethod(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitReturn(JmmNode retNode, SymbolTable table) {
        Type expected = table.getReturnType(currentMethod);
        if (retNode.getChildren().isEmpty()) {
            if (!expected.getName().equals("void")) {
                addReport(newError(retNode, "Expected return of type '" + expected + "', got void."));
            }
        } else {
            Type actual = inferType(retNode.getChild(0));
            if (!typeUtils.isCompatible(expected, actual)) {
                addReport(newError(retNode, "Return type mismatch: expected '" + expected + "', got '" + actual + "'"));
            }
        }
        return null;
    }

    private Void visitAssign(JmmNode assign, SymbolTable table) {
        String varName = assign.get("name");
        Type left = typeUtils.getVarType(varName, currentMethod);
        Type right = inferType(assign.getChild(0));

        if (assign.getChild(0).getKind().equals("ArrayLiteralExpr") &&
                !typeUtils.isValidArrayLiteralAssignment(left, right)) {
            addReport(newError(assign, "Array literal can only be assigned to int arrays, found: " + left));
        }

        if (!typeUtils.isCompatible(left, right)) {
            if (right.getName().equals(symbolTable.getClassName())) {
                if (!typeUtils.canAssignThisTo(left)) {
                    addReport(newError(assign, "Cannot assign 'this' to variable '" + varName + "' of type '" + left + "'"));
                }
            } else {
                addReport(newError(assign, "Cannot assign '" + right + "' to variable '" + varName + "' of type '" + left + "'"));
            }
        }

        return null;
    }

    private Void visitBinaryExpr(JmmNode expr, SymbolTable table) {
        Type left = inferType(expr.getChild(0));
        Type right = inferType(expr.getChild(1));
        String op = expr.get("op");

        switch (op) {
            case "+", "-", "*", "/" -> {
                if (!TypeUtils.isInt(left) || !TypeUtils.isInt(right)) {
                    addReport(newError(expr, "Arithmetic operation '" + op + "' requires int operands. Got: " + left + " and " + right));
                }
            }
            case "&&" -> {
                if (!TypeUtils.isBoolean(left) || !TypeUtils.isBoolean(right)) {
                    addReport(newError(expr, "Logical AND requires boolean operands. Got: " + left + " and " + right));
                }
            }
            case "<" -> {
                if (!TypeUtils.isInt(left) || !TypeUtils.isInt(right)) {
                    addReport(newError(expr, "Comparison '<' requires int operands. Got: " + left + " and " + right));
                }
            }
        }

        return null;
    }

    private Void visitIf(JmmNode ifStmt, SymbolTable table) {
        Type condition = inferType(ifStmt.getChild(0));
        if (!TypeUtils.isBoolean(condition)) {
            addReport(newError(ifStmt, "Condition of if must be boolean, got: " + condition));
        }
        return null;
    }

    private Void visitWhile(JmmNode whileStmt, SymbolTable table) {
        Type condition = inferType(whileStmt.getChild(0));
        if (!TypeUtils.isBoolean(condition)) {
            addReport(newError(whileStmt, "Condition of while must be boolean, got: " + condition));
        }
        return null;
    }

    private Void visitArrayAccess(JmmNode access, SymbolTable table) {
        Type arr = inferType(access.getChild(0));
        Type index = inferType(access.getChild(1));
        if (!arr.isArray()) {
            addReport(newError(access, "Trying to access a non-array variable: " + arr));
        }
        if (!TypeUtils.isInt(index)) {
            addReport(newError(access, "Array index must be of type int, got: " + index));
        }
        return null;
    }

    private Void visitArrayLiteral(JmmNode array, SymbolTable table) {
        List<JmmNode> elements = array.getChildren();
        if (elements.isEmpty()) return null;

        Type firstType = inferType(elements.get(0));
        for (JmmNode elem : elements) {
            Type elemType = inferType(elem);
            if (!elemType.equals(firstType)) {
                addReport(newError(array, "All elements in array literal must be of same type. Found: " + firstType + " and " + elemType));
                break;
            }
        }
        return null;
    }

    private Void visitMethodCall(JmmNode call, SymbolTable table) {
        String methodName = call.hasAttribute("methodName") ? call.get("methodName") : call.get("name");

        if (!symbolTable.getMethods().contains(methodName)) {
            return null;
        }

        List<Symbol> params = symbolTable.getParameters(methodName);
        List<JmmNode> args = call.getChildren().stream()
                .filter(child -> !child.getKind().equals("MethodName"))
                .collect(Collectors.toList());

        boolean hasVararg = typeUtils.hasVarargs(methodName);

        for (int i = 0; i < args.size(); i++) {
            if (i < params.size() - (hasVararg ? 1 : 0)) {
                Type expected = params.get(i).getType();
                Type actual = inferType(args.get(i));
                if (!typeUtils.isCompatible(expected, actual)) {
                    addReport(newError(call, "Type mismatch in argument " + (i + 1) + " of method '" +
                            methodName + "': expected " + expected + ", got " + actual));
                }
            } else if (hasVararg) {
                Type actual = inferType(args.get(i));
                Type expected = params.get(params.size() - 1).getType();
                if (!typeUtils.isCompatible(expected, actual)) {
                    addReport(newError(call, "Type mismatch in vararg argument " + (i + 1) + " of method '" +
                            methodName + "': expected " + expected + ", got " + actual));
                }
            } else {
                addReport(newError(call, "Too many arguments for method '" + methodName + "'"));
            }
        }

        return null;
    }

    private Type inferType(JmmNode node) {
        return switch (node.getKind()) {
            case "IntegerLiteral" -> TypeUtils.newIntType();
            case "BooleanLiteral" -> TypeUtils.newBooleanType();
            case "VarRefExpr" -> typeUtils.getVarType(node.get("name"), currentMethod);
            case "ThisExpr" -> {
                if ("main".equals(currentMethod)) {
                    addReport(newError(node, "'this' cannot be used in a static method like 'main'"));
                    yield new Type("unknown", false);
                }
                yield new Type(symbolTable.getClassName(), false);
            }
            case "NewArrayExpr" -> TypeUtils.newIntArrayType();
            case "NewObjectExpr" -> new Type(node.get("className"), false);
            case "ArrayAccessExpr" -> {
                Type arr = inferType(node.getChild(0));
                yield new Type(arr.getName(), false);
            }
            case "BinaryExpr" -> {
                String op = node.get("op");
                if (op.equals("&&") || op.equals("<") || op.equals("==")) {
                    yield TypeUtils.newBooleanType();
                }
                yield TypeUtils.newIntType();
            }
            case "ArrayLiteralExpr" -> {
                if (node.getChildren().isEmpty()) yield TypeUtils.newIntArrayType();
                Type elemType = inferType(node.getChild(0));
                yield new Type(elemType.getName(), true);
            }
            case "MethodCallExpr", "LocalMethodCallExpr" -> {
                String method = node.hasAttribute("methodName") ? node.get("methodName") : node.get("name");
                yield symbolTable.getReturnType(method);
            }
            default -> new Type("unknown", false);
        };
    }
}
