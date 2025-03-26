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
        addVisit("AssignStatement", this::visitAssign); // cuidado com "AssingStatement" se estiver errado

        // Binary exprs
        addVisit("AdditionExpr", this::visitBinaryExpr);
        addVisit("SubtractionExpr", this::visitBinaryExpr);
        addVisit("MultiplicationExpr", this::visitBinaryExpr);
        addVisit("DivisionExpr", this::visitBinaryExpr);
        addVisit("AndExpr", this::visitBinaryExpr);
        addVisit("LessThanExpr", this::visitBinaryExpr);
        addVisit("EqualsExpr", this::visitBinaryExpr);

        // Control flow
        addVisit("IfStatement", this::visitIf);
        addVisit("WhileStatement", this::visitWhile); // <-- CORRETO

        // Arrays
        addVisit("ArrayAccess", this::visitArrayAccess);
        addVisit("ArrayInit", this::visitArrayLiteral);

        // Method calls
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("LocalMethodCall", this::visitMethodCall);
    }


    private final Set<String> seenKinds = new HashSet<>();

    @Override
    public Void visit(JmmNode node, SymbolTable table) {
        this.symbolTable = table;
        this.typeUtils = new TypeUtils(table);

        String kind = node.getKind();
        if (!seenKinds.contains(kind)) {
            seenKinds.add(kind);
        }

        return super.visit(node, table);
    }


    /*@Override
    public Void visit(JmmNode node, SymbolTable table) {
        System.out.println(">> [DEBUG] Visiting node: " + node.getKind() + " @ line: " + node.getLine() + ", col: " + node.getColumn());
        System.out.println("   [INFO] Node attributes: " + node.getAttributes());
        System.out.println("   [INFO] Node children: " + node.getChildren().stream().map(JmmNode::getKind).toList());
        this.symbolTable = table;
        this.typeUtils = new TypeUtils(table);
        System.out.println(">> [DEBUG] Visiting node: " + node.getKind());
        return super.visit(node, table);
    }*/

    private Void visitMethod(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        // Verificar regras de varargs
        List<Symbol> parameters = table.getParameters(currentMethod);
        boolean foundVararg = false;

        for (int i = 0; i < parameters.size(); i++) {
            Type paramType = parameters.get(i).getType();

            if (paramType.isArray()) {
                if (foundVararg) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Only one vararg parameter allowed in method '" + currentMethod + "'", null));
                    break;
                }

                if (i != parameters.size() - 1) {
                    addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(),
                            "Vararg parameter must be the last in method '" + currentMethod + "'", null));
                    break;
                }

                foundVararg = true;
            }
        }

        return null;
    }


    private Void visitReturn(JmmNode retNode, SymbolTable table) {
        if (currentMethod == null) {
            return null;
        }

        Type expected = table.getReturnType(currentMethod);
        Type actual = inferType(retNode.getChild(0));

        if (!actual.getName().equals("unknown") && !typeUtils.isCompatible(expected, actual)) {
            addReport(Report.newError(Stage.SEMANTIC, retNode.getLine(), retNode.getColumn(),
                    "Return type mismatch: expected '" + expected + "', got '" + actual + "'", null));
        }
        return null;
    }


    private Void visitAssign(JmmNode assign, SymbolTable table) {
        String varName = assign.get("name");
        Type left = typeUtils.getVarType(varName, currentMethod);
        Type right = inferType(assign.getChild(0));
        System.out.println(">> [DEBUG] Assignment: left = " + left + ", right = " + right);


        if (assign.getChild(0).getKind().equals("ArrayInit")
                && !typeUtils.isValidArrayLiteralAssignment(left, right)) {
            addReport(Report.newError(Stage.SEMANTIC, assign.getLine(), assign.getColumn(),
                    "Array literal can only be assigned to int arrays, found: " + left, null));
        }

        System.out.println(">> [DEBUG] Assign: left = " + left + ", right = " + right);

        if (!right.getName().equals("unknown") && !typeUtils.isCompatible(left, right)) {
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
        Type left = inferType(expr.getChild(0));
        Type right = inferType(expr.getChild(1));
        String op = switch (expr.getKind()) {
            case "AdditionExpr" -> "+";
            case "SubtractionExpr" -> "-";
            case "MultiplicationExpr" -> "*";
            case "DivisionExpr" -> "/";
            case "AndExpr" -> "&&";
            case "LessExpr" -> "<";
            case "EqualsExpr" -> "==";
            default -> "unknown";
        };


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

        Type condition = inferType(ifStmt.getChild(0));

        if (!condition.getName().equals("unknown") &&
                (!TypeUtils.isBoolean(condition) || condition.isArray())) {
            addReport(Report.newError(Stage.SEMANTIC, ifStmt.getLine(), ifStmt.getColumn(),
                    "Condition of if must be a non-array boolean, got: " + condition, null));
        }

        return null;
    }

    private Void visitWhile(JmmNode whileStmt, SymbolTable table) {
        Type condition = inferType(whileStmt.getChild(0));

        System.out.println(">> [DEBUG] While condition type: " + condition);

        if (!condition.getName().equals("unknown") &&
                (!TypeUtils.isBoolean(condition) || condition.isArray())) {

            addReport(Report.newError(Stage.SEMANTIC, whileStmt.getLine(), whileStmt.getColumn(),
                    "Condition of while must be a non-array boolean, got: " + condition, null));
        }

        return null;
    }



    private Void visitArrayAccess(JmmNode access, SymbolTable table) {
        Type arr = inferType(access.getChild(0));
        Type index = inferType(access.getChild(1));

        if (!arr.getName().equals("unknown") && !arr.isArray()) {
            addReport(Report.newError(Stage.SEMANTIC, access.getLine(), access.getColumn(),
                    "Trying to access a non-array variable: " + arr, null));
        }
        if (!index.getName().equals("unknown") && !TypeUtils.isInt(index)) {
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

        Type firstType = inferType(elements.get(0));
        for (JmmNode elem : elements) {
            Type elemType = inferType(elem);
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

        if (!symbolTable.getMethods().contains(methodName)) {
            if (symbolTable.getSuper() == null || symbolTable.getSuper().isEmpty()) {
                addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                        "Method '" + methodName + "' is not declared in class '" + symbolTable.getClassName() + "' or its superclass.", null));
            } else {
                System.out.println(">> [DEBUG] Superclass exists. Assuming method exists.");
            }
            return null;
        }

        List<Symbol> params = symbolTable.getParameters(methodName);
        List<JmmNode> args = call.getChildren().stream()
                .filter(child -> !child.getKind().equals("MethodName"))
                .collect(Collectors.toList());

        boolean hasVararg = typeUtils.hasVarargs(methodName);

        for (int i = 0; i < args.size(); i++) {

            Type actual = inferType(args.get(i));

            if (actual.getName().equals("unknown")) continue;

            if (i < params.size() - (hasVararg ? 1 : 0)) {
                Type expected = params.get(i).getType();
                if (!typeUtils.isCompatible(expected, actual)) {
                    if (expected.isArray() && !actual.isArray() && expected.getName().equals(actual.getName())) {
                        continue;
                    }

                    addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                            "Type mismatch in " + (hasVararg && i >= params.size() - 1 ? "vararg " : "") +
                                    "argument " + (i + 1) + " of method '" + methodName +
                                    "': expected " + expected + ", got " + actual, null));
                }

            } else if (hasVararg) {
                Type expected = params.get(params.size() - 1).getType();
                if (!typeUtils.isCompatible(expected, actual)) {
                    addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                            "Type mismatch in vararg argument " + (i + 1) + " of method '" +
                                    methodName + "': expected " + expected + ", got " + actual, null));
                }
            } else {
                addReport(Report.newError(Stage.SEMANTIC, call.getLine(), call.getColumn(),
                        "Too many arguments for method '" + methodName + "'", null));
            }
        }

        return null;
    }

    private Type inferType(JmmNode node) {

        return switch (node.getKind()) {
            case "IntegerLiteral" -> {
                System.out.println(">> [inferType] IntegerLiteral");
                yield TypeUtils.newIntType();
            }
            case "BooleanLiteral" -> TypeUtils.newBooleanType();
            case "VarRefExpr" -> {

                String varName = node.get("name");

                System.out.println(">> [DEBUG] VarRefExpr: " + varName);
                System.out.println(">> [DEBUG] Type of '" + varName + "': " + typeUtils.getVarType(varName, currentMethod));


                if (varName.equals("true") || varName.equals("false")) {
                    yield TypeUtils.newBooleanType();
                }

                try {
                    yield typeUtils.getVarType(varName, currentMethod);
                } catch (RuntimeException e) {
                    yield new Type("unknown", false);
                }

            }


            case "ThisExpr" -> {
                if ("main".equals(currentMethod)) {
                    addReport(Report.newError(Stage.SEMANTIC, node.getLine(), node.getColumn(),
                            "'this' cannot be used in a static method like 'main'", null));
                    yield new Type("unknown", false);
                }
                yield new Type(symbolTable.getClassName(), false);
            }
            case "NewArrayExpr" -> TypeUtils.newIntArrayType();
            case "NewObjectExpr" -> new Type(node.get("className"), false);
            case "ArrayAccessExpr" -> {
                Type arr = inferType(node.getChild(0));
                yield arr.isArray() ? new Type(arr.getName(), false) : new Type("unknown", false);
            }
            case "BinaryExpr" -> {
                String op = node.get("op");
                yield (op.equals("&&") || op.equals("<") || op.equals("==")) ?
                        TypeUtils.newBooleanType() : TypeUtils.newIntType();
            }
            case "AdditionExpr", "SubtractionExpr", "MultiplicationExpr", "DivisionExpr" -> TypeUtils.newIntType();
            case "AndExpr", "LessExpr", "EqualsExpr" -> TypeUtils.newBooleanType();

            case "ArrayInit" -> {
                if (node.getChildren().isEmpty()) yield TypeUtils.newIntArrayType();
                Type elemType = inferType(node.getChild(0));
                yield new Type(elemType.getName(), true);
            }

            case "MethodCall", "LocalMethodCall" -> {
                String method = node.hasAttribute("methodName") ? node.get("methodName") : node.get("name");
                if (!symbolTable.getMethods().contains(method)) yield new Type("unknown", false);
                yield symbolTable.getReturnType(method);
            }
            default -> new Type("unknown", false);
        };
    }
}
