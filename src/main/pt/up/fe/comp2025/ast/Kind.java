package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Enum that mirrors the nodes that are supported by the AST.
 *
 * This enum allows handling nodes in a safer and more flexible way than using strings with node names.
 */
public enum Kind {
    PROGRAM,
    IMPORT_DECL,
    IMPORT_PART,
    CLASS_DECL,
    VAR_DECL,
    METHOD_DECL,
    PARAM_DECL,

    NORMAL_PARAM,
    VARARG_PARAM,
    PARAM_LIST,

    VAR_ARG_INT,
    VAR_ARG_BOOL,
    INT_TYPE,
    BOOLEAN_TYPE,
    CLASS_TYPE,
    CLASS_ARRAY_TYPE,
    INT_ARRAY_TYPE,
    VOID_TYPE,

    WHILE_STATEMENT,
    IF_STATEMENT,
    BLOCK_STATEMENT,
    ASSIGN_STATEMENT,
    ARRAY_ASSIGN_STATEMENT,
    EXPR_STATEMENT,
    RETURN_STATEMENT,


    METHOD_CALL,
    FIELD_ACCESS,
    ARRAY_ACCESS,
    MULTIPLICATION_EXPR,
    ADDITION_EXPR,
    SUBTRACTION_EXPR,
    DIVISION_EXPR,
    AND_EXPR,
    LESS_THAN_EXPR,
    ARRAY_INIT,
    NEW_ARRAY,
    NEW_OBJECT,
    NEGATION_EXPR,
    PARENTHESES_EXPR,
    BOOLEAN_LITERAL,
    INTEGER_LITERAL,
    VAR_REF_EXPR,
    THIS_EXPR;

    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {
        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public static List<String> toNodeName(Kind firstKind, Kind... otherKinds) {
        var nodeNames = new ArrayList<String>();
        nodeNames.add(firstKind.getNodeName());

        for (Kind kind : otherKinds) {
            nodeNames.add(kind.getNodeName());
        }
        return nodeNames;
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(this.name);
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {
        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {
        for (Kind k : kindsToTest) {
            if (k.check(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Performs a check on all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
