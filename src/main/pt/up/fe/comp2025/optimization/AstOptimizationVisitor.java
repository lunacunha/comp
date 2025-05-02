package pt.up.fe.comp2025.optimization;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2025.ast.Kind;


import java.util.*;

public class AstOptimizationVisitor extends AJmmVisitor<Boolean, Boolean> {
    private final Map<String, Map<String, Object>> constantValues = new HashMap<>();
    private String currentMethod = "";
    private boolean optimized = false;

    public AstOptimizationVisitor() {
        buildVisitor();
    }

    protected void buildVisitor() {
        addVisit(Kind.PROGRAM.getNodeName(), this::visitProgram);
        addVisit(Kind.CLASS_DECL.getNodeName(), this::visitClassDecl);
        addVisit(Kind.METHOD_DECL.getNodeName(), this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STATEMENT.getNodeName(), this::visitAssignStatement);
        addVisit(Kind.RETURN_STATEMENT.getNodeName(), this::visitReturnStatement);

        addVisit(Kind.INTEGER_LITERAL.getNodeName(), this::visitLiteral);
        addVisit(Kind.BOOLEAN_LITERAL.getNodeName(), this::visitLiteral);
        addVisit(Kind.VAR_REF_EXPR.getNodeName(), this::visitVarRefExpr);

        addVisit(Kind.ADDITION_EXPR.getNodeName(), this::visitBinOp);
        addVisit(Kind.SUBTRACTION_EXPR.getNodeName(), this::visitBinOp);
        addVisit(Kind.MULTIPLICATION_EXPR.getNodeName(), this::visitBinOp);
        addVisit(Kind.DIVISION_EXPR.getNodeName(), this::visitBinOp);
        addVisit(Kind.LESS_THAN_EXPR.getNodeName(), this::visitBinOp);
        addVisit(Kind.AND_EXPR.getNodeName(), this::visitBinOp);

        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitMethodDecl(JmmNode node, Boolean aBoolean) {
        // Set current method context for variable scoping
        currentMethod = node.get("name");
        constantValues.putIfAbsent(currentMethod, new HashMap<>());

        boolean changed = false;
        for (JmmNode child : node.getChildren()) {
            boolean childChanged = visit(child, false);
            changed = changed || childChanged;
        }

        return changed;
    }

    private Boolean visitProgram(JmmNode node, Boolean data) {
        constantValues.clear();
        optimized = false;

        boolean changed = false;
        for (JmmNode child : node.getChildren()) {
            boolean childChanged = visit(child, false);
            changed = changed || childChanged;
        }

        return changed;
    }

    private Boolean visitClassDecl(JmmNode node, Boolean data) {
        boolean changed = false;
        for (JmmNode child : node.getChildren()) {
            boolean childChanged = visit(child, false);
            changed = changed || childChanged;
        }
        return changed;
    }

    private Boolean visitNegationExpr(JmmNode node, Boolean data) {
        JmmNode child = node.getChildren().get(0);
        boolean childChanged = visit(child, false); // Optimize child first

        if (isConstant(child)) {
            Object value = getNodeValue(child);
            Object negated = null;

            if (value instanceof Integer) {
                negated = -((Integer) value);
            } else if (value instanceof Boolean) {
                negated = !((Boolean) value);
            }

            if (negated != null) {
                JmmNode parent = node.getParent();
                int index = parent.getChildren().indexOf(node);
                JmmNode newNode;

                if (negated instanceof Integer) {
                    newNode = new JmmNodeImpl(List.of(Kind.INTEGER_LITERAL.getNodeName()));
                } else {
                    newNode = new JmmNodeImpl(List.of(Kind.BOOLEAN_LITERAL.getNodeName()));
                }

                newNode.put("value", negated.toString());
                parent.removeChild(index);
                parent.add(newNode, index);
                optimized = true;

                System.out.println("Folded negation: " + value + " -> " + negated);
                return true;
            }
        }

        return childChanged;
    }

    private Boolean visitAssignStatement(JmmNode node, Boolean data) {
        String varName = node.get("name");
        JmmNode valueNode = node.getChildren().get(0);

        // Optimize the right-hand side expression
        boolean rightSideChanged = visit(valueNode, false);

        // Make sure we have a constants map for the current method
        constantValues.putIfAbsent(currentMethod, new HashMap<>());
        Map<String, Object> methodConstants = constantValues.get(currentMethod);

        // Check if the right-hand side is a constant
        boolean isConst = false;
        Object newValue = null;

        if (Kind.INTEGER_LITERAL.check(valueNode)) {
            newValue = Integer.parseInt(valueNode.get("value"));
            isConst = true;
        } else if (Kind.BOOLEAN_LITERAL.check(valueNode)) {
            newValue = Boolean.parseBoolean(valueNode.get("value"));
            isConst = true;
        }

        boolean valueChanged = false;

        if (isConst) {
            // Check if the value is different from what we already have
            if (!methodConstants.containsKey(varName) || !Objects.equals(methodConstants.get(varName), newValue)) {
                methodConstants.put(varName, newValue);
                valueChanged = true;
                optimized = true;
                System.out.println("Updated constant: " + varName + " = " + newValue);
            }
        } else {
            // If not a constant and we have it marked as one, remove it
            if (methodConstants.containsKey(varName)) {
                methodConstants.remove(varName);
                valueChanged = true;
                optimized = true;
                System.out.println("Removed constant: " + varName);
            }
        }

        return rightSideChanged || valueChanged;
    }

    // Constant propagation
    private Boolean visitVarRefExpr(JmmNode node, Boolean data) {
        String varName = node.get("name");

        // Check if the variable has a known constant value
        if (constantValues.containsKey(currentMethod) &&
                constantValues.get(currentMethod).containsKey(varName)) {

            Object value = constantValues.get(currentMethod).get(varName);

            // Already a constant with the same value?
            if (isConstant(node)) {
                Object currentValue = getNodeValue(node);
                if (Objects.equals(currentValue, value)) {
                    return false; // Already propagated
                }
            }

            // Create a new constant node to replace the variable reference
            JmmNode constantNode;
            if (value instanceof Integer) {
                constantNode = new JmmNodeImpl(List.of(Kind.INTEGER_LITERAL.getNodeName()));
            } else { // Boolean
                constantNode = new JmmNodeImpl(List.of(Kind.BOOLEAN_LITERAL.getNodeName()));
            }
            constantNode.put("value", value.toString());

            // Replace the variable reference with the constant
            JmmNode parent = node.getParent();
            int index = parent.getChildren().indexOf(node);
            parent.removeChild(index);
            parent.add(constantNode, index);

            System.out.println("Propagated constant: " + varName + " = " + value);
            optimized = true;
            return true;
        }

        return false;
    }

    // Constant folding
    private Boolean visitBinOp(JmmNode node, Boolean data) {
        // Visit operands first for potential optimization
        JmmNode leftOperand = node.getChildren().get(0);
        JmmNode rightOperand = node.getChildren().get(1);

        boolean leftChanged = visit(leftOperand, false);
        boolean rightChanged = visit(rightOperand, false);

        // Check if both operands are constants after optimization
        if (isConstant(leftOperand) && isConstant(rightOperand)) {
            String kind = node.getKind();
            Object result = evaluateConstantExpression(leftOperand, rightOperand, kind);

            if (result != null) {
                // Replace the binary operation with a constant
                JmmNode parent = node.getParent();
                int index = parent.getChildren().indexOf(node);
                JmmNode constantNode;

                if (result instanceof Integer) {
                    constantNode = new JmmNodeImpl(List.of(Kind.INTEGER_LITERAL.getNodeName()));
                } else { // Boolean
                    constantNode = new JmmNodeImpl(List.of(Kind.BOOLEAN_LITERAL.getNodeName()));
                }

                constantNode.put("value", result.toString());
                parent.removeChild(index);
                parent.add(constantNode, index);

                System.out.println("Folded binary op: " + kind + " -> " + result);
                optimized = true;
                return true;
            }
        }

        return leftChanged || rightChanged;
    }

    private Boolean visitLiteral(JmmNode node, Boolean data) {
        // Literals are already optimized
        return false;
    }

    private Boolean visitReturnStatement(JmmNode node, Boolean data) {
        boolean changed = false;
        if (!node.getChildren().isEmpty()) {
            changed = visit(node.getChildren().get(0), false);
        }
        System.out.println(node);
        return changed;
    }

    private Boolean defaultVisit(JmmNode node, Boolean data) {
        boolean changed = false;
        for (JmmNode child : node.getChildren()) {
            boolean childChanged = visit(child, false);
            changed = changed || childChanged;
        }
        return changed;
    }

    private boolean isConstant(JmmNode node) {
        return Kind.INTEGER_LITERAL.check(node) || Kind.BOOLEAN_LITERAL.check(node);
    }

    private Object getNodeValue(JmmNode node) {
        if (Kind.INTEGER_LITERAL.check(node)) {
            return Integer.parseInt(node.get("value"));
        }
        if (Kind.BOOLEAN_LITERAL.check(node)) {
            return Boolean.parseBoolean(node.get("value"));
        }
        return null;
    }

    private Object evaluateConstantExpression(JmmNode left, JmmNode right, String kindName) {
        Object leftVal = getNodeValue(left);
        Object rightVal = getNodeValue(right);

        if (leftVal instanceof Integer && rightVal instanceof Integer) {
            int l = (Integer) leftVal, r = (Integer) rightVal;
            switch (Kind.fromString(kindName)) {
                case ADDITION_EXPR:
                    return l + r;
                case SUBTRACTION_EXPR:
                    return l - r;
                case MULTIPLICATION_EXPR:
                    return l * r;
                case DIVISION_EXPR:
                    if (r == 0) return null;
                    return l / r;
                case LESS_THAN_EXPR:
                    return l < r;
                default:
                    return null;
            }
        }

        if (leftVal instanceof Boolean && rightVal instanceof Boolean) {
            boolean lb = (Boolean) leftVal, rb = (Boolean) rightVal;
            switch (Kind.fromString(kindName)) {
                case AND_EXPR:
                    return lb && rb;
                default:
                    break;
            }
        }

        return null;
    }

    public boolean hasOptimized() {
        return optimized;
    }

    public void resetOptimized() {
        optimized = false;
    }
}