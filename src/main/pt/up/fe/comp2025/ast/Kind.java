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
    IMPORT_DECL, // added
    IMPORT_PART, // added
    CLASS_DECL,
    SUPER_CLASS_DECL, // added
    VAR_DECL,
    FIELD_DECL, // added
    TYPE,
    ARRAY_TYPE, // added
    METHOD_DECL,
    NORMAL_METHOD_DECL, // added
    MAIN_METHOD_DECL, // added
    NORMAL_PARAM, // added
    VARARG_PARAM,
    PARAM_LIST, // added
    STMT,
    EXPR_STMT, // added
    WHILE_STMT, // added
    IF_STMT, // added
    METHOD_CALL_EXPR, // added
    LOCAL_METHOD_CALL_EXPR, // added
    ARGUMENT, // added
    NEW_CLASS_EXPR, // added
    NEW_ARRAY_EXPR, // added
    ARRAY_ACCESS_EXPR, // added
    ARRAY_LITERAL_EXPR, // added
    THIS_EXPR, // added
    ASSIGN_STMT,
    RETURN_STMT,
    EXPR,
    BINARY_EXPR,
    ADDITION_EXPR,
    SUBTRACTION_EXPR,
    MULTIPLICATION_EXPR,
    DIVISION_EXPR,
    AND_EXPR,
    LESS_EXPR,
    EQUALS_EXPR,
    INTEGER_LITERAL,
    BOOLEAN_LITERAL,
    VAR_REF_EXPR;

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
        return node.getKind().equals(this.name); // Corrigido para evitar erro
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
