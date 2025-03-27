package pt.up.fe.comp2025.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.symboltable.JmmSymbolTable;

import java.util.Optional;

public class TypeUtils {

    private final JmmSymbolTable table;

    public TypeUtils(SymbolTable table) {
        this.table = (JmmSymbolTable) table;
    }

    public static Type newIntType() {
        return new Type("int", false);
    }

    public static Type newIntArrayType() {
        return new Type("int", true);
    }

    public static Type newBooleanType() {
        return new Type("boolean", false);
    }

    public static Type newBooleanArrayType() {
        return new Type("boolean", true);
    }

    public static Type convertType(JmmNode typeNode) {
        switch (typeNode.getKind()) {
            case "IntType":
                return newIntType();
            case "BooleanType":
                return newBooleanType();
            case "IntArrayType":
                return newIntArrayType();
            case "BooleanArrayType":
                return newBooleanArrayType();
            case "ClassType":
                return new Type(typeNode.get("name"), false);
            case "VarArgInt":
                return new Type("int", true);
            case "VarargParam":
                return new Type("int", true);
            default:
                return new Type("unknown", false);
        }
    }

    public static boolean isInt(Type t) {
        return t.getName().equals("int") && !t.isArray();
    }

    public static boolean isBoolean(Type t) {
        return t.getName().equals("boolean") && !t.isArray();
    }

    public static boolean isValidVararg(Type type) {
        return type.isArray() && type.getName().equals("int");
    }

    public static boolean isPrimitive(String typeName) {
        return typeName.equals("int") || typeName.equals("boolean") || typeName.equals("void");
    }

    public boolean isCompatible(Type expected, Type actual) {
        if (expected.equals(actual)) return true;

        if (!isPrimitive(expected.getName()) && table.getSuper() != null) {
            if (actual.getName().equals(table.getClassName()) && expected.getName().equals(table.getSuper())) {
                return true;
            }
        }

        if (isPrimitive(expected.getName()) && isPrimitive(actual.getName())) {
            return expected.getName().equals(actual.getName());
        }

        return false;
    }


    public boolean canAssignThisTo(Type targetType) {
        String className = table.getClassName();
        String superClass = table.getSuper();

        return targetType.getName().equals(className) ||
                (superClass != null && targetType.getName().equals(superClass));
    }

    public Type getVarType(String name, String methodName) {
        Optional<Symbol> symbol =
                table.getLocalVariables(methodName).stream().filter(s -> s.getName().equals(name)).findFirst()
                        .or(() -> table.getParameters(methodName).stream().filter(s -> s.getName().equals(name)).findFirst())
                        .or(() -> table.getFields().stream().filter(s -> s.getName().equals(name)).findFirst());

        return symbol.map(Symbol::getType).orElse(new Type("unknown", false));
    }

    public boolean hasVarargs(String methodName) {
        var params = table.getParameters(methodName);
        return !params.isEmpty() && params.get(params.size() - 1).getType().isArray();
    }

    public boolean isValidArrayLiteralAssignment(Type target, Type source) {
        return source.isArray() && source.getName().equals("int") && target.isArray() && target.getName().equals("int");
    }

    public Type getExprType(JmmNode expr) {
        return new Type("int", false);
    }
}
