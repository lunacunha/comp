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

        addVisit(Kind.BINARY_EXPR.getNodeName(), this::visitBinaryExpr);

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

    private Boolean visitAssignStatement(JmmNode node, Boolean data) {
        String varName = node.get("name");
        JmmNode valueNode = node.getChildren().get(0);


        boolean rightSideChanged = visit(valueNode, false);

        constantValues.putIfAbsent(currentMethod, new HashMap<>());
        Map<String, Object> methodConstants = constantValues.get(currentMethod);


        if (Kind.INTEGER_LITERAL.check(valueNode)) {
            int newValue = Integer.parseInt(valueNode.get("value"));
            methodConstants.put(varName, newValue);
        } else if (Kind.BOOLEAN_LITERAL.check(valueNode)) {
            boolean newValue = Boolean.parseBoolean(valueNode.get("value"));
            methodConstants.put(varName, newValue);
        } else {
            methodConstants.remove(varName);
        }

        return rightSideChanged;
    }


    private Boolean visitVarRefExpr(JmmNode node, Boolean data) {
        String varName = node.get("name");

        if (constantValues.containsKey(currentMethod) &&
                constantValues.get(currentMethod).containsKey(varName)) {

            Object value = constantValues.get(currentMethod).get(varName);

            if (isConstant(node)) {
                Object currentValue = getNodeValue(node);
                if (Objects.equals(currentValue, value)) {
                    return false;
                }
            }

            JmmNode constantNode;
            if (value instanceof Integer) {
                constantNode = new JmmNodeImpl(List.of(Kind.INTEGER_LITERAL.getNodeName()));
            } else {
                constantNode = new JmmNodeImpl(List.of(Kind.BOOLEAN_LITERAL.getNodeName()));
            }
            constantNode.put("value", value.toString());

            JmmNode parent = node.getParent();
            int index = parent.getChildren().indexOf(node);
            parent.removeChild(index);
            parent.add(constantNode, index);

            optimized = true;
            return true;
        }

        return false;
    }

    private Boolean visitBinaryExpr(JmmNode node, Boolean dummy) {

        JmmNode left  = node.getChildren().get(0);
        JmmNode right = node.getChildren().get(1);
        boolean leftChanged = visit(left, false);
        boolean rightChanged = visit(right, false);

        if (isConstant(left) && isConstant(right)) {
            String op = node.get("op");
            Object leftVal = getNodeValue(left);
            Object rightVal = getNodeValue(right);
            Object result = switch (op) {
                case "+"  -> ((Integer) leftVal) + ((Integer) rightVal);
                case "-"  -> ((Integer) leftVal) - ((Integer) rightVal);
                case "*"  -> ((Integer) leftVal) * ((Integer) rightVal);
                case "/"  -> {
                    int denom = (Integer) rightVal;
                    yield denom != 0 ? ((Integer) leftVal) / denom : null;
                }
                case "<"  -> ((Integer) leftVal) < ((Integer) rightVal);
                case "&&" -> ((Boolean) leftVal) && ((Boolean) rightVal);
                default   -> null;
            };

            if (result != null) {

                JmmNode parent = node.getParent();
                int idx = parent.getChildren().indexOf(node);
                String litKind = (result instanceof Integer)
                        ? Kind.INTEGER_LITERAL.getNodeName()
                        : Kind.BOOLEAN_LITERAL.getNodeName();

                JmmNode literal = new JmmNodeImpl(List.of(litKind));
                literal.put("value", result.toString());

                parent.removeChild(idx);
                parent.add(literal, idx);

                optimized = true;
                return true;
            }
        }

        return leftChanged || rightChanged;
    }


    private Boolean visitLiteral(JmmNode node, Boolean data) {
        return false;
    }

    private Boolean visitReturnStatement(JmmNode node, Boolean data) {
        boolean changed = false;
        if (!node.getChildren().isEmpty()) {
            changed = visit(node.getChildren().get(0), false);
        }
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


    public boolean hasOptimized() {
        return optimized;
    }

    public void resetOptimized() {
        optimized = false;
    }
}